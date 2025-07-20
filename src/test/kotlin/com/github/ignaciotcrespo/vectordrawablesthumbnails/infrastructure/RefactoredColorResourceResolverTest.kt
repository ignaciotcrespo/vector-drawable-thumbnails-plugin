package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.*
import kotlin.test.*

class RefactoredColorResourceResolverTest {
    
    private lateinit var resolver: RefactoredColorResourceResolver
    private lateinit var mockCacheManager: ColorCacheManager
    private lateinit var mockResourceParser: ResourceFileParser
    private lateinit var mockProjectSearchStrategy: ProjectResourceSearchStrategy
    private lateinit var mockProject: Project
    
    @BeforeEach
    fun setUp() {
        mockCacheManager = mock<ColorCacheManager>()
        mockResourceParser = mock<ResourceFileParser>()
        mockProjectSearchStrategy = mock<ProjectResourceSearchStrategy>()
        mockProject = mock<Project>()
        
        resolver = RefactoredColorResourceResolver(
            cacheManager = mockCacheManager,
            resourceParser = mockResourceParser,
            projectSearchStrategy = mockProjectSearchStrategy
        )
    }
    
    @Test
    fun `resolveColorReference should return direct color from cache`() {
        // Given
        val cachedColors = mapOf(
            "primary" to "#FF0000",
            "secondary" to "#00FF00"
        )
        whenever(mockCacheManager.getCachedColors(mockProject)).thenReturn(cachedColors)
        
        // When
        val result = resolver.resolveColorReference("@color/primary", mockProject)
        
        // Then
        assertEquals("#FF0000", result)
    }
    
    @Test
    fun `resolveColorReference should handle color reference chains`() {
        // Given
        val cachedColors = mapOf(
            "primary" to "#FF0000",
            "primary_dark" to "@color/primary",
            "app_theme" to "@color/primary_dark"
        )
        whenever(mockCacheManager.getCachedColors(mockProject)).thenReturn(cachedColors)
        
        // When
        val result = resolver.resolveColorReference("@color/app_theme", mockProject)
        
        // Then
        assertEquals("#FF0000", result)
    }
    
    @Test
    fun `resolveColorReference should handle circular references`() {
        // Given
        val cachedColors = mapOf(
            "color1" to "@color/color2",
            "color2" to "@color/color3",
            "color3" to "@color/color1"
        )
        whenever(mockCacheManager.getCachedColors(mockProject)).thenReturn(cachedColors)
        
        // When
        val result = resolver.resolveColorReference("@color/color1", mockProject)
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `resolveColorReference should handle Android system colors`() {
        // Given
        val cachedColors = mapOf(
            "background" to "@android:color/white"
        )
        whenever(mockCacheManager.getCachedColors(mockProject)).thenReturn(cachedColors)
        
        // When
        val result = resolver.resolveColorReference("@color/background", mockProject)
        
        // Then
        assertEquals("#FFFFFF", result)
    }
    
    @Test
    fun `resolveColorReference should build cache if not available`() {
        // Given
        whenever(mockCacheManager.getCachedColors(mockProject)).thenReturn(null).thenReturn(mapOf("primary" to "#FF0000"))
        val mockFile = mock<VirtualFile>()
        whenever(mockProjectSearchStrategy.findResourceFiles(mockProject)).thenReturn(listOf(mockFile))
        whenever(mockResourceParser.parseResourceFile(mockFile)).thenReturn(mapOf("primary" to "#FF0000"))
        
        // When
        val result = resolver.resolveColorReference("@color/primary", mockProject)
        
        // Then
        assertEquals("#FF0000", result)
        verify(mockCacheManager).updateCache(mockProject, mapOf("primary" to "#FF0000"))
    }
    
    @Test
    fun `getAllColorResources should return cached colors`() {
        // Given
        val cachedColors = mapOf(
            "primary" to "#FF0000",
            "secondary" to "#00FF00"
        )
        whenever(mockCacheManager.getCachedColors(mockProject)).thenReturn(cachedColors)
        
        // When
        val result = resolver.getAllColorResources(mockProject)
        
        // Then
        assertEquals(2, result.size)
        assertEquals("#FF0000", result["primary"])
        assertEquals("#00FF00", result["secondary"])
    }
    
    @Test
    fun `clearCache should delegate to cache manager`() {
        // When
        resolver.clearCache()
        
        // Then
        verify(mockCacheManager).clearAllCaches()
    }
    
    @Test
    fun `resolveColorReference should handle max recursion depth`() {
        // Given
        val colors = mutableMapOf<String, String>()
        // Create a deep chain of references
        for (i in 0..15) {
            colors["color$i"] = "@color/color${i + 1}"
        }
        colors["color16"] = "#FF0000"
        
        whenever(mockCacheManager.getCachedColors(mockProject)).thenReturn(colors)
        
        // When
        val result = resolver.resolveColorReference("@color/color0", mockProject)
        
        // Then
        assertNull(result) // Should fail due to max depth
    }
    
    @Test
    fun `resolveColorReference should return null for unknown color`() {
        // Given
        val cachedColors = mapOf("primary" to "#FF0000")
        whenever(mockCacheManager.getCachedColors(mockProject)).thenReturn(cachedColors)
        
        // When
        val result = resolver.resolveColorReference("@color/unknown", mockProject)
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `resolveColorReference should handle empty cache gracefully`() {
        // Given
        whenever(mockCacheManager.getCachedColors(mockProject)).thenReturn(emptyMap())
        
        // When
        val result = resolver.resolveColorReference("@color/primary", mockProject)
        
        // Then
        assertNull(result)
    }
}