package com.github.ignaciotcrespo.vectordrawablesthumbnails.model

/**
 * Represents the different types of vector files supported by the plugin.
 * This allows the plugin to be extended with new file types in the future.
 */
enum class FileType(val extension: String, val displayName: String) {
    VECTOR_DRAWABLE(".xml", "Vector Drawable"),
    SVG(".svg", "SVG"),
    PNG(".png", "PNG"),
    JPG(".jpg", "JPG"),
    JPEG(".jpeg", "JPEG"),
    WEBP(".webp", "WebP"),
    GIF(".gif", "GIF"),
    BMP(".bmp", "BMP");

    /**
     * True for raster/bitmap image formats decoded via ImageIO.
     */
    val isRasterImage: Boolean
        get() = this in RASTER_TYPES

    /**
     * Checks if a filename matches this file type.
     */
    fun matches(fileName: String): Boolean {
        return fileName.endsWith(extension, ignoreCase = true)
    }

    companion object {
        val RASTER_TYPES: Set<FileType> = setOf(PNG, JPG, JPEG, WEBP, GIF, BMP)

        /**
         * Returns the FileType for the given filename, or null if not supported.
         */
        fun fromFileName(fileName: String): FileType? {
            return values().find { it.matches(fileName) }
        }
    }
}
