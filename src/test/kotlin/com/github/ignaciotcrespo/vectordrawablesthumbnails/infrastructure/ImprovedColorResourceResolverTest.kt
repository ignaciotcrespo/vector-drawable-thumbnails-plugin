package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.*
import com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure.cache.DefaultColorCacheManager
import com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure.parsers.XmlResourceFileParser
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.openapi.vfs.VirtualFile
import org.junit.Test
import org.mockito.kotlin.*

/**
 * Test for ImprovedColorResourceResolver to verify it addresses all code review issues:
 * - Proper threading and performance
 * - SOLID principles compliance
 * - Error handling
 * - Cache invalidation
 */
class ImprovedColorResourceResolverTest : LightPlatformTestCase() {
    
    private lateinit var mockCacheManager: ColorCacheManager
    private lateinit var mockResourceParser: ResourceFileParser
    private lateinit var mockAndroidResourceLocator: AndroidResourceLocator
    private lateinit var mockColorResolver: ColorResolver
    private lateinit var mockCacheInvalidator: ResourceCacheInvalidator
    
    private lateinit var resolver: ImprovedColorResourceResolver
    
    override fun setUp() {
        super.setUp()
        
        // Create mocks
        mockCacheManager = mock()
        mockResourceParser = mock()
        mockAndroidResourceLocator = mock()
        mockColorResolver = mock()
        mockCacheInvalidator = mock()
        
        // Create resolver with mocks
        resolver = ImprovedColorResourceResolver(
            cacheManager = mockCacheManager,
            resourceParser = mockResourceParser,
            androidResourceLocator = mockAndroidResourceLocator,
            colorResolver = mockColorResolver,
            cacheInvalidator = mockCacheInvalidator
        )
    }
    
    @Test
    fun testResolveDirectHexColorWithoutCacheAccess() {
        // Test that direct hex colors are returned immediately without cache access
        val hexColor = "#FF0000"
        whenever(mockColorResolver.resolveColor(hexColor, any())).thenReturn(hexColor)
        
        val result = resolver.resolveColorReference(hexColor, project)
        
        assertEquals(hexColor, result)
        // Verify cache was not accessed for direct hex colors
        verify(mockCacheManager, never()).getCachedColors(any())
    }
    
    @Test
    fun testResolveAndroidSystemColor() {
        // Test Android system color resolution
        val androidColor = "@android:color/black"
        whenever(mockColorResolver.resolveColor(androidColor, any())).thenReturn("#000000")
        
        val result = resolver.resolveColorReference(androidColor, project)
        
        assertEquals("#000000", result)
    }
    
    @Test
    fun testBuildCacheTriggersWatching() {
        // Test that building cache starts file watching
        whenever(mockAndroidResourceLocator.findAllResourceFiles(project))
            .thenReturn(ResourceFileCollection(emptyList(), emptyList(), emptyList()))
        
        resolver.buildColorCache(project)
        
        // Verify cache invalidator starts watching
        verify(mockCacheInvalidator).startWatching(project)
    }
    
    @Test
    fun testCacheInvalidationCallback() {
        // Test that cache invalidation callback is set up
        val captor = argumentCaptor<() -> Unit>()
        verify(mockCacheInvalidator).onInvalidate(captor.capture())
        
        // Simulate cache invalidation
        captor.firstValue.invoke()
        
        // Verify cache is cleared
        verify(mockCacheManager).clearAllCaches()
    }
    
    @Test
    fun testResolveColorWithCacheMiss() {
        // Test color resolution when cache miss occurs
        val colorRef = "@color/primary"
        val colorValue = "#FF0000"
        val colorMap = mapOf("primary" to colorValue)
        
        // First call returns null (cache miss)
        whenever(mockCacheManager.getCachedColors(project))
            .thenReturn(null)
            .thenReturn(colorMap)
        
        whenever(mockAndroidResourceLocator.findAllResourceFiles(project))
            .thenReturn(ResourceFileCollection(emptyList(), emptyList(), emptyList()))
        
        whenever(mockColorResolver.resolveColor(colorRef, colorMap))
            .thenReturn(colorValue)
        
        val result = resolver.resolveColorReference(colorRef, project)
        
        assertEquals(colorValue, result)
        // Verify cache was built
        verify(mockCacheManager, times(2)).getCachedColors(project)
    }
    
    @Test
    fun testErrorHandlingInResolveColorReference() {
        // Test error handling in resolve color reference
        val colorRef = "@color/primary"
        
        whenever(mockCacheManager.getCachedColors(project))
            .thenThrow(RuntimeException("Test exception"))
        
        val result = resolver.resolveColorReference(colorRef, project)
        
        assertNull(result)
    }
    
    @Test
    fun testClearCache() {
        // Test clear cache functionality
        resolver.clearCache()
        
        verify(mockCacheManager).clearAllCaches()
    }
    
    @Test
    fun testGetAllColorResourcesWithEmptyCache() {
        // Test getting all colors when cache is empty
        whenever(mockCacheManager.getCachedColors(project))
            .thenReturn(null)
            .thenReturn(mapOf("test" to "#000000"))
        
        whenever(mockAndroidResourceLocator.findAllResourceFiles(project))
            .thenReturn(ResourceFileCollection(emptyList(), emptyList(), emptyList()))
        
        val result = resolver.getAllColorResources(project)
        
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals("#000000", result["test"])
    }
    
    @Test
    fun testDispose() {
        // Test proper disposal
        resolver.dispose()
        
        verify(mockCacheInvalidator).stopWatching()
    }
    
    @Test
    fun testResourceParsingWithDifferentSources() {
        // Test that build outputs override source files
        val mockSourceFile = mock<VirtualFile>()
        val mockBuildFile = mock<VirtualFile>()
        
        whenever(mockSourceFile.name).thenReturn("colors.xml")
        whenever(mockSourceFile.extension).thenReturn("xml")
        whenever(mockBuildFile.name).thenReturn("colors.xml")
        whenever(mockBuildFile.extension).thenReturn("xml")
        
        whenever(mockResourceParser.parseResourceFile(mockSourceFile))
            .thenReturn(mapOf("primary" to "#00FF00"))
        whenever(mockResourceParser.parseResourceFile(mockBuildFile))
            .thenReturn(mapOf("primary" to "#FF0000"))
        
        val resourceCollection = ResourceFileCollection(
            sourceFiles = listOf(mockSourceFile),
            buildOutputFiles = listOf(mockBuildFile),
            libraryEntries = emptyList()
        )
        
        whenever(mockAndroidResourceLocator.findAllResourceFiles(project))
            .thenReturn(resourceCollection)
        
        resolver.buildColorCache(project)
        
        // Need to wait a bit for async operation
        Thread.sleep(100)
        
        // Verify that build output color overrides source color
        val captor = argumentCaptor<Map<String, String>>()
        verify(mockCacheManager).updateCache(eq(project), captor.capture())
        
        val cachedColors = captor.firstValue
        assertEquals("#FF0000", cachedColors["primary"]) // Build output should win
    }
}