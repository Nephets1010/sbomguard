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

/** Severity 是阻断判定的唯一依据，isError 的语义必须对所有枚举值保持一致。 */
class SeverityContractTest {
    @Test
    void warnIsNotError() {
        assertThat(Severity.WARN.isError()).isFalse();
    }

    @Test
    void errorIsError() {
        assertThat(Severity.ERROR.isError()).isTrue();
    }

    @Test
    void everySeverityHasConsistentIsError() {
        assertThat(Severity.values())
                .allSatisfy(s -> assertThat(s.isError()).isEqualTo(s == Severity.ERROR));
    }

    @Test
    void nameAndValueOfRoundTrip() {
        for (Severity s : Severity.values()) {
            assertThat(Severity.valueOf(s.name())).isEqualTo(s);
        }
    }
}
