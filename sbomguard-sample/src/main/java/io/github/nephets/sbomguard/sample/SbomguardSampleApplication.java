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

import io.github.nephets.sbomguard.core.SbomParseException;
import io.github.nephets.sbomguard.core.Violation;
import io.github.nephets.sbomguard.starter.SbomGuard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 示例应用：POST /validate 接收 CycloneDX SBOM，返回策略校验结果。 */
@SpringBootApplication
@RestController
public class SbomguardSampleApplication {
    private static final Logger log = LoggerFactory.getLogger(SbomguardSampleApplication.class);

    private final SbomGuard guard;

    public SbomguardSampleApplication(SbomGuard guard) {
        this.guard = guard;
    }

    public static void main(String[] args) {
        SpringApplication.run(SbomguardSampleApplication.class, args);
    }

    @PostMapping("/validate")
    public Map<String, Object> validate(@RequestBody String sbomJson) {
        log.info("收到 /validate 请求: jsonLength={}", sbomJson == null ? 0 : sbomJson.length());
        try {
            List<Violation> violations = guard.scan(sbomJson);
            log.info("/validate 处理完成: 命中违规 {} 条", violations.size());
            return Map.of(
                    "clean",
                    violations.isEmpty(),
                    "count",
                    violations.size(),
                    "violations",
                    violations);
        } catch (SbomParseException e) {
            log.error("/validate 解析 SBOM 失败: {}", e.getMessage(), e);
            return Map.of(
                    "clean", false, "count", 0, "violations", List.of(), "error", e.getMessage());
        }
    }
}
