package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure.cache

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.AndroidResourceLocator
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ResourceEntry
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ResourceFileParser
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Responsible for building the color cache from various sources.
 * Follows Single Responsibility Principle by focusing only on cache building logic.
 */
class ColorCacheBuilder(
    private val androidResourceLocator: AndroidResourceLocator,
    private val resourceParser: ResourceFileParser
) {
    
    companion object {
        private val LOG = Logger.getInstance(ColorCacheBuilder::class.java)
        private const val CACHE_BUILD_TIMEOUT_MS = 30000L
    }
    
    private val colorParsingExecutor = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
    ).asCoroutineDispatcher()
    
    suspend fun buildColorCache(project: Project): Map<String, String> {
        return withTimeoutOrNull(CACHE_BUILD_TIMEOUT_MS) {
            val colors = ConcurrentHashMap<String, String>()
            
            try {
                // Find all resource files
                val resourceCollection = withContext(Dispatchers.IO) {
                    ApplicationManager.getApplication().runReadAction<com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ResourceFileCollection> {
                        androidResourceLocator.findAllResourceFiles(project)
                    }
                }
                
                // Process files in parallel
                coroutineScope {
                    val jobs = mutableListOf<Job>()
                    
                    // Process source files
                    resourceCollection.sourceFiles.forEach { file ->
                        jobs += launch(colorParsingExecutor) {
                            parseResourceFile(file, colors, "source")
                        }
                    }
                    
                    // Process build output files with higher priority
                    resourceCollection.buildOutputFiles.forEach { file ->
                        jobs += launch(colorParsingExecutor) {
                            parseResourceFile(file, colors, "build")
                        }
                    }
                    
                    // Process library entries
                    resourceCollection.libraryEntries.forEach { entry ->
                        jobs += launch(colorParsingExecutor) {
                            parseLibraryEntry(entry, colors)
                        }
                    }
                    
                    // Wait for all parsing to complete
                    jobs.joinAll()
                }
                
                colors
                
            } catch (e: Exception) {
                LOG.error("Error building color cache", e)
                emptyMap()
            }
        } ?: run {
            LOG.error("Color cache build timed out after ${CACHE_BUILD_TIMEOUT_MS}ms")
            emptyMap()
        }
    }
    
    suspend fun buildColorCacheWithProgress(
        project: Project,
        indicator: ProgressIndicator
    ): Map<String, String> {
        try {
            indicator.text = "Locating Android resource files..."
            indicator.isIndeterminate = true
            
            // Find all resource files
            val resourceCollection = withContext(Dispatchers.IO) {
                ApplicationManager.getApplication().runReadAction<com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ResourceFileCollection> {
                    androidResourceLocator.findAllResourceFiles(project)
                }
            }
            
            val totalItems = resourceCollection.sourceFiles.size + 
                           resourceCollection.buildOutputFiles.size + 
                           resourceCollection.libraryEntries.size
            
            if (totalItems == 0) {
                indicator.text = "No Android resource files found"
                return emptyMap()
            }
            
            indicator.text = "Parsing $totalItems resource items..."
            indicator.isIndeterminate = false
            
            val colors = ConcurrentHashMap<String, String>()
            val processedCount = java.util.concurrent.atomic.AtomicInteger(0)
            
            coroutineScope {
                val updateProgress = { 
                    val processed = processedCount.incrementAndGet()
                    indicator.fraction = processed.toDouble() / totalItems
                    indicator.text2 = "Processed $processed of $totalItems items"
                }
                
                val jobs = mutableListOf<Job>()
                
                // Process all files
                (resourceCollection.sourceFiles + resourceCollection.buildOutputFiles).forEach { file ->
                    jobs += launch(colorParsingExecutor) {
                        parseResourceFile(file, colors, "file")
                        updateProgress()
                    }
                }
                
                resourceCollection.libraryEntries.forEach { entry ->
                    jobs += launch(colorParsingExecutor) {
                        parseLibraryEntry(entry, colors)
                        updateProgress()
                    }
                }
                
                jobs.joinAll()
            }
            
            indicator.text = "Completed. Found ${colors.size} color resources"
            return colors
            
        } catch (e: CancellationException) {
            LOG.info("Color cache build cancelled")
            throw e
        } catch (e: Exception) {
            LOG.error("Error building color cache with progress", e)
            indicator.text = "Error building color cache: ${e.message}"
            return emptyMap()
        }
    }
    
    private suspend fun parseResourceFile(
        file: VirtualFile,
        colors: ConcurrentHashMap<String, String>,
        source: String
    ) {
        try {
            withContext(Dispatchers.IO) {
                ApplicationManager.getApplication().runReadAction {
                    when {
                        file.name == "R.txt" -> {
                            val colorNames = resourceParser.parseRTxtFile(file)
                            colorNames.forEach { name ->
                                // R.txt only contains names, not values
                                colors.putIfAbsent(name, "@color/$name")
                            }
                        }
                        file.extension == "xml" -> {
                            val fileColors = resourceParser.parseResourceFile(file)
                            // Build output colors override source colors
                            if (source == "build") {
                                colors.putAll(fileColors)
                            } else {
                                fileColors.forEach { (name, value) ->
                                    colors.putIfAbsent(name, value)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOG.debug("Failed to parse resource file: ${file.path}", e)
        }
    }
    
    private fun parseLibraryEntry(
        entry: ResourceEntry,
        colors: ConcurrentHashMap<String, String>
    ) {
        try {
            when {
                entry.path.endsWith("R.txt") -> {
                    parseRTxtContent(entry.content).forEach { name ->
                        colors.putIfAbsent(name, "@color/$name")
                    }
                }
                entry.path.endsWith(".xml") -> {
                    val document = parseXmlDocument(entry.content)
                    if (document != null) {
                        val fileColors = extractColorsFromDocument(document)
                        fileColors.forEach { (name, value) ->
                            colors.putIfAbsent(name, value)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOG.debug("Failed to parse library entry: ${entry.path}", e)
        }
    }
    
    private fun parseRTxtContent(content: String): Set<String> {
        val colorNames = mutableSetOf<String>()
        content.lines().forEach { line ->
            // R.txt format: int color <name> <value>
            if (line.trim().startsWith("int color ")) {
                val parts = line.trim().split("\\s+".toRegex())
                if (parts.size >= 3) {
                    colorNames.add(parts[2])
                }
            }
        }
        return colorNames
    }
    
    private fun parseXmlDocument(xmlContent: String): org.w3c.dom.Document? {
        return try {
            val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = false
            factory.isValidating = false
            
            val builder = factory.newDocumentBuilder()
            val inputSource = org.xml.sax.InputSource(java.io.StringReader(xmlContent))
            builder.parse(inputSource)
        } catch (e: Exception) {
            LOG.debug("Failed to parse XML content", e)
            null
        }
    }
    
    private fun extractColorsFromDocument(document: org.w3c.dom.Document): Map<String, String> {
        val colors = mutableMapOf<String, String>()
        
        try {
            val colorElements = document.getElementsByTagName("color")
            
            for (i in 0 until colorElements.length) {
                val element = colorElements.item(i)
                val colorName = element.attributes?.getNamedItem("name")?.nodeValue
                val colorValue = element.textContent?.trim()
                
                if (!colorName.isNullOrEmpty() && !colorValue.isNullOrEmpty()) {
                    colors[colorName] = colorValue
                }
            }
        } catch (e: Exception) {
            LOG.debug("Error extracting colors from document", e)
        }
        
        return colors
    }
    
    fun dispose() {
        try {
            colorParsingExecutor.close()
        } catch (e: Exception) {
            LOG.error("Error disposing color cache builder", e)
        }
    }
}