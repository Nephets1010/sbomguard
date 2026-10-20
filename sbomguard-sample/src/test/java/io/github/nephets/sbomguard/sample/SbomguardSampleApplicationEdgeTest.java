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
package io.github.nephets.sbomguard.sample;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 示例应用的边界测试：多组件混合、缺失版本、黑名单大小写归一、空组件清单、缺失 PURL。 通过真实 HTTP 端点验证端到端链路（解析 + 校验 + 序列化）。 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "sbomguard.forbidden-licenses=GPL-3.0")
class SbomguardSampleApplicationEdgeTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void multipleComponentsOneForbidden() throws Exception {
        String sbom =
                "{ \"components\": [ "
                        + "{ \"name\": \"a\", \"version\": \"1.0\", \"purl\": \"p\", "
                        + "\"licenses\": [ { \"license\": { \"id\": \"MIT\" } } ] },"
                        + "{ \"name\": \"b\", \"version\": \"1.0\", \"purl\": \"p\", "
                        + "\"licenses\": [ { \"license\": { \"id\": \"GPL-3.0\" } } ] } ] }";
        mockMvc.perform(post("/validate").contentType(MediaType.APPLICATION_JSON).content(sbom))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void missingVersionFlagged() throws Exception {
        String sbom =
                "{ \"components\": [ { \"name\": \"a\", \"purl\": \"p\", "
                        + "\"licenses\": [ { \"license\": { \"id\": \"MIT\" } } ] } ] }";
        mockMvc.perform(post("/validate").contentType(MediaType.APPLICATION_JSON).content(sbom))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.violations[0].rule").value("VERSION_UNPINNED"));
    }

    @Test
    void forbiddenCaseInsensitiveViaEndpoint() throws Exception {
        String sbom =
                "{ \"components\": [ { \"name\": \"b\", \"version\": \"1.0\", \"purl\": \"p\", "
                        + "\"licenses\": [ { \"license\": { \"id\": \"gpl-3.0\" } } ] } ] }";
        mockMvc.perform(post("/validate").contentType(MediaType.APPLICATION_JSON).content(sbom))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.violations[0].rule").value("LICENSE_FORBIDDEN"));
    }

    @Test
    void emptyComponentsIsClean() throws Exception {
        String sbom = "{ \"components\": [] }";
        mockMvc.perform(post("/validate").contentType(MediaType.APPLICATION_JSON).content(sbom))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clean").value(true))
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void missingPurlFlagged() throws Exception {
        String sbom =
                "{ \"components\": [ { \"name\": \"a\", \"version\": \"1.0\", "
                        + "\"licenses\": [ { \"license\": { \"id\": \"MIT\" } } ] } ] }";
        mockMvc.perform(post("/validate").contentType(MediaType.APPLICATION_JSON).content(sbom))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.violations[0].rule").value("IDENTITY_MISSING_PURL"));
    }
}
