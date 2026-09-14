# Legacy application source

此目录保存 ACG Player X 1.x 的 Java/XML/Fragment 实现，仅用于功能核对、资源迁移和回归参考。

它已经不再映射为 Gradle 的 `:app` 模块，也不会参与默认 CI。当前活动应用位于 `../modern-app/`。

迁移旧功能时请将业务能力拆分到新架构中，不要重新启用此目录的旧 `build.gradle`、Fabric、ButterKnife、Kotlin Android Extensions、SlidingUpPanel 或 JCenter 依赖。
