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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 许可证护栏：要求声明许可证、拦截禁用许可证、对白名单外许可证告警。 */
public final class LicensePolicy implements Policy {
    private static final Logger log = LoggerFactory.getLogger(LicensePolicy.class);
    public static final String RULE_MISSING_LICENSE = "LICENSE_MISSING";
    public static final String RULE_FORBIDDEN_LICENSE = "LICENSE_FORBIDDEN";
    public static final String RULE_NOT_ALLOWED_LICENSE = "LICENSE_NOT_ALLOWED";

    private final boolean requireLicense;
    private final Set<String> forbidden;
    private final Set<String> allowed;

    private LicensePolicy(Builder b) {
        this.requireLicense = b.requireLicense;
        // 归一化（小写 + 去首尾空白，Locale.ROOT 与 PinnedVersionPolicy 一致），
        // 否则 SBOM 里写 "gpl-3.0" / "GPL-3.0 " 就能绕过禁用心心念念的黑名单。
        this.forbidden = normalize(b.forbidden);
        this.allowed = normalize(b.allowed);
    }

    private static Set<String> normalize(Set<String> in) {
        if (in == null) {
            return Set.of();
        }
        Set<String> out = new HashSet<>(in.size());
        for (String s : in) {
            out.add(norm(s));
        }
        return out;
    }

    private static String norm(String s) {
        return s == null ? null : s.trim().toLowerCase(Locale.ROOT);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public List<Violation> evaluate(Component c) {
        List<Violation> out = new ArrayList<>();
        Set<String> licenses = c.getLicenses();
        if (requireLicense && licenses.isEmpty()) {
            out.add(
                    Violation.of(
                            RULE_MISSING_LICENSE,
                            Severity.ERROR,
                            "组件 " + c.getName() + " 缺少许可证声明",
                            c.componentRef()));
        }
        for (String lic : licenses) {
            // 归一化一次复用：黑名单与白名单用的是同一套口径，重复 trim/lowercase
            // 只是在每次组件扫描里白白多分配两个字符串。
            String key = norm(lic);
            if (forbidden.contains(key)) {
                out.add(
                        Violation.of(
                                RULE_FORBIDDEN_LICENSE,
                                Severity.ERROR,
                                "组件 " + c.getName() + " 使用了禁用许可证 " + lic,
                                c.componentRef()));
            } else if (!allowed.isEmpty() && !allowed.contains(key)) {
                out.add(
                        Violation.of(
                                RULE_NOT_ALLOWED_LICENSE,
                                Severity.WARN,
                                "组件 " + c.getName() + " 的许可证 " + lic + " 不在白名单内",
                                c.componentRef()));
            }
        }
        if (out.isEmpty()) {
            log.debug(
                    "组件 {} 许可证护栏通过: requireLicense={}, licenses={}",
                    c.componentRef(),
                    requireLicense,
                    licenses);
        } else {
            log.info("组件 {} 许可证护栏命中 {} 条违规: {}", c.componentRef(), out.size(), out);
        }
        return out;
    }

    /** 构造器。 */
    public static final class Builder {
        private boolean requireLicense = true;
        private Set<String> forbidden;
        private Set<String> allowed;

        public Builder requireLicense(boolean v) {
            this.requireLicense = v;
            return this;
        }

        public Builder forbidden(Set<String> v) {
            this.forbidden = v;
            return this;
        }

        public Builder allowed(Set<String> v) {
            this.allowed = v;
            return this;
        }

        public LicensePolicy build() {
            return new LicensePolicy(this);
        }
    }
}
