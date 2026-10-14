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

import java.util.Objects;

/** SBOM 元数据（工具、时间戳、生成组件）。 */
public final class SbomMetadata {
    private final String tool;
    private final String timestamp;
    private final Component component;

    public SbomMetadata(String tool, String timestamp, Component component) {
        this.tool = tool;
        this.timestamp = timestamp;
        this.component = component;
    }

    public static SbomMetadata of(String tool, String timestamp) {
        return new SbomMetadata(tool, timestamp, null);
    }

    public String getTool() {
        return tool;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public Component getComponent() {
        return component;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SbomMetadata)) {
            return false;
        }
        SbomMetadata m = (SbomMetadata) o;
        return Objects.equals(tool, m.tool)
                && Objects.equals(timestamp, m.timestamp)
                && Objects.equals(component, m.component);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tool, timestamp, component);
    }
}
