package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure.parsers

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ResourceFileParser
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Implementation of ResourceFileParser for XML resource files.
 * Follows Single Responsibility Principle - only parses XML files.
 */
class XmlResourceFileParser : ResourceFileParser {
    
    companion object {
        private val LOG = Logger.getInstance(XmlResourceFileParser::class.java)
    }
    
    override fun parseResourceFile(file: VirtualFile): Map<String, String> {
        val colors = mutableMapOf<String, String>()
        
        try {
            val content = String(file.contentsToByteArray())
            val document = parseXmlDocument(content)
            
            if (document != null) {
                extractColorsFromDocument(document, colors)
            }
        } catch (e: Exception) {
            LOG.warn("Failed to parse resource file: ${file.path}", e)
        }
        
        return colors
    }
    
    override fun parseRTxtFile(file: VirtualFile): Set<String> {
        val colorNames = mutableSetOf<String>()
        
        try {
            file.inputStream.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.lines().forEach { line ->
                        // R.txt format: int color colorName 0x7f060001
                        if (line.startsWith("int color ")) {
                            val parts = line.split(" ")
                            if (parts.size >= 4) {
                                colorNames.add(parts[2])
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOG.warn("Failed to parse R.txt file: ${file.path}", e)
        }
        
        return colorNames
    }
    
    private fun parseXmlDocument(xmlContent: String): Document? {
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val inputSource = InputSource(StringReader(xmlContent))
            builder.parse(inputSource)
        } catch (e: Exception) {
            LOG.debug("Failed to parse XML content", e)
            null
        }
    }
    
    private fun extractColorsFromDocument(document: Document, colors: MutableMap<String, String>) {
        val colorElements = document.getElementsByTagName("color")
        for (i in 0 until colorElements.length) {
            val element = colorElements.item(i)
            val colorName = element.attributes?.getNamedItem("name")?.nodeValue
            val colorValue = element.textContent?.trim()
            
            if (colorName != null && colorValue != null && colorValue.isNotEmpty()) {
                when {
                    colorValue.startsWith("#") -> {
                        // Direct hex color value
                        colors[colorName] = colorValue.uppercase()
                    }
                    colorValue.startsWith("@color/") -> {
                        // Store color reference for later resolution
                        colors[colorName] = colorValue
                    }
                    colorValue.startsWith("@android:color/") -> {
                        // Android system color reference
                        colors[colorName] = colorValue
                    }
                }
            }
        }
    }
}