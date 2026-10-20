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

import java.io.InputStream;
import java.util.Objects;

/** SBOM 解析门面：当前实现基于 {@link CycloneDxParser}。 */
public final class SbomParsers {
    private static final Logger log = LoggerFactory.getLogger(SbomParsers.class);

    private final CycloneDxParser cycloneDx;

    public SbomParsers() {
        this(new CycloneDxParser());
    }

    public SbomParsers(CycloneDxParser cycloneDx) {
        this.cycloneDx = Objects.requireNonNull(cycloneDx, "cycloneDx 解析器不得为空");
    }

    public Sbom parse(String json) {
        log.debug("通过 SbomParsers 门面解析 SBOM: jsonLength={}", json == null ? 0 : json.length());
        return cycloneDx.parse(json);
    }

    public Sbom parse(InputStream in) {
        log.debug("通过 SbomParsers 门面解析 SBOM 输入流");
        return cycloneDx.parse(in);
    }
}
