# Workspace Client

从零开始的 Minecraft Fabric 客户端骨架，不是 Mio 或 Zio 的再发行。当前目标版本是 Minecraft 1.21.11、Fabric Loader 0.19.5、Java 21。包名为 `dev.workspace.client`，版本号 `0.1.0`，产品名可随时改。

## 边界

- 只读入站包，输出仅本地。
- 不加 HWID、后门或外传逻辑。
- 不纳入破解、泄漏或反编译产物。
- 发布时同时保留 clean 版、混淆版和源码。

## 构建

需要 JDK 21。在仓库根目录执行：

```bash
./gradlew build
```

Windows 可用：

```bat
.\gradlew.bat build
```

产物在 `build/libs/`。若 `maven.fabricmc.net` 访问失败，见 `docs/build-mirrors.md`。

## 当前进度

第一里程碑：可构建的空壳客户端，启动时只打印初始化日志。模块框架、GUI 和视觉功能尚未实现。
