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

import static org.assertj.core.api.Assertions.assertThat;

class SbomTest {
    @Test
    void ofVarargsBuildsComponents() {
        Sbom s = Sbom.of(Component.of("a"), Component.of("b"));
        assertThat(s.getComponents()).hasSize(2);
        assertThat(s.getBomFormat()).isEqualTo("CycloneDX");
    }

    @Test
    void ofListBuildsComponents() {
        Sbom s = Sbom.of(List.of(Component.of("a")));
        assertThat(s.getComponents()).hasSize(1);
    }

    @Test
    void nullComponentsYieldsEmptyList() {
        Sbom s = Sbom.builder().build();
        assertThat(s.getComponents()).isEmpty();
    }

    @Test
    void bomFormatDefaulted() {
        Sbom s = Sbom.builder().build();
        assertThat(s.getBomFormat()).isEqualTo("CycloneDX");
        Sbom s2 = Sbom.builder().bomFormat("SPDX").build();
        assertThat(s2.getBomFormat()).isEqualTo("SPDX");
    }

    @Test
    void metadataAndSpecAreCarried() {
        SbomMetadata m = SbomMetadata.of("tool", "ts");
        Sbom s = Sbom.builder().specVersion("1.5").serialNumber("sn").metadata(m).build();
        assertThat(s.getSpecVersion()).isEqualTo("1.5");
        assertThat(s.getSerialNumber()).isEqualTo("sn");
        assertThat(s.getMetadata()).isSameAs(m);
    }

    @Test
    void metadataNullable() {
        Sbom s = Sbom.builder().build();
        assertThat(s.getMetadata()).isNull();
    }

    @Test
    void getComponentByBomRef() {
        Component c = Component.builder().bomRef("r1").name("a").build();
        Sbom s = Sbom.of(c);
        assertThat(s.getComponentByBomRef("r1")).isSameAs(c);
        assertThat(s.getComponentByBomRef("missing")).isNull();
    }

    @Test
    void componentsAreUnmodifiable() {
        Sbom s = Sbom.of(Component.of("a"));
        assertThat(s.getComponents()).isUnmodifiable();
    }

    @Test
    void equalityAndHashCode() {
        Sbom a = Sbom.of(Component.of("a"));
        Sbom b = Sbom.of(Component.of("a"));
        Sbom c = Sbom.of(Component.of("z"));
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
    }
}
