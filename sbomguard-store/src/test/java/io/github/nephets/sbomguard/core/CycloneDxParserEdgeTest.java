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

/**
 * CycloneDX 解析的边界测试：缺省字段的默认值、SPDX 等非默认格式保留、嵌套组件不扁平化、 许可证的多种形状（id / name / expression / 空对象 /
 * 空数组）、tools 的 1.4 数组与 1.5 对象形状。 这些分支是"静默丢字段"类审计漏洞的高发区。
 */
class CycloneDxParserEdgeTest {
    private final CycloneDxParser parser = new CycloneDxParser();

    @Test
    void missingBomFormatDefaultsToCycloneDx() {
        Sbom s = parser.parse("{ \"components\": [] }");
        assertThat(s.getBomFormat()).isEqualTo("CycloneDX");
    }

    @Test
    void bomFormatNullInJsonDefaultsToCycloneDx() {
        Sbom s = parser.parse("{ \"bomFormat\": null, \"components\": [] }");
        assertThat(s.getBomFormat()).isEqualTo("CycloneDX");
    }

    @Test
    void nonCycloneDxBomFormatPreserved() {
        Sbom s = parser.parse("{ \"bomFormat\": \"SPDX\", \"components\": [] }");
        assertThat(s.getBomFormat()).isEqualTo("SPDX");
    }

    @Test
    void missingSpecVersionIsNull() {
        Sbom s = parser.parse("{ \"components\": [] }");
        assertThat(s.getSpecVersion()).isNull();
    }

    @Test
    void missingSerialNumberIsNull() {
        Sbom s = parser.parse("{ \"components\": [] }");
        assertThat(s.getSerialNumber()).isNull();
    }

    @Test
    void missingComponentsIsEmpty() {
        Sbom s = parser.parse("{ \"bomFormat\": \"CycloneDX\" }");
        assertThat(s.getComponents()).isEmpty();
    }

    @Test
    void nullComponentsFieldIsEmpty() {
        Sbom s = parser.parse("{ \"components\": null }");
        assertThat(s.getComponents()).isEmpty();
    }

    @Test
    void nestedComponentsAreNotFlattened() {
        // 仅解析顶层 components；组件内部的 components 子数组不应被当作独立物料。
        String json =
                "{ \"components\": [ { \"name\": \"a\", "
                        + "\"components\": [ { \"name\": \"nested\" } ] } ] }";
        Sbom s = parser.parse(json);
        assertThat(s.getComponents()).hasSize(1);
        assertThat(s.getComponents().get(0).getName()).isEqualTo("a");
    }

    @Test
    void componentWithoutNameHasNullName() {
        Sbom s = parser.parse("{ \"components\": [ { \"version\": \"1\" } ] }");
        assertThat(s.getComponents().get(0).getName()).isNull();
    }

    @Test
    void componentWithoutVersionHasNullVersion() {
        Sbom s = parser.parse("{ \"components\": [ { \"name\": \"a\" } ] }");
        assertThat(s.getComponents().get(0).getVersion()).isNull();
    }

    @Test
    void componentScopeCarried() {
        Sbom s =
                parser.parse(
                        "{ \"components\": [ { \"name\": \"a\", \"scope\": \"optional\" } ] }");
        assertThat(s.getComponents().get(0).getScope()).isEqualTo("optional");
    }

    @Test
    void emptyLicenseArrayYieldsNoLicenses() {
        Sbom s = parser.parse("{ \"components\": [ { \"name\": \"a\", \"licenses\": [] } ] }");
        assertThat(s.getComponents().get(0).getLicenses()).isEmpty();
    }

    @Test
    void emptyLicenseObjectYieldsNoLicenses() {
        Sbom s =
                parser.parse(
                        "{ \"components\": [ { \"name\": \"a\", " + "\"licenses\": [ {} ] } ] }");
        assertThat(s.getComponents().get(0).getLicenses()).isEmpty();
    }

    @Test
    void missingMetadataIsNull() {
        Sbom s = parser.parse("{ \"components\": [] }");
        assertThat(s.getMetadata()).isNull();
    }

    @Test
    void metadataWithoutToolsHasNullTool() {
        Sbom s = parser.parse("{ \"metadata\": { \"timestamp\": \"t\" }, \"components\": [] }");
        assertThat(s.getMetadata().getTool()).isNull();
        assertThat(s.getMetadata().getTimestamp()).isEqualTo("t");
    }

    @Test
    void emptyToolsArrayHasNullTool() {
        Sbom s = parser.parse("{ \"metadata\": { \"tools\": [] }, \"components\": [] }");
        assertThat(s.getMetadata().getTool()).isNull();
    }

    @Test
    void toolsObjectWithoutComponentsHasNullTool() {
        Sbom s = parser.parse("{ \"metadata\": { \"tools\": {} }, \"components\": [] }");
        assertThat(s.getMetadata().getTool()).isNull();
    }

    @Test
    void specVersion14ToolsArrayRecognized() {
        String json =
                "{ \"specVersion\": \"1.4\", \"metadata\": { \"tools\": "
                        + "[ { \"name\": \"m\" } ] }, \"components\": [] }";
        Sbom s = parser.parse(json);
        assertThat(s.getMetadata().getTool()).isEqualTo("m");
    }

    @Test
    void multipleToolsComponentsFirstChosen() {
        String json =
                "{ \"metadata\": { \"tools\": { \"components\": "
                        + "[ { \"name\": \"first\" }, { \"name\": \"second\" } ] } }, \"components\": [] }";
        Sbom s = parser.parse(json);
        assertThat(s.getMetadata().getTool()).isEqualTo("first");
    }

    @Test
    void componentWithMixedIdAndExpressionLicenses() {
        String json =
                "{ \"components\": [ { \"name\": \"a\", \"licenses\": [ "
                        + "{ \"license\": { \"id\": \"MIT\" } }, { \"expression\": \"Apache-2.0\" } ] } ] }";
        Sbom s = parser.parse(json);
        assertThat(s.getComponents().get(0).getLicenses()).containsExactly("MIT", "Apache-2.0");
    }

    @Test
    void parseInputStreamEmptyContentThrows() {
        assertThatThrownBy(
                        () ->
                                parser.parse(
                                        new ByteArrayInputStream(
                                                "   ".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(SbomParseException.class);
    }

    @Test
    void parseInputStreamInvalidJsonThrows() {
        assertThatThrownBy(
                        () ->
                                parser.parse(
                                        new ByteArrayInputStream(
                                                "{ bad".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(SbomParseException.class);
    }

    @Test
    void bomRefLookupIgnoresNullRefComponents() {
        Sbom s = parser.parse("{ \"components\": [ { \"name\": \"a\" } ] }");
        assertThat(s.getComponentByBomRef("a")).isNull();
    }
}
