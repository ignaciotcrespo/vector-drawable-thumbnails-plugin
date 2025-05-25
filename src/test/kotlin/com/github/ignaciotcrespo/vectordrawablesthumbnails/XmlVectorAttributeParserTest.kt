package com.github.ignaciotcrespo.vectordrawablesthumbnails

import com.android.ide.common.vectordrawable.VdPreview
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.mockStatic
import org.mockito.kotlin.whenever
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File

class XmlVectorAttributeParserTest {

    private lateinit var parser: XmlVectorAttributeParser
    private lateinit var mockValidFile: ValidFile
    private lateinit var mockFile: File

    @BeforeEach
    fun setUp() {
        parser = XmlVectorAttributeParser()
        mockFile = mock()
        mockValidFile = ValidFile(mockFile, "/mock/project/root")

        // Mock behavior for mockFile if necessary, e.g., name, length
        whenever(mockFile.name).thenReturn("test_vector.xml")
        whenever(mockFile.length()).thenReturn(100L)
    }

    @Test
    fun `parse valid vector xml`() {
        val xmlContent = """
            <vector xmlns:android="http://schemas.android.com/apk/res/android"
                android:width="24dp"
                android:height="24dp"
                android:viewportWidth="24.0"
                android:viewportHeight="24.0">
                <path
                    android:fillColor="#FF000000"
                    android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2z"/>
            </vector>
        """.trimIndent()

        val mockDocument = mock<Document>()
        val mockElement = mock<Element>()
        whenever(mockDocument.documentElement).thenReturn(mockElement)
        whenever(mockElement.tagName).thenReturn("vector")
        whenever(mockElement.getAttribute("android:viewportWidth")).thenReturn("24.0")
        whenever(mockElement.getAttribute("android:viewportHeight")).thenReturn("24.0")

        mockStatic(VdPreview::class.java).use { vdPreviewMock ->
            vdPreviewMock.`when`<Document> { VdPreview.parseVdStringIntoDocument(any(), any()) }.thenReturn(mockDocument)

            val result = parser.parse(xmlContent, "test_vector.xml", 100L, mockValidFile)

            assertNotNull(result)
            assertEquals("test_vector.xml", result!!.name)
            assertEquals(24, result.viewportW)
            assertEquals(24, result.viewportH)
            assertEquals(100L, result.fileSize)
            assertEquals(mockDocument, result.document)
            assertEquals(mockValidFile, result.validFile)
        }
    }

    @Test
    fun `parse vector xml with @color replacement`() {
        val xmlContent = """
            <vector xmlns:android="http://schemas.android.com/apk/res/android"
                android:width="24dp"
                android:height="24dp"
                android:viewportWidth="24.0"
                android:viewportHeight="24.0">
                <path
                    android:fillColor="@color/some_color"
                    android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2z"/>
            </vector>
        """.trimIndent()
        val expectedXmlAfterReplacement = xmlContent.replace("@color/some_color", "#000000")

        val mockDocument = mock<Document>()
        val mockElement = mock<Element>()
        whenever(mockDocument.documentElement).thenReturn(mockElement)
        whenever(mockElement.tagName).thenReturn("vector")
        whenever(mockElement.getAttribute("android:viewportWidth")).thenReturn("24.0")
        whenever(mockElement.getAttribute("android:viewportHeight")).thenReturn("24.0")

        mockStatic(VdPreview::class.java).use { vdPreviewMock ->
            vdPreviewMock.`when`<Document> { VdPreview.parseVdStringIntoDocument(expectedXmlAfterReplacement, any()) }.thenReturn(mockDocument)

            val result = parser.parse(xmlContent, "test_color_vector.xml", 120L, mockValidFile)

            assertNotNull(result)
            assertEquals("test_color_vector.xml", result!!.name)
            assertEquals(24, result.viewportW)
            assertEquals(24, result.viewportH)
            // Verify that parseVdStringIntoDocument was called with the processed XML
            vdPreviewMock.verify { VdPreview.parseVdStringIntoDocument(expectedXmlAfterReplacement, any()) }
        }
    }

    @Test
    fun `parse non-vector xml`() {
        val xmlContent = "<layout></layout>"
        // VdPreview.parseVdStringIntoDocument will likely throw an error or return a non-vector document
        // For this test, we'll assume it returns a document where root element is not "vector"
        val mockDocument = mock<Document>()
        val mockElement = mock<Element>()
        whenever(mockDocument.documentElement).thenReturn(mockElement)
        whenever(mockElement.tagName).thenReturn("layout") // Not "vector"

        mockStatic(VdPreview::class.java).use { vdPreviewMock ->
            vdPreviewMock.`when`<Document> { VdPreview.parseVdStringIntoDocument(any(), any()) }.thenReturn(mockDocument)
            val result = parser.parse(xmlContent, "not_a_vector.xml", 50L, mockValidFile)
            assertNull(result)
        }
    }

    @Test
    fun `parse malformed xml`() {
        val xmlContent = "<vector><unclosed_tag></vector>"
        mockStatic(VdPreview::class.java).use { vdPreviewMock ->
            vdPreviewMock.`when`<Document> { VdPreview.parseVdStringIntoDocument(any(), any()) }.thenThrow(RuntimeException("XML parsing error"))
            val result = parser.parse(xmlContent, "malformed.xml", 30L, mockValidFile)
            assertNull(result)
        }
    }

    @Test
    fun `parse xml that is not a vector file`() {
        val xmlContent = "<notvector></notvector>" // Content that doesn't contain "</vector>"
        // The initial check for `xmlContent.contains("</vector>")` should cause it to return null early.
        val result = parser.parse(xmlContent, "not_vector_tag.xml", 60L, mockValidFile)
        assertNull(result)
    }

    @Test
    fun `parse vector xml with missing viewport attributes`() {
        val xmlContent = """
            <vector xmlns:android="http://schemas.android.com/apk/res/android"
                android:width="24dp"
                android:height="24dp">
                <path android:pathData="M0,0h24v24H0z"/>
            </vector>
        """.trimIndent()

        val mockDocument = mock<Document>()
        val mockElement = mock<Element>()
        whenever(mockDocument.documentElement).thenReturn(mockElement)
        whenever(mockElement.tagName).thenReturn("vector")
        // Return null or empty for viewport attributes to simulate them missing
        whenever(mockElement.getAttribute("android:viewportWidth")).thenReturn(null)
        whenever(mockElement.getAttribute("android:viewportHeight")).thenReturn("") // or null

        mockStatic(VdPreview::class.java).use { vdPreviewMock ->
            vdPreviewMock.`when`<Document> { VdPreview.parseVdStringIntoDocument(any(), any()) }.thenReturn(mockDocument)

            val result = parser.parse(xmlContent, "missing_viewport.xml", 100L, mockValidFile)

            assertNotNull(result)
            assertEquals(0, result!!.viewportW) // Default to 0 if attribute missing/invalid
            assertEquals(0, result.viewportH) // Default to 0 if attribute missing/invalid
        }
    }
}
