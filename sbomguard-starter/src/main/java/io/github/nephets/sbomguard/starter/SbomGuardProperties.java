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

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/** sbomguard 自动装配配置属性。 */
@ConfigurationProperties(prefix = "sbomguard")
public class SbomGuardProperties {
    private boolean enabled = true;
    private List<String> forbiddenLicenses = new ArrayList<>();
    private List<String> allowedLicenses = new ArrayList<>();
    private boolean requireLicense = true;
    private boolean requirePinnedVersion = true;
    private boolean requirePurl = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getForbiddenLicenses() {
        return forbiddenLicenses;
    }

    public void setForbiddenLicenses(List<String> forbiddenLicenses) {
        this.forbiddenLicenses = forbiddenLicenses == null ? new ArrayList<>() : forbiddenLicenses;
    }

    public List<String> getAllowedLicenses() {
        return allowedLicenses;
    }

    public void setAllowedLicenses(List<String> allowedLicenses) {
        this.allowedLicenses = allowedLicenses == null ? new ArrayList<>() : allowedLicenses;
    }

    public boolean isRequireLicense() {
        return requireLicense;
    }

    public void setRequireLicense(boolean requireLicense) {
        this.requireLicense = requireLicense;
    }

    public boolean isRequirePinnedVersion() {
        return requirePinnedVersion;
    }

    public void setRequirePinnedVersion(boolean requirePinnedVersion) {
        this.requirePinnedVersion = requirePinnedVersion;
    }

    public boolean isRequirePurl() {
        return requirePurl;
    }

    public void setRequirePurl(boolean requirePurl) {
        this.requirePurl = requirePurl;
    }
}
