# Tellraw Generator Android App

**中文** | [English](README.en.md)

## 项目简介

这是一个将原Python版tellraw.py转换为Android应用程序的项目，完整实现了所有原版功能，包括选择器转换、文本格式化、命令生成等。应用支持自动检查GitHub仓库的新版本发布，提供完整的版本管理体验。

## 🚀 主要功能

### 核心功能
- **🔄 选择器转换**: Java版与基岩版选择器互转
- **🎨 文本格式化**: 支持§颜色代码和格式代码
- **⚡ 命令生成**: 生成Java版和基岩版tellraw命令
- **🔍 智能检测**: 自动检测选择器类型和文本格式
- **⚠️ 提醒系统**: 转换警告和提示信息

### 版本管理功能
- **🔄 自动更新检查**: 自动检查GitHub仓库的最新Release版本
- **🧠 智能版本比较**: 使用语义化版本比较，避免误判相同版本
- **📢 更新通知**: 发现新版本时弹出详细通知对话框
- **🌐 一键下载**: 直接打开GitHub Releases页面下载新版本
- **⚙️ 检查控制**: 支持禁用版本检查功能，配置保存在本地沙盒

### 用户体验功能
- **📋 复制分享**: 一键复制命令到剪贴板或分享到其他应用
- **📚 历史记录**: 本地命令历史保存和管理
- **🎨 颜色代码快速输入**: 内置颜色代码快速选择器
- **📱 响应式设计**: 适配不同屏幕尺寸的设备
- **💾 文件导出**: 支持将历史记录导出到自定义位置（SAF）或应用沙盒

## 🏗️ 技术架构

### 技术栈
- **语言**: Kotlin 1.9.22
- **UI框架**: Jetpack Compose
- **架构**: MVVM + Repository模式
- **依赖注入**: Hilt 2.48
- **数据库**: Room (SQLite)
- **网络**: Retrofit 2.9.0 + OkHttp 4.12.0 + GitHub API
- **JSON处理**: Gson 2.10.1
- **构建工具**: Gradle 8.5 + Android Gradle Plugin 8.1.2

### 项目架构图
```
📱 Android App
├── 🎯 UI Layer (Jetpack Compose)
│   ├── 📱 MainScreen (主界面)
│   ├── 📱 HelpScreen (帮助页面)
│   └── 🎨 Components (UI组件)
│       ├── 🔔 UpdateDialog (更新通知)
│       ├── 📋 CommandResults (命令显示)
│       ├── 🎨 ColorCodeQuickInput (颜色代码输入)
│       ├── 📱 MNCodeDialog (§m§n代码处理)
│       ├── 📁 HistoryStorageSettingsDialog (历史记录存储设置)
│       └── 📝 FilenameInputDialog (文件名输入)
│
├── 🧠 ViewModel Layer (MVVM)
│   └── 📱 TellrawViewModel (主业务逻辑)
│       ├── 🔄 命令生成逻辑
│       ├── 📋 复制分享功能
│       ├── 🔄 版本检查管理
│       └── 💾 历史记录管理
│
├── 📊 Repository Layer (数据仓库)
│   ├── 📱 TellrawRepository (命令数据)
│   ├── 🔄 VersionCheckRepository (版本检查)
│   └── ⚙️ SettingsRepository (设置管理)
│
├── 🌐 Remote Layer (网络层)
│   ├── 📡 ApiService (自定义API)
│   └── 🐙 GithubApiService (GitHub API)
│
├── 💾 Local Layer (本地存储)
│   └── 🗄️ AppDatabase (Room数据库)
│       └── 📝 CommandHistory (历史记录)
│
└── ⚙️ Util Layer (工具类)
    ├── 🔄 TextFormatter (文本格式化)
    ├── 🔄 SelectorConverter (选择器转换)
    └── 🎨 Components (UI组件工具)
```

## 🔄 版本检查功能详解

### 检查机制
应用启动时会自动检查GitHub仓库的最新Release版本：

1. **🕐 智能频率控制**: 24小时内只检查一次，避免频繁网络请求
2. **🔢 语义化版本比较**: 准确比较版本号（主版本.次版本.修订号）
3. **💾 本地配置存储**: 检查设置和版本信息保存在应用沙盒中
4. **🌐 网络错误处理**: 优雅处理网络异常和服务器错误
5. **🚫 用户控制**: 支持完全禁用版本检查功能

