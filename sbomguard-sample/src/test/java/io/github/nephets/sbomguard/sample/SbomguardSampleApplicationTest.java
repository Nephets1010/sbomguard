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

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "sbomguard.forbidden-licenses=GPL-3.0")
class SbomguardSampleApplicationTest {

    @Autowired private MockMvc mockMvc;

    private static final String CLEAN_SBOM =
            "{ \"components\": [ { \"name\": \"lib\", \"version\": \"1.0\", "
                    + "\"purl\": \"pkg:maven/a/lib@1.0\", \"licenses\": [ { \"license\": { \"id\": \"MIT\" } } ] } ] }";

    @Test
    void cleanSbomReturnsClean() throws Exception {
        mockMvc.perform(
                        post("/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CLEAN_SBOM))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clean").value(true))
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void forbiddenLicenseFlagged() throws Exception {
        String sbom =
                "{ \"components\": [ { \"name\": \"lib\", \"version\": \"1.0\", "
                        + "\"purl\": \"pkg:maven/a/lib@1.0\", \"licenses\": [ { \"license\": { \"id\": \"GPL-3.0\" } } ] } ] }";
        mockMvc.perform(post("/validate").contentType(MediaType.APPLICATION_JSON).content(sbom))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clean").value(false))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.violations[0].rule").value("LICENSE_FORBIDDEN"));
    }

    @Test
    void missingLicenseFlagged() throws Exception {
        String sbom =
                "{ \"components\": [ { \"name\": \"lib\", \"version\": \"1.0\", "
                        + "\"purl\": \"pkg:maven/a/lib@1.0\" } ] }";
        mockMvc.perform(post("/validate").contentType(MediaType.APPLICATION_JSON).content(sbom))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.violations[0].rule").value("LICENSE_MISSING"));
    }

    @Test
    void invalidJsonReturnsErrorPayload() throws Exception {
        mockMvc.perform(
                        post("/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{ not json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clean").value(false))
                .andExpect(jsonPath("$.error").exists());
    }
}
