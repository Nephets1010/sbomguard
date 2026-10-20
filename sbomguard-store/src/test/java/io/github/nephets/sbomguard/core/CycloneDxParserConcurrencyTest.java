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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CycloneDxParser 无可变状态（底层的 ObjectMapper 本身线程安全），并发解析同一份 SBOM
 * 必须得到逐字节一致的领域对象。把"解析可并发"做成可验证契约，防止将来误引入实例字段。 用 invokeAll 回收每个任务的结果，避免个别线程被中断而漏记导致的偶发断言失败。
 */
class CycloneDxParserConcurrencyTest {
    @Test
    void parseIsThreadSafeForSameInput() throws Exception {
        final String json = "{ \"components\": [ { \"name\": \"a\", \"version\": \"1\" } ] }";
        int threads = 16;
        CycloneDxParser parser = new CycloneDxParser();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Callable<Sbom>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> parser.parse(json));
        }
        List<Future<Sbom>> futures = pool.invokeAll(tasks);
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        assertThat(futures).hasSize(threads);
        Sbom first = futures.get(0).get();
        for (Future<Sbom> f : futures) {
            assertThat(f.get()).isEqualTo(first);
        }
    }
}
