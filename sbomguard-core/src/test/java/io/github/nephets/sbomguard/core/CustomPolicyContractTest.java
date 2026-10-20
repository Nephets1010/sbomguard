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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Policy 接口是可扩展点（函数式接口），验证自定义策略能被校验器正确调用与聚合。 */
class CustomPolicyContractTest {
    @Test
    void customPolicyIsInvokedAndItsViolationsCollected() {
        Policy custom =
                c ->
                        List.of(
                                Violation.of(
                                        "CUSTOM_RULE", Severity.WARN, "custom", c.componentRef()));
        SbomValidator v = SbomValidator.of(custom);
        Component c = Component.builder().name("lib").build();
        assertThat(v.validate(c)).anyMatch(x -> x.getRule().equals("CUSTOM_RULE"));
    }

    @Test
    void customPolicyReturningEmptyYieldsNoViolation() {
        Policy custom = c -> List.of();
        SbomValidator v = SbomValidator.of(custom);
        assertThat(v.validate(Component.builder().name("x").build())).isEmpty();
    }

    @Test
    void customPolicyCombinedWithBuiltIn() {
        Policy custom = c -> List.of(Violation.of("CUSTOM", Severity.ERROR, "e", c.componentRef()));
        SbomValidator v = SbomValidator.of(custom, new ComponentIdentityPolicy());
        Component c = Component.builder().build(); // 缺 name
        assertThat(v.validate(c))
                .anyMatch(x -> x.getRule().equals("CUSTOM"))
                .anyMatch(x -> x.getRule().equals(ComponentIdentityPolicy.RULE_MISSING_NAME));
    }

    @Test
    void policyIsFunctionalInterface() {
        Policy p = c -> List.of();
        assertThat(p.evaluate(Component.of("x"))).isEmpty();
    }
}
