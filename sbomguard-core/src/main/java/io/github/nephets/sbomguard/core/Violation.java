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

import java.util.Objects;

/** 一条策略校验违规。 */
public final class Violation {
    private final String rule;
    private final Severity severity;
    private final String message;
    private final String componentRef;

    public Violation(String rule, Severity severity, String message, String componentRef) {
        this.rule = Objects.requireNonNull(rule, "rule 不得为空");
        this.severity = Objects.requireNonNull(severity, "severity 不得为空");
        this.message = Objects.requireNonNull(message, "message 不得为空");
        this.componentRef = componentRef;
    }

    public static Violation of(
            String rule, Severity severity, String message, String componentRef) {
        return new Violation(rule, severity, message, componentRef);
    }

    public String getRule() {
        return rule;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public String getComponentRef() {
        return componentRef;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Violation)) {
            return false;
        }
        Violation v = (Violation) o;
        return Objects.equals(rule, v.rule)
                && severity == v.severity
                && Objects.equals(message, v.message)
                && Objects.equals(componentRef, v.componentRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rule, severity, message, componentRef);
    }

    @Override
    public String toString() {
        return "Violation{rule='"
                + rule
                + "', severity="
                + severity
                + ", message='"
                + message
                + "', componentRef='"
                + componentRef
                + "'}";
    }
}
