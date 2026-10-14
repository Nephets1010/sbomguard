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

class LicensePolicyTest {
    @Test
    void missingLicenseFlaggedWhenRequired() {
        LicensePolicy p = LicensePolicy.builder().requireLicense(true).build();
        Component c = Component.builder().name("lib").build();
        assertThat(p.evaluate(c))
                .anyMatch(
                        v ->
                                v.getRule().equals(LicensePolicy.RULE_MISSING_LICENSE)
                                        && v.getSeverity() == Severity.ERROR);
    }

    @Test
    void missingLicenseSkippedWhenNotRequired() {
        LicensePolicy p = LicensePolicy.builder().requireLicense(false).build();
        Component c = Component.builder().name("lib").build();
        assertThat(p.evaluate(c))
                .noneMatch(v -> v.getRule().equals(LicensePolicy.RULE_MISSING_LICENSE));
    }

    @Test
    void forbiddenLicenseFlagged() {
        LicensePolicy p = LicensePolicy.builder().forbidden(Set.of("GPL-3.0")).build();
        Component c = Component.builder().name("lib").license("GPL-3.0").build();
        assertThat(p.evaluate(c))
                .anyMatch(
                        v ->
                                v.getRule().equals(LicensePolicy.RULE_FORBIDDEN_LICENSE)
                                        && v.getSeverity() == Severity.ERROR);
    }

    @Test
    void allowedLicenseClean() {
        LicensePolicy p = LicensePolicy.builder().allowed(Set.of("MIT")).build();
        Component c = Component.builder().name("lib").license("MIT").build();
        assertThat(p.evaluate(c)).isEmpty();
    }

    @Test
    void notAllowedLicenseWarned() {
        LicensePolicy p = LicensePolicy.builder().allowed(Set.of("MIT")).build();
        Component c = Component.builder().name("lib").license("Apache-2.0").build();
        assertThat(p.evaluate(c))
                .anyMatch(
                        v ->
                                v.getRule().equals(LicensePolicy.RULE_NOT_ALLOWED_LICENSE)
                                        && v.getSeverity() == Severity.WARN);
    }

    @Test
    void multipleLicensesOneForbidden() {
        LicensePolicy p = LicensePolicy.builder().forbidden(Set.of("GPL-3.0")).build();
        Component c = Component.builder().name("lib").license("MIT", "GPL-3.0").build();
        assertThat(p.evaluate(c))
                .filteredOn(v -> v.getRule().equals(LicensePolicy.RULE_FORBIDDEN_LICENSE))
                .hasSize(1);
    }

    @Test
    void forbiddenAndMissingTogether() {
        LicensePolicy p =
                LicensePolicy.builder().requireLicense(true).forbidden(Set.of("GPL-3.0")).build();
        Component c = Component.builder().name("lib").license("GPL-3.0").build();
        assertThat(p.evaluate(c))
                .filteredOn(v -> v.getRule().equals(LicensePolicy.RULE_FORBIDDEN_LICENSE))
                .hasSize(1);
        assertThat(p.evaluate(c))
                .noneMatch(v -> v.getRule().equals(LicensePolicy.RULE_MISSING_LICENSE));
    }

    @Test
    void noForbiddenNoAllowedNoNoise() {
        LicensePolicy p = LicensePolicy.builder().requireLicense(true).build();
        Component c = Component.builder().name("lib").license("MIT").build();
        assertThat(p.evaluate(c)).isEmpty();
    }

    @Test
    void emptyForbiddenSetDoesNotFlag() {
        LicensePolicy p = LicensePolicy.builder().forbidden(Set.of()).build();
        Component c = Component.builder().name("lib").license("GPL-3.0").build();
        assertThat(p.evaluate(c)).isEmpty();
    }

    @Test
    void forbiddenMatchesLowerCaseVariant() {
        // 黑名单配 "GPL-3.0"，但 SBOM 写成 "gpl-3.0" 也必须被拦截（修复前会被绕过）。
        LicensePolicy p = LicensePolicy.builder().forbidden(Set.of("GPL-3.0")).build();
        Component c = Component.builder().name("lib").license("gpl-3.0").build();
        assertThat(p.evaluate(c))
                .anyMatch(
                        v ->
                                v.getRule().equals(LicensePolicy.RULE_FORBIDDEN_LICENSE)
                                        && v.getSeverity() == Severity.ERROR);
    }

    @Test
    void forbiddenMatchesWithSurroundingWhitespace() {
        // 黑名单配 "GPL-3.0"，SBOM 写成 " GPL-3.0 " 也必须被拦截（修复前会被绕过）。
        LicensePolicy p = LicensePolicy.builder().forbidden(Set.of("GPL-3.0")).build();
        Component c = Component.builder().name("lib").license(" GPL-3.0 ").build();
        assertThat(p.evaluate(c))
                .anyMatch(
                        v ->
                                v.getRule().equals(LicensePolicy.RULE_FORBIDDEN_LICENSE)
                                        && v.getSeverity() == Severity.ERROR);
    }

    @Test
    void allowedMatchesLowerCaseVariantClean() {
        // 白名单配 "MIT"，SBOM 写成 "mit" 不应再报 NOT_ALLOWED（修复前会误报）。
        LicensePolicy p = LicensePolicy.builder().allowed(Set.of("MIT")).build();
        Component c = Component.builder().name("lib").license("mit").build();
        assertThat(p.evaluate(c)).isEmpty();
    }

    @Test
    void notAllowedWarnsOnWhitespaceVariant() {
        // 白名单配 "MIT"，SBOM 写 " apache-2.0 " 仍应 WARN（大小写/空白归一但不放宽语义）。
        LicensePolicy p = LicensePolicy.builder().allowed(Set.of("MIT")).build();
        Component c = Component.builder().name("lib").license(" apache-2.0 ").build();
        assertThat(p.evaluate(c))
                .anyMatch(
                        v ->
                                v.getRule().equals(LicensePolicy.RULE_NOT_ALLOWED_LICENSE)
                                        && v.getSeverity() == Severity.WARN);
    }
}
