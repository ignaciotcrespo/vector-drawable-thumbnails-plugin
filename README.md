# Vector Drawable Thumbnails Plugin

![Build](https://github.com/ignaciotcrespo/vector-drawable-thumbnails-plugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![JetBrains Plugins](https://img.shields.io/badge/JetBrains-Plugin-orange.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![Compatibility](https://img.shields.io/badge/IDE-2022.3%2B-blue.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)

A professional IntelliJ Platform plugin that displays thumbnail previews of Android Vector Drawable files in a convenient tool window. **Compatible with all JetBrains IDEs**.

[![Sponsor](https://img.shields.io/badge/Sponsor-%E2%9D%A4-pink?logo=github-sponsors)](https://github.com/sponsors/ignaciotcrespo)

**Other projects:**
- [Color Manipulation](https://github.com/ignaciotcrespo/color-manipulation-plugin) — Color toolkit for JetBrains IDEs with 30+ format conversions
- [GitShelf](https://github.com/ignaciotcrespo/gitshelf) — Changelists and shelves for git in the terminal

<!-- Plugin description -->
**Universal JetBrains IDE Compatibility** - Works seamlessly across all JetBrains products including IntelliJ IDEA, Android Studio, WebStorm, PyCharm, PhpStorm, and more.

**Key Features:**
- 🖼️ **Real-time Thumbnails**: Automatically generates and displays vector drawable previews
- 🔍 **Smart Filtering**: Filter vectors by name with real-time search
- 📊 **Flexible Sorting**: Sort by name, size, or modification date
- 🎯 **Universal Compatibility**: Works with all JetBrains IDEs (2022.3+)
- ⚡ **Performance Optimized**: Efficient caching and background processing
- 🏗️ **Professional Architecture**: Built with SOLID principles for maintainability

**How to use**: Go to menu View > Tool Windows > Vector Drawable Thumbnails

Perfect for Android developers, UI/UX designers, and anyone working with vector graphics in JetBrains IDEs.

<!-- Plugin description end -->

## 🎯 JetBrains IDE Compatibility

### ✅ Fully Supported IDEs

| IDE | Version Support | Special Features |
|-----|----------------|------------------|
| **IntelliJ IDEA Community** | 2022.3+ | Core functionality |
| **IntelliJ IDEA Ultimate** | 2022.3+ | Enhanced Android support |
| **Android Studio** | 2022.3+ | Native Android integration |
| **WebStorm** | 2022.3+ | Web project vector assets |
| **PyCharm Community** | 2022.3+ | Python project resources |
| **PyCharm Professional** | 2022.3+ | Full feature set |
| **PhpStorm** | 2022.3+ | PHP project assets |
| **RubyMine** | 2022.3+ | Ruby project resources |
| **CLion** | 2022.3+ | C/C++ project assets |
| **GoLand** | 2022.3+ | Go project resources |
| **DataGrip** | 2022.3+ | Database project assets |
| **Rider** | 2022.3+ | .NET project resources |
| **AppCode** | 2022.3+ | iOS project vectors |

### 🔧 Compatibility Features

- **Universal Platform Support**: Built on stable IntelliJ Platform APIs
- **Optional Dependencies**: Enhanced features for specific IDEs without breaking compatibility
- **Graceful Degradation**: Core functionality always available
- **Version Range**: Supports 80%+ of active JetBrains users (2022.3 - 2024.3+)
- **Automated Testing**: Verified across multiple IDE versions

## 🏗️ Architecture

This plugin has been professionally refactored to follow **SOLID principles**, making it scalable, maintainable, and testable.

### Layered Architecture
```
┌─────────────────────────────────────────┐
│           Presentation Layer            │
│     (UI Controllers, Tool Windows)      │
├─────────────────────────────────────────┤
│           Application Layer             │
│      (Business Logic, Services)        │
├─────────────────────────────────────────┤
│             Domain Layer                │
│    (Interfaces, Models, Contracts)     │
├─────────────────────────────────────────┤
│          Infrastructure Layer           │
│   (File System, Parsers, Repositories) │
└─────────────────────────────────────────┘
```

### ✅ SOLID Principles Compliance
- **Single Responsibility**: Each class has one clear purpose
- **Open/Closed**: Easy to extend without modifying existing code
- **Liskov Substitution**: Implementations are interchangeable
- **Interface Segregation**: Small, focused interfaces
- **Dependency Inversion**: Depends on abstractions, not concretions

### 🧪 Benefits
- **Testable**: Each component can be unit tested in isolation
- **Maintainable**: Clear separation of concerns
- **Scalable**: Easy to add new features and implementations
- **Flexible**: Components can be swapped and configured
- **Professional**: Enterprise-grade code quality

## 📦 Installation

### From JetBrains Marketplace (Recommended)
1. Open your JetBrains IDE
2. Go to <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd>
3. Search for **"Vector Drawable Thumbnails"**
4. Click <kbd>Install</kbd>
5. Restart your IDE

### Manual Installation
1. Download the [latest release](https://github.com/ignaciotcrespo/vector-drawable-thumbnails-plugin/releases/latest)
2. Go to <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>
3. Select the downloaded file
4. Restart your IDE

### Accessing the Plugin
After installation, access the plugin via:
- **Menu**: View > Tool Windows > Vector Drawable Thumbnails
- **Tool Window**: Look for the Vector Drawable Thumbnails tab (usually on the right side)

## 🚀 Development

### Prerequisites
- **JDK 17+**
- **Gradle 8.5+**
- **IntelliJ IDEA** (recommended for development)

### Quick Start
```bash
# Clone the repository
git clone https://github.com/ignaciotcrespo/vector-drawable-thumbnails-plugin.git
cd vector-drawable-thumbnails-plugin

# Run tests
./gradlew test

# Build the plugin
./gradlew buildPlugin

# Run in development mode
./gradlew runIde
```

### 🧪 Testing

#### Unit Tests
```bash
./gradlew test
```

#### Compatibility Testing
```bash
# Run comprehensive compatibility tests
./scripts/test-compatibility.sh

# Test specific IDE configurations
./gradlew runPluginVerifier
```

#### Manual Testing
```bash
# Test in different IDEs
./gradlew runIde                # IntelliJ IDEA
./gradlew runAndroidStudio      # Android Studio
./gradlew runWebStorm          # WebStorm
./gradlew runPyCharm           # PyCharm
```

### 🔧 Build Configuration

The plugin uses the latest IntelliJ Platform Gradle Plugin with enhanced compatibility features:

- **Multi-version Testing**: Automatically tests against multiple IDE versions
- **Plugin Verification**: Ensures compatibility with JetBrains standards
- **Dependency Management**: Optimized for minimal conflicts
- **Performance Monitoring**: Built-in performance testing

### 📊 Quality Assurance

- **Code Coverage**: Kover integration for coverage reports
- **Static Analysis**: Qodana integration for code quality
- **Compatibility Verification**: Automated testing across IDE versions
- **Performance Testing**: Memory and CPU usage monitoring

### Development Guidelines
1. Follow SOLID principles
2. Write comprehensive tests
3. Ensure compatibility across JetBrains IDEs
4. Update documentation

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
