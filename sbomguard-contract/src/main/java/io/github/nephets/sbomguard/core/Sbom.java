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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** 一份软件物料清单（SBOM）。不可变，默认格式 CycloneDX。 */
public final class Sbom {
    private final String bomFormat;
    private final String specVersion;
    private final String serialNumber;
    private final SbomMetadata metadata;
    private final List<Component> components;

    private Sbom(Builder b) {
        this.bomFormat = b.bomFormat == null ? "CycloneDX" : b.bomFormat;
        this.specVersion = b.specVersion;
        this.serialNumber = b.serialNumber;
        this.metadata = b.metadata;
        this.components =
                b.components == null
                        ? Collections.emptyList()
                        : Collections.unmodifiableList(new ArrayList<>(b.components));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Sbom of(Component... cs) {
        return builder().components(List.of(cs)).build();
    }

    public static Sbom of(List<Component> cs) {
        return builder().components(cs).build();
    }

    public String getBomFormat() {
        return bomFormat;
    }

    public String getSpecVersion() {
        return specVersion;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public SbomMetadata getMetadata() {
        return metadata;
    }

    public List<Component> getComponents() {
        return components;
    }

    /**
     * 按 bom-ref 查找组件。
     *
     * @return 命中组件；未命中（含 ref 为 null）返回 {@code null}——调用方必须自行判空， 这里不抛异常是因为"查不到"是 SBOM 引用的正常状态，不是错误。
     */
    public Component getComponentByBomRef(String ref) {
        for (Component c : components) {
            if (Objects.equals(ref, c.getBomRef())) {
                return c;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Sbom)) {
            return false;
        }
        Sbom s = (Sbom) o;
        return Objects.equals(bomFormat, s.bomFormat)
                && Objects.equals(specVersion, s.specVersion)
                && Objects.equals(serialNumber, s.serialNumber)
                && Objects.equals(metadata, s.metadata)
                && Objects.equals(components, s.components);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bomFormat, specVersion, serialNumber, metadata, components);
    }

    /** SBOM 构造器。 */
    public static final class Builder {
        private String bomFormat;
        private String specVersion;
        private String serialNumber;
        private SbomMetadata metadata;
        private List<Component> components;

        public Builder bomFormat(String v) {
            this.bomFormat = v;
            return this;
        }

        public Builder specVersion(String v) {
            this.specVersion = v;
            return this;
        }

        public Builder serialNumber(String v) {
            this.serialNumber = v;
            return this;
        }

        public Builder metadata(SbomMetadata v) {
            this.metadata = v;
            return this;
        }

        public Builder components(List<Component> v) {
            this.components = v;
            return this;
        }

        public Sbom build() {
            return new Sbom(this);
        }
    }
}
