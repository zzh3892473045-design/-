# Workspace Client 交接

- 项目名称：Workspace Client（占位名，未改）
- 仓库地址：https://github.com/zzh3892473045-design/-
- 本地路径（计划）：C:\Users\Administrator\Documents\workspace-client
- 维护状态：主线，里程碑 2–5（模块框架、ClickGUI / HUD、安全模块、双产物接线）
- 游戏版本与加载器：MC 1.21.11 Fabric，Loader 0.19.5，Fabric API 0.141.6+1.21.11
- 语言与构建：编译目标 Java 21、Gradle 9.7.1（wrapper）、Fabric Loom 1.18.4、官方 Mojang 映射（`loom.officialMojangMappings()`）。跑 Gradle 用 JDK 25，编译 toolchain 仍是 21。
- 目标平台：先 Windows 桌面；Pojav 后续再适配
- 上次更新（北京时间）：2026-11-14

## 构建与发布

- 干净构建：`./gradlew build` 或 `.\gradlew.bat build`（`JAVA_HOME` 指向 JDK 25；编译 toolchain 仍是 21）
- 可选混淆：`./gradlew obfJar`（**不**挂在 `build` 上，同样用 JDK 25 跑 Gradle）
- 国内镜像：见 `docs/build-mirrors.md`
- 产物路径：`build/libs/`
  - clean：`workspace-client-0.2.0.jar`
  - 源码：`workspace-client-0.2.0-sources.jar`（Loom `withSourcesJar()`，原本就有）
  - 混淆：`workspace-client-0.2.0-obf.jar`（`obfJar` 任务 + `proguard.conf`）
- 明确不要改动的构建结构：不要加入 Baritone、ViaFabricPlus、DevAuth 或其他第三方客户端依赖；改版本号时同步 `gradle.properties`（`fabric.mod.json` 由 `processResources` 展开）
- 已知构建问题：中国大陆访问 `maven.fabricmc.net` 可能失败
- **构建环境：** 仓库 wrapper 为 Gradle 9.7.1。跑 `./gradlew` / `.\gradlew.bat` 必须用 JDK 25；编译 toolchain 仍是 21。不要用 JDK 21 启动 Gradle（Loom 1.18.4 会在配置期失败）。
- **正式复测包（2026-11-14 02:49:28 Asia/Shanghai 快照，已冻结）：** 已纳入两处编译修复、ClickGUI P0、HUD/准星 P0（含已收口的 `BossHealthOverlayAccessor`，Boss 条避让不再走反射）。干净包与 `obfJar` 均用仓库自己的 `./gradlew`（9.7.1）打出。产物目录：`/workspace/workspace-client-0.2.0-retest-build/build/libs/`。干净包 `workspace-client-0.2.0.jar` sha256 `1fc6bf0b597b85a6a897fa22aa7fd08cc4a450c037ae6d4f63d1fcdcf0ab3928`，已定为「有条件可交付」；混淆包 `workspace-client-0.2.0-obf.jar` sha256 `33f660e40e88b2c6029f05fc3236fa44d62094e9bcc4d60bb300dec65f1ee997`。已知限制：自定义准星不复刻原版攻击指示器（热键栏指示器仍走原版），见 `/workspace/reports/workspace-client-0.2.0-RELEASE-NOTES.md`。下一批第一波/第二波不在本包；不要用当前工作树重打来替换这份正式产物。02:43 那批产物仅作中间记录。

## 设计要点

- 事件总线：`EventBus` 在客户端线程同步派发；`subscribe` / `unsubscribe` / `post`；`CancellableEvent` 可取消但默认仍送达全部监听者。至少有 `ClientTickEvent` 与 `HudRenderEvent`。模块用 `Module.listen(...)` 注册，仅在启用时挂到总线上。
- 配置路径：`FabricLoader.getConfigDir()` 下的 `workspace-client/config.json`，即运行目录中的 `config/workspace-client/config.json`。Gson 使用游戏自带的 classpath，无网络。变更后约 20 tick（约 1 秒）防抖写入，游戏停止时立即保存。文件损坏时记日志并回退默认值，不崩溃。ClickGUI 的启用状态不持久化为 true，避免下次启动自动弹窗。
- 默认打开 ClickGUI：Right Shift。同时注册了 Fabric `KeyMapping`（原版控制菜单可见）和模块自身的绑定。关闭：Escape 或同一绑定。聊天或文本框聚焦时不捕获按键。
- HUD：通过 `HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ...)` 注册，继承原版 `hideGui`。不使用已弃用的 `HudRenderCallback`。

## 模块清单

