# Tellraw Generator Android App

## 项目简介

这是一个将原Python版tellraw.py转换为Android应用程序的项目，完整实现了所有原版功能，包括选择器转换、文本格式化、命令生成等。

## 功能特性

### 核心功能
- **选择器转换**: Java版与基岩版选择器互转
- **文本格式化**: 支持§颜色代码和格式代码
- **命令生成**: 生成Java版和基岩版tellraw命令
- **智能检测**: 自动检测选择器类型和文本格式
- **提醒系统**: 转换警告和提示信息

### 高级功能
- **云端同步**: 命令云端存储和同步
- **历史记录**: 本地命令历史保存

## 技术架构

### 技术栈
- **语言**: Kotlin
- **UI框架**: Jetpack Compose
- **架构**: MVVM + Repository模式
- **依赖注入**: Hilt
- **数据库**: Room
- **网络**: Retrofit + OkHttp
- **构建工具**: Gradle

### 项目结构
```
app/
├── src/main/java/com/tellraw/app/
│   ├── data/           # 数据层
│   │   ├── local/      # 本地数据库
│   │   ├── remote/     # 网络API
│   │   └── repository/ # 数据仓库
│   ├── di/             # 依赖注入
│   ├── model/          # 数据模型
│   ├── ui/             # UI层
│   │   ├── components/ # UI组件
│   │   ├── navigation/ # 导航
│   │   ├── screens/    # 页面
│   │   └── viewmodel/  # ViewModel
│   ├── util/           # 工具类
│   └── TellrawApplication.kt
└── src/main/res/       # 资源文件
```

## 构建说明

### 环境要求
- Android Studio Arctic Fox 或更高版本
- JDK 17 或更高版本
- Android SDK API 24+ (Android 7.0)
- Gradle 8.1+

### 构建步骤
1. 克隆项目到本地
2. 使用Android Studio打开项目
3. 等待Gradle同步完成
4. 连接Android设备或启动模拟器
5. 点击运行按钮构建并安装应用

### 云端构建配置
项目支持云端构建，配置了以下CI/CD工具：

#### GitHub Actions
```yaml
name: Android CI
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    - name: Set up JDK 11
      uses: actions/setup-java@v3
      with:
        java-version: '11'
        distribution: 'temurin'
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    - name: Build with Gradle
      run: ./gradlew build
```

## 功能实现细节

### 选择器转换
完整实现了Python版的选择器转换逻辑：

#### Java版特有参数
- distance, x_rotation, y_rotation
- nbt, team, limit, sort
- predicate, advancements, level
- gamemode, attributes

#### 基岩版特有参数
- r, rm, rx, rxm, ry, rym
- hasitem, family, l, lm
- m, haspermission, has_property, c

### 文本格式化
支持所有Minecraft颜色代码和格式代码：

#### 颜色代码
- §0-§f: 标准颜色
- §g-§v: 基岩版特有颜色

#### 格式代码
- §l: 粗体
- §m: 删除线 (仅Java版)
- §n: 下划线 (仅Java版)
- §o: 斜体
- §k: 混乱字
- §r: 重置

### §m§n代码处理
实现了Python版的§m§n代码处理逻辑：
- 检测文本中的§m§n代码
- 提供两种处理方式选择
- Java版字体方式 vs 颜色代码方式

## 测试

### 单元测试
```bash
./gradlew test
```

### UI测试
```bash
./gradlew connectedAndroidTest
```

### 代码覆盖率
```bash
./gradlew jacocoTestReport
```

## 部署

### 发布版本
```bash
./gradlew assembleRelease
```

### 调试版本
```bash
./gradlew assembleDebug
```

## 许可证

本项目基于原tellraw.py项目，遵循相同的许可证。

## 贡献

欢迎提交Issue和Pull Request来改进这个项目。

## 联系方式

如有问题或建议，请通过以下方式联系：
- 提交GitHub Issue
- 发送邮件至项目维护者

---

**注意**: By AI🤖🤖🤖