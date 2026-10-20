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

class SbomGuardAutoConfigurationTest {
    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(SbomGuardAutoConfiguration.class));

    @Test
    void beansRegisteredByDefault() {
        runner.run(
                ctx -> {
                    assertThat(ctx).hasSingleBean(SbomValidator.class);
                    assertThat(ctx).hasSingleBean(SbomParsers.class);
                    assertThat(ctx).hasSingleBean(SbomGuard.class);
                    assertThat(ctx).hasSingleBean(CycloneDxParser.class);
                });
    }

    @Test
    void userValidatorOverrideWins() {
        runner.withUserConfiguration(CustomValidatorConfig.class)
                .run(
                        ctx -> {
                            // 用户提供的 SbomValidator 存在，且自动装配的 sbomValidator 被
                            // @ConditionalOnMissingBean 跳过
                            assertThat(ctx).hasSingleBean(SbomValidator.class);
                            assertThat(ctx.getBean(SbomValidator.class)).isNotNull();
                        });
    }

    @Test
    void disabledWhenEnabledFalse() {
        runner.withPropertyValues("sbomguard.enabled=false")
                .run(
                        ctx -> {
                            assertThat(ctx).doesNotHaveBean(SbomGuard.class);
                            assertThat(ctx).doesNotHaveBean(SbomValidator.class);
                            assertThat(ctx).doesNotHaveBean(SbomParsers.class);
                        });
    }

    @Test
    void forbiddenLicenseBoundFromProperties() {
        runner.withPropertyValues("sbomguard.forbidden-licenses=GPL-3.0")
                .run(
                        ctx -> {
                            SbomValidator v = ctx.getBean(SbomValidator.class);
                            Sbom sbom =
                                    Sbom.of(
                                            Component.builder()
                                                    .name("x")
                                                    .version("1")
                                                    .purl("p")
                                                    .license("GPL-3.0")
                                                    .build());
                            assertThat(v.validate(sbom))
                                    .anyMatch(
                                            vi ->
                                                    vi.getRule()
                                                            .equals(
                                                                    LicensePolicy
                                                                            .RULE_FORBIDDEN_LICENSE));
                        });
    }

    @Test
    void requirePurlFalseDisablesPurlPolicy() {
        runner.withPropertyValues("sbomguard.require-purl=false")
                .run(
                        ctx -> {
                            SbomValidator v = ctx.getBean(SbomValidator.class);
                            Sbom sbom = Sbom.of(Component.builder().name("x").version("1").build());
                            assertThat(v.validate(sbom))
                                    .noneMatch(vi -> vi.getRule().equals("IDENTITY_MISSING_PURL"));
                        });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomValidatorConfig {
        @Bean
        SbomValidator myValidator() {
            return SbomValidator.of();
        }
    }
}