| 模块 | 职责 | 关键类或目录 | 状态 |
| --- | --- | --- | --- |
| 客户端入口 | 接线：总线、模块、配置、按键、HUD 层 | `dev.workspace.client.WorkspaceClient` | 已写源码，未在本机构建验证 |
| 事件总线 | 订阅 / 退订 / 派发，可取消事件 | `dev.workspace.client.event.*` | 已写源码，未在本机构建验证 |
| 模块框架 | 名称、分类、描述、开关、绑定、设置、生命周期 | `module.Module`、`ModuleManager`、`Category` | 已写源码，未在本机构建验证 |
| 设置 | Boolean、Number（min/max/step）、Enum、Color（ARGB） | `dev.workspace.client.setting.*` | 已写源码，未在本机构建验证 |
| 配置 | 本地 JSON 读写与防抖 | `config.ConfigManager` | 已写源码，未在本机构建验证 |
| 按键 | Fabric KeyMapping + 原始 GLFW | `input.KeybindManager`、`adapter.ClientAdapters` | 已写源码，未在本机构建验证 |
| ClickGUI | 自制 Screen，分类面板可拖动，展开设置 | `gui.ClickGuiScreen`、`CategoryPanel`、`ClickGuiTheme` | 已写源码，未在本机构建验证 |
| HUD | 水印、FPS、坐标、模块列表（均可开关，含缩放和颜色） | `module.impl.HudModule`、`hud.HudRenderer` | 已写源码，未在本机构建验证 |
| Fullbright | 在光贴图更新期间临时提高 gamma，禁用即恢复，不写 options.txt | `module.impl.FullbrightModule` + `mixin.LightTextureMixin` | 已写源码；效果依赖 mixin 目标方法仍读取 gamma，见下方不确定项 |
| Sprint | 原版风格前进时自动冲刺，尊重饥饿与潜行，无绕过 | `module.impl.SprintModule` | 已写源码，未在本机构建验证 |
| Zoom | 启用或按住时覆盖世界 FOV，禁用即恢复，不写 FOV 选项 | `module.impl.ZoomModule` + `mixin.GameRendererMixin` | 已写源码；见下方不确定项 |
| Crosshair | 纯客户端自定义准星与可选命中闪烁 | `module.impl.CrosshairModule`（`replaceElement(CROSSHAIR)`） | 已写源码，未在本机构建验证 |
| 混淆 | 只混淆模组包，保留入口与 mixin | `proguard.conf`、`obfJar` 任务 | 已在正式复测包跑通 |

默认：HUD 启用；其余关闭。Zoom 默认键为 C，模式默认为 Toggle。Sprint / Fullbright / Crosshair 默认无绑定，在 ClickGUI 里设置。

## 包与类地图

```
dev.workspace.client.WorkspaceClient
dev.workspace.client.event.{EventBus, EventSubscription, CancellableEvent, ClientTickEvent, HudRenderEvent}
dev.workspace.client.module.{Category, Module, ModuleManager}
dev.workspace.client.module.impl.{ClickGuiModule, HudModule, FullbrightModule, SprintModule, ZoomModule, CrosshairModule}
dev.workspace.client.setting.{Setting, BooleanSetting, NumberSetting, EnumSetting, ColorSetting}
dev.workspace.client.config.ConfigManager
dev.workspace.client.input.KeybindManager
dev.workspace.client.gui.{ClickGuiScreen, CategoryPanel, ClickGuiTheme}
dev.workspace.client.hud.HudRenderer
dev.workspace.client.adapter.ClientAdapters
dev.workspace.client.mixin.{OptionInstanceAccessor, LightTextureMixin, GameRendererMixin}
```

## 配置与安全

- 配置存储：`config/workspace-client/config.json`（已实现；损坏则日志 + 默认值）
- 网络：仅只读入站、仅本地输出（本版本不读数据包，无 HTTP / webhook / Discord）
- 不包含 HWID、后门或外传逻辑：已按源码核对
- 无 COMBAT / EXPLOIT 分类，无光环、自瞄、绕过、飞行、加速、踏水、无摔落、脚手架

## 映射与 API 不确定项

以下名称已对照 mappings.dev 1.21.11 官方映射、Fabric 文档或 Fabric API 0.141.6 源码；仍建议在本机第一次编译时核对。不确定的调用尽量集中在 `ClientAdapters` 与两个 mixin 中。

**已对照、较有把握：**

