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

/**
 * 版本钉死护栏的边界测试：FLOATERS 集合（* / latest / snapshot / release）、大小写归一（LATEST）、 区间/通配符字符（[ ] , + *
 * x）、稳定版本不误报。
 */
class PinnedVersionPolicyEdgeTest {
    private final PinnedVersionPolicy policy = new PinnedVersionPolicy();

    @Test
    void snapshotFlaggedWarn() {
        assertThat(policy.evaluate(Component.builder().name("n").version("snapshot").build()))
                .anyMatch(v -> v.getSeverity() == Severity.WARN);
    }

    @Test
    void releaseFlaggedWarn() {
        assertThat(policy.evaluate(Component.builder().name("n").version("release").build()))
                .anyMatch(v -> v.getSeverity() == Severity.WARN);
    }

    @Test
    void uppercaseLatestFlaggedWarn() {
        assertThat(policy.evaluate(Component.builder().name("n").version("LATEST").build()))
                .anyMatch(v -> v.getSeverity() == Severity.WARN);
    }

    @Test
    void plusSignFlaggedWarn() {
        assertThat(policy.evaluate(Component.builder().name("n").version("1.0+").build()))
                .anyMatch(v -> v.getSeverity() == Severity.WARN);
    }

    @Test
    void multiWildcardFlaggedWarn() {
        assertThat(policy.evaluate(Component.builder().name("n").version("1.*.*").build()))
                .anyMatch(v -> v.getSeverity() == Severity.WARN);
    }

    @Test
    void openBracketOnlyFlaggedWarn() {
        assertThat(policy.evaluate(Component.builder().name("n").version("[1.0").build()))
                .anyMatch(v -> v.getSeverity() == Severity.WARN);
    }

    @Test
    void closeBracketOnlyFlaggedWarn() {
        assertThat(policy.evaluate(Component.builder().name("n").version("1.0]").build()))
                .anyMatch(v -> v.getSeverity() == Severity.WARN);
    }

    @Test
    void commaOnlyFlaggedWarn() {
        assertThat(policy.evaluate(Component.builder().name("n").version("1.0,2.0").build()))
                .anyMatch(v -> v.getSeverity() == Severity.WARN);
    }

    @Test
    void stableReleaseVersionClean() {
        assertThat(policy.evaluate(Component.builder().name("n").version("1.2.3").build()))
                .isEmpty();
    }

    @Test
    void versionWithBuildMetadataClean() {
        assertThat(policy.evaluate(Component.builder().name("n").version("1.0.0-rc1").build()))
                .isEmpty();
    }

    @Test
    void missingVersionIsErrorNotWarn() {
        assertThat(policy.evaluate(Component.builder().name("n").build()))
                .allMatch(v -> v.getSeverity() == Severity.ERROR);
    }
}
