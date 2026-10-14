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

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComponentTest {
    @Test
    void builderSetsAllFields() {
        Component c =
                Component.builder()
                        .bomRef("ref1")
                        .group("com.a")
                        .name("lib")
                        .version("1.0")
                        .purl("pkg:maven/com.a/lib@1.0")
                        .scope("required")
                        .license("MIT")
                        .build();
        assertThat(c.getBomRef()).isEqualTo("ref1");
        assertThat(c.getGroup()).isEqualTo("com.a");
        assertThat(c.getName()).isEqualTo("lib");
        assertThat(c.getVersion()).isEqualTo("1.0");
        assertThat(c.getPurl()).isEqualTo("pkg:maven/com.a/lib@1.0");
        assertThat(c.getScope()).isEqualTo("required");
        assertThat(c.getLicenses()).containsExactly("MIT");
    }

    @Test
    void ofNameShortcut() {
        Component c = Component.of("x");
        assertThat(c.getName()).isEqualTo("x");
        assertThat(c.getLicenses()).isEmpty();
    }

    @Test
    void nameMayBeNull() {
        Component c = Component.builder().build();
        assertThat(c.getName()).isNull();
    }

    @Test
    void licensesAreUnmodifiable() {
        Component c = Component.builder().name("x").license("MIT", "Apache-2.0").build();
        assertThat(c.getLicenses()).containsExactly("MIT", "Apache-2.0");
        assertThatThrownBy(() -> c.getLicenses().add("GPL"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void nullLicensesYieldsEmptySet() {
        Component c = Component.builder().name("x").licenses((Set<String>) null).build();
        assertThat(c.getLicenses()).isEmpty();
    }

    @Test
    void licenseExpressionJoinsWithOr() {
        Component c = Component.builder().name("x").license("MIT").license("Apache-2.0").build();
        assertThat(c.getLicenseExpression()).isEqualTo("MIT OR Apache-2.0");
    }

    @Test
    void hasLicenseMatches() {
        Component c = Component.builder().name("x").license("MIT").build();
        assertThat(c.hasLicense("MIT")).isTrue();
        assertThat(c.hasLicense("GPL")).isFalse();
    }

    @Test
    void hasLicenseIsCaseAndWhitespaceInsensitive() {
        // 修复前 "MIT" 与 "mit" / " MIT " 被当成不同许可证，会漏判。
        Component c = Component.builder().name("x").license("MIT").build();
        assertThat(c.hasLicense("mit")).isTrue();
        assertThat(c.hasLicense(" MIT ")).isTrue();
        assertThat(c.hasLicense("Apache-2.0")).isFalse();
        assertThat(c.hasLicense(null)).isFalse();
    }

    @Test
    void licenseBuilderAcceptsSet() {
        Set<String> set = new LinkedHashSet<>();
        set.add("MIT");
        Component c = Component.builder().name("x").licenses(set).build();
        assertThat(c.getLicenses()).containsExactly("MIT");
    }

    @Test
    void componentRefPrefersBomRef() {
        Component c = Component.builder().bomRef("r").name("n").build();
        assertThat(c.componentRef()).isEqualTo("r");
        Component c2 = Component.builder().name("n2").build();
        assertThat(c2.componentRef()).isEqualTo("n2");
    }

    @Test
    void equalityAndHashCode() {
        Component a =
                Component.builder().bomRef("r").group("g").name("n").version("1").purl("p").build();
        Component b =
                Component.builder().bomRef("r").group("g").name("n").version("1").purl("p").build();
        Component c = Component.builder().bomRef("r").name("n").build();
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
    }

    @Test
    void toStringContainsName() {
        Component c = Component.builder().name("lib").version("1.0").build();
        assertThat(c.toString()).contains("lib").contains("1.0");
    }
}
