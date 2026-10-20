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
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SbomValidator 是不可变对象（策略列表用 List.copyOf 冻结、组件值对象不可变）， 并发校验必须得到一致结果。这个测试把"线程安全"从一个口头承诺变成可被验证的契约。 用
 * invokeAll 回收每个任务的结果，避免因个别线程被中断而漏记导致的偶发断言失败。
 */
class SbomValidatorConcurrencyTest {
    @Test
    void validatorIsThreadSafeAcrossConcurrentValidations() throws Exception {
        SbomValidator v =
                SbomValidator.of(
                        LicensePolicy.builder()
                                .requireLicense(true)
                                .forbidden(Set.of("GPL-3.0"))
                                .build(),
                        new PinnedVersionPolicy(),
                        new ComponentIdentityPolicy());
        Sbom sbom =
                Sbom.of(
                        Component.builder().name("a").version("1").purl("p").license("MIT").build(),
                        Component.builder()
                                .name("b")
                                .version("1")
                                .purl("p")
                                .license("GPL-3.0")
                                .build());
        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Callable<int[]>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(
                    () -> {
                        List<Violation> vs = v.validate(sbom);
                        return new int[] {vs.size(), v.blockingViolations(sbom).size()};
                    });
        }
        List<Future<int[]>> futures = pool.invokeAll(tasks);
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        assertThat(futures).hasSize(threads);
        for (Future<int[]> f : futures) {
            int[] r = f.get();
            // 组件 b 仅命中禁用许可证一条 ERROR，组件 a 全清，故总 1、阻断 1。
            assertThat(r[0]).isEqualTo(1);
            assertThat(r[1]).isEqualTo(1);
        }
    }

    @Test
    void validateDoesNotMutateSharedState() {
        SbomValidator v = SbomValidator.of(new ComponentIdentityPolicy());
        Sbom sbom = Sbom.of(Component.builder().name("a").build());
        for (int i = 0; i < 100; i++) {
            assertThat(v.validate(sbom)).hasSize(1);
        }
    }
}
