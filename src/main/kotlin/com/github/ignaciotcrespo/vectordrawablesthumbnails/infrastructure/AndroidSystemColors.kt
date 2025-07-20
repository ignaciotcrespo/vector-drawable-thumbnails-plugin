package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

/**
 * Maps Android system color names to their hex values.
 * Based on Android's default color resources.
 */
object AndroidSystemColors {
    
    private val systemColors = mapOf(
        // Primary colors
        "black" to "#000000",
        "white" to "#FFFFFF",
        "transparent" to "#00000000",
        
        // Material Design colors
        "holo_blue_light" to "#FF33B5E5",
        "holo_blue_dark" to "#FF0099CC",
        "holo_blue_bright" to "#FF00DDFF",
        "holo_green_light" to "#FF99CC00",
        "holo_green_dark" to "#FF669900",
        "holo_orange_light" to "#FFFFBB33",
        "holo_orange_dark" to "#FFFF8800",
        "holo_red_light" to "#FFFF4444",
        "holo_red_dark" to "#FFCC0000",
        "holo_purple" to "#FFAA66CC",
        
        // Background colors
        "background_dark" to "#FF000000",
        "background_light" to "#FFFFFFFF",
        
        // System UI colors
        "primary_text_dark" to "#FFFFFFFF",
        "primary_text_light" to "#DE000000",
        "secondary_text_dark" to "#B3FFFFFF",
        "secondary_text_light" to "#8A000000",
        "tertiary_text_dark" to "#80FFFFFF",
        "tertiary_text_light" to "#61000000",
        
        // Widget colors
        "widget_edittext_dark" to "#FF000000",
        "tab_indicator_text" to "#FFFFFFFF",
        
        // Common colors
        "darker_gray" to "#FFaaaaaa",
        "lighter_gray" to "#FFdddddd"
    )
    
    fun getSystemColor(colorName: String): String? {
        return systemColors[colorName]
    }
    
    fun isSystemColor(colorReference: String): Boolean {
        return colorReference.startsWith("@android:color/") && 
               systemColors.containsKey(colorReference.substring(15))
    }
}