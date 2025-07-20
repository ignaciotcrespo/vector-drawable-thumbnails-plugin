package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ColorResolver
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ColorResolutionResult
import com.intellij.openapi.diagnostic.Logger

/**
 * Improved implementation of ColorResolver with better error handling
 * and support for complex color resolution scenarios.
 */
class ImprovedColorResolver : ColorResolver {
    
    companion object {
        private val LOG = Logger.getInstance(ImprovedColorResolver::class.java)
        private const val MAX_RECURSION_DEPTH = 10
        
        // Android system colors mapping
        private val ANDROID_SYSTEM_COLORS = mapOf(
            "@android:color/black" to "#000000",
            "@android:color/white" to "#FFFFFF",
            "@android:color/transparent" to "#00000000",
            "@android:color/darker_gray" to "#AAA",
            "@android:color/holo_blue_bright" to "#FF00DDFF",
            "@android:color/holo_blue_dark" to "#FF0099CC",
            "@android:color/holo_blue_light" to "#FF33B5E5",
            "@android:color/holo_green_dark" to "#FF669900",
            "@android:color/holo_green_light" to "#FF99CC00",
            "@android:color/holo_orange_dark" to "#FFFF8800",
            "@android:color/holo_orange_light" to "#FFFFBB33",
            "@android:color/holo_purple" to "#FFAA66CC",
            "@android:color/holo_red_dark" to "#FFCC0000",
            "@android:color/holo_red_light" to "#FFFF4444",
            "@android:color/background_dark" to "#FF000000",
            "@android:color/background_light" to "#FFFFFFFF",
            "@android:color/primary_text_dark" to "#FFFFFFFF",
            "@android:color/primary_text_light" to "#FF000000",
            "@android:color/secondary_text_dark" to "#BEBEBE",
            "@android:color/secondary_text_light" to "#323232"
        )
    }
    
    override fun resolveColor(colorReference: String, colorMap: Map<String, String>): String? {
        val result = resolveColorWithPath(colorReference, colorMap)
        return result.resolvedColor
    }
    
    override fun isColorReference(value: String): Boolean {
        return value.startsWith("@color/") || 
               value.startsWith("@android:color/") ||
               value.startsWith("?attr/") ||
               value.startsWith("?android:attr/")
    }
    
    /**
     * Resolves a color with full resolution path tracking.
     */
    fun resolveColorWithPath(
        colorReference: String,
        colorMap: Map<String, String>,
        visitedColors: MutableSet<String> = mutableSetOf(),
        resolutionPath: MutableList<String> = mutableListOf(),
        depth: Int = 0
    ): ColorResolutionResult {
        // Check for circular references or max depth
        if (depth > MAX_RECURSION_DEPTH) {
            LOG.warn("Max recursion depth reached for color: $colorReference")
            return ColorResolutionResult(null, resolutionPath)
        }
        
        if (colorReference in visitedColors) {
            LOG.warn("Circular reference detected: $colorReference. Path: ${resolutionPath.joinToString(" -> ")}")
            return ColorResolutionResult(null, resolutionPath)
        }
        
        visitedColors.add(colorReference)
        resolutionPath.add(colorReference)
        
        try {
            // Handle direct hex colors
            if (colorReference.startsWith("#")) {
                return ColorResolutionResult(
                    normalizeHexColor(colorReference),
                    resolutionPath
                )
            }
            
            // Handle Android system colors
            if (colorReference.startsWith("@android:color/")) {
                val systemColor = ANDROID_SYSTEM_COLORS[colorReference]
                return ColorResolutionResult(
                    systemColor,
                    resolutionPath,
                    isSystemColor = true
                )
            }
            
            // Handle theme attributes (not fully supported, but handle gracefully)
            if (colorReference.startsWith("?attr/") || colorReference.startsWith("?android:attr/")) {
                LOG.debug("Theme attribute reference not supported: $colorReference")
                return ColorResolutionResult(null, resolutionPath)
            }
            
            // Extract color name from reference
            val colorName = when {
                colorReference.startsWith("@color/") -> colorReference.removePrefix("@color/")
                colorReference.startsWith("@+id/color/") -> colorReference.removePrefix("@+id/color/")
                else -> colorReference
            }.trim()
            
            // Look up in color map
            val colorValue = colorMap[colorName] ?: return ColorResolutionResult(null, resolutionPath)
            
            // If the value is another reference, resolve recursively
            return when {
                isColorReference(colorValue) -> {
                    resolveColorWithPath(colorValue, colorMap, visitedColors, resolutionPath, depth + 1)
                }
                colorValue.startsWith("#") -> {
                    ColorResolutionResult(
                        normalizeHexColor(colorValue),
                        resolutionPath
                    )
                }
                else -> {
                    // Try to parse as a color state list or other format
                    LOG.debug("Unsupported color format: $colorValue")
                    ColorResolutionResult(null, resolutionPath)
                }
            }
            
        } catch (e: Exception) {
            LOG.error("Error resolving color: $colorReference", e)
            return ColorResolutionResult(null, resolutionPath)
        }
    }
    
    /**
     * Normalizes hex color to #RRGGBB or #AARRGGBB format.
     */
    private fun normalizeHexColor(hexColor: String): String {
        val color = hexColor.trim().uppercase()
        
        return when (color.length) {
            4 -> {
                // #RGB -> #RRGGBB
                val r = color[1]
                val g = color[2]
                val b = color[3]
                "#$r$r$g$g$b$b"
            }
            5 -> {
                // #ARGB -> #AARRGGBB
                val a = color[1]
                val r = color[2]
                val g = color[3]
                val b = color[4]
                "#$a$a$r$r$g$g$b$b"
            }
            7, 9 -> {
                // #RRGGBB or #AARRGGBB
                color
            }
            else -> {
                LOG.warn("Invalid hex color format: $hexColor")
                hexColor
            }
        }
    }
}