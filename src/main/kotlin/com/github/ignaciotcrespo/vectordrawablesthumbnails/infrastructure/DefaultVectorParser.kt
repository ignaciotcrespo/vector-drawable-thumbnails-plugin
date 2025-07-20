package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.android.ide.common.vectordrawable.VdPreview
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ColorResourceResolver
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.VectorParser
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.ValidFile
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import com.intellij.openapi.project.Project
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
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
class DefaultVectorParser(private val colorResourceResolver: ColorResourceResolver) : VectorParser {
    
    override fun parseVectorFile(validFile: ValidFile, project: Project?): Observable<VectorItem> {
        return Observable.create<VectorItem> { emitter ->
            try {
//                println("Parsing vector file: ${validFile.file.name}")
                val vectorItem = parseVector(validFile, project)
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

    private fun parseVector(validFile: ValidFile, project: Project?): VectorItem? {
        return try {
            var xml = FileInputStream(validFile.file).bufferedReader().use(BufferedReader::readText)
            
            if (!xml.contains("</vector>")) {
                return null
            }

            // Replace color references with resolved colors
            if (xml.contains("@color/") && project != null) {
                xml = resolveColorReferences(xml, project)
            } else if (xml.contains("@color/")) {
                // Fallback to black if no project context
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
    
    private fun resolveColorReferences(xml: String, project: Project): String {
        var resolvedXml = xml
        
        try {
            // Find all color references including Android system colors
            val colorReferencePatterns = listOf(
                "@color/(\\w+)".toRegex(),
                "@android:color/(\\w+)".toRegex(),
                "@\\+id/color/(\\w+)".toRegex()
            )
            
            val allMatches = mutableSetOf<MatchResult>()
            colorReferencePatterns.forEach { pattern ->
                allMatches.addAll(pattern.findAll(xml))
            }
            
            // Sort matches by position to handle nested references correctly
            val sortedMatches = allMatches.sortedByDescending { it.range.first }
            
            sortedMatches.forEach { match ->
                val colorReference = match.value
                
                try {
                    val resolvedColor = colorResourceResolver.resolveColorReference(colorReference, project)
                    
                    if (resolvedColor != null && resolvedColor.startsWith("#")) {
                        // Only replace if we got a valid hex color
                        resolvedXml = resolvedXml.replace(colorReference, resolvedColor)
                    } else {
                        // Log unresolved references for debugging
                        LOG.debug("Could not resolve color reference: $colorReference")
                        // Keep original reference or use fallback based on configuration
                        resolvedXml = resolvedXml.replace(colorReference, "#000000")
                    }
                } catch (e: Exception) {
                    LOG.warn("Error resolving individual color reference: $colorReference", e)
                    // Use fallback color on error
                    resolvedXml = resolvedXml.replace(colorReference, "#000000")
                }
            }
            
            // Handle theme attributes (not fully supported, use fallback)
            val themeAttrPattern = "\\?attr/(\\w+)|\\?android:attr/(\\w+)".toRegex()
            resolvedXml = themeAttrPattern.replace(resolvedXml) {
                LOG.debug("Theme attribute not supported: ${it.value}")
                "#808080" // Use gray for theme attributes
            }
            
        } catch (e: Exception) {
            LOG.error("Error resolving color references in XML", e)
            // Return original XML on error
            return xml
        }
        
        return resolvedXml
    }
    
    companion object {
        private val LOG = com.intellij.openapi.diagnostic.Logger.getInstance(DefaultVectorParser::class.java)
    }
} 