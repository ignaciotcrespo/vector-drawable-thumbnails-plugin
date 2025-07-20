package com.github.ignaciotcrespo.vectordrawablesthumbnails.domain

import com.intellij.openapi.project.Project

/**
 * Interface for resolving color resource references.
 * Follows the Single Responsibility Principle by focusing only on color resolution.
 */
interface ColorResourceResolver {
    /**
     * Resolves a color reference (e.g., "@color/colorPrimary") to its hex value.
     * 
     * @param colorReference The color reference string (e.g., "@color/colorPrimary")
     * @param project The current project context
     * @return The resolved hex color value, or null if not found
     */
    fun resolveColorReference(colorReference: String, project: Project): String?
    
    /**
     * Builds a cache of all color resources in the project.
     * This should be called when the project opens or color resources change.
     * 
     * @param project The current project context
     */
    fun buildColorCache(project: Project)
    
    /**
     * Clears the color cache.
     */
    fun clearCache()
    
    /**
     * Gets all available color resources in the project.
     * 
     * @param project The current project context
     * @return Map of color names to hex values
     */
    fun getAllColorResources(project: Project): Map<String, String>
}