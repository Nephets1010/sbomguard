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

/** 身份完整性护栏：组件必须有 name，建议带 PURL 以利溯源。 */
public final class ComponentIdentityPolicy implements Policy {
    private static final Logger log = LoggerFactory.getLogger(ComponentIdentityPolicy.class);

    public static final String RULE_MISSING_NAME = "IDENTITY_MISSING_NAME";
    public static final String RULE_MISSING_PURL = "IDENTITY_MISSING_PURL";

    @Override
    public List<Violation> evaluate(Component c) {
        List<Violation> out = new ArrayList<>();
        if (c.getName() == null || c.getName().isBlank()) {
            out.add(
                    Violation.of(
                            RULE_MISSING_NAME,
                            Severity.ERROR,
                            "组件缺少 name 字段，无法唯一标识",
                            c.componentRef()));
        }
        if (c.getPurl() == null || c.getPurl().isBlank()) {
            out.add(
                    Violation.of(
                            RULE_MISSING_PURL,
                            Severity.WARN,
                            "组件 " + c.getName() + " 缺少 PURL（包 URL），溯源能力下降",
                            c.componentRef()));
        }
        if (out.isEmpty()) {
            log.debug("组件 {} 身份完整性护栏通过", c.componentRef());
        } else {
            log.info("组件 {} 身份完整性护栏命中 {} 条违规: {}", c.componentRef(), out.size(), out);
        }
        return out;
    }
}
