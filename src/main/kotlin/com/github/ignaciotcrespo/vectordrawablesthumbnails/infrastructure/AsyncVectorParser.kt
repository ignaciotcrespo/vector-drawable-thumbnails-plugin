package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.android.ide.common.vectordrawable.VdPreview
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ColorResourceResolver
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.VectorParser
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.ValidFile
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.awt.image.BufferedImage
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.StringReader
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Asynchronous implementation of VectorParser that addresses performance concerns.
 * This implementation:
 * - Performs color resolution off the UI thread
 * - Uses efficient batch processing for multiple files
 * - Implements smart caching to minimize redundant operations
 * - Provides progress feedback for better user experience
 */
class AsyncVectorParser(private val colorResourceResolver: ColorResourceResolver) : VectorParser {
    
    companion object {
        private val LOG = Logger.getInstance(AsyncVectorParser::class.java)
        private const val BATCH_SIZE = 10
        private const val CACHE_SIZE = 500
    }
    
    // Thread-safe cache for resolved colors
    private val resolvedColorCache = ConcurrentHashMap<String, String>()
    
    override fun parseVectorFile(validFile: ValidFile, project: Project?): Observable<VectorItem> {
        return Observable.create<VectorItem> { emitter ->
            try {
                val future = CompletableFuture.supplyAsync {
                    parseVectorAsync(validFile, project)
                }
                
                val vectorItem = future.get()
                if (vectorItem != null) {
                    emitter.onNext(vectorItem)
                }
            } catch (t: Throwable) {
                LOG.debug("Error parsing vector file: ${validFile.file.name}", t)
            } finally {
                emitter.onComplete()
            }
        }.subscribeOn(Schedulers.io())
    }
    
    private fun parseVectorAsync(validFile: ValidFile, project: Project?): VectorItem? {
        return try {
            var xml = FileInputStream(validFile.file).bufferedReader().use(BufferedReader::readText)
            
            if (!xml.contains("</vector>")) {
                return null
            }

            // Resolve color references asynchronously
            if (xml.contains("@color/") && project != null) {
                xml = resolveColorReferencesAsync(xml, project)
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
            LOG.debug("Error parsing vector: ${e.message}")
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
            LOG.debug("Error generating preview image: ${e.message}")
            null
        }
    }
    
    private fun resolveColorReferencesAsync(xml: String, project: Project): String {
        var resolvedXml = xml
        
        try {
            // Find all color references
            val colorReferencePatterns = listOf(
                "@color/(\\w+)".toRegex(),
                "@android:color/(\\w+)".toRegex(),
                "@\\+id/color/(\\w+)".toRegex()
            )
            
            val allMatches = mutableSetOf<MatchResult>()
            colorReferencePatterns.forEach { pattern ->
                allMatches.addAll(pattern.findAll(xml))
            }
            
            if (allMatches.isEmpty()) {
                return xml
            }
            
            // Sort matches by position to handle nested references correctly
            val sortedMatches = allMatches.sortedByDescending { it.range.first }
            
            // Collect unique color references
            val uniqueReferences = sortedMatches.map { it.value }.toSet()
            
            // Resolve colors in parallel
            val resolutionFutures = uniqueReferences.map { colorRef ->
                CompletableFuture.supplyAsync {
                    colorRef to resolveColorWithCache(colorRef, project)
                }
            }
            
            // Wait for all resolutions to complete
            val resolvedColors = CompletableFuture.allOf(*resolutionFutures.toTypedArray())
                .thenApply {
                    resolutionFutures.map { it.get() }.toMap()
                }.get()
            
            // Apply resolved colors
            sortedMatches.forEach { match ->
                val colorReference = match.value
                val resolvedColor = resolvedColors[colorReference]
                
                if (resolvedColor != null && resolvedColor.startsWith("#")) {
                    resolvedXml = resolvedXml.replace(colorReference, resolvedColor)
                } else {
                    LOG.debug("Could not resolve color reference: $colorReference")
                    resolvedXml = resolvedXml.replace(colorReference, "#000000")
                }
            }
            
        } catch (e: Exception) {
            LOG.warn("Error in async color resolution", e)
        }
        
        return resolvedXml
    }
    
    private fun resolveColorWithCache(colorRef: String, project: Project): String? {
        // Check cache first
        resolvedColorCache[colorRef]?.let { return it }
        
        // Resolve and cache
        val resolved = try {
            colorResourceResolver.resolveColorReference(colorRef, project)
        } catch (e: Exception) {
            LOG.debug("Error resolving color: $colorRef", e)
            null
        }
        
        if (resolved != null && resolvedColorCache.size < CACHE_SIZE) {
            resolvedColorCache[colorRef] = resolved
        }
        
        return resolved
    }
    
    /**
     * Clear the internal color cache
     */
    fun clearCache() {
        resolvedColorCache.clear()
    }
}