### 配置管理
- **✅ 启用/禁用**: 用户可以随时启用或禁用版本检查
- **💾 本地存储**: 配置保存在SharedPreferences和JSON文件中
- **📝 版本记录**: 记录当前版本和最后检查时间
- **⏰ 间隔控制**: 默认24小时检查间隔

## 🎯 核心功能实现

### 选择器转换
完整实现了Python版的选择器转换逻辑：

#### Java版特有参数
```kotlin
JAVA_SPECIFIC_PARAMS = listOf(
    "distance", "x_rotation", "y_rotation", "nbt", "team", "limit", "sort", 
    "predicate", "advancements", "level", "gamemode", "attributes"
)
```

#### 基岩版特有参数
```kotlin
BEDROCK_SPECIFIC_PARAMS = listOf(
    "r", "rm", "rx", "rxm", "ry", "rym", "hasitem", "family", "l", "lm", 
    "m", "haspermission", "has_property", "c"
)
```

### 文本格式化
支持所有Minecraft颜色代码和格式代码：

#### 颜色代码映射
```kotlin
TEXT_COLOR_CODES = mapOf(
    "§g" to "§6",  // minecoin_gold -> 金色
    "§h" to "§f",  // material_quartz -> 白色 
    "§i" to "§7",  // material_iron -> 灰色
    "§j" to "§8",  // material_netherite -> 深灰色
    "§m" to "§4",  // material_redstone -> 深红色 (特殊处理)
    "§n" to "§c",  // material_copper -> 红色 (特殊处理)
    "§p" to "§6",  // material_gold -> 金色
    "§q" to "§a",  // material_emerald -> 绿色
    "§s" to "§b",  // material_diamond -> 青色
    "§t" to "§1",  // material_lapis -> 深蓝色
    "§u" to "§d",  // material_amethyst -> 粉色
    "§v" to "§6",  // material_resin -> 金色
    // ... 更多颜色代码
)
```

#### 格式代码
- **§l**: 粗体
- **§m**: 删除线 (仅Java版)
- **§n**: 下划线 (仅Java版)
- **§o**: 斜体
- **§k**: 混乱字
- **§r**: 重置

### §m§n代码处理
提供了三种§m§n代码处理方式：

#### 方式一：字体方式（默认）
- **Java版**: 使用字体格式化代码（删除线/下划线）
- **基岩版**: 使用颜色代码（深红色/红色）
- **适用场景**: Java版需要保留字体效果，基岩版兼容

#### 方式二：颜色代码方式
- **Java版**: 使用颜色代码（深红色/红色）
- **基岩版**: 使用颜色代码（深红色/红色）
- **适用场景**: 两版都需要颜色效果

#### 方式三：§m/§n_c/f模式
- **使用格式**: §m_f（字体方式）、§m_c（颜色方式）、§n_f（字体方式）、§n_c（颜色方式）
- **特点**: 为每个§m/§n代码单独指定处理方式
- **适用场景**: 需要精细控制每个代码的处理方式

### 混合模式
混合模式允许为每个§m/§n代码单独选择处理方式：
- 输入框显示原始§m/§n代码
- 后台自动转换为§m_f/§m_c/§n_f/§n_c
- 每次输入§m/§n时弹出对话框选择处理方式

## ⚠️ JAVA基岩混合模式提示

当前JAVA基岩混合模式并不健全，可能存在以下问题：
- 转换结果可能不完全准确
- 某些参数组合可能无法正确处理
- 提醒信息可能不完整

建议在正式使用前先测试转换结果，确保符合预期。

## 🔄 合并模式说明

当选择器参数出现多次时，应用会根据参数类型进行合并：

### 合并逻辑选择

应用支持两种合并逻辑模式：

**模式一：源代码合并逻辑（混合模式）**
- 适用于同时包含Java版和基岩版特有参数的复杂场景
- 范围参数（distance, x_rotation, y_rotation, level）：取所有最小值的最小值和所有最大值的最大值
- 示例：distance=5..7, distance=3..9 → distance=3..9

**模式二：新合并逻辑（默认）**
- 适用于大多数场景
- 范围参数（distance, x_rotation, y_rotation, level）：选取差的绝对值最大的范围
- 示例：distance=5..7 (差2), distance=3..9 (差6) → distance=3..9

### 参数合并规则

**选取最大值的参数**：x, y, z, dx, dy, dz, r, rx, ry, l, c, limit
- 示例：x=8, x=9.5, y=5, y=6 → x=9.5, y=6

**选取最小值的参数**：rm, rxm, rym, lm
- 示例：rm=1, rm=3.5, rxm=-5.5, rxm=-1 → rm=1, rxm=-5.5

