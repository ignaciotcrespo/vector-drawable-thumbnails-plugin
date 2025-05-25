package com.github.ignaciotcrespo.vectordrawablesthumbnails

import com.android.ide.common.vectordrawable.VdPreview
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.w3c.dom.Document
import java.awt.image.BufferedImage
import java.io.File

class VdPreviewRendererTest {

    private lateinit var renderer: VdPreviewRenderer
    private lateinit var mockParsedVectorAttributes: ParsedVectorAttributes
    private lateinit var mockDocument: Document
    private lateinit var mockValidFile: ValidFile
    private lateinit var mockFile: File // For ValidFile

    @BeforeEach
    fun setUp() {
        renderer = VdPreviewRenderer()
        mockDocument = mock()
        mockFile = mock() // Mock the File object for ValidFile
        mockValidFile = ValidFile(mockFile, "/mock/project/root") // Create ValidFile with mocked File

        mockParsedVectorAttributes = ParsedVectorAttributes(
            name = "test_vector.xml",
            viewportW = 24,
            viewportH = 24,
            fileSize = 100L,
            document = mockDocument,
            validFile = mockValidFile // Use the mocked ValidFile
        )
    }

    @Test
    fun `render successful`() {
        val mockImage = mock<BufferedImage>()

        mockStatic(VdPreview::class.java).use { vdPreviewMock ->
            vdPreviewMock.`when`<BufferedImage> {
                VdPreview.getPreviewFromVectorDocument(
                    any(), // VdPreview.TargetSize can be tricky to mock specifically, any() is safer
                    eq(mockDocument),
                    any()
                )
            }.thenReturn(mockImage)

            val result = renderer.render(mockParsedVectorAttributes)

            assertNotNull(result)
            assertEquals(mockImage, result)
            // Verify that the static method was called with the correct document
            vdPreviewMock.verify { VdPreview.getPreviewFromVectorDocument(any(), eq(mockDocument), any()) }
        }
    }

    @Test
    fun `render returns null when VdPreview throws exception`() {
        mockStatic(VdPreview::class.java).use { vdPreviewMock ->
            vdPreviewMock.`when`<BufferedImage> {
                VdPreview.getPreviewFromVectorDocument(
                    any(),
                    eq(mockDocument),
                    any()
                )
            }.thenThrow(RuntimeException("Rendering failed"))

            val result = renderer.render(mockParsedVectorAttributes)

            assertNull(result)
        }
    }

    @Test
    fun `render returns null when VdPreview returns null`() {
        mockStatic(VdPreview::class.java).use { vdPreviewMock ->
            vdPreviewMock.`when`<BufferedImage?> { // Note the nullable BufferedImage?
                VdPreview.getPreviewFromVectorDocument(
                    any(),
                    eq(mockDocument),
                    any()
                )
            }.thenReturn(null) // Simulate VdPreview returning null

            val result = renderer.render(mockParsedVectorAttributes)

            assertNull(result)
        }
    }
}
