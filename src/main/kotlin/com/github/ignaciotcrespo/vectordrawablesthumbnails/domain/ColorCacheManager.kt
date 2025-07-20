package com.github.ignaciotcrespo.vectordrawablesthumbnails.domain

import com.intellij.openapi.project.Project

/**
 * Interface for managing color resource cache.
 * Follows Single Responsibility Principle - only responsible for cache management.
 */
interface ColorCacheManager {
    /**
     * Gets cached colors for a project.
     * 
     * @param project The project
     * @return Map of color names to values, or null if not cached
     */
    fun getCachedColors(project: Project): Map<String, String>?
    
    /**
     * Updates the cache for a project.
     * 
     * @param project The project
     * @param colors Map of color names to values
     */
    fun updateCache(project: Project, colors: Map<String, String>)
    
    /**
     * Clears the cache for a project.
     * 
     * @param project The project
     */
    fun clearCache(project: Project)
    
    /**
     * Clears all caches.
     */
    fun clearAllCaches()
    
    /**
     * Checks if cache needs to be invalidated.
     * 
     * @param project The project
     * @return true if cache should be rebuilt
     */
    fun isCacheInvalid(project: Project): Boolean
}