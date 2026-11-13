# 构建镜像备忘

面向中国大陆网络。不存放凭证。

- GitHub 与 `maven.fabricmc.net` 常被阻断时，可改用腾讯云镜像、教育网镜像或 ghfast 等代理。
- 本机若仍有 `C:\Users\Administrator\fabric-mirror`，使用前先确认它是否有效、是否只给本机构建用。
- Gradle 发行包默认来自 `services.gradle.org`，不通时可把 `gradle/wrapper/gradle-wrapper.properties` 里的 `distributionUrl` 改成可用镜像。
- 具体可用的 `settings.gradle` 镜像地址待在本机构建时核实后再写入。
