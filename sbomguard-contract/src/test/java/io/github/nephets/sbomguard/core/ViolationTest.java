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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ViolationTest {
    @Test
    void ofCreatesViolation() {
        Violation v = Violation.of("RULE", Severity.ERROR, "msg", "ref");
        assertThat(v.getRule()).isEqualTo("RULE");
        assertThat(v.getSeverity()).isEqualTo(Severity.ERROR);
        assertThat(v.getMessage()).isEqualTo("msg");
        assertThat(v.getComponentRef()).isEqualTo("ref");
    }

    @Test
    void nullRuleRejected() {
        assertThatThrownBy(() -> Violation.of(null, Severity.ERROR, "m", "r"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullSeverityRejected() {
        assertThatThrownBy(() -> Violation.of("R", null, "m", "r"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullMessageRejected() {
        assertThatThrownBy(() -> Violation.of("R", Severity.ERROR, null, "r"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void componentRefNullable() {
        Violation v = Violation.of("R", Severity.WARN, "m", null);
        assertThat(v.getComponentRef()).isNull();
    }

    @Test
    void equalityAndHashCode() {
        Violation a = Violation.of("R", Severity.ERROR, "m", "r");
        Violation b = Violation.of("R", Severity.ERROR, "m", "r");
        Violation c = Violation.of("R", Severity.WARN, "m", "r");
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
    }

    @Test
    void toStringContainsRuleAndMessage() {
        Violation v = Violation.of("LICENSE_MISSING", Severity.ERROR, "no license", "lib");
        assertThat(v.toString()).contains("LICENSE_MISSING").contains("no license");
    }
}
