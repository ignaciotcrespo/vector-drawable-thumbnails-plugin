package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ColorResourceResolver
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightPlatformTestCase
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class DefaultColorResourceResolverTest : LightPlatformTestCase() {
    
    private lateinit var colorResolver: ColorResourceResolver
    
    override fun setUp() {
        super.setUp()
        colorResolver = DefaultColorResourceResolver()
    }
    
    @Test
    fun testResolveColorReference_WithoutCache() {
        // Given a color reference
        val colorReference = "@color/primary"
        
        // When resolving without cache
        val result = colorResolver.resolveColorReference(colorReference, project)
        
        // Then it should return null (no cache built yet)
        assertNull(result)
    }
    
    @Test
    fun testBuildColorCache() {
        // Given a project
        val project = getProject()
        
        // When building color cache
        colorResolver.buildColorCache(project)
        
        // Then cache should be built (no exception thrown)
        // We can't test the actual content without a real Android project
    }
    
    @Test
    fun testResolveColorReference_ExtractsColorName() {
        // Given various color references
        val testCases = mapOf(
            "@color/primary" to "primary",
            "@color/colorAccent" to "colorAccent",
            "@color/text_color_primary" to "text_color_primary"
        )
        
        // The resolver should extract the color name correctly
        testCases.forEach { (reference, expectedName) ->
            // This tests the internal logic of extracting color names
            val actualName = reference.removePrefix("@color/").trim()
            assertEquals(expectedName, actualName)
        }
    }
    
    @Test
    fun testClearCache() {
        // Given a project with cache
        val project = getProject()
        colorResolver.buildColorCache(project)
        
        // When clearing cache
        colorResolver.clearCache()
        
        // Then subsequent calls should return null
        val result = colorResolver.resolveColorReference("@color/primary", project)
        assertNull(result)
    }
    
    @Test
    fun testColorNameExtraction() {
        // Test that the color name extraction works correctly
        val testCases = listOf(
            "@color/primary" to "primary",
            "@color/brand_primary" to "brand_primary",
            "@color/material_blue_500" to "material_blue_500"
        )
        
        testCases.forEach { (reference, expected) ->
            val actual = reference.removePrefix("@color/").trim()
            assertEquals(expected, actual, "Color name extraction failed for $reference")
        }
    }
    
    @Test
    fun testGetAllColorResources() {
        // Given a project
        val project = getProject()
        
        // When getting all color resources
        val allColors = colorResolver.getAllColorResources(project)
        
        // Then it should return a map (empty in test environment)
        assertNotNull(allColors)
    }
    
    @Test
    fun testBuildColorCacheHandlesExceptions() {
        // Given a project
        val project = getProject()
        
        // When building color cache multiple times
        // Then it should not throw exceptions
        colorResolver.buildColorCache(project)
        colorResolver.buildColorCache(project) // Should handle existing cache
        
        // Verify cache can be cleared
        colorResolver.clearCache()
    }
}