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

/** Violation 是校验结果的不可变载体，equals/hashCode/toString 的边界必须稳定，否则聚合去重会出错。 */
class ViolationContractTest {
    @Test
    void nullComponentRefAllowedInEquality() {
        Violation a = Violation.of("R", Severity.ERROR, "m", null);
        Violation b = Violation.of("R", Severity.ERROR, "m", null);
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void differentMessageNotEqual() {
        Violation a = Violation.of("R", Severity.ERROR, "m1", "r");
        Violation b = Violation.of("R", Severity.ERROR, "m2", "r");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void differentRuleNotEqual() {
        Violation a = Violation.of("R1", Severity.ERROR, "m", "r");
        Violation b = Violation.of("R2", Severity.ERROR, "m", "r");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void differentSeverityNotEqual() {
        Violation a = Violation.of("R", Severity.ERROR, "m", "r");
        Violation b = Violation.of("R", Severity.WARN, "m", "r");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalsNullAndDifferentType() {
        Violation v = Violation.of("R", Severity.ERROR, "m", "r");
        assertThat(v.equals(null)).isFalse();
        assertThat(v.equals("x")).isFalse();
    }

    @Test
    void sameInstanceEquals() {
        Violation v = Violation.of("R", Severity.ERROR, "m", "r");
        assertThat(v).isEqualTo(v);
    }

    @Test
    void toStringIncludesSeverityAndRef() {
        Violation v = Violation.of("LICENSE_FORBIDDEN", Severity.ERROR, "bad", "lib");
        assertThat(v.toString()).contains("LICENSE_FORBIDDEN").contains("ERROR").contains("lib");
    }

    @Test
    void toStringWithNullRef() {
        Violation v = Violation.of("R", Severity.WARN, "m", null);
        assertThat(v.toString()).contains("WARN");
    }
}
