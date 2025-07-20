package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for ImprovedColorResolver focusing on edge cases,
 * circular references, and complex resolution scenarios.
 */
class ImprovedColorResolverEdgeCasesTest {
    
    private lateinit var resolver: ImprovedColorResolver
    
    @Before
    fun setUp() {
        resolver = ImprovedColorResolver()
    }
    
    @Test
    fun `test resolve direct hex color`() {
        val result = resolver.resolveColor("#FF0000", emptyMap())
        assertEquals("#FF0000", result)
    }
    
    @Test
    fun `test normalize short hex color RGB to RRGGBB`() {
        val result = resolver.resolveColor("#F00", emptyMap())
        assertEquals("#FF0000", result)
    }
    
    @Test
    fun `test normalize short hex color ARGB to AARRGGBB`() {
        val result = resolver.resolveColor("#8F00", emptyMap())
        assertEquals("#88FF0000", result)
    }
    
    @Test
    fun `test resolve Android system color`() {
        val result = resolver.resolveColor("@android:color/black", emptyMap())
        assertEquals("#000000", result)
    }
    
    @Test
    fun `test resolve Android system color white`() {
        val result = resolver.resolveColor("@android:color/white", emptyMap())
        assertEquals("#FFFFFF", result)
    }
    
    @Test
    fun `test resolve Android system color transparent`() {
        val result = resolver.resolveColor("@android:color/transparent", emptyMap())
        assertEquals("#00000000", result)
    }
    
    @Test
    fun `test resolve simple color reference`() {
        val colorMap = mapOf("primary" to "#FF0000")
        val result = resolver.resolveColor("@color/primary", colorMap)
        assertEquals("#FF0000", result)
    }
    
    @Test
    fun `test resolve nested color reference`() {
        val colorMap = mapOf(
            "primary" to "@color/brand",
            "brand" to "#FF0000"
        )
        val result = resolver.resolveColor("@color/primary", colorMap)
        assertEquals("#FF0000", result)
    }
    
    @Test
    fun `test resolve deeply nested color reference`() {
        val colorMap = mapOf(
            "primary" to "@color/secondary",
            "secondary" to "@color/tertiary",
            "tertiary" to "@color/brand",
            "brand" to "#FF0000"
        )
        val result = resolver.resolveColor("@color/primary", colorMap)
        assertEquals("#FF0000", result)
    }
    
    @Test
    fun `test detect circular reference with two colors`() {
        val colorMap = mapOf(
            "color1" to "@color/color2",
            "color2" to "@color/color1"
        )
        val result = resolver.resolveColor("@color/color1", colorMap)
        assertNull(result)
    }
    
    @Test
    fun `test detect circular reference with three colors`() {
        val colorMap = mapOf(
            "color1" to "@color/color2",
            "color2" to "@color/color3",
            "color3" to "@color/color1"
        )
        val result = resolver.resolveColor("@color/color1", colorMap)
        assertNull(result)
    }
    
    @Test
    fun `test max recursion depth protection`() {
        // Create a chain longer than MAX_RECURSION_DEPTH (10)
        val colorMap = mutableMapOf<String, String>()
        for (i in 0..15) {
            colorMap["color$i"] = "@color/color${i + 1}"
        }
        colorMap["color16"] = "#FF0000"
        
        val result = resolver.resolveColor("@color/color0", colorMap)
        assertNull(result) // Should return null due to max depth
    }
    
    @Test
    fun `test resolve color with resolution path tracking`() {
        val colorMap = mapOf(
            "primary" to "@color/brand",
            "brand" to "#FF0000"
        )
        
        val result = resolver.resolveColorWithPath("@color/primary", colorMap)
        
        assertEquals("#FF0000", result.resolvedColor)
        assertEquals(listOf("@color/primary", "@color/brand"), result.resolutionPath)
    }
    
    @Test
    fun `test handle theme attribute references`() {
        // Theme attributes are not fully supported but should be handled gracefully
        val result = resolver.resolveColor("?attr/colorPrimary", emptyMap())
        assertNull(result)
    }
    
    @Test
    fun `test handle android theme attribute references`() {
        val result = resolver.resolveColor("?android:attr/colorPrimary", emptyMap())
        assertNull(result)
    }
    
    @Test
    fun `test resolve color with plus id prefix`() {
        val colorMap = mapOf("primary" to "#FF0000")
        val result = resolver.resolveColor("@+id/color/primary", colorMap)
        assertEquals("#FF0000", result)
    }
    
    @Test
    fun `test resolve missing color returns null`() {
        val result = resolver.resolveColor("@color/nonexistent", emptyMap())
        assertNull(result)
    }
    
    @Test
    fun `test resolve invalid hex color format`() {
        val result = resolver.resolveColor("#GGGGGG", emptyMap())
        assertEquals("#GGGGGG", result) // Returns as-is when invalid
    }
    
    @Test
    fun `test isColorReference correctly identifies references`() {
        assertTrue(resolver.isColorReference("@color/primary"))
        assertTrue(resolver.isColorReference("@android:color/black"))
        assertTrue(resolver.isColorReference("?attr/colorPrimary"))
        assertTrue(resolver.isColorReference("?android:attr/colorPrimary"))
        
        assertFalse(resolver.isColorReference("#FF0000"))
        assertFalse(resolver.isColorReference("primary"))
        assertFalse(resolver.isColorReference(""))
    }
    
    @Test
    fun `test resolve color with whitespace`() {
        val colorMap = mapOf("primary" to "  #FF0000  ")
        val result = resolver.resolveColor("@color/primary", colorMap)
        assertEquals("#FF0000", result)
    }
    
    @Test
    fun `test resolution result includes system color flag`() {
        val result = resolver.resolveColorWithPath("@android:color/black", emptyMap())
        
        assertEquals("#000000", result.resolvedColor)
        assertTrue(result.isSystemColor)
        assertEquals(listOf("@android:color/black"), result.resolutionPath)
    }
    
    @Test
    fun `test unsupported color format returns null`() {
        val colorMap = mapOf("primary" to "rgb(255, 0, 0)")
        val result = resolver.resolveColor("@color/primary", colorMap)
        assertNull(result)
    }
    
    private fun assertFalse(condition: Boolean) {
        assertTrue(!condition)
    }
}