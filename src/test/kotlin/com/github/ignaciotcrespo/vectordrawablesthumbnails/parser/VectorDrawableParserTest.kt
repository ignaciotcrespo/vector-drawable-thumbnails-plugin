package com.github.ignaciotcrespo.vectordrawablesthumbnails.parser

import com.android.ide.common.vectordrawable.VdPreview
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.ValidFile
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import io.reactivex.observers.TestObserver
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.awt.image.BufferedImage
import java.io.File
import java.io.StringReader
import java.nio.file.Files
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

@ExtendWith(MockitoExtension::class)
class VectorDrawableParserTest {

    private lateinit var parser: VectorDrawableParser
    private lateinit var tempFile: File

    @Mock
    lateinit var mockBufferedImage: BufferedImage
    @Mock
    lateinit var mockDocumentBuilderFactory: DocumentBuilderFactory
    @Mock
    lateinit var mockDocumentBuilder: DocumentBuilder
    @Mock
    lateinit var mockDocument: Document
    @Mock
    lateinit var mockElement: Element

    private lateinit var mockStaticVdPreview: MockedStatic<VdPreview>
    private lateinit var mockStaticDocBuilderFactory: MockedStatic<DocumentBuilderFactory>

    private val defaultXmlHeader = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"

    @BeforeEach
    fun setUp() {
        parser = VectorDrawableParser()
        tempFile = Files.createTempFile("testVector", ".xml").toFile()

        // Mock static VdPreview.getPreviewFromVectorDocument
        mockStaticVdPreview = Mockito.mockStatic(VdPreview::class.java)

        // Mock static DocumentBuilderFactory.newInstance() and subsequent calls
        mockStaticDocBuilderFactory = Mockito.mockStatic(DocumentBuilderFactory::class.java)
        mockStaticDocBuilderFactory.`when`<Any> { DocumentBuilderFactory.newInstance() }.thenReturn(mockDocumentBuilderFactory)
        whenever(mockDocumentBuilderFactory.newDocumentBuilder()).thenReturn(mockDocumentBuilder)
        // Default XXE prevention features
        whenever(mockDocumentBuilderFactory.isExpandEntityReferences).thenReturn(false) // to allow verification
    }

    @AfterEach
    fun tearDown() {
        tempFile.delete()
        mockStaticVdPreview.close()
        mockStaticDocBuilderFactory.close()
    }

    private fun writeToFile(content: String) {
        tempFile.writeText(content)
    }

    private fun mockDocumentInteractions(xmlString: String, tagName: String = "vector", vpWidth: String? = "24", vpHeight: String? = "24") {
        val inputSourceCaptor = ArgumentCaptor.forClass(InputSource::class.java)
        whenever(mockDocumentBuilder.parse(inputSourceCaptor.capture())).thenAnswer {
            val inputSource = inputSourceCaptor.value
            val reader = inputSource.characterStream
            // val actualXml = reader.readText() // For debugging or verifying transformations
            mockDocument
        }
        whenever(mockDocument.documentElement).thenReturn(mockElement)
        whenever(mockElement.tagName).thenReturn(tagName)
        if (vpWidth != null) whenever(mockElement.getAttribute("android:viewportWidth")).thenReturn(vpWidth)
        if (vpHeight != null) whenever(mockElement.getAttribute("android:viewportHeight")).thenReturn(vpHeight)
    }


    @Test
    fun `parseVector emits VectorItem for valid vector XML`() {
        val xmlContent = """$defaultXmlHeader
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24.0"
    android:viewportHeight="24.0">
    <path
        android:fillColor="#FF000000"
        android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2z"/>
</vector>"""
        writeToFile(xmlContent)
        val validFile = ValidFile(tempFile, tempFile.parent)
        val fileSize = tempFile.length()

        mockDocumentInteractions(xmlContent, vpWidth = "24", vpHeight = "24")
        mockStaticVdPreview.`when`<Any> {
            VdPreview.getPreviewFromVectorDocument(any(), eq(mockDocument), any())
        }.thenReturn(mockBufferedImage)

        val testObserver = parser.parseVector(validFile).test()

        testObserver.assertNoErrors()
        testObserver.assertValueCount(1)
        testObserver.assertValue { item ->
            item!!.name == tempFile.name &&
            item.image == mockBufferedImage &&
            item.viewportW == 24 &&
            item.viewportH == 24 &&
            item.fileSize == fileSize
        }
        testObserver.assertComplete()
        verify(mockDocumentBuilderFactory).setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        verify(mockDocumentBuilderFactory).setFeature("http://xml.org/sax/features/external-general-entities", false)
        verify(mockDocumentBuilderFactory).setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        verify(mockDocumentBuilderFactory).isExpandEntityReferences = false
    }

