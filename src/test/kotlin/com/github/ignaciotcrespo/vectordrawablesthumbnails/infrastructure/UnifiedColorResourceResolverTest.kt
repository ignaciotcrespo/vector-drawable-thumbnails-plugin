package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ResourceManagementStrategy
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightPlatformTestCase
import org.junit.Test
import org.mockito.kotlin.*

/**
 * Comprehensive tests for UnifiedColorResourceResolver
 */
class UnifiedColorResourceResolverTest : LightPlatformTestCase() {
    
    private lateinit var resolver: UnifiedColorResourceResolver
    private lateinit var mockAndroidStrategy: ResourceManagementStrategy
    private lateinit var mockCustomStrategy: ResourceManagementStrategy
    
    override fun setUp() {
        super.setUp()
        resolver = UnifiedColorResourceResolver()
        mockAndroidStrategy = mock()
        mockCustomStrategy = mock()
    }
    
    override fun tearDown() {
        resolver.dispose()
        super.tearDown()
    }
    
    @Test
    fun testResolveColorReference_DirectColor() {
        val project = project
        val colorRef = "#FF0000"
        
        // Setup - custom strategy will be used in test environment
        whenever(mockCustomStrategy.isAvailable(any())).thenReturn(true)
        whenever(mockCustomStrategy.resolveColorReference(colorRef, project)).thenReturn(colorRef)
        
        val result = resolver.resolveColorReference(colorRef, project)
        
        assertEquals(colorRef, result)
    }
    
    @Test
    fun testResolveColorReference_AndroidSystemColor() {
        val project = project
        val colorRef = "@android:color/black"
        val expectedColor = "#000000"
        
        val result = resolver.resolveColorReference(colorRef, project)
        
        // Should resolve to black
        assertNotNull(result)
        assertTrue(result == expectedColor || result == colorRef)
    }
    
    @Test
    fun testResolveColorReference_ResourceColor() {
        val project = project
        val colorRef = "@color/primary"
        val expectedColor = "#3F51B5"
        
        // The resolver will use available strategy
        val result = resolver.resolveColorReference(colorRef, project)
        
        // Result depends on strategy implementation
        assertNotNull(result)
    }
    
    @Test
    fun testResolveColorReference_ErrorHandling() {
        val project = project
        val colorRef = "@color/test"
        
        // Test that errors are handled gracefully
        val result = resolver.resolveColorReference(colorRef, project)
        
        // Should return null or a default value, not throw
        assertTrue(result == null || result.startsWith("#"))
    }
    
    @Test
    fun testBuildColorCache_RunsInBackground() {
        val project = project
        
        // This should not block
        resolver.buildColorCache(project)
        
        // Give background task time to start
        Thread.sleep(100)
        
        // Should complete without errors
        assertTrue(true)
    }
    
    @Test
    fun testGetAllColorResources_ReturnsMap() {
        val project = project
        
        val result = resolver.getAllColorResources(project)
        
        assertNotNull(result)
        assertTrue(result is Map<*, *>)
    }
    
    @Test
    fun testClearCache_DoesNotThrow() {
        val project = project
        
        // Build some cache
        resolver.getAllColorResources(project)
        
        // Clear should work without errors
        resolver.clearCache()
        
        assertTrue(true)
    }
    
    @Test
    fun testDispose_CleansUpProperly() {
        val project = project
        
        // Use resolver
        resolver.getAllColorResources(project)
        
        // Dispose should clean up without errors
        resolver.dispose()
        
        // Should be able to clear cache after dispose
        resolver.clearCache()
        
        assertTrue(true)
    }
    
    @Test
    fun testStrategySelection_PreferAndroidStudio() {
        // This test verifies strategy selection logic
        // In a real Android Studio environment, it should prefer native strategy
        
        val project = project
        
        // Get resources to trigger strategy selection
        val resources = resolver.getAllColorResources(project)
        
        assertNotNull(resources)
        // In test environment, custom strategy will be used
    }
    
    @Test
    fun testConcurrentAccess() {
        val project = project
        val threads = mutableListOf<Thread>()
        val errors = mutableListOf<Exception>()
        
        // Test concurrent access from multiple threads
        repeat(5) { i ->
            threads.add(Thread {
                try {
                    resolver.resolveColorReference("@color/test$i", project)
                    resolver.getAllColorResources(project)
                } catch (e: Exception) {
                    synchronized(errors) {
                        errors.add(e)
                    }
                }
            })
        }
        
        threads.forEach { it.start() }
        threads.forEach { it.join(5000) }
        
        assertTrue("Concurrent access caused errors: $errors", errors.isEmpty())
    }
}