package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImprovedColorResolverTest {
    
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
    fun `test resolve hex color with normalization`() {
        val result = resolver.resolveColor("#F00", emptyMap())
        assertEquals("#FF0000", result)
    }
    
    @Test
    fun `test resolve android system color`() {
        val result = resolver.resolveColor("@android:color/black", emptyMap())
        assertEquals("#000000", result)
    }
    
    @Test
    fun `test resolve color reference`() {
        val colorMap = mapOf("primary" to "#FF0000")
        val result = resolver.resolveColor("@color/primary", colorMap)
        assertEquals("#FF0000", result)
    }
    
    @Test
    fun `test resolve indirect color reference`() {
        val colorMap = mapOf(
            "primary" to "@color/brand_primary",
            "brand_primary" to "#FF0000"
        )
        val result = resolver.resolveColor("@color/primary", colorMap)
        assertEquals("#FF0000", result)
    }
    
    @Test
    fun `test resolve circular reference returns null`() {
        val colorMap = mapOf(
            "color1" to "@color/color2",
            "color2" to "@color/color1"
        )
        val result = resolver.resolveColor("@color/color1", colorMap)
        assertNull(result)
    }
    
    @Test
    fun `test resolve deep nested reference`() {
        val colorMap = mutableMapOf<String, String>()
        // Create a deep chain
        for (i in 0..8) {
            colorMap["color$i"] = "@color/color${i + 1}"
        }
        colorMap["color9"] = "#FF0000"
        
        val result = resolver.resolveColor("@color/color0", colorMap)
        assertEquals("#FF0000", result)
    }
    
    @Test
    fun `test resolve too deep nested reference returns null`() {
        val colorMap = mutableMapOf<String, String>()
        // Create a chain deeper than MAX_RECURSION_DEPTH
        for (i in 0..15) {
            colorMap["color$i"] = "@color/color${i + 1}"
        }
        colorMap["color16"] = "#FF0000"
        
        val result = resolver.resolveColor("@color/color0", colorMap)
        assertNull(result)
    }
    
    @Test
    fun `test isColorReference`() {
        assertTrue(resolver.isColorReference("@color/primary"))
        assertTrue(resolver.isColorReference("@android:color/black"))
        assertTrue(resolver.isColorReference("?attr/colorPrimary"))
        assertTrue(resolver.isColorReference("?android:attr/textColor"))
        
        assertTrue(!resolver.isColorReference("#FF0000"))
        assertTrue(!resolver.isColorReference("red"))
    }
    
    @Test
    fun `test resolveColorWithPath tracks resolution path`() {
        val colorMap = mapOf(
            "primary" to "@color/brand",
            "brand" to "#FF0000"
        )
        
        val result = resolver.resolveColorWithPath("@color/primary", colorMap)
        assertEquals("#FF0000", result.resolvedColor)
        assertEquals(listOf("@color/primary", "@color/brand"), result.resolutionPath)
    }
    
    @Test
    fun `test theme attribute returns null`() {
        val result = resolver.resolveColor("?attr/colorPrimary", emptyMap())
        assertNull(result)
    }
    
    @Test
    fun `test normalize ARGB hex color`() {
        val result = resolver.resolveColor("#8FFF", emptyMap())
        assertEquals("#88FFFFFF", result)
    }
    
    @Test
    fun `test missing color returns null`() {
        val result = resolver.resolveColor("@color/nonexistent", emptyMap())
        assertNull(result)
    }
}