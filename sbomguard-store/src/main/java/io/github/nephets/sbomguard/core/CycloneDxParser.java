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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** 将 CycloneDX 1.4/1.5 JSON 解析为 {@link Sbom}。 */
public final class CycloneDxParser {
    private static final Logger log = LoggerFactory.getLogger(CycloneDxParser.class);

    private final ObjectMapper mapper;

    public CycloneDxParser() {
        this(new ObjectMapper());
    }

    public CycloneDxParser(ObjectMapper mapper) {
        // 构造期就拒绝 null：否则缺陷会推迟到 parse 时才以 NPE 暴露，
        // 届时堆栈指向的是调用点而不是装配点，排查成本高得多。
        this.mapper = Objects.requireNonNull(mapper, "mapper 不得为空");
    }

    public Sbom parse(String json) {
        if (json == null || json.isBlank()) {
            log.error("CycloneDX 解析失败: SBOM 内容为空");
            throw new SbomParseException("SBOM 内容为空");
        }
        log.info("开始解析 CycloneDX SBOM: jsonLength={}", json.length());
        try {
            JsonNode root = mapper.readTree(json);
            Sbom sbom = fromNode(root);
            log.info(
                    "CycloneDX 解析完成: 组件数={}, bomFormat={}, specVersion={}",
                    sbom.getComponents().size(),
                    sbom.getBomFormat(),
                    sbom.getSpecVersion());
            return sbom;
        } catch (IOException e) {
            log.error("CycloneDX 解析失败: {}", e.getMessage(), e);
            throw new SbomParseException("SBOM JSON 解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 读取整个输入流并解析。
     *
     * <p>本方法读取完毕后会关闭传入的流：解析入口已经把字节全部读走，继续持有流只会让调用方 漏关文件句柄或连接。调用方不应再复用同一个流实例。
     */
    public Sbom parse(InputStream in) {
        Objects.requireNonNull(in, "SBOM 输入流不得为空");
        try (InputStream source = in) {
            byte[] bytes = source.readAllBytes();
            return parse(new String(bytes, StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("读取 SBOM 输入流失败", e);
            throw new UncheckedIOException("读取 SBOM 输入流失败", e);
        }
    }

    private Sbom fromNode(JsonNode root) {
        Sbom.Builder b = Sbom.builder();
        if (root.has("bomFormat")) {
            b.bomFormat(textOrNull(root, "bomFormat"));
        }
        if (root.has("specVersion")) {
            b.specVersion(textOrNull(root, "specVersion"));
        }
        if (root.has("serialNumber")) {
            b.serialNumber(textOrNull(root, "serialNumber"));
        }
        if (root.has("metadata")) {
            b.metadata(metadataFrom(root.get("metadata")));
        }
        List<Component> components = new ArrayList<>();
        JsonNode arr = root.get("components");
        if (arr != null && arr.isArray()) {
            for (JsonNode n : arr) {
                components.add(componentFrom(n));
            }
        }
        b.components(components);
        return b.build();
    }

    private SbomMetadata metadataFrom(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String tool = toolFrom(node.get("tools"));
        String timestamp = textOrNull(node, "timestamp");
        return new SbomMetadata(tool, timestamp, null);
    }

    /**
     * 提取生成工具名。CycloneDX 1.4 的 {@code tools} 是数组、1.5 起改为带 {@code components} 的 对象；只认一种形状会让另一种形状的
     * SBOM 静默丢失工具信息（审计时无法追溯 SBOM 由谁生成）， 因此两种都要认，认不出时留一条 debug 而不是直接返回 null 了事。
     */
    private String toolFrom(JsonNode tools) {
        if (tools == null || tools.isNull()) {
            return null;
        }
        if (tools.isArray()) {
            if (tools.size() > 0) {
                return textOrNull(tools.get(0), "name");
            }
            return null;
        }
        JsonNode tc = tools.get("components");
        if (tc != null && tc.isArray() && tc.size() > 0) {
            return textOrNull(tc.get(0), "name");
        }
        log.debug("metadata.tools 形状无法识别，跳过工具名提取: {}", tools.getNodeType());
        return null;
    }

    private Component componentFrom(JsonNode n) {
        Component.Builder b = Component.builder();
        if (n.has("bom-ref")) {
            b.bomRef(textOrNull(n, "bom-ref"));
        }
        if (n.has("group")) {
            b.group(textOrNull(n, "group"));
        }
        b.name(textOrNull(n, "name"));
        if (n.has("version")) {
            b.version(textOrNull(n, "version"));
        }
        if (n.has("purl")) {
            b.purl(textOrNull(n, "purl"));
        }
        if (n.has("scope")) {
            b.scope(textOrNull(n, "scope"));
        }
        b.licenses(licensesFrom(n));
        return b.build();
    }

    private Set<String> licensesFrom(JsonNode n) {
        Set<String> out = new LinkedHashSet<>();
        JsonNode licenses = n.get("licenses");
        if (licenses != null && licenses.isArray()) {
            for (JsonNode l : licenses) {
                if (l.has("license")) {
                    JsonNode lic = l.get("license");
                    String id = textOrNull(lic, "id");
                    String name = textOrNull(lic, "name");
                    if (id != null) {
                        out.add(id);
                    } else if (name != null) {
                        out.add(name);
                    }
                } else if (l.has("expression")) {
                    out.add(textOrNull(l, "expression"));
                }
            }
        }
        return out;
    }

    private static String textOrNull(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        return v.asText();
    }
}
