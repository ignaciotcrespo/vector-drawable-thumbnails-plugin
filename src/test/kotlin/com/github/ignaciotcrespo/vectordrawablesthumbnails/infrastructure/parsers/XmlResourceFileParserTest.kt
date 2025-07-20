package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure.parsers

import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.LightVirtualFile
import org.junit.Test

class XmlResourceFileParserTest : LightPlatformTestCase() {
    
    private lateinit var parser: XmlResourceFileParser
    
    override fun setUp() {
        super.setUp()
        parser = XmlResourceFileParser()
    }
    
    @Test
    fun `parseResourceFile should extract direct hex colors`() {
        // Given
        val xmlContent = """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <color name="primary">#FF0000</color>
                <color name="secondary">#00FF00</color>
            </resources>
        """.trimIndent()
        val virtualFile = LightVirtualFile("colors.xml", xmlContent)
        
        // When
        val result = parser.parseResourceFile(virtualFile)
        
        // Then
        assertEquals(2, result.size)
        assertEquals("#FF0000", result["primary"])
        assertEquals("#00FF00", result["secondary"])
    }
    
    @Test
    fun `parseResourceFile should extract color references`() {
        // Given
        val xmlContent = """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <color name="primary_dark">@color/primary</color>
                <color name="text_primary">@color/black</color>
            </resources>
        """.trimIndent()
        val virtualFile = LightVirtualFile("colors.xml", xmlContent)
        
        // When
        val result = parser.parseResourceFile(virtualFile)
        
        // Then
        assertEquals(2, result.size)
        assertEquals("@color/primary", result["primary_dark"])
        assertEquals("@color/black", result["text_primary"])
    }
    
    @Test
    fun `parseResourceFile should extract Android system colors`() {
        // Given
        val xmlContent = """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <color name="background">@android:color/white</color>
                <color name="text">@android:color/black</color>
            </resources>
        """.trimIndent()
        val virtualFile = LightVirtualFile("colors.xml", xmlContent)
        
        // When
        val result = parser.parseResourceFile(virtualFile)
        
        // Then
        assertEquals(2, result.size)
        assertEquals("@android:color/white", result["background"])
        assertEquals("@android:color/black", result["text"])
    }
    
    @Test
    fun `parseResourceFile should uppercase hex colors`() {
        // Given
        val xmlContent = """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <color name="primary">#ff0000</color>
                <color name="secondary">#00ff00</color>
            </resources>
        """.trimIndent()
        val virtualFile = LightVirtualFile("colors.xml", xmlContent)
        
        // When
        val result = parser.parseResourceFile(virtualFile)
        
        // Then
        assertEquals("#FF0000", result["primary"])
        assertEquals("#00FF00", result["secondary"])
    }
    
    @Test
    fun `parseResourceFile should handle empty color values`() {
        // Given
        val xmlContent = """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <color name="empty"></color>
                <color name="valid">#FF0000</color>
            </resources>
        """.trimIndent()
        val virtualFile = LightVirtualFile("colors.xml", xmlContent)
        
        // When
        val result = parser.parseResourceFile(virtualFile)
        
        // Then
        assertEquals(1, result.size)
        assertEquals("#FF0000", result["valid"])
        assertTrue(result["empty"] == null)
    }
    
    @Test
    fun `parseResourceFile should handle malformed XML gracefully`() {
        // Given
        val xmlContent = "This is not valid XML"
        val virtualFile = LightVirtualFile("colors.xml", xmlContent)
        
        // When
        val result = parser.parseResourceFile(virtualFile)
        
        // Then
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `parseRTxtFile should extract color names`() {
        // Given
        val rTxtContent = """
            int anim fade_in 0x7f010000
            int color colorPrimary 0x7f060001
            int color colorPrimaryDark 0x7f060002
            int color colorAccent 0x7f060003
            int drawable ic_launcher 0x7f080000
        """.trimIndent()
        val virtualFile = LightVirtualFile("R.txt", rTxtContent)
        
        // When
        val result = parser.parseRTxtFile(virtualFile)
        
        // Then
        assertEquals(3, result.size)
        assertTrue("colorPrimary" in result)
        assertTrue("colorPrimaryDark" in result)
        assertTrue("colorAccent" in result)
    }
    
    @Test
    fun `parseRTxtFile should handle empty file`() {
        // Given
        val virtualFile = LightVirtualFile("R.txt", "")
        
        // When
        val result = parser.parseRTxtFile(virtualFile)
        
        // Then
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `parseRTxtFile should handle file read errors gracefully`() {
        // Given
        // Create a custom VirtualFile that throws exception on inputStream
        val virtualFile = object : LightVirtualFile("R.txt", "") {
            override fun getInputStream(): java.io.InputStream {
                throw RuntimeException("File not found")
            }
        }
        
        // When
        val result = parser.parseRTxtFile(virtualFile)
        
        // Then
        assertTrue(result.isEmpty())
    }
}