# sbomguard

SBOM 守卫库：纯 Java（零 Spring）的 CycloneDX 解析与策略校验核心 + Spring Boot 自动装配，对软件物料清单做**许可证 / 版本钉死 / 身份完整性**护栏。

## 模块

| 模块 | 说明 |
| --- | --- |
| `sbomguard-core` | 零 Spring 核心：CycloneDX 解析、组件模型、许可证 / 版本钉死 / 身份完整性策略与 `SbomValidator`。仅依赖 JDK 21 + Jackson。 |
| `sbomguard-starter` | Spring Boot 自动装配：基于配置构造 `SbomValidator`，提供 `CycloneDxParser` / `SbomParsers` / `SbomGuard` Bean。 |
| `sbomguard-sample` | 示例 Web 应用：POST `/validate` 接收 CycloneDX SBOM JSON，返回策略校验违规清单。 |

## 快速开始

```yaml
sbomguard:
  forbidden-licenses: GPL-3.0      # 命中即 ERROR
  allowed-licenses: MIT, Apache-2.0
  require-license: true            # 缺许可证声明 → ERROR
  require-pinned-version: true     # 版本缺失/浮动 → 见 PinnedVersionPolicy
  require-purl: true               # 缺 PURL → WARN
```

```java
@RestController
public class Api {
  private final SbomGuard guard;
  public Api(SbomGuard guard) { this.guard = guard; }

  @PostMapping("/validate")
  public List<Violation> validate(@RequestBody String sbomJson) {
    return guard.scan(sbomJson);
  }
}
```

## 内置策略

- `LicensePolicy` — 要求声明许可证、拦截禁用许可证、对白名单外许可证告警。
- `PinnedVersionPolicy` — 缺失版本判 ERROR，浮动范围（`*` / `latest` / `x` / 区间）判 WARN。
- `ComponentIdentityPolicy` — 组件必须有 `name`，建议带 `PURL` 以利溯源。

## 构建

```bash
./mvnw -B verify
```

## 架构约束

`sbomguard-core` 严禁依赖 `org.springframework.*`（CI 的 `architecture-boundaries` job 强制扫描）。