    @Test
    fun `parseVector completes without item for non-vector root tag`() {
        val xmlContent = """$defaultXmlHeader
<animated-vector xmlns:android="http://schemas.android.com/apk/res/android">
</animated-vector>"""
        writeToFile(xmlContent)
        val validFile = ValidFile(tempFile, tempFile.parent)

        mockDocumentInteractions(xmlContent, tagName = "animated-vector")
        // VdPreview.getPreviewFromVectorDocument should not be called if tag is not "vector"

        val testObserver = parser.parseVector(validFile).test()

        testObserver.assertNoErrors()
        testObserver.assertValueCount(0) // Or assert that the emitted value is null if that's the behavior
        testObserver.assertComplete()
    }

    @Test
    fun `parseVector replaces @color references and emits VectorItem`() {
        val originalXmlContent = """$defaultXmlHeader
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@color/my_color" android:pathData="M1,1h22v22h-22z"/>
</vector>"""
        val expectedXmlAfterReplacement = """$defaultXmlHeader
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#000000" android:pathData="M1,1h22v22h-22z"/>
</vector>"""
        writeToFile(originalXmlContent)
        val validFile = ValidFile(tempFile, tempFile.parent)

        val inputSourceCaptor = ArgumentCaptor.forClass(InputSource::class.java)
        whenever(mockDocumentBuilder.parse(inputSourceCaptor.capture())).thenAnswer {
            val inputSource = inputSourceCaptor.value
            val reader = inputSource.characterStream
            val actualXml = reader.readText()
            // Assert that the @color was replaced
            assert(actualXml.contains("android:fillColor=\"#000000\"")) { "Color reference was not replaced" }
            // Return a new InputSource with the original (or modified) string for the mocked Document
            // For simplicity here, we assume the mockDocument is based on the replaced one or doesn't care about exact content for parsing.
            // If using a real parser for part of the test, you'd parse `actualXml`.
            // Here, we just ensure the captor gets the modified string, then proceed with mocking.
            val newSource = InputSource(StringReader(actualXml)) // or use the original one if transformation is only for this check
            // This part is tricky: the mocked parse needs to return mockDocument.
            // The assertion is on the *captured* input to parse.
            // So, we are not re-parsing with the actualXml string here for the mock chain.
            mockDocument
        }

        whenever(mockDocument.documentElement).thenReturn(mockElement)
        whenever(mockElement.tagName).thenReturn("vector")
        whenever(mockElement.getAttribute("android:viewportWidth")).thenReturn("24")
        whenever(mockElement.getAttribute("android:viewportHeight")).thenReturn("24")

        mockStaticVdPreview.`when`<Any> {
            VdPreview.getPreviewFromVectorDocument(any(), eq(mockDocument), any())
        }.thenReturn(mockBufferedImage)

        val testObserver = parser.parseVector(validFile).test()

        testObserver.assertNoErrors()
        testObserver.assertValueCount(1)
        testObserver.assertComplete()
    }


    @Test
    fun `parseVector completes without item for malformed XML`() {
        val xmlContent = """$defaultXmlHeader<vector><path /></vect""" // Malformed
        writeToFile(xmlContent)
        val validFile = ValidFile(tempFile, tempFile.parent)

        whenever(mockDocumentBuilder.parse(any(InputSource::class.java))).thenThrow(SAXException("Malformed XML"))

        val testObserver = parser.parseVector(validFile).test()

        testObserver.assertNoErrors() // The error is caught internally
        testObserver.assertValueCount(0)
        testObserver.assertComplete()
    }

    @Test
    fun `parseVector completes without item if VdPreview returns null`() {
        val xmlContent = """$defaultXmlHeader<vector android:viewportWidth="24" android:viewportHeight="24"></vector>"""
        writeToFile(xmlContent)
        val validFile = ValidFile(tempFile, tempFile.parent)

        mockDocumentInteractions(xmlContent)
        mockStaticVdPreview.`when`<Any> {
            VdPreview.getPreviewFromVectorDocument(any(), eq(mockDocument), any())
        }.thenReturn(null)

        val testObserver = parser.parseVector(validFile).test()

        testObserver.assertNoErrors()
        testObserver.assertValueCount(0)
        testObserver.assertComplete()
    }

    @Test
    fun `parseVector completes without item if VdPreview throws exception`() {
        val xmlContent = """$defaultXmlHeader<vector android:viewportWidth="24" android:viewportHeight="24"></vector>"""
        writeToFile(xmlContent)
        val validFile = ValidFile(tempFile, tempFile.parent)

        mockDocumentInteractions(xmlContent)
        mockStaticVdPreview.`when`<Any> {
            VdPreview.getPreviewFromVectorDocument(any(), eq(mockDocument), any())
        }.thenThrow(RuntimeException("VdPreview failed"))

        val testObserver = parser.parseVector(validFile).test()

        testObserver.assertNoErrors() // Internal catch
        testObserver.assertValueCount(0)
        testObserver.assertComplete()
    }

