package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure.parsers

import com.intellij.openapi.vfs.VirtualFile
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class XmlResourceFileParserTest {
    
    private lateinit var parser: XmlResourceFileParser
    
    @BeforeEach
    fun setUp() {
        parser = XmlResourceFileParser()
    }
    
    @Test
    fun `parseResourceFile should extract direct hex colors`() {
        // Given
        val mockFile = mock<VirtualFile>()
        val xmlContent = """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <color name="primary">#FF0000</color>
                <color name="secondary">#00FF00</color>
            </resources>
        """.trimIndent()
        whenever(mockFile.contentsToByteArray()).thenReturn(xmlContent.toByteArray())
        
        // When
        val result = parser.parseResourceFile(mockFile)
        
        // Then
        assertEquals(2, result.size)
        assertEquals("#FF0000", result["primary"])
        assertEquals("#00FF00", result["secondary"])
    }
    
    @Test
    fun `parseResourceFile should extract color references`() {
        // Given
        val mockFile = mock<VirtualFile>()
        val xmlContent = """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <color name="primary_dark">@color/primary</color>
                <color name="text_primary">@color/black</color>
            </resources>
        """.trimIndent()
        whenever(mockFile.contentsToByteArray()).thenReturn(xmlContent.toByteArray())
        
        // When
        val result = parser.parseResourceFile(mockFile)
        
        // Then
        assertEquals(2, result.size)
        assertEquals("@color/primary", result["primary_dark"])
        assertEquals("@color/black", result["text_primary"])
    }
    
    @Test
    fun `parseResourceFile should extract Android system colors`() {
        // Given
        val mockFile = mock<VirtualFile>()
        val xmlContent = """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <color name="background">@android:color/white</color>
                <color name="text">@android:color/black</color>
            </resources>
        """.trimIndent()
        whenever(mockFile.contentsToByteArray()).thenReturn(xmlContent.toByteArray())
        
        // When
        val result = parser.parseResourceFile(mockFile)
        
        // Then
        assertEquals(2, result.size)
        assertEquals("@android:color/white", result["background"])
        assertEquals("@android:color/black", result["text"])
    }
    
    @Test
    fun `parseResourceFile should uppercase hex colors`() {
        // Given
        val mockFile = mock<VirtualFile>()
        val xmlContent = """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <color name="primary">#ff0000</color>
                <color name="secondary">#00ff00</color>
            </resources>
        """.trimIndent()
        whenever(mockFile.contentsToByteArray()).thenReturn(xmlContent.toByteArray())
        
        // When
        val result = parser.parseResourceFile(mockFile)
        
        // Then
        assertEquals("#FF0000", result["primary"])
        assertEquals("#00FF00", result["secondary"])
    }
    
    @Test
    fun `parseResourceFile should handle empty color values`() {
        // Given
        val mockFile = mock<VirtualFile>()
        val xmlContent = """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <color name="empty"></color>
                <color name="valid">#FF0000</color>
            </resources>
        """.trimIndent()
        whenever(mockFile.contentsToByteArray()).thenReturn(xmlContent.toByteArray())
        
        // When
        val result = parser.parseResourceFile(mockFile)
        
        // Then
        assertEquals(1, result.size)
        assertEquals("#FF0000", result["valid"])
        assertTrue(result["empty"] == null)
    }
    
    @Test
    fun `parseResourceFile should handle malformed XML gracefully`() {
        // Given
        val mockFile = mock<VirtualFile>()
        val xmlContent = "This is not valid XML"
        whenever(mockFile.contentsToByteArray()).thenReturn(xmlContent.toByteArray())
        whenever(mockFile.path).thenReturn("/test/colors.xml")
        
        // When
        val result = parser.parseResourceFile(mockFile)
        
        // Then
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `parseRTxtFile should extract color names`() {
        // Given
        val mockFile = mock<VirtualFile>()
        val rTxtContent = """
            int anim fade_in 0x7f010000
            int color colorPrimary 0x7f060001
            int color colorPrimaryDark 0x7f060002
            int color colorAccent 0x7f060003
            int drawable ic_launcher 0x7f080000
        """.trimIndent()
        val inputStream = rTxtContent.byteInputStream()
        whenever(mockFile.inputStream).thenReturn(inputStream)
        
        // When
        val result = parser.parseRTxtFile(mockFile)
        
        // Then
        assertEquals(3, result.size)
        assertTrue("colorPrimary" in result)
        assertTrue("colorPrimaryDark" in result)
        assertTrue("colorAccent" in result)
    }
    
    @Test
    fun `parseRTxtFile should handle empty file`() {
        // Given
        val mockFile = mock<VirtualFile>()
        val inputStream = "".byteInputStream()
        whenever(mockFile.inputStream).thenReturn(inputStream)
        
        // When
        val result = parser.parseRTxtFile(mockFile)
        
        // Then
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `parseRTxtFile should handle file read errors gracefully`() {
        // Given
        val mockFile = mock<VirtualFile>()
        whenever(mockFile.inputStream).thenThrow(RuntimeException("File not found"))
        whenever(mockFile.path).thenReturn("/test/R.txt")
        
        // When
        val result = parser.parseRTxtFile(mockFile)
        
        // Then
        assertTrue(result.isEmpty())
    }
}