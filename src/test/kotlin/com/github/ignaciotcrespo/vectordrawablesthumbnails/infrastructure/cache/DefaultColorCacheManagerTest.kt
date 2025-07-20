package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure.cache

import com.intellij.openapi.project.Project
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.*
import kotlin.test.*

class DefaultColorCacheManagerTest {
    
    private lateinit var cacheManager: DefaultColorCacheManager
    private lateinit var mockProject: Project
    
    @BeforeEach
    fun setUp() {
        cacheManager = DefaultColorCacheManager()
        mockProject = mock<Project>()
        whenever(mockProject.name).thenReturn("TestProject")
    }
    
    @Test
    fun `getCachedColors should return null when cache is empty`() {
        // When
        val result = cacheManager.getCachedColors(mockProject)
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `updateCache should store colors for project`() {
        // Given
        val colors = mapOf(
            "primary" to "#FF0000",
            "secondary" to "#00FF00"
        )
        
        // When
        cacheManager.updateCache(mockProject, colors)
        val result = cacheManager.getCachedColors(mockProject)
        
        // Then
        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals("#FF0000", result["primary"])
        assertEquals("#00FF00", result["secondary"])
    }
    
    @Test
    fun `clearCache should remove cached colors for project`() {
        // Given
        val colors = mapOf("primary" to "#FF0000")
        cacheManager.updateCache(mockProject, colors)
        
        // When
        cacheManager.clearCache(mockProject)
        val result = cacheManager.getCachedColors(mockProject)
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `clearAllCaches should remove all cached colors`() {
        // Given
        val project1 = mock<Project>()
        whenever(project1.name).thenReturn("Project1")
        val project2 = mock<Project>()
        whenever(project2.name).thenReturn("Project2")
        
        cacheManager.updateCache(project1, mapOf("color1" to "#FF0000"))
        cacheManager.updateCache(project2, mapOf("color2" to "#00FF00"))
        
        // When
        cacheManager.clearAllCaches()
        
        // Then
        assertNull(cacheManager.getCachedColors(project1))
        assertNull(cacheManager.getCachedColors(project2))
    }
    
    @Test
    fun `isCacheInvalid should return true when cache is empty`() {
        // When
        val result = cacheManager.isCacheInvalid(mockProject)
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `isCacheInvalid should return false when cache exists`() {
        // Given
        cacheManager.updateCache(mockProject, mapOf("primary" to "#FF0000"))
        
        // When
        val result = cacheManager.isCacheInvalid(mockProject)
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `updateCache should replace existing cache`() {
        // Given
        cacheManager.updateCache(mockProject, mapOf("primary" to "#FF0000"))
        
        // When
        cacheManager.updateCache(mockProject, mapOf("secondary" to "#00FF00"))
        val result = cacheManager.getCachedColors(mockProject)
        
        // Then
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals("#00FF00", result["secondary"])
        assertNull(result["primary"])
    }
    
    @Test
    fun `getCachedColors should return immutable map`() {
        // Given
        val colors = mapOf("primary" to "#FF0000")
        cacheManager.updateCache(mockProject, colors)
        
        // When
        val result = cacheManager.getCachedColors(mockProject)
        
        // Then
        assertNotNull(result)
        assertFailsWith<UnsupportedOperationException> {
            (result as MutableMap<String, String>)["new"] = "#0000FF"
        }
    }
}