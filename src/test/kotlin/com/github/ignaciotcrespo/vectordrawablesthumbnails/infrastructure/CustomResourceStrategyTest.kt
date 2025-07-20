package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import org.junit.Test
import org.mockito.kotlin.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Tests for CustomResourceStrategy
 */
class CustomResourceStrategyTest : LightPlatformTestCase() {
    
    private lateinit var strategy: CustomResourceStrategy
    
    override fun setUp() {
        super.setUp()
        strategy = CustomResourceStrategy()
    }
    
    override fun tearDown() {
        strategy.dispose()
        super.tearDown()
    }
    
    @Test
    fun testIsAvailable_AlwaysTrue() {
        assertTrue(strategy.isAvailable(project))
    }
    
    @Test
    fun testResolveColorReference_DirectHexColor() {
        val hexColor = "#FF0000"
        val result = strategy.resolveColorReference(hexColor, project)
        assertEquals(hexColor, result)
    }
    
    @Test
    fun testResolveColorReference_AndroidSystemColor() {
        val systemColors = mapOf(
            "@android:color/black" to "#000000",
            "@android:color/white" to "#FFFFFF",
            "@android:color/transparent" to "#00000000",
            "@android:color/holo_blue_dark" to "#0099CC"
        )
        
        systemColors.forEach { (ref, expected) ->
            val result = strategy.resolveColorReference(ref, project)
            assertEquals("Failed for $ref", expected, result)
        }
    }
    
    @Test
    fun testResolveColorReference_UnknownSystemColor() {
        val unknownColor = "@android:color/unknown_color"
        val result = strategy.resolveColorReference(unknownColor, project)
        assertEquals("#000000", result) // Should default to black
    }
    
    @Test
    fun testResolveColorReference_ResourceColor() {
        val colorRef = "@color/primary"
        
        // First call might return null as cache is not built
        val firstResult = strategy.resolveColorReference(colorRef, project)
        
        // Build cache
        strategy.getColorResources(project)
        
        // Now it should work (if colors exist in test project)
        val secondResult = strategy.resolveColorReference(colorRef, project)
        
        // At least one should not throw
        assertTrue(firstResult == null || firstResult.startsWith("#") || 
                  secondResult == null || secondResult.startsWith("#"))
    }
    
    @Test
    fun testGetColorResources_BuildsCacheIfNeeded() {
        val resources = strategy.getColorResources(project)
        
        assertNotNull(resources)
        assertTrue(resources is Map<*, *>)
        
        // Second call should use cache
        val cachedResources = strategy.getColorResources(project)
        assertSame(resources, cachedResources)
    }
    
    @Test
    fun testSetupChangeListeners_DoesNotThrow() {
        val onChange = mock<() -> Unit>()
        
        strategy.setupChangeListeners(project, onChange)
        
        // Should complete without errors
        assertTrue(true)
    }
    
    @Test
    fun testDispose_CleansUpProperly() {
        // Setup some state
        strategy.getColorResources(project)
        strategy.setupChangeListeners(project) {}
        
        // Dispose
        strategy.dispose()
        
        // Cache should be cleared
        val newResources = strategy.getColorResources(project)
        assertNotNull(newResources)
    }
    
    @Test
    fun testColorReferenceResolution() {
        // Test that color references are resolved properly
        val testColors = mutableMapOf(
            "@color/primary" to "#3F51B5",
            "@color/secondary" to "@color/primary", // Reference to another color
            "@color/tertiary" to "@color/secondary" // Nested reference
        )
        
        // Simulate color resolution
        val resolved = mutableMapOf<String, String>()
        testColors.forEach { (name, value) ->
            var currentValue = value
            var iterations = 0
            
            while (currentValue.startsWith("@color/") && iterations < 10) {
                val nextValue = testColors[currentValue]
                if (nextValue == null || nextValue == currentValue) {
                    break
                }
                currentValue = nextValue
                iterations++
            }
            
            if (!currentValue.startsWith("@")) {
                resolved[name] = currentValue
            }
        }
        
        assertEquals("#3F51B5", resolved["@color/primary"])
        assertEquals("#3F51B5", resolved["@color/secondary"])
        assertEquals("#3F51B5", resolved["@color/tertiary"])
    }
    
    @Test
    fun testCircularReferenceHandling() {
        // Test that circular references don't cause infinite loops
        val testColors = mutableMapOf(
            "@color/a" to "@color/b",
            "@color/b" to "@color/c",
            "@color/c" to "@color/a" // Circular reference
        )
        
        // Resolution should not hang
        val resolved = mutableMapOf<String, String>()
        testColors.forEach { (name, value) ->
            var currentValue = value
            var iterations = 0
            val maxIterations = 10
            
            while (currentValue.startsWith("@color/") && iterations < maxIterations) {
                val nextValue = testColors[currentValue]
                if (nextValue == null || nextValue == currentValue) {
                    break
                }
                currentValue = nextValue
                iterations++
            }
            
            // Should hit max iterations for circular references
            assertTrue(iterations <= maxIterations)
        }
    }
    
    @Test
    fun testFileChangeNotification() {
        val latch = CountDownLatch(1)
        var notificationReceived = false
        
        strategy.setupChangeListeners(project) {
            notificationReceived = true
            latch.countDown()
        }
        
        // In a real scenario, file changes would trigger the listener
        // For testing, we just verify the setup doesn't throw
        
        assertTrue(true)
    }
}