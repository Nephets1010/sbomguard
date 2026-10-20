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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Sbom 聚合根的契约测试：防御性拷贝、默认 bomFormat、bom-ref 查询边界、equals 语义。 */
class SbomContractTest {
    @Test
    void componentsListIsDefensiveCopy() {
        // 传入的列表在构建后被外部修改，不能影响已构建的 Sbom：防御性拷贝必须生效。
        List<Component> input = new ArrayList<>();
        input.add(Component.of("a"));
        Sbom s = Sbom.of(input);
        input.add(Component.of("b"));
        assertThat(s.getComponents()).hasSize(1);
    }

    @Test
    void ofVarargsIgnoresExtraMutations() {
        Component a = Component.of("a");
        Sbom s = Sbom.of(a);
        assertThat(s.getComponents()).containsExactly(a);
    }

    @Test
    void builderComponentsNullYieldsEmpty() {
        Sbom s = Sbom.builder().components(null).build();
        assertThat(s.getComponents()).isEmpty();
    }

    @Test
    void bomFormatNullInJsonDefaultsToCycloneDx() {
        Sbom s = Sbom.builder().bomFormat(null).build();
        assertThat(s.getBomFormat()).isEqualTo("CycloneDX");
    }

    @Test
    void specVersionAndSerialNullable() {
        Sbom s = Sbom.builder().build();
        assertThat(s.getSpecVersion()).isNull();
        assertThat(s.getSerialNumber()).isNull();
    }

    @Test
    void getComponentByBomRefNullRefReturnsNull() {
        Sbom s = Sbom.of(Component.builder().bomRef("r").name("a").build());
        assertThat(s.getComponentByBomRef(null)).isNull();
    }

    @Test
    void getComponentByBomRefMissingReturnsNull() {
        Sbom s = Sbom.of(Component.of("a"));
        assertThat(s.getComponentByBomRef("nope")).isNull();
    }

    @Test
    void equalityDistinguishesMetadata() {
        Sbom a = Sbom.builder().metadata(SbomMetadata.of("t", "ts")).build();
        Sbom b = Sbom.builder().build();
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalityDistinguishesSerialNumber() {
        Sbom a = Sbom.builder().serialNumber("s1").build();
        Sbom b = Sbom.builder().serialNumber("s2").build();
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalityDistinguishesComponentSet() {
        Sbom a = Sbom.of(Component.of("a"));
        Sbom b = Sbom.of(Component.of("b"));
        assertThat(a).isNotEqualTo(b);
    }
}
