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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SbomParsersTest {
    @Test
    void defaultParsersDelegatesToCycloneDx() {
        SbomParsers parsers = new SbomParsers();
        Sbom s = parsers.parse("{ \"components\": [ { \"name\": \"a\", \"version\": \"1\" } ] }");
        assertThat(s.getComponents()).hasSize(1);
        assertThat(s.getComponents().get(0).getName()).isEqualTo("a");
    }

    @Test
    void customCycloneDxParserWired() {
        SbomParsers parsers = new SbomParsers(new CycloneDxParser());
        Sbom s = parsers.parse("{ \"components\": [ { \"name\": \"b\" } ] }");
        assertThat(s.getComponents().get(0).getName()).isEqualTo("b");
    }

    @Test
    void nullCycloneDxParserRejected() {
        assertThatThrownBy(() -> new SbomParsers(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void parseInputStreamDelegatesToCycloneDx() {
        SbomParsers parsers = new SbomParsers();
        Sbom s =
                parsers.parse(
                        new ByteArrayInputStream(
                                "{ \"components\": [ { \"name\": \"c\" } ] }"
                                        .getBytes(StandardCharsets.UTF_8)));
        assertThat(s.getComponents()).hasSize(1);
        assertThat(s.getComponents().get(0).getName()).isEqualTo("c");
    }
}
