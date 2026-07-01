# JNU Smart Edu

面向暨南大学教务场景的原生 Android 应用，使用 Kotlin、Jetpack Compose、Room 和 Material 3 开发。

## 主要功能

- 登录教务系统后一键同步培养方案、课程、课表和考试安排
- 周课表浏览、周数切换及桌面课表小组件
- 学分进度、课群完成情况和在修学分统计
- 待办任务与考试安排分区管理
- 基于本地课程数据和 DeepSeek API 的选课建议

## 本地构建

环境要求：JDK 17、Android SDK 35。项目已包含 Gradle Wrapper，无需单独安装 Gradle。

```powershell
$env:GRADLE_USER_HOME="$PWD\.gradle-user-home"
.\gradlew.bat --no-daemon assembleDebug testDebugUnitTest
```

调试 APK 输出到：

```text
.build/app/outputs/apk/debug/app-debug.apk
```

## 隐私说明

教务数据默认保存在设备本地。DeepSeek API Key 由用户在应用内填写，仓库不包含任何 API Key。

用于解析器调试的教务网页快照可能包含个人课表、成绩和考试信息，因此仅保存在开发者本机，不提交到 GitHub。部分 HTML 解析回归测试需要这些本地快照。
