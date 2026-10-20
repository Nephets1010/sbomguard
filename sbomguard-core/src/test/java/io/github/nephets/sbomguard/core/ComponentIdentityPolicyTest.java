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

class ComponentIdentityPolicyTest {
    private final ComponentIdentityPolicy policy = new ComponentIdentityPolicy();

    @Test
    void missingNameFlaggedError() {
        Component c = Component.builder().build();
        assertThat(policy.evaluate(c))
                .anyMatch(
                        v ->
                                v.getRule().equals(ComponentIdentityPolicy.RULE_MISSING_NAME)
                                        && v.getSeverity() == Severity.ERROR);
    }

    @Test
    void blankNameFlaggedError() {
        Component c = Component.builder().name("  ").build();
        assertThat(policy.evaluate(c))
                .anyMatch(v -> v.getRule().equals(ComponentIdentityPolicy.RULE_MISSING_NAME));
    }

    @Test
    void presentNameNoMissingNameViolation() {
        Component c = Component.builder().name("lib").build();
        assertThat(policy.evaluate(c))
                .noneMatch(v -> v.getRule().equals(ComponentIdentityPolicy.RULE_MISSING_NAME));
    }

    @Test
    void missingPurlWarned() {
        Component c = Component.builder().name("lib").build();
        assertThat(policy.evaluate(c))
                .anyMatch(
                        v ->
                                v.getRule().equals(ComponentIdentityPolicy.RULE_MISSING_PURL)
                                        && v.getSeverity() == Severity.WARN);
    }

    @Test
    void presentPurlNoMissingPurlViolation() {
        Component c = Component.builder().name("lib").purl("pkg:maven/a/lib@1.0").build();
        assertThat(policy.evaluate(c))
                .noneMatch(v -> v.getRule().equals(ComponentIdentityPolicy.RULE_MISSING_PURL));
    }

    @Test
    void fullyIdentifiedClean() {
        Component c = Component.builder().name("lib").purl("pkg:maven/a/lib@1.0").build();
        assertThat(policy.evaluate(c)).isEmpty();
    }

    @Test
    void violationReferencesComponentName() {
        Component c = Component.builder().name("lib").build();
        assertThat(policy.evaluate(c)).anyMatch(v -> "lib".equals(v.getComponentRef()));
    }
}
