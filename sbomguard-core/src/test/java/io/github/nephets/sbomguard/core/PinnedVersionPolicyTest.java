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

class PinnedVersionPolicyTest {
    private final PinnedVersionPolicy policy = new PinnedVersionPolicy();

    @Test
    void missingVersionFlaggedError() {
        Component c = Component.builder().name("lib").build();
        assertThat(policy.evaluate(c))
                .anyMatch(
                        v ->
                                v.getRule().equals(PinnedVersionPolicy.RULE_UNPINNED_VERSION)
                                        && v.getSeverity() == Severity.ERROR);
    }

    @Test
    void blankVersionFlaggedError() {
        Component c = Component.builder().name("lib").version("   ").build();
        assertThat(policy.evaluate(c))
                .anyMatch(v -> v.getRule().equals(PinnedVersionPolicy.RULE_UNPINNED_VERSION));
    }

    @Test
    void pinnedVersionClean() {
        Component c = Component.builder().name("lib").version("1.2.3").build();
        assertThat(policy.evaluate(c)).isEmpty();
    }

    @Test
    void wildcardVersionWarned() {
        Component c = Component.builder().name("lib").version("1.*").build();
        assertThat(policy.evaluate(c)).anyMatch(v -> v.getSeverity() == Severity.WARN);
    }

    @Test
    void latestVersionWarned() {
        Component c = Component.builder().name("lib").version("latest").build();
        assertThat(policy.evaluate(c)).anyMatch(v -> v.getSeverity() == Severity.WARN);
    }

    @Test
    void xRangeVersionWarned() {
        Component c = Component.builder().name("lib").version("1.x").build();
        assertThat(policy.evaluate(c)).anyMatch(v -> v.getSeverity() == Severity.WARN);
    }

    @Test
    void uppercaseXRangeVersionWarned() {
        // 浮动判定基于小写副本：大写 "1.X" 必须与 "1.x" 一样被判为浮动范围。
        Component c = Component.builder().name("lib").version("1.X").build();
        assertThat(policy.evaluate(c)).anyMatch(v -> v.getSeverity() == Severity.WARN);
    }

    @Test
    void bracketRangeWarned() {
        Component c = Component.builder().name("lib").version("[1.0,2.0)").build();
        assertThat(policy.evaluate(c)).anyMatch(v -> v.getSeverity() == Severity.WARN);
    }

    @Test
    void commaRangeWarned() {
        Component c = Component.builder().name("lib").version("1.0,2.0").build();
        assertThat(policy.evaluate(c)).anyMatch(v -> v.getSeverity() == Severity.WARN);
    }

    @Test
    void plainMinorVersionClean() {
        Component c = Component.builder().name("lib").version("2.0").build();
        assertThat(policy.evaluate(c)).isEmpty();
    }
}
