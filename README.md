# Workspace Client

从零开始的 Minecraft Fabric 客户端，不是 Mio 或 Zio 的再发行。当前目标版本是 Minecraft 1.21.11、Fabric Loader 0.19.5、Fabric API 0.141.6+1.21.11、Java 21。包名为 `dev.workspace.client`，版本号 `0.2.0`，产品名仍是占位「Workspace Client」。

## 功能

- 模块框架、事件总线、按键绑定与本地 JSON 配置
- 自制 ClickGUI（分类面板、开关、滑条、枚举、按键捕获，可拖动）
- 游戏内 HUD：水印「Workspace Client」、FPS、坐标、已启用模块列表，以及朝向、护甲、状态效果
- 安全的视觉 / 辅助模块：Fullbright、Sprint、Zoom、Crosshair（含可选命中闪烁）、Notifications、ToggleSneak、AutoRespawn
- 双产物：Loom 的 clean 映射 jar + sources jar；另有可选的 ProGuard 混淆 jar

默认打开 ClickGUI 的按键是 **Right Shift**。也可以在原版「控制」菜单或 ClickGUI 里改绑定。Escape 或同一绑定可关闭界面。

## 边界

- 配置只写在游戏配置目录：`config/workspace-client/config.json`。无网络 I/O。
- 只读入站包（本版本不处理数据包）；输出仅本地（日志、本地配置、屏幕 HUD）。
- 不加 HWID、后门、许可证校验、webhook、Discord 或外传逻辑。
- 不加战斗光环、自瞄、Anti-Cheat 绕过、假延迟、崩溃器或漏洞模块。
- 分类仅有 RENDER、PLAYER、MISC、CLIENT；没有 COMBAT 或 EXPLOIT。
- 不纳入破解、泄漏或反编译产物。发布时同时保留 clean 版、混淆版和源码。

## 构建

跑 Gradle（`./gradlew` / `.\gradlew.bat`）需要 **JDK 25**。编译 toolchain 仍是 **Java 21**（`options.release = 21`）。不要用 JDK 21 作为 `JAVA_HOME` 启动 Gradle，否则 Fabric Loom 1.18.4 会在配置期失败。仓库 wrapper 为 Gradle 9.7.1；Loom 版本不要改，仍为 1.18.4。

在仓库根目录执行：

```bash
./gradlew build
```

Windows 可用：

```bat
.\gradlew.bat build
```

clean 版与源码 jar 在 `build/libs/`（例如 `workspace-client-0.2.0.jar` 与 `-sources.jar`）。`build` **不会**运行混淆，因此 ProGuard 失败也不会挡住 clean 产物。

可选混淆（只混淆 `dev.workspace.client.**`，保留入口类与 mixin 类名）：

```bash
./gradlew obfJar
```

产物为 `build/libs/workspace-client-0.2.0-obf.jar`。规则在 `proguard.conf`。若本机未在此任务中成功构建，见 `docs/handoff.md`。

若 `maven.fabricmc.net` 访问失败，见 `docs/build-mirrors.md`。

## 当前进度

里程碑 2–5 已落地：模块框架与配置、ClickGUI / HUD、安全视觉模块、双产物 Gradle 接线。后续已纳入 Notifications、HUD 朝向/护甲/效果、ToggleSneak、AutoRespawn，以及 Gradle wrapper 9.7.1。发布说明见 `docs/RELEASE-NOTES.md` 与 `docs/CHANGELOG.md`，交接与构建注意见 `docs/handoff.md`。本构建环境可能没有 JDK 或无法访问 Fabric Maven，不以本文声称「已在本机构建成功」。自定义准星不复刻原版攻击指示器。
