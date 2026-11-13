# Workspace Client 交接

- 项目名称：Workspace Client（占位名，未改）
- 仓库地址：https://github.com/zzh3892473045-design/-
- 本地路径（计划）：C:\Users\Administrator\Documents\workspace-client
- 维护状态：主线，里程碑 2–5（模块框架、ClickGUI / HUD、安全模块、双产物接线）
- 游戏版本与加载器：MC 1.21.11 Fabric，Loader 0.19.5，Fabric API 0.141.6+1.21.11
- 语言与构建：Java 21、Gradle、Fabric Loom 1.18.4、官方 Mojang 映射（`loom.officialMojangMappings()`）
- 目标平台：先 Windows 桌面；Pojav 后续再适配
- 上次更新（北京时间）：2026-11-14

## 构建与发布

- 干净构建：`./gradlew build` 或 `.\gradlew.bat build`
- 可选混淆：`./gradlew obfJar`（**不**挂在 `build` 上）
- 国内镜像：见 `docs/build-mirrors.md`
- 产物路径：`build/libs/`
  - clean：`workspace-client-0.2.0.jar`
  - 源码：`workspace-client-0.2.0-sources.jar`（Loom `withSourcesJar()`，原本就有）
  - 混淆：`workspace-client-0.2.0-obf.jar`（`obfJar` 任务 + `proguard.conf`）
- 明确不要改动的构建结构：不要加入 Baritone、ViaFabricPlus、DevAuth 或其他第三方客户端依赖；改版本号时同步 `gradle.properties`（`fabric.mod.json` 由 `processResources` 展开）
- 已知构建问题：中国大陆访问 `maven.fabricmc.net` 可能失败
- **构建验证状态：未在本机构建验证。** 本箱没有预装 JDK（`java` / `javac` 均不存在），因此没有运行 `./gradlew build` 或 `obfJar`。源码已按 1.21.11 官方映射与 Fabric API 文档写完整，但编译与混淆结果需在装有 JDK 21 且能下载依赖的机器上确认。

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
| 混淆 | 只混淆模组包，保留入口与 mixin | `proguard.conf`、`obfJar` 任务 | 接线已提交，未在本机运行验证 |

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
- `InputConstants.isKeyDown(long, int)`；窗口句柄用 `GLFW.glfwGetCurrentContext()`，不依赖 `Window.handle()` 名称。
- `OptionInstance` 泛型类上的 `@Accessor("value")` 在 Loom 官方映射下是否一次通过。
- `Matrix3x2fStack.pushMatrix()` / `popMatrix()` / `scale(float, float)`（JOML 惯例）。
- 原版控制菜单里的 ClickGUI `KeyMapping` 与模块绑定若被分别改掉，两者都会打开界面；本轮没有调用可能名为 `setKey` 的 API 去同步它们。
- `obfJar`：ProGuard 7.7.0 接线已写，但未在本机下载或执行。需要 JDK 的 `jmods` 作为 `-libraryjars`。只做混淆、不收缩，以便在缺少 Minecraft library jar 时仍能改名。

## 已知限制与待办

- 限制：产品名仍是占位；本环境未编译。
- 待办：在装有 JDK 21 的机器上跑 `./gradlew build` 与 `./gradlew obfJar`，并进游戏确认 Fullbright / Zoom mixin 生效。
- 不加的内容（有意）：战斗模块、移动作弊、任何网络外传。
