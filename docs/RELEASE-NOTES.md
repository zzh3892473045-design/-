# Workspace Client 0.2.0 发布说明

- 版本：0.2.0（未升号）
- 游戏 / 加载器：Minecraft 1.21.11，Fabric Loader 0.19.5，Fabric API 0.141.6+1.21.11，Loom 1.18.4
- 跑 Gradle 需要 JDK 25，编译目标仍是 Java 21；仓库 wrapper 为 Gradle 9.7.1

本仓库当前源码在 0.2.0 正式复测包的基础上，已包含下一批第一波（Notifications、HUD 朝向/护甲/效果）和第二波（ToggleSneak、AutoRespawn），以及 ClickGUI / HUD / 准星 P0 修复。构建产物不入库。

## 已知限制（交付时必须写明）

自定义准星不复刻原版攻击指示器。开启自定义准星并接管准星层时，不会在准星上绘制原版攻击指示器（满格 / 进度条）。热键栏上的攻击指示器仍走原版。

## 2026-11-14 02:49（北京时间）正式复测包

该包已离线入机，结论是「有条件可交付」。它包含两处编译修复、ClickGUI P0、HUD / 准星 P0（含 `BossHealthOverlayAccessor`）、Gradle 9.7.1 wrapper。**不含**下一批第一波 / 第二波。下面哈希只对这份冻结产物有效，不要用后续工作树重打来替换。

| 产物 | 文件 | 字节 | sha256 |
| --- | --- | ---: | --- |
| 可安装干净包 | `workspace-client-0.2.0.jar` | 73589 | `1fc6bf0b597b85a6a897fa22aa7fd08cc4a450c037ae6d4f63d1fcdcf0ab3928` |
| 源码包 | `workspace-client-0.2.0-sources.jar` | 39853 | `2e9fba73075e54d33e82764229edb665029f5556c359f238974dc1b015dd486d` |
| 混淆包 | `workspace-client-0.2.0-obf.jar` | 45663 | `33f660e40e88b2c6029f05fc3236fa44d62094e9bcc4d60bb300dec65f1ee997` |

测试岗建议的发布前补测（当时未作为强制条件）：坏配置回退、完全退出再进的持久化、Sprint 在饥饿和潜行时的表现，以及睡觉或旁观时隐藏准星。
