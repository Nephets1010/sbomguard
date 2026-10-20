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

import io.github.nephets.sbomguard.core.Sbom;
import io.github.nephets.sbomguard.core.SbomParsers;
import io.github.nephets.sbomguard.core.SbomValidator;
import io.github.nephets.sbomguard.core.Violation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/** 对外的校验门面：解析 SBOM 并跑全部策略。 */
public class SbomGuard {
    private static final Logger log = LoggerFactory.getLogger(SbomGuard.class);

    private final SbomParsers parsers;
    private final SbomValidator validator;

    public SbomGuard(SbomParsers parsers, SbomValidator validator) {
        // 门面是 Spring 自动装配的入口，构造期拒绝 null 可以让装配错误在启动阶段就炸，
        // 而不是等到第一次 /validate 请求进来才以 NPE 形式出现。
        this.parsers = Objects.requireNonNull(parsers, "parsers 不得为空");
        this.validator = Objects.requireNonNull(validator, "validator 不得为空");
    }

    public List<Violation> scan(String sbomJson) {
        log.info("开始扫描 SBOM (JSON): jsonLength={}", sbomJson == null ? 0 : sbomJson.length());
        Sbom sbom = parsers.parse(sbomJson);
        List<Violation> violations = validator.validate(sbom);
        log.info("扫描完成: 命中违规 {} 条", violations.size());
        return violations;
    }

    public List<Violation> scan(Sbom sbom) {
        Objects.requireNonNull(sbom, "sbom 不得为空");
        log.info("开始扫描 SBOM (对象): 组件数={}", sbom.getComponents().size());
        List<Violation> violations = validator.validate(sbom);
        log.info("扫描完成: 命中违规 {} 条", violations.size());
        return violations;
    }

    public boolean isClean(String sbomJson) {
        return scan(sbomJson).isEmpty();
    }
}
