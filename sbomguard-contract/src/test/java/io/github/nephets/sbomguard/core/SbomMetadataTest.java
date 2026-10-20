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

import static org.assertj.core.api.Assertions.assertThat;

class SbomMetadataTest {
    @Test
    void ofSetsToolAndTimestamp() {
        SbomMetadata m = SbomMetadata.of("tool", "ts");
        assertThat(m.getTool()).isEqualTo("tool");
        assertThat(m.getTimestamp()).isEqualTo("ts");
        assertThat(m.getComponent()).isNull();
    }

    @Test
    void fullConstructorCarriesComponent() {
        Component c = Component.of("self");
        SbomMetadata m = new SbomMetadata("t", "ts", c);
        assertThat(m.getComponent()).isSameAs(c);
    }

    @Test
    void equalityAndHashCode() {
        SbomMetadata a = SbomMetadata.of("t", "ts");
        SbomMetadata b = SbomMetadata.of("t", "ts");
        SbomMetadata c = SbomMetadata.of("other", "ts");
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
    }
}
