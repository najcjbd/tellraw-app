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

## 🏗️ Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Architecture**: MVVM + Repository
- **DI**: Hilt
- **Database**: Room (SQLite)
- **Network**: Retrofit + OkHttp + GitHub API
- **JSON**: Gson
- **Build**: Gradle 8.1.2

## 🔄 Version Check

The app checks GitHub for new releases on startup:

1. **🕐 Smart Frequency**: Check once every 24 hours
2. **🔢 Semantic Version**: Compare versions correctly
3. **💾 Local Storage**: Save settings locally
4. **🌐 Error Handling**: Handle network errors
5. **🚫 User Control**: Can disable version check

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
    "§n" to "§6",  // material_copper -> gold (special)
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
Handle §m§n codes like Python version:
- Detect §m§n codes in text
- Offer two handling options
- Java font style vs color style

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