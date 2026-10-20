# sbomguard

SBOM 守卫库：纯 Java（零 Spring）的 CycloneDX 解析与策略校验核心 + Spring Boot 自动装配，对软件物料清单做**许可证 / 版本钉死 / 身份完整性**护栏。

## 模块

| 模块 | 职责 |
| --- | --- |
| `sbomguard-contract` | 跨模块共享契约（零三方依赖）：SBOM 领域值对象 `Component` / `Sbom` / `SbomMetadata`、策略接口 `Policy`、违规模型 `Violation` / `Severity`、解析异常 `SbomParseException`。仅依赖 JDK 21，被 `core` / `store` / `starter` 共同依赖。 |
| `sbomguard-core` | 纯 Java 校验引擎：许可证 / 版本钉死 / 身份完整性策略与组合校验器 `SbomValidator`。依赖 `contract` + SLF4J。 |
| `sbomguard-store` | SBOM 摄入 / 解析层：将 CycloneDX 1.4/1.5 JSON 解析为 `contract` 领域模型，并对外暴露解析门面 `SbomParsers`。依赖 `contract` + Jackson + SLF4J。 |
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

## 设计决策（why）

1. **为什么拆出 `contract` / `store` 两个新模块**：领域模型与策略接口是最稳定的"契约"，被引擎、解析层、Spring 装配三方共享；把它们独立成 `contract`（零三方依赖）可避免任何上层模块被迫传递 Jackson / Spring。`store` 独立承载解析（摄入）职责，使 `core` 只聚焦于"校验规则"这一单一职责，符合单一职责与关注点分离。
2. **为什么全限定类名保持不变（只搬目录）**：模块拆分是物理重组而非 API 演进；保持包名 + 类名不变，意味着所有 `import` 一行都不用改，零破坏性，且 git 历史仍可线性追溯每个类的演进。
3. **为什么三套策略各自独立且实现 `Policy` 接口**：每条护栏是独立的可组合规则，便于单独开关、单独单测、单独扩展（自定义 `Policy` 即插即用）；校验器只认 `Policy` 接口，符合开闭原则。
4. **为什么许可证 / 版本判定要做大小写 + 空白归一化**：SBOM 由不同工具生成，同一许可证可能写作 `GPL-3.0` / `gpl-3.0` / `GPL-3.0 `；不归一化就会被绕过或误报，归一化是安全护栏正确性的前提。
5. **为什么 `SbomValidator` / `CycloneDxParser` 做成不可变、线程安全**：校验器策略列表用 `List.copyOf` 冻结，解析器无实例可变状态，于是可在 Web 请求并发下安全复用同一实例，无需每次 `new`。
6. **为什么违规区分 ERROR / WARN（阻断 vs 告警）**：阻断级（ERROR）才是"门禁不通过"，告警（WARN）只提示风险；把判定口径收敛在 `Severity.isError()` 一处，避免多处分支漂移导致口径不一致。
7. **为什么 `store` 解析层只解析顶层 `components`、不递归嵌套**：CycloneDX 的 `components` 数组是物料清单的顶层列表，嵌套 `components` 在规范语义里不是独立物料；只取顶层既符合规范，也避免把"内部子组件"误算成独立物料。
8. **为什么 jacoco 门禁分档（`contract`/`core` 0.75，`store` 0.70，其余 0.60）**：纯逻辑模块分支简单、应被高覆盖；解析层分支较多（多规格版本 / 多种 `tools` 形状 / 多种许可证形状）给略低但仍严格的下限；门禁挂 `verify` 阶段且 `haltOnFailure`，确保覆盖率只升不降。

## 构建

```bash
./mvnw -B verify
```

## 架构约束

`sbomguard-contract` / `sbomguard-core` / `sbomguard-store` 严禁依赖 `org.springframework.*`（CI 的 `architecture-boundaries` job 强制扫描）。`contract` 与 `store` 作为纯逻辑层，被 `core` 与 `starter` 共享，更不应引入 Spring，以守住"零 Spring 边界"。
