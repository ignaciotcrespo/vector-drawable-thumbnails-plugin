package com.github.ignaciotcrespo.vectordrawablesthumbnails.domain

/**
 * Configuration for color resolution behavior.
 * This allows users to customize how the plugin handles missing colors,
 * fallback mechanisms, and other resolution behaviors.
 */
data class ColorResolutionConfig(
    /**
     * The default color to use when a color reference cannot be resolved.
     * Default: "#000000" (black)
     */
    val defaultFallbackColor: String = "#000000",
    
    /**
     * Whether to show unresolved color references in the preview.
     * If true, keeps the original @color/xxx reference.
     * If false, replaces with fallback color.
     * Default: false
     */
    val showUnresolvedReferences: Boolean = false,
    
    /**
     * Whether to log warnings for unresolved color references.
     * Default: true
     */
    val logUnresolvedReferences: Boolean = true,
    
    /**
     * Maximum number of recursive color reference resolutions.
     * Prevents infinite loops in circular references.
     * Default: 10
     */
    val maxRecursionDepth: Int = 10,
    
    /**
     * Whether to cache resolved colors for performance.
     * Default: true
     */
    val enableCaching: Boolean = true,
    
    /**
     * Maximum cache size for resolved colors.
     * Default: 1000
     */
    val maxCacheSize: Int = 1000,
    
    /**
     * Whether to use async color resolution for better performance.
     * Default: true
     */
    val enableAsyncResolution: Boolean = true,
    
    /**
     * Timeout in milliseconds for async color resolution.
     * Default: 5000 (5 seconds)
     */
    val asyncResolutionTimeout: Long = 5000,
    
    /**
     * Whether to resolve theme attributes (?attr/xxx).
     * Default: false (not fully supported yet)
     */
    val resolveThemeAttributes: Boolean = false
) {
    companion object {
        /**
         * Default configuration with sensible defaults.
         */
        val DEFAULT = ColorResolutionConfig()
        
        /**
         * Configuration for development/debugging.
         * Shows unresolved references and enables all logging.
         */
        val DEBUG = ColorResolutionConfig(
            showUnresolvedReferences = true,
            logUnresolvedReferences = true,
            enableCaching = false
        )
        
        /**
         * Configuration for maximum performance.
         * Disables logging and uses aggressive caching.
         */
        val PERFORMANCE = ColorResolutionConfig(
            logUnresolvedReferences = false,
            enableCaching = true,
            maxCacheSize = 5000,
            enableAsyncResolution = true,
            asyncResolutionTimeout = 2000
        )
    }
}