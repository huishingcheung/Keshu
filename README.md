# JNU Smart Edu

[简体中文](#简体中文) | [English](#english)

![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
[![Android CI](https://github.com/huishingcheung/JNU-Smart-Edu/actions/workflows/android.yml/badge.svg)](https://github.com/huishingcheung/JNU-Smart-Edu/actions/workflows/android.yml)
[![Download APK](https://img.shields.io/badge/Download-v0.2.1%20Preview-3F5FCE?logo=android&logoColor=white)](https://github.com/huishingcheung/JNU-Smart-Edu/releases/download/v0.2.1/JNU-Smart-Edu-v0.2.1-debug.apk)

## 简体中文

JNU Smart Edu 是一款面向暨南大学学生的 Android 教务助手，将课表、学分进度、待办任务、考试安排和选课建议集中在一个应用中。

### 下载

[直接下载 v0.2.1 测试版 APK](https://github.com/huishingcheung/JNU-Smart-Edu/releases/download/v0.2.1/JNU-Smart-Edu-v0.2.1-debug.apk) · [查看所有版本](https://github.com/huishingcheung/JNU-Smart-Edu/releases)

> 当前提供调试签名的预览版本，适合功能体验与测试。Android 安装时可能需要允许浏览器或文件管理器安装未知来源应用。

### 功能亮点

- **一键同步**：登录教务系统后，同步培养方案、课程、课表和考试安排
- **周课表**：完整展示一周课程，支持上下浏览、左右切换周数和学期管理
- **准确排课**：优先识别课程说明中的特殊见面课周次与周末上课时间
- **学分进度**：按培养方案课群展示要求学分、已修学分和在修学分
- **任务与考试**：待办任务支持添加、编辑、完成和删除，考试安排独立展示
- **智能选课建议**：结合未完成课群、已修课程和在修课程，通过 DeepSeek 生成可读的选课建议
- **全新界面**：采用 Material 3 深色视觉、紧凑卡片和统一的 Indigo/Mint 配色
- **桌面小组件**：直接完成待办任务，或查看每日课程与最近考试

### 快速开始

1. 构建并安装应用（Android 8.0 或更高版本）。
2. 在同步页面登录学校教务系统。
3. 点击“一键同步”，等待应用自动导入教务数据并返回首页。
4. 如需智能选课建议，在 AI 页面填写自己的 DeepSeek API Key 后点击“生成建议”。

### 技术栈

- Kotlin
- Jetpack Compose + Material 3
- Room
- Retrofit + OkHttp + Moshi
- WorkManager
- Android App Widgets（RemoteViews）

### 从源码构建

需要 JDK 17、Android SDK 35 和 Android Studio。项目包含 Gradle Wrapper，无需单独安装 Gradle。

```powershell
git clone https://github.com/huishingcheung/JNU-Smart-Edu.git
cd JNU-Smart-Edu
.\gradlew.bat assembleDebug
```

生成的调试 APK 位于：

```text
.build/app/outputs/apk/debug/app-debug.apk
```

### 声明

本项目是非官方学生项目，与暨南大学官方无隶属或授权关系。教务系统页面结构变化可能影响数据导入功能。

---

## English

JNU Smart Edu is an Android academic assistant for Jinan University students. It brings schedules, credit progress, tasks, exams, and course recommendations into one app.

### Download

[Download the v0.2.1 preview APK](https://github.com/huishingcheung/JNU-Smart-Edu/releases/download/v0.2.1/JNU-Smart-Edu-v0.2.1-debug.apk) · [View all releases](https://github.com/huishingcheung/JNU-Smart-Edu/releases)

> The current package is a debug-signed preview intended for evaluation and testing. Android may ask you to allow installations from your browser or file manager.

### Highlights

- **One-tap sync**: Sign in to the academic portal and import curriculum, courses, schedules, and exams
- **Weekly timetable**: View the full week, scroll vertically, swipe between weeks, and manage semesters
- **Accurate scheduling**: Prioritize special meeting weeks and weekend sessions described in course notes
- **Credit progress**: Track required, completed, and in-progress credits by curriculum group
- **Tasks and exams**: Add, edit, complete, and delete tasks while keeping exam arrangements separate
- **Smart course advice**: Use DeepSeek to generate readable recommendations based on unmet groups and completed or in-progress courses
- **Redesigned interface**: Material 3 dark surfaces, compact cards, and a consistent Indigo/Mint palette
- **Home-screen widgets**: Complete tasks directly or check today's classes and the nearest exam

### Getting Started

1. Build and install the app on Android 8.0 or later.
2. Sign in to the university academic portal from the sync page.
3. Tap **One-tap Sync** and wait for the app to import the data and return to the home screen.
4. For AI-assisted course recommendations, enter your own DeepSeek API key on the AI page and tap **Generate Advice**.

### Tech Stack

- Kotlin
- Jetpack Compose + Material 3
- Room
- Retrofit + OkHttp + Moshi
- WorkManager
- Android App Widgets (RemoteViews)

### Build from Source

Install JDK 17, Android SDK 35, and Android Studio. The Gradle Wrapper is included, so a separate Gradle installation is not required.

```powershell
git clone https://github.com/huishingcheung/JNU-Smart-Edu.git
cd JNU-Smart-Edu
.\gradlew.bat assembleDebug
```

The debug APK is generated at:

```text
.build/app/outputs/apk/debug/app-debug.apk
```

### Disclaimer

This is an unofficial student project and is not affiliated with or endorsed by Jinan University. Changes to the university portal may affect data import functionality.
