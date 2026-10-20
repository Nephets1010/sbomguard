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

import java.util.LinkedHashSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 组件值对象的契约测试：不可变性的防御性拷贝、许可证归一化、componentRef 退化规则、equals 边界。 这些值对象在多个模块间传递，任何泄露可变状态或被错误判等的 bug
 * 都会放大到校验结果上。
 */
class ComponentContractTest {
    @Test
    void licensesBuilderCopyIsIndependent() {
        // 构造后外部修改入参集合不能污染已构建的组件：防御性拷贝必须真正生效。
        LinkedHashSet<String> src = new LinkedHashSet<>();
        src.add("MIT");
        Component c = Component.builder().name("x").licenses(src).build();
        src.add("GPL-3.0");
        assertThat(c.getLicenses()).containsExactly("MIT");
    }

    @Test
    void licenseBuilderVarargsMultiple() {
        Component c =
                Component.builder().name("x").license("MIT", "Apache-2.0", "BSD-3-Clause").build();
        assertThat(c.getLicenses()).containsExactly("MIT", "Apache-2.0", "BSD-3-Clause");
    }

    @Test
    void getLicenseExpressionEmptyWhenNoLicenses() {
        Component c = Component.builder().name("x").build();
        assertThat(c.getLicenseExpression()).isEmpty();
    }

    @Test
    void getLicenseExpressionJoinsThree() {
        Component c = Component.builder().name("x").license("A", "B", "C").build();
        assertThat(c.getLicenseExpression()).isEqualTo("A OR B OR C");
    }

    @Test
    void hasLicenseFalseOnEmptyLicenses() {
        Component c = Component.builder().name("x").build();
        assertThat(c.hasLicense("MIT")).isFalse();
    }

    @Test
    void hasLicenseMatchesExactAfterNormalization() {
        // hasLicense 与 LicensePolicy 共用同一套归一化口径：空白 + 小写。
        Component c = Component.builder().name("x").license(" Apache-2.0 ").build();
        assertThat(c.hasLicense("APACHE-2.0")).isTrue();
    }

    @Test
    void componentRefNullNameAndNullBomRef() {
        Component c = Component.builder().build();
        assertThat(c.componentRef()).isNull();
    }

    @Test
    void componentRefPrefersBomRefOverName() {
        Component c = Component.builder().bomRef("b").name("n").build();
        assertThat(c.componentRef()).isEqualTo("b");
    }

    @Test
    void componentRefFallsBackToNameWhenNoBomRef() {
        Component c = Component.builder().name("n").build();
        assertThat(c.componentRef()).isEqualTo("n");
    }

    @Test
    void nullGroupAndNullScopeAreAccepted() {
        Component c = Component.builder().name("x").build();
        assertThat(c.getGroup()).isNull();
        assertThat(c.getScope()).isNull();
    }

    @Test
    void scopeIsCarried() {
        Component c = Component.builder().name("x").scope("optional").build();
        assertThat(c.getScope()).isEqualTo("optional");
    }

    @Test
    void equalityDistinguishesName() {
        Component a = Component.builder().name("a").build();
        Component b = Component.builder().name("b").build();
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalityDistinguishesPurl() {
        Component a = Component.builder().name("n").purl("p1").build();
        Component b = Component.builder().name("n").purl("p2").build();
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalsAgainstNullAndDifferentType() {
        Component a = Component.builder().name("n").build();
        assertThat(a.equals(null)).isFalse();
        assertThat(a.equals("not a component")).isFalse();
    }

    @Test
    void sameInstanceEquals() {
        Component a = Component.builder().name("n").build();
        assertThat(a).isEqualTo(a);
    }

    @Test
    void toStringIncludesPurlWhenPresent() {
        Component c = Component.builder().name("lib").version("1.0").purl("pkg:x").build();
        assertThat(c.toString()).contains("pkg:x").contains("lib");
    }
}
