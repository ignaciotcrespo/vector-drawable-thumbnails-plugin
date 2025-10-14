package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.android.ide.common.vectordrawable.VdPreview
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.VectorParser
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.ValidFile
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.ImageTranscoder
import org.apache.batik.transcoder.image.PNGTranscoder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.awt.image.BufferedImage
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Default implementation of VectorParser.
 * Follows the Single Responsibility Principle by focusing only on vector parsing logic.
 */
class DefaultVectorParser : VectorParser {
    
    override fun parseVectorFile(validFile: ValidFile): Observable<VectorItem> {
        return Observable.create<VectorItem> { emitter ->
            try {
//                println("Parsing vector file: ${validFile.file.name}")
                val vectorItem = if (validFile.file.name.endsWith(".svg")) {
                    parseSvg(validFile)
                } else {
                    parseVector(validFile)
                }

                if (vectorItem != null) {
//                    println("Successfully parsed vector: ${vectorItem.name}")
                    emitter.onNext(vectorItem)
                } else {
//                    println("Failed to parse vector file: ${validFile.file.name}")
                }
            } catch (t: Throwable) {
//                println("Error parsing vector file: ${validFile.file.name} - ${t.message}")
                t.printStackTrace()
                // Don't emit anything for errors, just complete
            } finally {
                emitter.onComplete()
            }
        }
    }

    private fun parseVector(validFile: ValidFile): VectorItem? {
        return try {
            var xml = FileInputStream(validFile.file).bufferedReader().use(BufferedReader::readText)
            
            if (!xml.contains("</vector>")) {
                return null
            }

            // Replace color references with default color
            if (xml.contains("@color/")) {
                xml = xml.replace("@color/\\w+".toRegex(), "#000000")
            }

            val log = StringBuilder()
            val doc = parseXmlDocument(xml, log) ?: return null
            val documentElement = doc.documentElement

            if (documentElement?.tagName != "vector") {
                return null
            }

            val viewportW = documentElement.getAttribute("android:viewportWidth")?.toIntOrNull() ?: 0
            val viewportH = documentElement.getAttribute("android:viewportHeight")?.toIntOrNull() ?: 0
            
            val bitmap = generatePreviewImage(doc, log) ?: return null

            VectorItem(
                name = validFile.file.name,
                image = bitmap,
                validFile = validFile,
                viewportW = viewportW,
                viewportH = viewportH,
                fileSize = validFile.file.length()
            )
        } catch (e: Exception) {
            println("Error parsing vector: ${e.message}")
            null
        }
    }

    private fun parseXmlDocument(xmlContent: String, errorLog: StringBuilder): Document? {
        val dbf = DocumentBuilderFactory.newInstance()
        return try {
            val db = dbf.newDocumentBuilder()
            db.parse(InputSource(StringReader(xmlContent)))
        } catch (e: Exception) {
            errorLog.append("Exception while parsing XML file:\n").append(e.message)
            null
        }
    }

    private fun generatePreviewImage(document: Document, log: StringBuilder): BufferedImage? {
        return try {
            VdPreview.getPreviewFromVectorDocument(
                VdPreview.TargetSize.createFromMaxDimension(50),
                document,
                log
            )
        } catch (e: Exception) {
            println("Error generating preview image: ${e.message}")
            null
        }
    }

    private fun parseSvg(validFile: ValidFile): VectorItem? {
        return try {
            val file = validFile.file
            val bitmap = renderSvgToImage(file.absolutePath) ?: return null

            // Try to extract viewBox or width/height from SVG
            val (viewportW, viewportH) = extractSvgDimensions(file)

            VectorItem(
                name = file.name,
                image = bitmap,
                validFile = validFile,
                viewportW = viewportW,
                viewportH = viewportH,
                fileSize = file.length()
            )
        } catch (e: Exception) {
            println("Error parsing SVG: ${e.message}")
            null
        }
    }

    private fun renderSvgToImage(svgPath: String): BufferedImage? {
        return try {
            val transcoder = PNGTranscoder()

            // Set the transcoding hints for size
            transcoder.addTranscodingHint(
                ImageTranscoder.KEY_WIDTH,
                50f
            )
            transcoder.addTranscodingHint(
                ImageTranscoder.KEY_HEIGHT,
                50f
            )

            val input = TranscoderInput(FileInputStream(svgPath))
            val outputStream = ByteArrayOutputStream()
            val output = TranscoderOutput(outputStream)

            transcoder.transcode(input, output)
            outputStream.flush()

            val byteArray = outputStream.toByteArray()
            ImageIO.read(ByteArrayInputStream(byteArray))
        } catch (e: Exception) {
            println("Error rendering SVG to image: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    private fun extractSvgDimensions(file: java.io.File): Pair<Int, Int> {
        return try {
            val content = file.readText()
            val doc = parseXmlDocument(content, StringBuilder()) ?: return Pair(0, 0)
            val root = doc.documentElement

            // Try to get width and height attributes
            val widthStr = root.getAttribute("width")
            val heightStr = root.getAttribute("height")

            if (widthStr.isNotEmpty() && heightStr.isNotEmpty()) {
                val width = widthStr.replace(Regex("[^0-9.]"), "").toDoubleOrNull()?.toInt() ?: 0
                val height = heightStr.replace(Regex("[^0-9.]"), "").toDoubleOrNull()?.toInt() ?: 0
                return Pair(width, height)
            }

            // Try to get viewBox attribute
            val viewBox = root.getAttribute("viewBox")
            if (viewBox.isNotEmpty()) {
                val parts = viewBox.split(Regex("\\s+"))
                if (parts.size == 4) {
                    val width = parts[2].toDoubleOrNull()?.toInt() ?: 0
                    val height = parts[3].toDoubleOrNull()?.toInt() ?: 0
                    return Pair(width, height)
                }
            }

            Pair(0, 0)
        } catch (e: Exception) {
            println("Error extracting SVG dimensions: ${e.message}")
            Pair(0, 0)
        }
    }
} 