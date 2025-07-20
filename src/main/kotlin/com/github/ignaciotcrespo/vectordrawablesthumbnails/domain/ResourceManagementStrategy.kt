package com.github.ignaciotcrespo.vectordrawablesthumbnails.domain

import com.intellij.openapi.project.Project

/**
 * Strategy interface for resource management.
 * Allows switching between Android Studio's native resource management
 * and a custom fallback implementation.
 */
interface ResourceManagementStrategy {
    /**
     * Indicates if this strategy is available in the current environment
     */
    fun isAvailable(project: Project): Boolean
    
    /**
     * Gets all color resources for the project
     */
    fun getColorResources(project: Project): Map<String, String>
    
    /**
     * Resolves a single color reference
     */
    fun resolveColorReference(colorRef: String, project: Project): String?
    
    /**
     * Sets up resource change listeners
     */
    fun setupChangeListeners(project: Project, onChange: () -> Unit)
    
    /**
     * Cleans up resources
     */
    fun dispose()
}