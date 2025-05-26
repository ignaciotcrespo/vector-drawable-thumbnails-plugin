package com.github.ignaciotcrespo.vectordrawablesthumbnails.parser

import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.ValidFile
import io.reactivex.observers.TestObserver
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.nio.file.Files

@ExtendWith(MockitoExtension::class)
class VectorDrawableParserTest {

    private lateinit var parser: VectorDrawableParser
    private lateinit var tempFile: File

    @BeforeEach
    fun setUp() {
        parser = VectorDrawableParser()
        tempFile = Files.createTempFile("testVector", ".xml").toFile()
    }

    @AfterEach
    fun tearDown() {
        tempFile.delete()
    }

    private fun writeToFile(content: String) {
        tempFile.writeText(content)
    }

    @Test
    fun `parseVdStringIntoDocument should parse valid XML`() {
        val xmlContent = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24.0"
    android:viewportHeight="24.0">
    <path android:fillColor="#FF000000" android:pathData="M12,2C6.48,2 2,6.48 2,12"/>
</vector>"""

        val errorLog = StringBuilder()
        val document = parser.parseVdStringIntoDocument(xmlContent, errorLog)

        assert(document != null) { "Document should not be null for valid XML" }
        assert(document!!.documentElement.tagName == "vector") { "Root element should be 'vector'" }
        assert(errorLog.isEmpty()) { "Error log should be empty for valid XML" }
    }

    @Test
    fun `parseVdStringIntoDocument should handle malformed XML gracefully`() {
        val malformedXml = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android">
    <path android:fillColor="#FF000000" android:pathData="M12,2C6.48,2 2,6.48 2,12"/>
</vect""" // Missing closing tag

        val errorLog = StringBuilder()
        val document = parser.parseVdStringIntoDocument(malformedXml, errorLog)

        assert(document == null) { "Document should be null for malformed XML" }
        assert(errorLog.isNotEmpty()) { "Error log should contain error message for malformed XML" }
        assert(errorLog.toString().contains("Exception while parsing XML file")) { "Error log should contain expected error message" }
    }

    @Test
    fun `parseVdStringIntoDocument should handle empty XML`() {
        val emptyXml = ""

        val errorLog = StringBuilder()
        val document = parser.parseVdStringIntoDocument(emptyXml, errorLog)

        assert(document == null) { "Document should be null for empty XML" }
        assert(errorLog.isNotEmpty()) { "Error log should contain error message for empty XML" }
    }

    @Test
    fun `parseVdStringIntoDocument should handle null XML`() {
        val errorLog = StringBuilder()
        val document = parser.parseVdStringIntoDocument(null, errorLog)

        assert(document == null) { "Document should be null for null XML" }
        assert(errorLog.isNotEmpty()) { "Error log should contain error message for null XML" }
    }

    @Test
    fun `parseVdStringIntoDocument should extract viewport dimensions correctly`() {
        val xmlContent = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:viewportWidth="48"
    android:viewportHeight="32">
</vector>"""

        val errorLog = StringBuilder()
        val document = parser.parseVdStringIntoDocument(xmlContent, errorLog)

        assert(document != null) { "Document should not be null" }
        val element = document!!.documentElement
        assert(element.getAttribute("android:viewportWidth") == "48") { "ViewportWidth should be 48" }
        assert(element.getAttribute("android:viewportHeight") == "32") { "ViewportHeight should be 32" }
    }

    @Test
    fun `parseVdStringIntoDocument should handle XML without viewport dimensions`() {
        val xmlContent = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android">
    <path android:fillColor="#FF000000" android:pathData="M12,2"/>
</vector>"""

        val errorLog = StringBuilder()
        val document = parser.parseVdStringIntoDocument(xmlContent, errorLog)

        assert(document != null) { "Document should not be null" }
        val element = document!!.documentElement
        assert(element.getAttribute("android:viewportWidth").isEmpty()) { "ViewportWidth should be empty when not specified" }
        assert(element.getAttribute("android:viewportHeight").isEmpty()) { "ViewportHeight should be empty when not specified" }
    }

    @Test
    fun `parseVdStringIntoDocument should handle complex vector XML`() {
        val xmlContent = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24.0"
    android:viewportHeight="24.0"
    android:tint="?attr/colorOnSurface">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM13,17h-2v-6h2v6zM13,9h-2L11,7h2v2z"/>
    <path
        android:fillColor="#FF000000"
        android:pathData="M8,8h8v8h-8z"/>
</vector>"""

        val errorLog = StringBuilder()
        val document = parser.parseVdStringIntoDocument(xmlContent, errorLog)

        assert(document != null) { "Document should not be null for complex XML" }
        assert(document!!.documentElement.tagName == "vector") { "Root element should be 'vector'" }
        assert(errorLog.isEmpty()) { "Error log should be empty for valid complex XML" }
        
        // Check that paths are parsed
        val paths = document.getElementsByTagName("path")
        assert(paths.length == 2) { "Should find 2 path elements" }
    }

    @Test
    fun `parseVdStringIntoDocument should handle XML with comments and whitespace`() {
        val xmlContent = """<?xml version="1.0" encoding="utf-8"?>
<!-- This is a comment -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp">
    <!-- Another comment -->
    <path android:fillColor="#FF000000" android:pathData="M12,2"/>
    
</vector>"""

        val errorLog = StringBuilder()
        val document = parser.parseVdStringIntoDocument(xmlContent, errorLog)

        assert(document != null) { "Document should not be null for XML with comments" }
        assert(document!!.documentElement.tagName == "vector") { "Root element should be 'vector'" }
        assert(errorLog.isEmpty()) { "Error log should be empty for valid XML with comments" }
    }
}
