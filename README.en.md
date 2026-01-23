# Tellraw Generator Android App

**English** | [中文](README.md)

## About

This is an Android app that converts the Python version of tellraw.py. It has all the features of the original, including selector conversion, text formatting, and command generation. The app can check for new releases from GitHub automatically.

## 🚀 Main Features

### Core Features
- **🔄 Selector Conversion**: Convert between Java and Bedrock selectors
- **🎨 Text Formatting**: Support § color codes and format codes
- **⚡ Command Generation**: Generate Java and Bedrock tellraw commands
- **🔍 Smart Detection**: Auto-detect selector type and text format
- **⚠️ Warnings**: Show conversion warnings and tips

### Version Management
- **🔄 Auto Update Check**: Check GitHub for new releases
- **🧠 Smart Version Compare**: Use semantic versioning
- **📢 Update Alerts**: Show detailed update dialog
- **🌐 One-Click Download**: Open GitHub Releases page
- **⚙️ Check Control**: Can disable version check

### User Experience
- **📋 Copy & Share**: Copy commands or share to other apps
- **📚 History**: Save and manage command history
- **🎨 Quick Color Input**: Built-in color code picker
- **📱 Responsive Design**: Works on different screen sizes
- **💾 File Export**: Export history to custom location (SAF) or app sandbox

## 🏗️ Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Architecture**: MVVM + Repository
- **DI**: Hilt
- **Database**: Room (SQLite)
- **Network**: Retrofit + OkHttp + GitHub API
- **JSON**: Gson
- **Build**: Gradle 8.1.2

### Project Architecture
```
📱 Android App
├── 🎯 UI Layer (Jetpack Compose)
│   ├── 📱 MainScreen (Main Screen)
│   ├── 📱 HelpScreen (Help Page)
│   └── 🎨 Components (UI Components)
│       ├── 🔔 UpdateDialog (Update Notification)
│       ├── 📋 CommandResults (Command Display)
│       ├── 🎨 ColorCodeQuickInput (Color Code Input)
│       ├── 📱 MNCodeDialog (§m§n Code Handling)
│       ├── 📁 HistoryStorageSettingsDialog (History Storage Settings)
│       └── 📝 FilenameInputDialog (Filename Input)
│
├── 🧠 ViewModel Layer (MVVM)
│   └── 📱 TellrawViewModel (Main Business Logic)
│       ├── 🔄 Command Generation Logic
│       ├── 📋 Copy & Share Functionality
│       ├── 🔄 Version Check Management
│       └── 💾 History Management
│
├── 📊 Repository Layer (Data Repository)
│   ├── 📱 TellrawRepository (Command Data)
│   ├── 🔄 VersionCheckRepository (Version Check)
│   └── ⚙️ SettingsRepository (Settings Management)
│
├── 🌐 Remote Layer (Network Layer)
│   ├── 📡 ApiService (Custom API)
│   └── 🐙 GithubApiService (GitHub API)
│
├── 💾 Local Layer (Local Storage)
│   └── 🗄️ AppDatabase (Room Database)
│       └── 📝 CommandHistory (History Records)
│
└── ⚙️ Util Layer (Utility Classes)
    ├── 🔄 TextFormatter (Text Formatting)
    ├── 🔄 SelectorConverter (Selector Conversion)
    └── 🎨 Components (UI Component Utilities)
```

## 🔄 Version Check

The app checks GitHub for new releases on startup:

1. **🕐 Smart Frequency**: Check once every 24 hours
2. **🔢 Semantic Version**: Compare versions correctly
3. **💾 Local Storage**: Save settings locally
4. **🌐 Error Handling**: Handle network errors
5. **🚫 User Control**: Can disable version check

### Configuration Management
- **✅ Enable/Disable**: Users can enable or disable version check anytime
- **💾 Local Storage**: Configuration saved in SharedPreferences and JSON file
- **📝 Version Record**: Record current version and last check time
- **⏰ Interval Control**: Default 24-hour check interval

## 🎯 Core Features

### Selector Conversion
Full Python version selector conversion logic:

#### Java-Only Parameters
```kotlin
JAVA_SPECIFIC_PARAMS = listOf(
    "distance", "x_rotation", "y_rotation", "nbt", "team", "limit", "sort", 
    "predicate", "advancements", "level", "gamemode", "attributes"
)
```

#### Bedrock-Only Parameters
```kotlin
BEDROCK_SPECIFIC_PARAMS = listOf(
    "r", "rm", "rx", "rxm", "ry", "rym", "hasitem", "family", "l", "lm", 
    "m", "haspermission", "has_property", "c"
)
```

### Text Formatting
Support all Minecraft color and format codes:

