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

import io.github.nephets.sbomguard.core.ComponentIdentityPolicy;
import io.github.nephets.sbomguard.core.CycloneDxParser;
import io.github.nephets.sbomguard.core.LicensePolicy;
import io.github.nephets.sbomguard.core.PinnedVersionPolicy;
import io.github.nephets.sbomguard.core.Policy;
import io.github.nephets.sbomguard.core.SbomParsers;
import io.github.nephets.sbomguard.core.SbomValidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** sbomguard Spring Boot 自动装配。 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SbomGuardProperties.class)
public class SbomGuardAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger(SbomGuardAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "sbomguard.enabled", havingValue = "true", matchIfMissing = true)
    public CycloneDxParser cycloneDxParser() {
        return new CycloneDxParser();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "sbomguard.enabled", havingValue = "true", matchIfMissing = true)
    public SbomParsers sbomParsers(CycloneDxParser parser) {
        return new SbomParsers(parser);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "sbomguard.enabled", havingValue = "true", matchIfMissing = true)
    public SbomValidator sbomValidator(SbomGuardProperties props) {
        List<Policy> policies = new ArrayList<>();
        policies.add(
                LicensePolicy.builder()
                        .requireLicense(props.isRequireLicense())
                        .forbidden(new HashSet<>(props.getForbiddenLicenses()))
                        .allowed(new HashSet<>(props.getAllowedLicenses()))
                        .build());
        if (props.isRequirePinnedVersion()) {
            policies.add(new PinnedVersionPolicy());
        }
        if (props.isRequirePurl()) {
            policies.add(new ComponentIdentityPolicy());
        }
        log.info(
                "装配 SbomValidator: 启用策略={}, 禁用许可证数={}, 白名单许可证数={}",
                policies.stream().map(p -> p.getClass().getSimpleName()).toList(),
                props.getForbiddenLicenses().size(),
                props.getAllowedLicenses().size());
        return SbomValidator.of(policies);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "sbomguard.enabled", havingValue = "true", matchIfMissing = true)
    public SbomGuard sbomGuard(SbomParsers parsers, SbomValidator validator) {
        log.info("装配 SbomGuard 校验门面");
        return new SbomGuard(parsers, validator);
    }
}
