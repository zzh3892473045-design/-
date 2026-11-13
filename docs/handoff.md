# Workspace Client 交接

- 项目名称：Workspace Client（可改）
- 仓库地址：https://github.com/zzh3892473045-design/-
- 本地路径（计划）：C:\Users\Administrator\Documents\workspace-client
- 维护状态：主线，第一里程碑（工程骨架）
- 游戏版本与加载器：MC 1.21.11 Fabric
- 语言与构建：Java 21、Gradle、Fabric Loom 1.18.4
- 目标平台：先 Windows 桌面；Pojav 后续再适配

## 构建与发布

- 构建命令：`./gradlew build` 或 `.\gradlew.bat build`
- 国内镜像：见 `docs/build-mirrors.md`
- 产物路径：`build/libs/`（clean 版；混淆版尚未接入）
- 明确不要改动的构建结构：暂无历史约束，改版本号时同步 `gradle.properties` 与 `fabric.mod.json`
- 已知构建问题：中国大陆访问 `maven.fabricmc.net` 可能失败

## 模块清单

| 模块 | 职责 | 关键类或目录 | 状态 |
| --- | --- | --- | --- |
| 客户端入口 | 初始化日志 | `src/main/java/dev/workspace/client/WorkspaceClient.java` | 已确认 |

## 配置与安全

- 配置存储：尚未实现
- 网络：仅只读入站、仅本地输出（是）
- 不包含 HWID、后门或外传逻辑：已按空壳核对

## 已知限制与待办

- 限制：产品名和包名是占位，尚未做模块框架与 GUI
- 待办：里程碑 2 起做开关、配置持久化、事件总线
- 上次更新（北京时间）：2026-11-14