- HUD：`HudElementRegistry` / `VanillaHudElements` / `HudElement`（`GuiGraphics`, `DeltaTracker`）。`HudRenderCallback` 自 Fabric API 0.116 起弃用，未使用。
- 刻与关闭：`ClientTickEvents.END_CLIENT_TICK`、`ClientLifecycleEvents.CLIENT_STOPPING`，参数类型为官方 `Minecraft`。
- 按键：`KeyMapping`、`KeyMapping.Category.register(Identifier)`、`KeyBindingHelper.registerKeyBinding`、`InputConstants.Type.KEYSYM`、`consumeClick` / `isDown`。
- 标识符：官方 1.21.11 使用 `net.minecraft.resources.Identifier`（`fromNamespaceAndPath`）。
- Screen：`render(GuiGraphics, int, int, float)`；`keyPressed(KeyEvent)`；`mouseClicked(MouseButtonEvent, boolean)`；`mouseDragged(MouseButtonEvent, double, double)`；`mouseReleased(MouseButtonEvent)`。`KeyEvent.key()`、`MouseButtonEvent.x()/y()/button()` 已确认。
- Screen 字段：`minecraft`、`font`、`width`、`height`；`init()`、`onClose()`、`shouldCloseOnEsc()`、`isPauseScreen()`、`removed()`。
- `Minecraft.getInstance()`、`setScreen`、`getFps()`、`player`、`options`、`screen`、`font`、`level`、`crosshairPickEntity`。
- `Options.gamma()` → `OptionInstance<Double>`（gamma 在 1.21.11 **仍存在**）；`Options.fov()`；`Options.keyUp`；`OptionInstance.get()` / `set()`；字段官方名为 `value`。
- `GuiGraphics.fill`、`drawString`、`renderOutline`、`pose()`（返回 `Matrix3x2fStack`）、`guiWidth()` / `guiHeight()`。文本颜色需 ARGB。
- `LightTexture.updateLightTexture(float)`、`GameRenderer.getFov(Camera, float, boolean)`（private，官方名如此）。
- `ContainerEventHandler.getFocused()`。

**需在本机编译或进游戏后确认：**

- `LightTexture.updateLightTexture` 在 1.21.11 是否仍在方法体内读取 `options.gamma().get()`。若光贴图已改为只走 GPU、不再读该值，Fullbright 将没有可见效果，需要改 mixin 注入点。当前做法是在 HEAD 用 accessor 临时写入 `value`，在 RETURN 恢复，避免污染 `options.txt`。
- `GameRenderer.getFov` 的第三个布尔参数被当作「是否使用 FOV 设置」；为 false 时不覆盖（避免改手部 FOV）。若语义相反，Zoom 可能只对手部生效或相反。
- `AbstractSignEditScreen`、`BookEditScreen`、`EditBox` 的包名按长期官方名使用，本轮未重新打开源码。
- `Player.getFoodData()` / `FoodData.getFoodLevel()` / `Player.isCrouching()` / `Abilities.instabuild`：按近年官方名，用于 Sprint 的原版限制。
- `LivingEntity.hurtTime` 公共字段：用于命中闪烁的上升沿检测。
- `Font.width(String)` 与 `Font.lineHeight`。
- `InputConstants.isKeyDown(Window, int)`（1.21.11 官方；旧 `isKeyDown(long, int)` 已不存在）。适配为 `Minecraft.getWindow()`，见 `ClientAdapters.isKeyDown`。`Window.handle()` 官方名仍为 `handle()`。
- `OptionInstance` 泛型类上的 `@Accessor("value")` 在 Loom 官方映射下是否一次通过。
- `Matrix3x2fStack.pushMatrix()` / `popMatrix()` / `scale(float, float)`（JOML 惯例）。
- 原版控制菜单里的 ClickGUI `KeyMapping` 与模块绑定若被分别改掉，两者都会打开界面；本轮没有调用可能名为 `setKey` 的 API 去同步它们。
- `obfJar`：ProGuard 7.7.0 已在正式复测包跑通（保留入口与全部四个 mixin）。需要 JDK 的 `jmods` 作为 `-libraryjars`。只做混淆、不收缩，以便在缺少 Minecraft library jar 时仍能改名。

## 已知限制与待办

- 已知限制：自定义准星不复刻原版攻击指示器（热键栏指示器仍走原版）。产品名仍是占位。
- 正式复测包的干净包已定为「有条件可交付」；`obfJar` 已跑通，入机复测已由测试岗完成。下一批第一波/第二波不在该包中，勿用当前工作树重打替换。2026-11-14 已在本箱用官方 1.21.11 映射 jar 核对注入点，并 `compileJava` 通过（Gradle 9.7.1 + JDK 25/21）。`isKeyDown` 已按 `Window` 重载适配。详情见 `/workspace/reports/workspace-client-12111-render-adapt.md`。
- 不加的内容（有意）：战斗模块、移动作弊、任何网络外传。
