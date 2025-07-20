package com.github.ignaciotcrespo.vectordrawablesthumbnails.domain

/**
 * Interface for resolving color values from references.
 * Follows Single Responsibility Principle - only resolves colors.
 */
interface ColorResolver {
    /**
     * Resolves a color reference to its hex value.
     * Supports direct colors (#RRGGBB), color references (@color/name),
     * and Android system colors (@android:color/name).
     * 
     * @param colorReference The color reference to resolve
     * @param colorMap Map of color names to their values
     * @return The resolved hex color value, or null if not found
     */
    fun resolveColor(colorReference: String, colorMap: Map<String, String>): String?
    
    /**
     * Checks if a string is a color reference that needs resolution.
     */
    fun isColorReference(value: String): Boolean
}

/**
 * Result of color resolution with metadata.
 */
data class ColorResolutionResult(
    val resolvedColor: String?,
    val resolutionPath: List<String> = emptyList(),
    val isSystemColor: Boolean = false
)