package com.github.ignaciotcrespo.vectordrawablesthumbnails

import com.android.ide.common.vectordrawable.VdPreview
import org.w3c.dom.Document

class XmlVectorAttributeParser : VectorAttributeParser {

    override fun parse(xmlContent: String, fileName: String, fileSize: Long, validFile: ValidFile): ParsedVectorAttributes? {
        try {
            var processedXmlContent = xmlContent
            if (processedXmlContent.contains("</vector>")) {
                if (processedXmlContent.contains("@color/")) {
                    processedXmlContent = processedXmlContent.replace("@color/\\w+".toRegex(), "#000000")
                }
                val log = StringBuilder()
                val document: Document = VdPreview.parseVdStringIntoDocument(processedXmlContent, log)
                val documentElement = document.documentElement
                if (documentElement.tagName == "vector") {
                    val viewportW = documentElement.getAttribute("android:viewportWidth")?.toIntOrNull() ?: 0
                    val viewportH = documentElement.getAttribute("android:viewportHeight")?.toIntOrNull() ?: 0
                    return ParsedVectorAttributes(
                        name = fileName,
                        viewportW = viewportW,
                        viewportH = viewportH,
                        fileSize = fileSize,
                        document = document,
                        validFile = validFile
                    )
                }
            }
        } catch (t: Throwable) {
            // Log error or handle appropriately
            println("Error parsing XML for $fileName: ${t.message}")
        }
        return null
    }
}