#### Color Codes
```kotlin
TEXT_COLOR_CODES = mapOf(
    "§g" to "§6",  // minecoin_gold -> gold
    "§h" to "§f",  // material_quartz -> white 
    "§i" to "§7",  // material_iron -> gray
    "§j" to "§8",  // material_netherite -> dark gray
    "§m" to "§4",  // material_redstone -> dark red (special)
    "§n" to "§c",  // material_copper -> red (special)
    "§p" to "§6",  // material_gold -> gold
    "§q" to "§a",  // material_emerald -> green
    "§s" to "§b",  // material_diamond -> aqua
    "§t" to "§1",  // material_lapis -> dark blue
    "§u" to "§d",  // material_amethyst -> light purple
    "§v" to "§6",  // material_resin -> gold
    // ... more color codes
)
```

#### Format Codes
- **§l**: Bold
- **§m**: Strikethrough (Java only)
- **§n**: Underline (Java only)
- **§o**: Italic
- **§k**: Obfuscated
- **§r**: Reset

### §m§n Code Handling
Three handling modes for §m§n codes:

#### Mode 1: Font Style (Default)
- **Java Edition**: Use font formatting codes (strikethrough/underline)
- **Bedrock Edition**: Use color codes (dark red/red)
- **Use Case**: Java needs font effects, Bedrock compatibility

#### Mode 2: Color Code Style
- **Java Edition**: Use color codes (dark red/red)
- **Bedrock Edition**: Use color codes (dark red/red)
- **Use Case**: Both versions need color effects

#### Mode 3: §m/§n_c/f Mode
- **Format**: §m_f (font), §m_c (color), §n_f (font), §n_c (color)
- **Feature**: Specify handling method for each §m/§n code individually
- **Use Case**: Fine-grained control over each code's handling

### Mixed Mode
Mixed mode allows selecting handling method for each §m/§n code:
- Input box displays original §m/§n codes
- Backend automatically converts to §m_f/§m_c/§n_f/§n_c
- Dialog pops up for each §m/§n input to select handling method

## 📚 History Management

### Local Storage
- Use Room database to store command history
- Support search, load, delete history records
- Auto-save each generated command

### File Export
- **SAF Support**: Support selecting export location through Storage Access Framework (SAF)
- **Sandbox Storage**: Default save to app sandbox if no location selected
- **File Handling**:
  - Auto-create if file doesn't exist
  - Prompt user to choose if file exists (append or customize filename)
  - Support custom filename (default: TellrawCommand.txt)
- **Format Standard**: Use txt text format, includes commands, time, etc.

### Storage Location
- **Select Directory**: Users can select any accessible directory through SAF
- **App Sandbox**: Default save to `Android/data/[package]/files/` directory
- **Configuration Persistence**: Storage settings saved locally, auto-load on next startup

## 🛠️ Build

### Requirements
- **IDE**: Android Studio Arctic Fox or higher
- **JDK**: JDK 17 or higher
- **Android SDK**: API 24+ (Android 7.0)
- **Gradle**: 8.1.2+
- **Kotlin**: 1.9.10+

### Steps
1. Clone the project
   ```bash
   git clone https://github.com/najcjbd/tellraw-app.git
   ```
2. Open in Android Studio
3. Wait for Gradle sync
4. Connect device or start emulator
5. Click run button

### Build Commands
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test

# Generate test report
./gradlew jacocoTestReport
```

### File Access
The app integrates MTDataFilesProvider, allowing MT Manager to access app private directory:

**How to Use**:
1. Build and install the app (debug version has it integrated)
2. Open MT Manager
3. Click "Add Local Storage" in the sidebar
4. Find and select this app in the app list
5. Click "Select" to access the app private directory

**Note**: File provider is only injected in debug version. To inject in release version, change `debugImplementation` to `implementation` in `app/build.gradle`.

### Cloud Build Configuration
Project supports GitHub Actions cloud build:

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

## 🧪 Test

### Unit Tests
```bash
./gradlew test
```

### Integration Tests
```bash
./gradlew connectedAndroidTest
```

### Test Coverage
```bash
./gradlew jacocoTestReport
```

## 📦 Deploy

### Build Variants
| Variant | Use | Command |
|---------|-----|---------|
| Debug | Development | `./gradlew assembleDebug` |
| Release | Production | `./gradlew assembleRelease` |
| Test | Testing | `./gradlew assembleDebugAndroidTest` |

### Bug Reports
- 🐛 Use GitHub Issues
- 📝 Provide detailed steps

### Project Links
- 🌐 **GitHub**: [najcjbd/tellraw-app](https://github.com/najcjbd/tellraw-app)
- 📱 **Download**: [GitHub Releases](https://github.com/najcjbd/tellraw-app/releases)
- 📧 **Issues**: [GitHub Issues](https://github.com/najcjbd/tellraw-app/issues)

---

**🤖 Developer**: By AI🤖🤖🤖