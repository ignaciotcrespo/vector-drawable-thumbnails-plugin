# Vector Drawable Thumbnails Plugin Documentation

## Overview

This plugin provides thumbnail previews for Android Vector Drawable (AVD) files within JetBrains IDEs. It resolves color references, including Android system colors and project-specific colors, to display accurate color previews.

## Architecture

### Core Components

1. **Color Resolution System**
   - `UnifiedColorResourceResolver`: Main orchestrator using strategy pattern
   - `EnhancedAndroidResourceStrategy`: Integrates with Android Studio's resource APIs
   - `AndroidStudioResourceStrategy`: Standard Android Studio integration
   - `CustomResourceStrategy`: Fallback implementation for other IDEs
   - `AndroidSystemColors`: Centralized Android system color definitions

2. **Vector Processing**
   - `VectorParser`: Interface for parsing vector files
   - `DefaultVectorParser`: Main implementation with color resolution
   - `AsyncVectorParser`: Parallel processing for performance

3. **UI Components**
   - `VectorUIController`: Main UI controller
   - `VectorItemPanel`: Individual vector item display
   - `VectorDrawablesToolWindowFactory`: Tool window creation

### Design Patterns

- **Strategy Pattern**: For resource management strategies
- **Dependency Injection**: Via `DependencyContainer`
- **Repository Pattern**: For vector data access
- **Factory Pattern**: For parser creation

## Color Resolution Process

1. Vector file is loaded by `VectorParser`
2. Color references are detected (e.g., `@color/primary`, `@android:color/white`)
3. `UnifiedColorResourceResolver` determines the appropriate strategy:
   - Tries `EnhancedAndroidResourceStrategy` first
   - Falls back to `AndroidStudioResourceStrategy`
   - Uses `CustomResourceStrategy` as last resort
4. Android system colors are resolved via `AndroidSystemColors`
5. Project colors are resolved via Android Studio APIs or file scanning
6. References are replaced with hex values before rendering

## Performance Optimizations

- **Background Processing**: Color cache building runs off UI thread
- **Caching**: Color resolutions cached with `ConcurrentHashMap`
- **Lazy Loading**: Vectors loaded only when tool window is shown
- **Parallel Processing**: `AsyncVectorParser` processes multiple files concurrently
- **File Watching**: Automatic cache invalidation on resource changes

## SOLID Principles Implementation

- **Single Responsibility**: Each class has one clear purpose
- **Open/Closed**: New strategies can be added without modifying existing code
- **Liskov Substitution**: All strategies implement `ResourceManagementStrategy`
- **Interface Segregation**: Focused interfaces for each concern
- **Dependency Inversion**: Dependencies on abstractions, not concretions

## Threading and Concurrency

- UI operations properly dispatched to EDT
- Background tasks use `Task.Backgroundable`
- Thread-safe collections for concurrent access
- Proper synchronization for resource initialization

## Error Handling

- Comprehensive exception handling at all levels
- Graceful fallbacks when color resolution fails
- Detailed logging for debugging
- User-friendly default values

## Testing

Comprehensive test coverage including:
- Unit tests for all color resolvers
- Integration tests for color resolution
- Edge case tests for error scenarios
- Mock-based tests for Android Studio APIs

## Known Limitations

- Theme attributes (`?attr/`) not fully supported
- Some complex color state lists may not resolve
- Requires Android plugin for full functionality

## Future Enhancements

- Support for theme attribute resolution
- Color state list preview
- Performance metrics dashboard
- Extended IDE compatibility