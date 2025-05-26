# vector-drawable-thumbnails-plugin

![Build](https://github.com/ignaciotcrespo/vector-drawable-thumbnails-plugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)

## Template ToDo list
- [x] Create a new [IntelliJ Platform Plugin Template][template] project.
- [ ] Verify the [pluginGroup](/gradle.properties), [plugin ID](/src/main/resources/META-INF/plugin.xml) and [sources package](/src/main/kotlin).
- [ ] Review the [Legal Agreements](https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html).
- [ ] [Publish a plugin manually](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate) for the first time.
- [ ] Set the Plugin ID in the above README badges.
- [ ] Set the [Deployment Token](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html).
- [ ] Click the <kbd>Watch</kbd> button on the top of the [IntelliJ Platform Plugin Template][template] to be notified about releases containing new features and fixes.

<!-- Plugin description -->
    Display all android vector drawables in the entire project
    Click on the thumbnail to open the xml file

    How to use: Go to menu View > Tool Windows > Vector Drawable Thumbnails

    [Donations are welcome!](https://paypal.me/itcrespo)

<!-- Plugin description end -->

## Architecture

This plugin has been refactored to follow **SOLID principles**, making it more scalable, maintainable, and testable. The architecture is organized into clear layers:

### 🏗️ Layered Architecture
- **Presentation Layer**: UI components and controllers
- **Application Layer**: Business logic orchestration
- **Domain Layer**: Core business interfaces and models
- **Infrastructure Layer**: Concrete implementations

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

For detailed information about the refactoring, see [SOLID_REFACTORING.md](SOLID_REFACTORING.md).

## Installation

- Using IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "vector-drawable-thumbnails-plugin"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/ignaciotcrespo/vector-drawable-thumbnails-plugin/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Development

### Running Tests
```bash
./gradlew test
```

### Building the Plugin
```bash
./gradlew buildPlugin
```

### Running in Development
```bash
./gradlew runIde
```

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
