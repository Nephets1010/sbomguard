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
package io.github.nephets.sbomguard.starter;

import io.github.nephets.sbomguard.core.ComponentIdentityPolicy;
import io.github.nephets.sbomguard.core.LicensePolicy;
import io.github.nephets.sbomguard.core.PinnedVersionPolicy;
import io.github.nephets.sbomguard.core.Sbom;
import io.github.nephets.sbomguard.core.SbomParsers;
import io.github.nephets.sbomguard.core.SbomValidator;
import io.github.nephets.sbomguard.core.Violation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 跨特性端到端测试：解析层（store 的 SbomParsers / CycloneDxParser）与校验层（core 的策略栈） 在本模块相遇，验证"解析 -> 校验 ->
 * 归一化命中"的完整链路，以及归一化的跨层一致性。
 */
class SbomGuardEndToEndTest {
    private SbomGuard guard() {
        SbomValidator v =
                SbomValidator.of(
                        LicensePolicy.builder()
                                .requireLicense(true)
                                .forbidden(Set.of("GPL-3.0"))
                                .build(),
                        new PinnedVersionPolicy(),
                        new ComponentIdentityPolicy());
        return new SbomGuard(new SbomParsers(), v);
    }

    @Test
    void endToEndDetectsAllViolationCategories() {
        String json =
                "{ \"components\": [ "
                        + "{ \"name\": \"good\", \"version\": \"1.0\", \"purl\": \"p\", "
                        + "\"licenses\": [ { \"license\": { \"id\": \"MIT\" } } ] },"
                        + "{ \"name\": \"bad\", \"version\": \"latest\", "
                        + "\"licenses\": [ { \"license\": { \"id\": \"GPL-3.0\" } } ] } ] }";
        List<Violation> vs = guard().scan(json);
        assertThat(vs).anyMatch(v -> v.getRule().equals(LicensePolicy.RULE_FORBIDDEN_LICENSE));
        assertThat(vs).anyMatch(v -> v.getRule().equals(PinnedVersionPolicy.RULE_UNPINNED_VERSION));
        // good 组件不贡献任何违规
        assertThat(vs).noneMatch(v -> "good".equals(v.getComponentRef()));
    }

    @Test
    void endToEndParsedObjectScanned() {
        Sbom sbom =
                new SbomParsers()
                        .parse(
                                "{ \"components\": [ { \"name\": \"a\", \"version\": \"1\", \"purl\":"
                                        + " \"p\", \"licenses\": [ { \"license\": { \"id\": \"MIT\" }"
                                        + " } ] } ] }");
        assertThat(guard().scan(sbom)).isEmpty();
    }

    @Test
    void endToEndBlockingViolationsOnly() {
        String json =
                "{ \"components\": [ { \"name\": \"b\", \"version\": \"latest\", "
                        + "\"licenses\": [ { \"license\": { \"id\": \"GPL-3.0\" } } ] } ] }";
        assertThat(guard().isClean(json)).isFalse();
    }

    @Test
    void endToEndForbiddenCaseInsensitiveViaNormalization() {
        // 黑名单 GPL-3.0，SBOM 写 gpl-3.0，跨解析+策略仍需命中（归一化在策略层生效）。
        String json =
                "{ \"components\": [ { \"name\": \"b\", \"version\": \"1\", \"purl\": \"p\", "
                        + "\"licenses\": [ { \"license\": { \"id\": \"gpl-3.0\" } } ] } ] }";
        assertThat(guard().scan(json))
                .anyMatch(v -> v.getRule().equals(LicensePolicy.RULE_FORBIDDEN_LICENSE));
    }

    @Test
    void endToEndMissingVersionAndPurlTogether() {
        String json =
                "{ \"components\": [ { \"name\": \"b\", "
                        + "\"licenses\": [ { \"license\": { \"id\": \"MIT\" } } ] } ] }";
        List<Violation> vs = guard().scan(json);
        assertThat(vs).anyMatch(v -> v.getRule().equals(PinnedVersionPolicy.RULE_UNPINNED_VERSION));
        assertThat(vs).anyMatch(v -> v.getRule().equals("IDENTITY_MISSING_PURL"));
    }

    @Test
    void endToEndCleanIsClean() {
        String json =
                "{ \"components\": [ { \"name\": \"good\", \"version\": \"1.0\", "
                        + "\"purl\": \"p\", \"licenses\": [ { \"license\": { \"id\": \"MIT\" } } ] } ] }";
        assertThat(guard().isClean(json)).isTrue();
    }
}
