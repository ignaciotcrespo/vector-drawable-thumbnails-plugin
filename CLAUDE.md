# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the Vector Drawable Thumbnails Plugin for IntelliJ Platform - a professional plugin that displays thumbnail previews of Android Vector Drawable files in JetBrains IDEs.

## Development Commands

### Testing
- `./gradlew test` - Run all unit tests
- `./gradlew test --tests "*.DefaultVectorAnalyticsServiceTest"` - Run a specific test class
- `./gradlew test --tests "*.DefaultVectorAnalyticsServiceTest.testMethod"` - Run a specific test method
- `./gradlew check` - Run all checks including tests and static analysis

### Running the Plugin
- `./gradlew runIde` - Launch in IntelliJ IDEA
- `./gradlew runAndroidStudio` - Launch in Android Studio
- `./gradlew runWebStorm` - Launch in WebStorm
- `./gradlew runPyCharm` - Launch in PyCharm

### Building
- `./gradlew buildPlugin` - Build the plugin distribution
- `./gradlew verifyPlugin` - Verify plugin compatibility
- `./gradlew runPluginVerifier` - Run comprehensive plugin verification

### Code Quality
- `./gradlew detekt` - Run Kotlin static analysis (configured with 8-space continuation indent)
- `./gradlew koverXmlReport` - Generate code coverage report

## Architecture

The codebase follows a clean architecture pattern with SOLID principles:

### Layer Structure
1. **UI Layer** (`src/main/kotlin/ui/`): Swing-based UI components
   - Entry point: `VectorDrawablesToolWindowFactory`
   - Main controller: `VectorUIController`
   
2. **Service Layer** (`src/main/kotlin/service/`): Business orchestration
   - Central service: `VectorService` coordinates all operations
   
3. **Domain Layer** (`src/main/kotlin/domain/`): Core business interfaces
   - Repository pattern for data access
   - Strategy pattern for filtering/sorting
   
4. **Infrastructure Layer** (`src/main/kotlin/infrastructure/`): Concrete implementations
   - Default implementations of domain interfaces
   - XML parsing and file system operations

### Key Patterns
- **Dependency Injection**: Manual DI through `VectorDIContainer`
- **Reactive Programming**: RxJava for asynchronous operations
- **Repository Pattern**: Abstracts data access
- **Strategy Pattern**: Flexible filtering and sorting

### Testing Approach
- Unit tests use JUnit 5 with Mockito
- Tests follow given-when-then pattern
- Mock heavy dependencies (file system, UI components)
- Test files mirror source structure in `src/test/kotlin/`

## Plugin Development Notes

- **Plugin ID**: `com.crespodev.vectordrawablesthumbnailsplugin`
- **Target Platforms**: IntelliJ 2024.2+
- **Main Extension Point**: Tool window factory registered in `plugin.xml`
- **Resource Bundle**: Messages in `src/main/resources/messages/VectorDrawableMessages.properties`

## Performance Considerations

- Uses caching for parsed vector drawables
- Implements pagination for large directories
- Lazy loading of thumbnails
- Reactive streams for non-blocking operations