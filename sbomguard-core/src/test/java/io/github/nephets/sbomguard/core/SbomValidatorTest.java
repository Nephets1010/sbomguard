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

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SbomValidatorTest {
    private SbomValidator full() {
        return SbomValidator.of(
                LicensePolicy.builder().requireLicense(true).forbidden(Set.of("GPL-3.0")).build(),
                new PinnedVersionPolicy(),
                new ComponentIdentityPolicy());
    }

    @Test
    void cleanComponentProducesNoViolations() {
        SbomValidator v = full();
        Component c =
                Component.builder()
                        .name("lib")
                        .version("1.0")
                        .purl("pkg:maven/a/lib@1.0")
                        .license("MIT")
                        .build();
        assertThat(v.validate(c)).isEmpty();
        assertThat(v.isClean(Sbom.of(c))).isTrue();
    }

    @Test
    void multipleViolationsCollectedAcrossPolicies() {
        SbomValidator v = full();
        Component c =
                Component.builder()
                        .name("lib")
                        .license("GPL-3.0")
                        .build(); // no version, no purl, forbidden
        List<Violation> vs = v.validate(c);
        assertThat(vs).anyMatch(x -> x.getRule().equals(PinnedVersionPolicy.RULE_UNPINNED_VERSION));
        assertThat(vs).anyMatch(x -> x.getRule().equals(ComponentIdentityPolicy.RULE_MISSING_PURL));
        assertThat(vs).anyMatch(x -> x.getRule().equals(LicensePolicy.RULE_FORBIDDEN_LICENSE));
    }

    @Test
    void isCleanFalseWhenViolations() {
        SbomValidator v = full();
        Sbom sbom = Sbom.of(Component.builder().name("lib").build());
        assertThat(v.isClean(sbom)).isFalse();
    }

    @Test
    void blockingViolationsFiltersErrorsOnly() {
        SbomValidator v = full();
        Component c =
                Component.builder()
                        .name("lib")
                        .license("GPL-3.0")
                        .build(); // ERROR missing version + forbidden; WARN missing purl
        List<Violation> blocking = v.blockingViolations(Sbom.of(c));
        assertThat(blocking).allMatch(x -> x.getSeverity() == Severity.ERROR);
        assertThat(blocking)
                .noneMatch(x -> x.getRule().equals(ComponentIdentityPolicy.RULE_MISSING_PURL));
    }

    @Test
    void emptySbomIsClean() {
        SbomValidator v = full();
        assertThat(v.isClean(Sbom.of())).isTrue();
        assertThat(v.validate(Sbom.of())).isEmpty();
    }

    @Test
    void addPolicyAugmentsChecks() {
        SbomValidator v = SbomValidator.of(new ComponentIdentityPolicy());
        Component c =
                Component.builder().name("lib").version("1.0").purl("pkg:maven/a/lib@1.0").build();
        assertThat(v.isClean(Sbom.of(c))).isTrue();
        SbomValidator v2 = v.addPolicy(new PinnedVersionPolicy());
        assertThat(v2).isNotSameAs(v);
        assertThat(v2.validate(c)).isEmpty();
    }

    @Test
    void addNullPolicyRejected() {
        SbomValidator v = SbomValidator.of();
        assertThatThrownBy(() -> v.addPolicy(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void validateOverSbomVisitsEveryComponent() {
        SbomValidator v = SbomValidator.of(new ComponentIdentityPolicy());
        Sbom sbom =
                Sbom.of(
                        Component.builder().name("a").purl("p").version("1").build(),
                        Component.builder().name("b").build());
        List<Violation> vs = v.validate(sbom);
        assertThat(vs).hasSize(1);
        assertThat(vs.get(0).getComponentRef()).isEqualTo("b");
    }

    @Test
    void nullSbomRejected() {
        SbomValidator v = full();
        assertThatThrownBy(() -> v.validate((Sbom) null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullComponentRejected() {
        SbomValidator v = full();
        assertThatThrownBy(() -> v.validate((Component) null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void blockingViolationsMatchesErrorSeverityOnly() {
        // 摘要中的"阻断 N 条"与 blockingViolations 走同一判定口径，避免两处口径漂移。
        SbomValidator v = full();
        Sbom sbom =
                Sbom.of(
                        Component.builder().name("a").license("GPL-3.0").build(),
                        Component.builder().name("b").version("1.0").purl("p").build());
        List<Violation> blocking = v.blockingViolations(sbom);
        assertThat(blocking).isNotEmpty();
        assertThat(blocking).allMatch(x -> x.getSeverity() == Severity.ERROR);
        assertThat(v.validate(sbom))
                .filteredOn(x -> x.getSeverity() == Severity.ERROR)
                .hasSameSizeAs(blocking);
    }
}
