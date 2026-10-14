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
package io.github.nephets.sbomguard.starter;

import io.github.nephets.sbomguard.core.Component;
import io.github.nephets.sbomguard.core.ComponentIdentityPolicy;
import io.github.nephets.sbomguard.core.LicensePolicy;
import io.github.nephets.sbomguard.core.PinnedVersionPolicy;
import io.github.nephets.sbomguard.core.Sbom;
import io.github.nephets.sbomguard.core.SbomParsers;
import io.github.nephets.sbomguard.core.SbomValidator;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SbomGuardTest {
    private SbomGuard guard() {
        SbomValidator validator =
                SbomValidator.of(
                        LicensePolicy.builder()
                                .requireLicense(true)
                                .forbidden(Set.of("GPL-3.0"))
                                .build(),
                        new PinnedVersionPolicy(),
                        new ComponentIdentityPolicy());
        return new SbomGuard(new SbomParsers(), validator);
    }

    @Test
    void cleanSbomHasNoViolations() {
        SbomGuard g = guard();
        String json =
                "{ \"components\": [ { \"name\": \"lib\", \"version\": \"1.0\", "
                        + "\"purl\": \"pkg:maven/a/lib@1.0\", \"licenses\": [ { \"license\": { \"id\": \"MIT\" } } ] } ] }";
        assertThat(g.scan(json)).isEmpty();
        assertThat(g.isClean(json)).isTrue();
    }

    @Test
    void forbiddenLicenseDetected() {
        SbomGuard g = guard();
        String json =
                "{ \"components\": [ { \"name\": \"lib\", \"version\": \"1.0\", "
                        + "\"purl\": \"pkg:maven/a/lib@1.0\", \"licenses\": [ { \"license\": { \"id\": \"GPL-3.0\" } } ] } ] }";
        assertThat(g.scan(json))
                .anyMatch(v -> v.getRule().equals(LicensePolicy.RULE_FORBIDDEN_LICENSE));
    }

    @Test
    void missingLicenseDetected() {
        SbomGuard g = guard();
        String json =
                "{ \"components\": [ { \"name\": \"lib\", \"version\": \"1.0\", "
                        + "\"purl\": \"pkg:maven/a/lib@1.0\" } ] }";
        assertThat(g.scan(json))
                .anyMatch(v -> v.getRule().equals(LicensePolicy.RULE_MISSING_LICENSE));
    }

    @Test
    void missingPurlDetected() {
        SbomGuard g = guard();
        String json =
                "{ \"components\": [ { \"name\": \"lib\", \"version\": \"1.0\", "
                        + "\"licenses\": [ { \"license\": { \"id\": \"MIT\" } } ] } ] }";
        assertThat(g.scan(json)).anyMatch(v -> v.getRule().equals("IDENTITY_MISSING_PURL"));
    }

    @Test
    void scanAcceptsPreParsedSbom() {
        SbomGuard g = guard();
        Sbom sbom =
                Sbom.of(
                        Component.builder()
                                .name("lib")
                                .version("1.0")
                                .purl("p")
                                .license("MIT")
                                .build());
        assertThat(g.scan(sbom)).isEmpty();
    }

    @Test
    void unpinnedVersionDetected() {
        SbomGuard g = guard();
        String json =
                "{ \"components\": [ { \"name\": \"lib\", \"version\": \"latest\", "
                        + "\"purl\": \"p\", \"licenses\": [ { \"license\": { \"id\": \"MIT\" } } ] } ] }";
        assertThat(g.scan(json))
                .anyMatch(v -> v.getRule().equals(PinnedVersionPolicy.RULE_UNPINNED_VERSION));
    }

    @Test
    void nullParsersRejected() {
        assertThatThrownBy(() -> new SbomGuard(null, SbomValidator.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullValidatorRejected() {
        assertThatThrownBy(() -> new SbomGuard(new SbomParsers(), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullSbomRejected() {
        SbomGuard g = guard();
        assertThatThrownBy(() -> g.scan((Sbom) null)).isInstanceOf(NullPointerException.class);
    }
}
