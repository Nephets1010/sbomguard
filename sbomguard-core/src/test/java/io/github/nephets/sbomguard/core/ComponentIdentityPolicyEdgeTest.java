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

/** 身份完整性护栏的边界测试：name/purl 双缺失的数量、componentRef 退化为 null 的记录、空白处理。 */
class ComponentIdentityPolicyEdgeTest {
    private final ComponentIdentityPolicy policy = new ComponentIdentityPolicy();

    @Test
    void bothMissingProducesTwoViolations() {
        assertThat(policy.evaluate(Component.builder().build())).hasSize(2);
    }

    @Test
    void missingNameRefersToNullRefWhenNameAbsent() {
        // name 与 bom-ref 都缺失时 componentRef() 退化为 null，违规需如实记录 null 而非 NPE。
        assertThat(policy.evaluate(Component.builder().build()))
                .filteredOn(v -> v.getRule().equals(ComponentIdentityPolicy.RULE_MISSING_NAME))
                .allMatch(v -> v.getComponentRef() == null);
    }

    @Test
    void blankPurlWarned() {
        assertThat(policy.evaluate(Component.builder().name("n").purl("   ").build()))
                .anyMatch(v -> v.getRule().equals(ComponentIdentityPolicy.RULE_MISSING_PURL));
    }

    @Test
    void namePresentPurlAbsentWarnsOnly() {
        assertThat(policy.evaluate(Component.builder().name("n").build()))
                .noneMatch(v -> v.getRule().equals(ComponentIdentityPolicy.RULE_MISSING_NAME))
                .anyMatch(v -> v.getRule().equals(ComponentIdentityPolicy.RULE_MISSING_PURL));
    }

    @Test
    void fullyIdentifiedNoViolations() {
        assertThat(policy.evaluate(Component.builder().name("n").purl("pkg:maven/a/b@1").build()))
                .isEmpty();
    }

    @Test
    void missingNameErrorMessageMentionsName() {
        assertThat(policy.evaluate(Component.builder().build()))
                .filteredOn(v -> v.getRule().equals(ComponentIdentityPolicy.RULE_MISSING_NAME))
                .allMatch(v -> v.getMessage().contains("name"));
    }
}
