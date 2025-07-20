package com.github.ignaciotcrespo.vectordrawablesthumbnails.model

import java.awt.image.BufferedImage
import java.time.LocalDateTime

/**
 * Enhanced VectorItem with analytics and metadata.
 * Represents a vector drawable with comprehensive information.
 */
data class VectorItem(
    val name: String,
    val image: BufferedImage,
    val validFile: ValidFile,
    val viewportW: Int = 0,
    val viewportH: Int = 0,
    val fileSize: Long = 0,
    val analytics: VectorAnalytics? = null,
    val lastModified: LocalDateTime? = null,
    val category: String? = null,
    val description: String? = null
) {
    /**
     * Convenience property for aspect ratio.
     */
    val aspectRatio: Double
        get() = if (viewportH != 0) viewportW.toDouble() / viewportH.toDouble() else 1.0
    
    /**
     * Convenience property for display size.
     */
    val displaySize: String
        get() = "${viewportW}×${viewportH}"
    
    /**
     * Convenience property for file size in human-readable format.
     */
    val fileSizeFormatted: String
        get() = when {
            fileSize < 1024 -> "${fileSize}B"
            fileSize < 1024 * 1024 -> "${fileSize / 1024}KB"
            else -> "${fileSize / (1024 * 1024)}MB"
        }
    
    /**
     * Check if this vector is considered large (over 10KB).
     */
    val isLarge: Boolean
        get() = fileSize > 10 * 1024
    
    /**
     * Check if this vector has a square aspect ratio.
     */
    val isSquare: Boolean
        get() = viewportW == viewportH
}