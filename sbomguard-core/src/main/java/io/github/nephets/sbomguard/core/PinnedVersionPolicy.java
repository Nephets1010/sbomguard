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
import java.util.Locale;
import java.util.Set;

/** 版本钉死护栏：缺失版本判 ERROR，浮动范围（* / latest / x / 区间）判 WARN。 */
public final class PinnedVersionPolicy implements Policy {
    private static final Logger log = LoggerFactory.getLogger(PinnedVersionPolicy.class);

    public static final String RULE_UNPINNED_VERSION = "VERSION_UNPINNED";
    private static final Set<String> FLOATERS = Set.of("*", "latest", "snapshot", "release");

    @Override
    public List<Violation> evaluate(Component c) {
        List<Violation> out = new ArrayList<>();
        String v = c.getVersion();
        if (v == null || v.isBlank()) {
            out.add(
                    Violation.of(
                            RULE_UNPINNED_VERSION,
                            Severity.ERROR,
                            "组件 " + c.getName() + " 版本缺失，无法钉死",
                            c.componentRef()));
            log.info("组件 {} 版本钉死护栏命中 1 条违规(版本缺失)", c.componentRef());
            return out;
        }
        String lv = v.toLowerCase(Locale.ROOT);
        boolean floating =
                FLOATERS.contains(lv)
                        || v.contains("*")
                        || lv.contains("x")
                        || v.contains("[")
                        || v.contains("]")
                        || v.contains(",")
                        || v.contains("+");
        if (floating) {
            out.add(
                    Violation.of(
                            RULE_UNPINNED_VERSION,
                            Severity.WARN,
                            "组件 " + c.getName() + " 版本 " + v + " 疑似浮动范围，未钉死",
                            c.componentRef()));
            log.info("组件 {} 版本钉死护栏命中 1 条违规(疑似浮动范围): version={}", c.componentRef(), v);
        } else {
            log.debug("组件 {} 版本钉死护栏通过: version={}", c.componentRef(), v);
        }
        return out;
    }
}
