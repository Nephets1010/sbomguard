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

class SbomGuardPropertiesTest {
    @Test
    void defaults() {
        SbomGuardProperties p = new SbomGuardProperties();
        assertThat(p.isEnabled()).isTrue();
        assertThat(p.isRequireLicense()).isTrue();
        assertThat(p.isRequirePinnedVersion()).isTrue();
        assertThat(p.isRequirePurl()).isTrue();
        assertThat(p.getForbiddenLicenses()).isEmpty();
        assertThat(p.getAllowedLicenses()).isEmpty();
    }

    @Test
    void settersUpdateState() {
        SbomGuardProperties p = new SbomGuardProperties();
        p.setEnabled(false);
        p.setRequireLicense(false);
        p.setRequirePinnedVersion(false);
        p.setRequirePurl(false);
        p.setForbiddenLicenses(List.of("GPL-3.0"));
        p.setAllowedLicenses(List.of("MIT"));
        assertThat(p.isEnabled()).isFalse();
        assertThat(p.isRequireLicense()).isFalse();
        assertThat(p.isRequirePinnedVersion()).isFalse();
        assertThat(p.isRequirePurl()).isFalse();
        assertThat(p.getForbiddenLicenses()).containsExactly("GPL-3.0");
        assertThat(p.getAllowedLicenses()).containsExactly("MIT");
    }

    @Test
    void nullLicenseListsBecomeEmpty() {
        SbomGuardProperties p = new SbomGuardProperties();
        p.setForbiddenLicenses(null);
        p.setAllowedLicenses(null);
        assertThat(p.getForbiddenLicenses()).isEmpty();
        assertThat(p.getAllowedLicenses()).isEmpty();
    }
}
