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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** SBOM 中的单个组件（物料）描述。不可变。 */
public final class Component {
    private final String bomRef;
    private final String group;
    private final String name;
    private final String version;
    private final String purl;
    private final Set<String> licenses;
    private final String scope;

    private Component(Builder b) {
        this.bomRef = b.bomRef;
        this.group = b.group;
        this.name = b.name;
        this.version = b.version;
        this.purl = b.purl;
        this.licenses =
                b.licenses == null
                        ? Collections.emptySet()
                        : Collections.unmodifiableSet(new LinkedHashSet<>(b.licenses));
        this.scope = b.scope;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Component of(String name) {
        return builder().name(name).build();
    }

    public String getBomRef() {
        return bomRef;
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getPurl() {
        return purl;
    }

    public String getScope() {
        return scope;
    }

    public Set<String> getLicenses() {
        return licenses;
    }

    public String getLicenseExpression() {
        return String.join(" OR ", licenses);
    }

    public boolean hasLicense(String id) {
        // 归一化查找（小写 + 去空白，Locale.ROOT），与 LicensePolicy 一致，
        // 避免 "gpl-3.0" 与 "GPL-3.0" 被当成不同许可证而漏判。
        if (id == null) {
            return false;
        }
        String key = id.trim().toLowerCase(Locale.ROOT);
        for (String l : licenses) {
            if (l != null && l.trim().toLowerCase(Locale.ROOT).equals(key)) {
                return true;
            }
        }
        return false;
    }

    /** 用于关联违规的组件引用：优先 bom-ref，回退到 name。 */
    public String componentRef() {
        return bomRef != null ? bomRef : name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Component)) {
            return false;
        }
        Component c = (Component) o;
        return Objects.equals(bomRef, c.bomRef)
                && Objects.equals(group, c.group)
                && Objects.equals(name, c.name)
                && Objects.equals(version, c.version)
                && Objects.equals(purl, c.purl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bomRef, group, name, version, purl);
    }

    @Override
    public String toString() {
        return "Component{bomRef='"
                + bomRef
                + "', group='"
                + group
                + "', name='"
                + name
                + "', version='"
                + version
                + "', purl='"
                + purl
                + "'}";
    }

    /** 组件构造器。 */
    public static final class Builder {
        private String bomRef;
        private String group;
        private String name;
        private String version;
        private String purl;
        private Set<String> licenses;
        private String scope;

        public Builder bomRef(String v) {
            this.bomRef = v;
            return this;
        }

        public Builder group(String v) {
            this.group = v;
            return this;
        }

        public Builder name(String v) {
            this.name = v;
            return this;
        }

        public Builder version(String v) {
            this.version = v;
            return this;
        }

        public Builder purl(String v) {
            this.purl = v;
            return this;
        }

        public Builder scope(String v) {
            this.scope = v;
            return this;
        }

        public Builder license(String... vs) {
            if (this.licenses == null) {
                this.licenses = new LinkedHashSet<>();
            }
            for (String v : vs) {
                this.licenses.add(v);
            }
            return this;
        }

        public Builder licenses(Set<String> vs) {
            this.licenses = vs == null ? null : new LinkedHashSet<>(vs);
            return this;
        }

        public Component build() {
            return new Component(this);
        }
    }
}