**范围参数（JAVA版）**：distance, x_rotation, y_rotation, level
- 根据所选合并逻辑进行处理

**负数支持**：
- 可以触及负数的参数（范围相关）：rx, rxm, ry, rym, x_rotation, y_rotation
- 可以是负数的参数（值相关）：c, x, y, z, dx, dy, dz

### 合并时机

应用会在以下时机进行参数合并：
1. **转换前合并**：在参数转换之前合并输入的重复参数
2. **转换后合并**：在参数转换之后合并可能产生的重复参数

## 📚 历史记录管理

### 本地存储
- 使用Room数据库存储命令历史
- 支持搜索、加载、删除历史记录
- 自动保存每次生成的命令

### 文件导出
- **SAF支持**: 支持通过存储访问框架（SAF）选择导出位置
- **沙盒存储**: 未选择位置时默认保存到应用沙盒
- **文件处理**:
  - 文件不存在时自动创建
  - 文件存在时提示用户选择（追加或自定义文件名）
  - 支持自定义文件名（默认：TellrawCommand.txt）
- **格式规范**: 使用txt文本格式，包含命令、时间等信息

### 存储位置
- **选择目录**: 用户可通过SAF选择任意可访问的目录
- **应用沙盒**: 默认保存在`Android/data/[包名]/files/`目录
- **配置持久化**: 存储设置保存在本地，下次启动自动加载

## 🛠️ 构建说明

### 环境要求
- **IDE**: Android Studio Ladybug | 2024.2.1 或更高版本
- **JDK**: JDK 8 或更高版本（推荐 JDK 17+）
- **Android SDK**: API 24+ (Android 7.0)
- **Gradle**: 8.5+
- **Kotlin**: 1.9.22+

### 构建步骤
1. 📥 克隆项目到本地
   ```bash
   git clone https://github.com/najcjbd/tellraw-app.git
   ```
2. 📱 使用Android Studio打开项目
3. ⏳ 等待Gradle同步完成
4. 📱 连接Android设备或启动模拟器
5. ▶️ 点击运行按钮构建并安装应用

### 构建命令
```bash
# 调试版本
./gradlew assembleDebug

# 发布版本
./gradlew assembleRelease

# 运行测试
./gradlew test
```

### 文件访问
应用集成了 MTDataFilesProvider，允许 MT 管理器访问应用私有目录文件：

**使用方式**：
1. 编译并安装应用（debug 版本已自动集成）
2. 打开 MT 管理器
3. 在侧拉栏点击「添加本地存储」
4. 在应用列表中找到并选中此应用
5. 点击「选择」即可访问应用私有目录

**注意**：文件提供器仅在 debug 版本中注入。如需在 release 版本中也注入，在 `app/build.gradle` 中将 `debugImplementation` 改为 `implementation`。

### 云端构建配置
项目支持GitHub Actions云端构建：

```yaml
name: Android CI
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    - name: Build with Gradle
      run: ./gradlew test
```

## 🧪 测试

### 单元测试
```bash
./gradlew test
```

### 集成测试
```bash
./gradlew connectedAndroidTest
```

## 📦 部署

### 构建变体
| 变体 | 用途 | 命令 |
|------|------|------|
| Debug | 开发调试 | `./gradlew assembleDebug` |
| Release | 正式发布 | `./gradlew assembleRelease` |
| Test | 测试构建 | `./gradlew assembleDebugAndroidTest` |

### 签名配置
```kotlin
android {
    signingConfigs {
        release {
            storeFile file('path/to/keystore')
            storePassword 'password'
            keyAlias 'keyAlias'
            keyPassword 'keyPassword'
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
        }
    }
}
```

### 问题报告
- 🐛 使用GitHub Issues报告bug
- 📝 提供详细的重现步骤

### 项目地址
- 🌐 **GitHub仓库**: [najcjbd/tellraw-app](https://github.com/najcjbd/tellraw-app)
- 📱 **下载页面**: [GitHub Releases](https://github.com/najcjbd/tellraw-app/releases)
- 📧 **问题反馈**: [GitHub Issues](https://github.com/najcjbd/tellraw-app/issues)

## 🤖 关于本项目

本项目主要由AI（人工智能）辅助开发完成。AI在以下方面提供了重要支持：
- 核心功能实现
- 代码编写和优化
- 测试用例设计
- 文档编写

AI辅助开发大大提高了开发效率，使项目能够在短时间内完成并发布。

---

**🤖 开发团队**: By AI🤖🤖🤖