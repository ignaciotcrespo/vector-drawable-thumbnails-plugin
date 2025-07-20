# JetBrains Products Compatibility Guide

## Overview

The Vector Drawable Thumbnails Plugin is designed for **maximum compatibility** across all JetBrains IDEs, ensuring a consistent and professional experience regardless of which IDE you use.

## Supported JetBrains Products

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

## Platform Compatibility Strategy

### Version Support Range
- **Minimum Version**: 2022.3 (Build 223)
- **Maximum Version**: 2024.3+ (Build 243.*)
- **Coverage**: 80%+ of active JetBrains users

### Compatibility Architecture

```
┌─────────────────────────────────────────┐
│           Core Platform Module          │
│        (com.intellij.modules.platform)  │
├─────────────────────────────────────────┤
│         Optional Dependencies           │
├─────────────────────────────────────────┤
│  Android Support  │  Java Support      │
│  (org.jetbrains.  │  (com.intellij.    │
│   android)        │   modules.java)    │
├─────────────────────────────────────────┤
│         Language Support               │
│      (com.intellij.modules.lang)       │
└─────────────────────────────────────────┘
```

## IDE-Specific Features

### Android Studio / IntelliJ IDEA Ultimate
- **Enhanced Android Integration**: Direct integration with Android resource system
- **Vector Asset Studio**: Open vectors in Android's vector asset studio
- **Resource Detection**: Automatic detection of Android resource directories
- **APK Analysis**: Vector analysis in APK files

### IntelliJ IDEA Community
- **Core Functionality**: Full vector thumbnail display
- **Universal File Support**: Works with any XML vector files
- **Project Integration**: Seamless project file scanning

### WebStorm
- **Web Asset Management**: Vector assets for web projects
- **SVG Compatibility**: Enhanced SVG vector support
- **Build Tool Integration**: Webpack/Vite asset pipeline support

### Other IDEs
- **Universal Support**: Core functionality works across all IDEs
- **Consistent UI**: Native look and feel for each IDE
- **Performance Optimized**: Efficient resource usage

## Technical Compatibility Features

### 1. Platform API Usage
```kotlin
// Uses only stable platform APIs
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.application.ApplicationManager
```

### 2. Optional Dependencies
```xml
<!-- Safe optional dependencies -->
<depends optional="true" config-file="android-support.xml">
    org.jetbrains.android
</depends>
<depends optional="true" config-file="java-support.xml">
    com.intellij.modules.java
</depends>
```

### 3. Graceful Degradation
- Features gracefully disable if dependencies unavailable
- Core functionality always available
- No hard dependencies on IDE-specific features

## Testing Strategy

### Multi-Version Testing
```bash
# Automated testing across versions
./gradlew runPluginVerifier
```

Tested against:
- IntelliJ IDEA 2022.3.3
- IntelliJ IDEA 2023.1.5
- IntelliJ IDEA 2023.2.5
- IntelliJ IDEA 2023.3.6
- IntelliJ IDEA 2024.1.4
- IntelliJ IDEA 2024.2.4
- IntelliJ IDEA 2024.3.1

### IDE-Specific Testing
1. **Manual Testing**: Each major IDE tested manually
2. **Automated Verification**: Plugin verifier for compatibility
3. **Performance Testing**: Memory and CPU usage across IDEs
4. **UI Testing**: Consistent appearance verification

## Installation & Deployment

### JetBrains Marketplace
- **Single Plugin**: One plugin works for all IDEs
- **Automatic Updates**: Consistent updates across all platforms
- **Version Management**: Backward compatibility maintained

### Manual Installation
1. Download plugin JAR
2. Install in any JetBrains IDE
3. Restart IDE
4. Access via "Vector Drawable Thumbnails" tool window

## Performance Considerations

### Memory Usage
- **Optimized Caching**: Efficient thumbnail caching
- **Lazy Loading**: Load thumbnails on demand
- **Memory Management**: Automatic cleanup of unused resources

### CPU Usage
- **Background Processing**: Non-blocking thumbnail generation
- **Smart Indexing**: Efficient file system monitoring
- **Throttling**: Prevents UI freezing during large scans

## Troubleshooting

### Common Issues

#### Plugin Not Appearing
1. Check IDE version (must be 2022.3+)
2. Verify plugin installation
3. Restart IDE

#### Performance Issues
1. Check available memory
2. Reduce thumbnail cache size
3. Disable real-time scanning for large projects

#### IDE-Specific Problems
1. Check optional dependencies
2. Verify IDE-specific features are enabled
3. Review IDE logs for errors

### Debug Information
```kotlin
// Enable debug logging
Logger.getInstance("VectorDrawableThumbnails").info("Debug info")
```

## Future Compatibility

### Upcoming JetBrains Versions
- **2024.4+**: Ready for future versions
- **API Changes**: Monitoring for breaking changes
- **New IDEs**: Support for new JetBrains products

### Maintenance Strategy
- **Regular Updates**: Quarterly compatibility updates
- **API Monitoring**: Track JetBrains API changes
- **Community Feedback**: User-reported compatibility issues

## Contributing

### Compatibility Testing
1. Test on your specific IDE version
2. Report compatibility issues
3. Submit IDE-specific improvements

### Development Guidelines
1. Use only stable platform APIs
2. Test across multiple IDE versions
3. Maintain backward compatibility

## Support

For compatibility issues:
1. **GitHub Issues**: Report IDE-specific problems
2. **JetBrains Marketplace**: Leave compatibility feedback
3. **Documentation**: Check this guide for solutions

---

**Last Updated**: December 2024  
**Plugin Version**: 1.3.0  
**Supported Platform Range**: 2022.3 - 2024.3+ 