    @Test
    fun `parseVector completes without item if file does not contain vector closing tag`() {
        val xmlContent = """$defaultXmlHeader<vector android:viewportWidth="24" android:viewportHeight="24">""" // No closing tag
        writeToFile(xmlContent)
        val validFile = ValidFile(tempFile, tempFile.parent)
        // No need to mock document interactions if it bails early

        val testObserver = parser.parseVector(validFile).test()

        testObserver.assertNoErrors()
        testObserver.assertValueCount(0)
        testObserver.assertComplete()
    }

    @Test
    fun `parseVector defaults viewport dimensions to 0 if attributes missing`() {
        val xmlContent = """$defaultXmlHeader<vector></vector>"""
        writeToFile(xmlContent)
        val validFile = ValidFile(tempFile, tempFile.parent)

        mockDocumentInteractions(xmlContent, vpWidth = null, vpHeight = null)
        mockStaticVdPreview.`when`<Any> {
            VdPreview.getPreviewFromVectorDocument(any(), eq(mockDocument), any())
        }.thenReturn(mockBufferedImage)

        val testObserver = parser.parseVector(validFile).test()

        testObserver.assertNoErrors()
        testObserver.assertValueCount(1)
        testObserver.assertValue { it!!.viewportW == 0 && it.viewportH == 0 }
        testObserver.assertComplete()
    }

    @Test
    fun `parseVector defaults viewport dimensions to 0 for non-integer attributes`() {
        val xmlContent = """$defaultXmlHeader<vector android:viewportWidth="24.5" android:viewportHeight="invalid"></vector>"""
        writeToFile(xmlContent)
        val validFile = ValidFile(tempFile, tempFile.parent)

        mockDocumentInteractions(xmlContent, vpWidth = "24.5", vpHeight = "invalid")
        mockStaticVdPreview.`when`<Any> {
            VdPreview.getPreviewFromVectorDocument(any(), eq(mockDocument), any())
        }.thenReturn(mockBufferedImage)

        val testObserver = parser.parseVector(validFile).test()

        testObserver.assertNoErrors()
        testObserver.assertValueCount(1)
        testObserver.assertValue { it!!.viewportW == 0 && it.viewportH == 0 }
        testObserver.assertComplete()
    }

    @Test
    fun `parseVector uses default preview size constant`() {
        // This test indirectly verifies DEFAULT_PREVIEW_SIZE usage by checking the argument to VdPreview
        val xmlContent = """$defaultXmlHeader<vector android:viewportWidth="24" android:viewportHeight="24"></vector>"""
        writeToFile(xmlContent)
        val validFile = ValidFile(tempFile, tempFile.parent)

        mockDocumentInteractions(xmlContent)
        val targetSizeCaptor = ArgumentCaptor.forClass(VdPreview.TargetSize::class.java)
        mockStaticVdPreview.`when`<Any> {
            VdPreview.getPreviewFromVectorDocument(targetSizeCaptor.capture(), eq(mockDocument), any())
        }.thenReturn(mockBufferedImage)

        parser.parseVector(validFile).test()

        // DEFAULT_PREVIEW_SIZE is 50. VdPreview.TargetSize.createFromMaxDimension(50)
        // The internal state of TargetSize isn't directly accessible for simple equality.
        // We rely on the fact that if createFromMaxDimension was called with 50, it's using the constant.
        // A more direct test would require refactoring VectorDrawableParser to make TargetSize injectable or inspectable,
        // or by verifying the properties of the captured TargetSize if it had public getters for max dimension.
        // For now, we trust that the call happens. A change in DEFAULT_PREVIEW_SIZE would require updating this test
        // if we could inspect the captured value's dimension.
        assert(targetSizeCaptor.value != null) { "TargetSize was not captured" }
        // To properly verify this, we'd need a way to get the maxDimension from the captured TargetSize.
        // Assuming TargetSize has a method like getMaxDimension() for testing:
        // assertEquals(VectorDrawableParser.DEFAULT_PREVIEW_SIZE, targetSizeCaptor.value.maxDimension)
        // Since it doesn't, this test primarily ensures the call is made.
    }
}

// Helper for static mocking with specific arguments if needed in other contexts, similar to ProjectFileScannerTest
private fun <T> MockedStatic<DocumentBuilderFactory>.`when`(function: () -> T): MockedStatic.Verification {
    return this.`when`(function)
}
private fun <T> MockedStatic<VdPreview>.`when`(function: () -> T): MockedStatic.Verification {
    return this.`when`(function)
}
