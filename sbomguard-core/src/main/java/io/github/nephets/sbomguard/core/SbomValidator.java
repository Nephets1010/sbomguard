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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 组合多条策略对 SBOM 做整体校验。 */
public final class SbomValidator {
    private static final Logger log = LoggerFactory.getLogger(SbomValidator.class);

    private final List<Policy> policies;

    private SbomValidator(List<Policy> policies) {
        this.policies = List.copyOf(policies);
    }

    public static SbomValidator of(Policy... ps) {
        return new SbomValidator(List.of(ps));
    }

    public static SbomValidator of(List<Policy> ps) {
        return new SbomValidator(ps);
    }

    public SbomValidator addPolicy(Policy p) {
        List<Policy> next = new ArrayList<>(policies);
        next.add(Objects.requireNonNull(p, "policy 不得为空"));
        log.debug("追加策略: {}", p.getClass().getSimpleName());
        return new SbomValidator(next);
    }

    public List<Violation> validate(Sbom sbom) {
        Objects.requireNonNull(sbom, "sbom 不得为空");
        log.info("开始校验 SBOM: 策略数={}, 组件数={}", policies.size(), sbom.getComponents().size());
        List<Violation> out = new ArrayList<>();
        for (Component c : sbom.getComponents()) {
            out.addAll(validate(c));
        }
        log.info("SBOM 校验完成: 命中违规 {} 条 (阻断 {} 条)", out.size(), countBlocking(out));
        return out;
    }

    public List<Violation> validate(Component c) {
        Objects.requireNonNull(c, "component 不得为空");
        List<Violation> out = new ArrayList<>();
        for (Policy p : policies) {
            out.addAll(p.evaluate(c));
        }
        if (out.isEmpty()) {
            log.debug("组件 {} 全部策略通过", c.componentRef());
        } else {
            log.info("组件 {} 校验命中 {} 条违规: {}", c.componentRef(), out.size(), out);
        }
        return out;
    }

    public boolean isClean(Sbom sbom) {
        return validate(sbom).isEmpty();
    }

    public List<Violation> blockingViolations(Sbom sbom) {
        List<Violation> out = blockingOf(validate(sbom));
        log.info("阻断级违规: {} 条", out.size());
        return out;
    }

    /** 阻断级（ERROR）判定收敛在一处，避免多处各写一遍严重级别判断导致口径漂移。 */
    private static List<Violation> blockingOf(List<Violation> vs) {
        List<Violation> out = new ArrayList<>();
        for (Violation v : vs) {
            if (v.getSeverity().isError()) {
                out.add(v);
            }
        }
        return out;
    }

    private static int countBlocking(List<Violation> vs) {
        return blockingOf(vs).size();
    }
}
