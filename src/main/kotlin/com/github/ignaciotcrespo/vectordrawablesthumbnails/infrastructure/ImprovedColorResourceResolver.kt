package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.*
import com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure.cache.DefaultColorCacheManager
import com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure.parsers.XmlResourceFileParser
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Improved implementation of ColorResourceResolver that addresses code review issues:
 * - Uses proper Android resource location strategies
 * - Performs heavy operations off UI thread
 * - Follows SOLID principles
 * - Includes comprehensive error handling
 * - Supports cache invalidation
 */
class ImprovedColorResourceResolver(
    private val cacheManager: ColorCacheManager = DefaultColorCacheManager(),
    private val resourceParser: ResourceFileParser = XmlResourceFileParser(),
    private val androidResourceLocator: AndroidResourceLocator = AndroidResourceLocatorImpl(),
    private val colorResolver: ColorResolver = ImprovedColorResolver(),
    private val cacheInvalidator: ResourceCacheInvalidator = ResourceCacheInvalidatorImpl()
) : ColorResourceResolver {
    
    companion object {
        private val LOG = Logger.getInstance(ImprovedColorResourceResolver::class.java)
        private const val CACHE_BUILD_TIMEOUT_MS = 30000L
        private const val DEFAULT_COLOR_FALLBACK = "#000000"
    }
    
    private val coroutineScope = CoroutineScope(
        Dispatchers.IO + SupervisorJob() + CoroutineName("ColorResourceResolver")
    )
    
    private val colorParsingExecutor = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
    ).asCoroutineDispatcher()
    
    init {
        // Set up cache invalidation
        cacheInvalidator.onInvalidate {
            LOG.info("Resource files changed, invalidating color cache")
            clearCache()
        }
    }
    
    override fun resolveColorReference(colorReference: String, project: Project): String? {
        return try {
            // First, check if it's a direct color or system color
            if (colorReference.startsWith("#")) {
                return colorReference
            }
            
            if (colorReference.startsWith("@android:color/")) {
                return colorResolver.resolveColor(colorReference, emptyMap())
            }
            
            // Get cached colors or build cache if needed
            val colors = cacheManager.getCachedColors(project) ?: run {
                LOG.info("Color cache not available, building synchronously")
                runBlocking {
                    buildColorCacheSuspend(project)
                }
                cacheManager.getCachedColors(project)
            } ?: run {
                LOG.warn("Failed to build color cache for project")
                return null
            }
            
            // Resolve the color
            val resolvedColor = colorResolver.resolveColor(colorReference, colors)
            
            if (resolvedColor == null) {
                LOG.warn("Could not resolve color reference: $colorReference. Available colors: ${colors.size}")
            }
            
            resolvedColor
            
        } catch (e: Exception) {
            LOG.error("Error resolving color reference: $colorReference", e)
            null
        }
    }
    
    override fun buildColorCache(project: Project) {
        // Start watching for file changes
        cacheInvalidator.startWatching(project)
        
        // Build cache in background with progress
        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project,
            "Building Android color resource cache...",
            true
        ) {
            override fun run(indicator: ProgressIndicator) {
                runBlocking {
                    buildColorCacheWithProgress(project, indicator)
                }
            }
            
            override fun onSuccess() {
                val cachedColors = cacheManager.getCachedColors(project)
                LOG.info("Color cache built successfully with ${cachedColors?.size ?: 0} colors")
            }
            
            override fun onThrowable(error: Throwable) {
                LOG.error("Failed to build color cache", error)
            }
        })
    }
    
    override fun clearCache() {
        cacheManager.clearAllCaches()
    }
    
    override fun getAllColorResources(project: Project): Map<String, String> {
        return cacheManager.getCachedColors(project) ?: run {
            LOG.info("Cache not available, building synchronously")
            runBlocking {
                buildColorCacheSuspend(project)
            }
            cacheManager.getCachedColors(project) ?: emptyMap()
        }
    }
    
    private suspend fun buildColorCacheSuspend(project: Project): Map<String, String> {
        return withTimeoutOrNull(CACHE_BUILD_TIMEOUT_MS) {
            val colors = ConcurrentHashMap<String, String>()
            
            try {
                // Find all resource files
                val resourceCollection = withContext(Dispatchers.IO) {
                    ApplicationManager.getApplication().runReadAction<ResourceFileCollection> {
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
                
                // Update cache
                cacheManager.updateCache(project, colors)
                
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
    
    private suspend fun buildColorCacheWithProgress(
        project: Project,
        indicator: ProgressIndicator
    ) {
        try {
            indicator.text = "Locating Android resource files..."
            indicator.isIndeterminate = true
            
            // Find all resource files
            val resourceCollection = withContext(Dispatchers.IO) {
                ApplicationManager.getApplication().runReadAction<ResourceFileCollection> {
                    androidResourceLocator.findAllResourceFiles(project)
                }
            }
            
            val totalItems = resourceCollection.sourceFiles.size + 
                           resourceCollection.buildOutputFiles.size + 
                           resourceCollection.libraryEntries.size
            
            if (totalItems == 0) {
                indicator.text = "No Android resource files found"
                return
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
            
            // Update cache
            cacheManager.updateCache(project, colors)
            
            indicator.text = "Completed. Found ${colors.size} color resources"
            
        } catch (e: CancellationException) {
            LOG.info("Color cache build cancelled")
            throw e
        } catch (e: Exception) {
            LOG.error("Error building color cache with progress", e)
            indicator.text = "Error building color cache: ${e.message}"
        }
    }
    
    private suspend fun parseResourceFile(
        file: com.intellij.openapi.vfs.VirtualFile,
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
            cacheInvalidator.stopWatching()
            coroutineScope.cancel()
            colorParsingExecutor.close()
        } catch (e: Exception) {
            LOG.error("Error disposing color resolver", e)
        }
    }
}