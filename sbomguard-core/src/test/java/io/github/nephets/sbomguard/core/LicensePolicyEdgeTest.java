/*
 * Copyright 2026 The sbomguard Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.nephets.sbomguard.core;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** 许可证护栏的边界测试：归一化口径（黑名单自身空白、组件侧大小写/空白）、白名单缺省语义、 SPDX 表达式按整串比较的语义边界。这些边界是"绕过黑名单"类漏洞的高发区。 */
class LicensePolicyEdgeTest {
    @Test
    void forbiddenSetWithSurroundingWhitespaceNormalized() {
        // 黑名单自身带空白也要归一，否则 " GPL-3.0 " 这种写法会漏配。
        LicensePolicy p = LicensePolicy.builder().forbidden(Set.of(" GPL-3.0 ")).build();
        Component c = Component.builder().name("lib").license("GPL-3.0").build();
        assertThat(p.evaluate(c))
                .anyMatch(v -> v.getRule().equals(LicensePolicy.RULE_FORBIDDEN_LICENSE));
    }

    @Test
    void allowedEmptyMeansNoWhitelistWarning() {
        // 仅设禁用、不设白名单：不在黑名单的许可证不应被误报 NOT_ALLOWED。
        LicensePolicy p = LicensePolicy.builder().forbidden(Set.of("GPL-3.0")).build();
        Component c = Component.builder().name("lib").license("Apache-2.0").build();
        assertThat(p.evaluate(c)).isEmpty();
    }

    @Test
    void requireLicenseFalseNoMissingViolation() {
        LicensePolicy p = LicensePolicy.builder().requireLicense(false).build();
        Component c = Component.builder().name("lib").build();
        assertThat(p.evaluate(c)).isEmpty();
    }

    @Test
    void licensePresentWhenRequiredIsClean() {
        LicensePolicy p = LicensePolicy.builder().requireLicense(true).build();
        Component c = Component.builder().name("lib").license("MIT").build();
        assertThat(p.evaluate(c)).isEmpty();
    }

    @Test
    void multipleLicensesAllAllowedIsClean() {
        LicensePolicy p = LicensePolicy.builder().allowed(Set.of("MIT", "Apache-2.0")).build();
        Component c = Component.builder().name("lib").license("MIT", "Apache-2.0").build();
        assertThat(p.evaluate(c)).isEmpty();
    }

    @Test
    void mixedAllowedAndForbiddenOnlyFlagsForbidden() {
        LicensePolicy p =
                LicensePolicy.builder().forbidden(Set.of("GPL-3.0")).allowed(Set.of("MIT")).build();
        Component c = Component.builder().name("lib").license("MIT", "GPL-3.0").build();
        assertThat(p.evaluate(c))
                .filteredOn(v -> v.getRule().equals(LicensePolicy.RULE_FORBIDDEN_LICENSE))
                .hasSize(1);
        assertThat(p.evaluate(c))
                .noneMatch(v -> v.getRule().equals(LicensePolicy.RULE_NOT_ALLOWED_LICENSE));
    }

    @Test
    void forbiddenDoesNotMatchWithinExpression() {
        // 许可证以 SPDX 表达式整体存储时，黑名单按整串比较，不会在表达式内部命中。
        // 这是有意的语义边界：表达式需先拆解为具体许可证再判定。
        LicensePolicy p = LicensePolicy.builder().forbidden(Set.of("gpl-3.0")).build();
        Component c = Component.builder().name("lib").license("MIT OR GPL-3.0").build();
        assertThat(p.evaluate(c))
                .noneMatch(v -> v.getRule().equals(LicensePolicy.RULE_FORBIDDEN_LICENSE));
    }

    @Test
    void allowedWhitelistWarnsForUnknownExpression() {
        LicensePolicy p = LicensePolicy.builder().allowed(Set.of("MIT")).build();
        Component c = Component.builder().name("lib").license("MIT OR Apache-2.0").build();
        assertThat(p.evaluate(c))
                .anyMatch(v -> v.getRule().equals(LicensePolicy.RULE_NOT_ALLOWED_LICENSE));
    }

    @Test
    void normalizationMakesForbiddenCaseInsensitiveOnComponent() {
        LicensePolicy p = LicensePolicy.builder().forbidden(Set.of("GPL-3.0")).build();
        Component c = Component.builder().name("lib").license("GPL-3.0").build();
        assertThat(c.hasLicense("gpl-3.0")).isTrue();
        assertThat(p.evaluate(c))
                .anyMatch(v -> v.getRule().equals(LicensePolicy.RULE_FORBIDDEN_LICENSE));
    }

    @Test
    void emptyForbiddenAndEmptyAllowedNoNoise() {
        LicensePolicy p = LicensePolicy.builder().requireLicense(false).build();
        Component c = Component.builder().name("lib").license("MIT").build();
        assertThat(p.evaluate(c)).isEmpty();
    }
}
