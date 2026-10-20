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

import io.github.nephets.sbomguard.core.Component;
import io.github.nephets.sbomguard.core.CycloneDxParser;
import io.github.nephets.sbomguard.core.LicensePolicy;
import io.github.nephets.sbomguard.core.Sbom;
import io.github.nephets.sbomguard.core.SbomParsers;
import io.github.nephets.sbomguard.core.SbomValidator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/** 自动装配的边界测试：各开关的默认值与覆盖语义、用户 Bean 对自动 Bean 的优先（@ConditionalOnMissingBean）。 */
class SbomGuardAutoConfigurationEdgeTest {
    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(SbomGuardAutoConfiguration.class));

    @Test
    void requireLicenseFalseNoMissingViolation() {
        runner.withPropertyValues("sbomguard.require-license=false")
                .run(
                        ctx -> {
                            SbomValidator v = ctx.getBean(SbomValidator.class);
                            Sbom sbom =
                                    Sbom.of(
                                            Component.builder()
                                                    .name("x")
                                                    .version("1")
                                                    .purl("p")
                                                    .build());
                            assertThat(v.validate(sbom))
                                    .noneMatch(
                                            vi ->
                                                    vi.getRule()
                                                            .equals(
                                                                    LicensePolicy
                                                                            .RULE_MISSING_LICENSE));
                        });
    }

    @Test
    void requirePinnedVersionFalseNoUnpinnedViolation() {
        runner.withPropertyValues("sbomguard.require-pinned-version=false")
                .run(
                        ctx -> {
                            SbomValidator v = ctx.getBean(SbomValidator.class);
                            Sbom sbom = Sbom.of(Component.builder().name("x").build());
                            assertThat(v.validate(sbom))
                                    .noneMatch(vi -> vi.getRule().equals("VERSION_UNPINNED"));
                        });
    }

    @Test
    void allowedLicensesBoundFromProperties() {
        runner.withPropertyValues("sbomguard.allowed-licenses=MIT")
                .run(
                        ctx -> {
                            SbomValidator v = ctx.getBean(SbomValidator.class);
                            Sbom ok =
                                    Sbom.of(
                                            Component.builder()
                                                    .name("x")
                                                    .version("1")
                                                    .purl("p")
                                                    .license("MIT")
                                                    .build());
                            assertThat(v.validate(ok)).isEmpty();
                            Sbom bad =
                                    Sbom.of(
                                            Component.builder()
                                                    .name("x")
                                                    .version("1")
                                                    .purl("p")
                                                    .license("Apache-2.0")
                                                    .build());
                            assertThat(v.validate(bad))
                                    .anyMatch(
                                            vi ->
                                                    vi.getRule()
                                                            .equals(
                                                                    LicensePolicy
                                                                            .RULE_NOT_ALLOWED_LICENSE));
                        });
    }

    @Test
    void explicitEnabledTrueRegistersBeans() {
        runner.withPropertyValues("sbomguard.enabled=true")
                .run(
                        ctx -> {
                            assertThat(ctx).hasSingleBean(SbomGuard.class);
                            assertThat(ctx).hasSingleBean(SbomValidator.class);
                        });
    }

    @Test
    void defaultsRegisterFullPolicyStack() {
        // 不传任何属性：默认开启，缺许可证的组件应被判定违规。
        runner.run(
                ctx -> {
                    SbomValidator v = ctx.getBean(SbomValidator.class);
                    Sbom sbom =
                            Sbom.of(Component.builder().name("x").version("1").purl("p").build());
                    assertThat(v.validate(sbom))
                            .anyMatch(
                                    vi -> vi.getRule().equals(LicensePolicy.RULE_MISSING_LICENSE));
                });
    }

    @Test
    void customCycloneDxParserNotOverridden() {
        runner.withUserConfiguration(CustomParserConfig.class)
                .run(
                        ctx -> {
                            assertThat(ctx).hasSingleBean(CycloneDxParser.class);
                            assertThat(ctx.getBean(CycloneDxParser.class))
                                    .isSameAs(CustomParserConfig.PARSER);
                        });
    }

    @Test
    void customSbomParsersNotOverridden() {
        runner.withUserConfiguration(CustomParsersConfig.class)
                .run(
                        ctx -> {
                            assertThat(ctx).hasSingleBean(SbomParsers.class);
                            assertThat(ctx.getBean(SbomParsers.class))
                                    .isSameAs(CustomParsersConfig.PARSERS);
                        });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomParserConfig {
        static final CycloneDxParser PARSER = new CycloneDxParser();

        @Bean
        CycloneDxParser cycloneDxParser() {
            return PARSER;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomParsersConfig {
        static final SbomParsers PARSERS = new SbomParsers();

        @Bean
        SbomParsers sbomParsers() {
            return PARSERS;
        }
    }
}
