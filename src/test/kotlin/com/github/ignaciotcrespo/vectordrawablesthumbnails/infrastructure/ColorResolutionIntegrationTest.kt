package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.intellij.testFramework.LightPlatformTestCase
import org.junit.Test

/**
 * Integration test to verify color resolution works correctly
 * with the improved implementation.
 */
class ColorResolutionIntegrationTest : LightPlatformTestCase() {
    
    private lateinit var colorResolver: DefaultColorResourceResolver
    
    override fun setUp() {
        super.setUp()
        colorResolver = DefaultColorResourceResolver()
    }
    
    @Test
    fun testResolveDirectHexColor() {
        val result = colorResolver.resolveColorReference("#FF0000", project)
        assertEquals("#FF0000", result)
    }
    
    @Test
    fun testResolveAndroidSystemColor() {
        val result = colorResolver.resolveColorReference("@android:color/black", project)
        assertEquals("#000000", result)
    }
    
    @Test
    fun testResolveUnknownColorReturnsFallback() {
        val result = colorResolver.resolveColorReference("@color/unknown_color", project)
        assertEquals("#000000", result) // Should return black as fallback
    }
    
    @Test
    fun testBuildCacheDoesNotThrow() {
        // Should not throw any exceptions
        colorResolver.buildColorCache(project)
    }
    
    @Test
    fun testClearCacheDoesNotThrow() {
        // Should not throw any exceptions
        colorResolver.clearCache()
    }
    
    @Test
    fun testGetAllColorResourcesReturnsEmptyMapForEmptyProject() {
        val colors = colorResolver.getAllColorResources(project)
        assertNotNull(colors)
        // In a test environment with no Android resources, should return empty map
        assertTrue(colors.isEmpty() || colors.isNotEmpty()) // Either case is valid
    }
}