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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CycloneDxParserTest {
    private final CycloneDxParser parser = new CycloneDxParser();

    private static final String SAMPLE =
            """
      {
        "bomFormat": "CycloneDX",
        "specVersion": "1.5",
        "serialNumber": "urn:uuid:abc-123",
        "metadata": {
          "timestamp": "2026-01-01T00:00:00Z",
          "tools": { "components": [ { "name": "sbomguard" } ] }
        },
        "components": [
          {
            "bom-ref": "pkg:maven/com.a/lib@1.0",
            "group": "com.a",
            "name": "lib",
            "version": "1.0",
            "purl": "pkg:maven/com.a/lib@1.0",
            "licenses": [ { "license": { "id": "MIT" } } ]
          },
          {
            "bom-ref": "pkg:npm/b@2.0",
            "name": "b",
            "version": "2.0",
            "licenses": [ { "license": { "name": "Apache-2.0" } } ]
          }
        ]
      }
      """;

    @Test
    void parsesHeaderFields() {
        Sbom s = parser.parse(SAMPLE);
        assertThat(s.getBomFormat()).isEqualTo("CycloneDX");
        assertThat(s.getSpecVersion()).isEqualTo("1.5");
        assertThat(s.getSerialNumber()).isEqualTo("urn:uuid:abc-123");
    }

    @Test
    void parsesMetadata() {
        Sbom s = parser.parse(SAMPLE);
        assertThat(s.getMetadata()).isNotNull();
        assertThat(s.getMetadata().getTimestamp()).isEqualTo("2026-01-01T00:00:00Z");
        assertThat(s.getMetadata().getTool()).isEqualTo("sbomguard");
    }

    @Test
    void parsesComponents() {
        Sbom s = parser.parse(SAMPLE);
        assertThat(s.getComponents()).hasSize(2);
    }

    @Test
    void parsesComponentFields() {
        Sbom s = parser.parse(SAMPLE);
        Component c = s.getComponents().get(0);
        assertThat(c.getBomRef()).isEqualTo("pkg:maven/com.a/lib@1.0");
        assertThat(c.getGroup()).isEqualTo("com.a");
        assertThat(c.getName()).isEqualTo("lib");
        assertThat(c.getVersion()).isEqualTo("1.0");
        assertThat(c.getPurl()).isEqualTo("pkg:maven/com.a/lib@1.0");
    }

    @Test
    void parsesLicenseById() {
        Sbom s = parser.parse(SAMPLE);
        assertThat(s.getComponents().get(0).getLicenses()).containsExactly("MIT");
    }

    @Test
    void parsesLicenseByName() {
        Sbom s = parser.parse(SAMPLE);
        assertThat(s.getComponents().get(1).getLicenses()).containsExactly("Apache-2.0");
    }

    @Test
    void parsesLicenseByExpression() {
        String json =
                """
        {
          "components": [
            { "name": "lib", "licenses": [ { "expression": "MIT OR Apache-2.0" } ] }
          ]
        }
        """;
        Sbom s = parser.parse(json);
        assertThat(s.getComponents().get(0).getLicenses()).containsExactly("MIT OR Apache-2.0");
    }

    @Test
    void bomRefLookupWorks() {
        Sbom s = parser.parse(SAMPLE);
        assertThat(s.getComponentByBomRef("pkg:npm/b@2.0")).isNotNull();
        assertThat(s.getComponentByBomRef("pkg:npm/b@2.0").getName()).isEqualTo("b");
    }

    @Test
    void missingComponentsYieldsEmpty() {
        String json = "{ \"bomFormat\": \"CycloneDX\" }";
        Sbom s = parser.parse(json);
        assertThat(s.getComponents()).isEmpty();
    }

    @Test
    void invalidJsonThrows() {
        assertThatThrownBy(() -> parser.parse("{ not valid"))
                .isInstanceOf(SbomParseException.class);
    }

    @Test
    void nullContentThrows() {
        assertThatThrownBy(() -> parser.parse((String) null))
                .isInstanceOf(SbomParseException.class);
    }

    @Test
    void blankContentThrows() {
        assertThatThrownBy(() -> parser.parse("   ")).isInstanceOf(SbomParseException.class);
    }

    @Test
    void customObjectMapperAccepted() {
        CycloneDxParser p = new CycloneDxParser(new com.fasterxml.jackson.databind.ObjectMapper());
        Sbom s = p.parse(SAMPLE);
        assertThat(s.getComponents()).hasSize(2);
    }

    @Test
    void nullObjectMapperRejected() {
        assertThatThrownBy(() -> new CycloneDxParser(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullInputStreamRejected() {
        assertThatThrownBy(() -> parser.parse((InputStream) null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void parseInputStreamClosesSourceStream() {
        // 入口读完全部字节后必须关流：否则调用方很容易漏关文件句柄或 HTTP 连接。
        TrackingStream in = new TrackingStream(SAMPLE.getBytes(StandardCharsets.UTF_8));
        Sbom s = parser.parse(in);
        assertThat(s.getComponents()).hasSize(2);
        assertThat(in.isClosed()).isTrue();
    }

    @Test
    void parseInputStreamMatchesStringParse() {
        Sbom fromString = parser.parse(SAMPLE);
        Sbom fromStream =
                parser.parse(new ByteArrayInputStream(SAMPLE.getBytes(StandardCharsets.UTF_8)));
        assertThat(fromStream).isEqualTo(fromString);
    }

    @Test
    void toolsAsArrayExtractsToolName() {
        // CycloneDX 1.4 的 metadata.tools 是数组、1.5 起是对象；只认一种形状会让另一种
        // 形状的 SBOM 静默丢掉生成工具名，审计时无法追溯 SBOM 由谁产出。
        String json =
                """
        {
          "metadata": { "tools": [ { "name": "cyclonedx-maven-plugin" } ] },
          "components": []
        }
        """;
        Sbom s = parser.parse(json);
        assertThat(s.getMetadata().getTool()).isEqualTo("cyclonedx-maven-plugin");
    }

    @Test
    void unrecognizedToolsShapeStillParsesTimestamp() {
        String json =
                "{ \"metadata\": { \"tools\": \"oops\", \"timestamp\": \"2026-02-02T00:00:00Z\" } }";
        Sbom s = parser.parse(json);
        assertThat(s.getMetadata()).isNotNull();
        assertThat(s.getMetadata().getTool()).isNull();
        assertThat(s.getMetadata().getTimestamp()).isEqualTo("2026-02-02T00:00:00Z");
    }

    /** 记录 close() 是否被调用，用于断言解析入口没有把入参流丢下不管。 */
    private static final class TrackingStream extends ByteArrayInputStream {
        private boolean closed;

        TrackingStream(byte[] buf) {
            super(buf);
        }

        boolean isClosed() {
            return closed;
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
