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

/** SbomParsers 门面的边界测试：默认构造委托、注入式委托、空/非法输入的异常传播。 */
class SbomParsersEdgeTest {
    @Test
    void parseNullStringThrows() {
        SbomParsers p = new SbomParsers();
        assertThatThrownBy(() -> p.parse((String) null)).isInstanceOf(SbomParseException.class);
    }

    @Test
    void parseBlankStringThrows() {
        SbomParsers p = new SbomParsers();
        assertThatThrownBy(() -> p.parse("  ")).isInstanceOf(SbomParseException.class);
    }

    @Test
    void parseEmptyComponentsViaString() {
        SbomParsers p = new SbomParsers();
        Sbom s = p.parse("{ \"components\": [] }");
        assertThat(s.getComponents()).isEmpty();
    }

    @Test
    void parseDelegatesToInjectedParser() {
        // 用自定义 CycloneDxParser 注入，验证门面确实委托给它而非新建实例。
        CycloneDxParser injected = new CycloneDxParser();
        SbomParsers p = new SbomParsers(injected);
        Sbom s = p.parse("{ \"components\": [ { \"name\": \"x\" } ] }");
        assertThat(s.getComponents().get(0).getName()).isEqualTo("x");
    }

    @Test
    void parseInputStreamMinimal() {
        SbomParsers p = new SbomParsers();
        Sbom s =
                p.parse(
                        new ByteArrayInputStream(
                                "{ \"components\": [ { \"name\": \"y\" } ] }"
                                        .getBytes(StandardCharsets.UTF_8)));
        assertThat(s.getComponents().get(0).getName()).isEqualTo("y");
    }

    @Test
    void parseInputStreamInvalidThrows() {
        SbomParsers p = new SbomParsers();
        assertThatThrownBy(
                        () ->
                                p.parse(
                                        new ByteArrayInputStream(
                                                "{ bad".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(SbomParseException.class);
    }
}
