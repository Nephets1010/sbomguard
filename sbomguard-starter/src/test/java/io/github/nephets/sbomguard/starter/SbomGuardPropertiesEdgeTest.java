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
package io.github.nephets.sbomguard.starter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 配置属性的边界测试：缺省值、空集合的防御性归一、enabled 默认开启。 */
class SbomGuardPropertiesEdgeTest {
    @Test
    void forbiddenLicensesDefaultEmpty() {
        assertThat(new SbomGuardProperties().getForbiddenLicenses()).isEmpty();
    }

    @Test
    void allowedLicensesDefaultEmpty() {
        assertThat(new SbomGuardProperties().getAllowedLicenses()).isEmpty();
    }

    @Test
    void setForbiddenLicensesEmptyList() {
        SbomGuardProperties p = new SbomGuardProperties();
        p.setForbiddenLicenses(List.of());
        assertThat(p.getForbiddenLicenses()).isEmpty();
    }

    @Test
    void setAllowedLicensesEmptyList() {
        SbomGuardProperties p = new SbomGuardProperties();
        p.setAllowedLicenses(List.of());
        assertThat(p.getAllowedLicenses()).isEmpty();
    }

    @Test
    void enabledDefaultTrue() {
        assertThat(new SbomGuardProperties().isEnabled()).isTrue();
    }
}
