package com.github.ignaciotcrespo.vectordrawablesthumbnails.parser

import com.android.ide.common.vectordrawable.VdPreview
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.ValidFile
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import io.reactivex.Emitter
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.awt.image.BufferedImage
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

class VectorDrawableParser : IVectorDrawableParser {

    override fun parseVector(validFile: ValidFile): Observable<VectorItem?> {
        return Observable.create { emitter: ObservableEmitter<VectorItem?> -> searchVectors(emitter, validFile) }
    }

    private fun searchVectors(emitter: Emitter<VectorItem?>, file: ValidFile) {
        var bmp: BufferedImage? = null
        try {
            var xml = FileInputStream(file.file).bufferedReader().use(BufferedReader::readText)
            if (xml.contains("</vector>")) {
                if (xml.contains("@color/")) {
                    // Replace color references with a placeholder black color.
                    // This is a simplification. A more robust solution might involve
                    // resolving these references against actual project resources.
                    xml = xml.replace("@color/\\w+".toRegex(), "#000000")
                }
                val log = StringBuilder()
                val doc = parseVdStringIntoDocument(xml, log)
                val documentElement = doc?.documentElement
                if (documentElement?.tagName == "vector") {
                    val viewportW = documentElement.getAttribute("android:viewportWidth")?.toIntOrNull() ?: 0
                    val viewportH = documentElement.getAttribute("android:viewportHeight")?.toIntOrNull() ?: 0
                    bmp = VdPreview.getPreviewFromVectorDocument(
                        VdPreview.TargetSize.createFromMaxDimension(DEFAULT_PREVIEW_SIZE), // Use a constant for clarity
                        doc,
                        log
                    )
                    if (bmp != null) {
                        emitter.onNext(VectorItem(file.file.name, bmp, file, viewportW, viewportH, file.file.length()))
                    }
                }
            }
        } catch (t: Throwable) {
            // Log or handle the error appropriately
            println("Error parsing vector ${file.file.name}: ${t.message}")
        } finally {
            emitter.onComplete()
        }
    }

    fun parseVdStringIntoDocument(xmlFileContent: String?, errorLog: java.lang.StringBuilder?): Document? {
        val dbf = DocumentBuilderFactory.newInstance()
        // Mitigate XXE
        dbf.isExpandEntityReferences = false
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false)
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false)


        try {
            val db = dbf.newDocumentBuilder()
            val document = db.parse(InputSource(StringReader(xmlFileContent)))
            return document
        } catch (var6: Exception) {
            errorLog?.append("Exception while parsing XML file:\n")?.append(var6.message)
            return null
        }
    }

    companion object {
        private const val DEFAULT_PREVIEW_SIZE = 50
    }
}
