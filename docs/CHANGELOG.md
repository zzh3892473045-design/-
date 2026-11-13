# Workspace Client 更新说明

## 0.2.0（相对仓库提交 26a96df）

- 两处编译修复：`ClientAdapters.isKeyDown` 改为 1.21.11 的 `Window` API；`ConfigManager` 加载改为合法的 `IOException | RuntimeException` 捕获。
- ClickGUI 三项 P0：关闭键跟随当前绑定、模块绑定与原版控制菜单同步、颜色标签可读并增加不透明度滑条。
- HUD / 准星五项打磨：与 ClickGUI 统一主题、左栈与省略号、模块列表与 Zoom 显示规则、F3 时隐藏 HUD、自定义准星描边与原版隐藏条件。Boss 条避让使用 `BossHealthOverlayAccessor`，不走反射。自定义准星不复刻原版攻击指示器。
- Gradle wrapper 升到 9.7.1；文档写明跑 Gradle 用 JDK 25，编译仍是 21。
- 下一批第一波：Notifications，以及 HUD 加深（朝向、护甲、效果）。
- 下一批第二波：ToggleSneak、AutoRespawn（按原版流程：载具上不强制潜行下车，重生走原版确认且不少于 20 tick）。

2026-11-14 02:49（北京时间）冻结的可安装复测包（干净包 sha256 `1fc6bf0b597b85a6a897fa22aa7fd08cc4a450c037ae6d4f63d1fcdcf0ab3928`）已离线入机，结论是「有条件可交付」，**不含**上述第一波 / 第二波。产物哈希见 `docs/RELEASE-NOTES.md`。构建产物不入库。
