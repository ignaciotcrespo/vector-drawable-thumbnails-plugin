package com.github.ignaciotcrespo.vectordrawablesthumbnails.domain

import com.intellij.openapi.project.Project

/**
 * Interface for invalidating resource caches when files change.
 * Follows Single Responsibility Principle - only handles cache invalidation.
 */
interface ResourceCacheInvalidator {
    /**
     * Starts watching for resource file changes in the project.
     */
    fun startWatching(project: Project)
    
    /**
     * Stops watching for resource file changes.
     */
    fun stopWatching()
    
    /**
     * Registers a callback to be called when cache needs invalidation.
     */
    fun onInvalidate(callback: () -> Unit)
    
    /**
     * Checks if a file path is a resource file that should trigger invalidation.
     */
    fun isResourceFile(filePath: String): Boolean
}