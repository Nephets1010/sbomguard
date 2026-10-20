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

/** SbomMetadata 是审计溯源的关键字段，equals 必须覆盖 tool/timestamp/component 三个维度。 */
class SbomMetadataContractTest {
    @Test
    void ofProducesNullComponent() {
        SbomMetadata m = SbomMetadata.of("t", "ts");
        assertThat(m.getComponent()).isNull();
    }

    @Test
    void equalityDistinguishesTool() {
        SbomMetadata a = SbomMetadata.of("t1", "ts");
        SbomMetadata b = SbomMetadata.of("t2", "ts");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalityDistinguishesTimestamp() {
        SbomMetadata a = SbomMetadata.of("t", "ts1");
        SbomMetadata b = SbomMetadata.of("t", "ts2");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalityDistinguishesComponent() {
        SbomMetadata a = new SbomMetadata("t", "ts", Component.of("a"));
        SbomMetadata b = new SbomMetadata("t", "ts", Component.of("b"));
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalsNullAndDifferentType() {
        SbomMetadata m = SbomMetadata.of("t", "ts");
        assertThat(m.equals(null)).isFalse();
        assertThat(m.equals("x")).isFalse();
    }

    @Test
    void sameInstanceEquals() {
        SbomMetadata m = SbomMetadata.of("t", "ts");
        assertThat(m).isEqualTo(m);
    }
}
