package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.*
import com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure.cache.DefaultColorCacheManager
import com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure.parsers.XmlResourceFileParser
import com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure.search.ProjectResourceSearchStrategyImpl
import com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure.search.BuildOutputResourceSearchStrategyImpl
import com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure.search.LibraryResourceSearchStrategyImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * Refactored implementation of ColorResourceResolver following SOLID principles.
 * Uses dependency injection and separates concerns into different components.
 */
class RefactoredColorResourceResolver(
    private val cacheManager: ColorCacheManager = DefaultColorCacheManager(),
    private val resourceParser: ResourceFileParser = XmlResourceFileParser(),
    private val projectSearchStrategy: ProjectResourceSearchStrategy = ProjectResourceSearchStrategyImpl(),
    private val buildOutputSearchStrategy: BuildOutputResourceSearchStrategy = BuildOutputResourceSearchStrategyImpl(),
    private val librarySearchStrategy: LibraryResourceSearchStrategy = LibraryResourceSearchStrategyImpl()
) : ColorResourceResolver {
    
    companion object {
        private val LOG = Logger.getInstance(RefactoredColorResourceResolver::class.java)
        private const val MAX_RECURSION_DEPTH = 10
    }
    
    override fun resolveColorReference(colorReference: String, project: Project): String? {
        // Extract color name from reference
        val colorName = extractColorName(colorReference)
        
        // Get cached colors or build cache if needed
        val colors = cacheManager.getCachedColors(project) ?: run {
            // Build cache synchronously if not available
            buildColorCacheSync(project)
            cacheManager.getCachedColors(project)
        } ?: return null
        
        // Resolve with support for indirect references
        return resolveColorWithIndirectReferences(colorName, colors)
    }
    
    override fun buildColorCache(project: Project) {
        // Build cache asynchronously in background
        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project, 
            "Building color resource cache...", 
            false
        ) {
            override fun run(indicator: ProgressIndicator) {
                buildColorCacheInBackground(project, indicator)
            }
        })
    }
    
    override fun clearCache() {
        cacheManager.clearAllCaches()
    }
    
    override fun getAllColorResources(project: Project): Map<String, String> {
        return cacheManager.getCachedColors(project) ?: run {
            buildColorCacheSync(project)
            cacheManager.getCachedColors(project)
        } ?: emptyMap()
    }
    
    private fun extractColorName(colorReference: String): String {
        return when {
            colorReference.startsWith("@color/") -> colorReference.removePrefix("@color/")
            colorReference.startsWith("@android:color/") -> colorReference
            else -> colorReference
        }.trim()
    }
    
    private fun resolveColorWithIndirectReferences(
        colorName: String,
        colors: Map<String, String>,
        visitedColors: MutableSet<String> = mutableSetOf(),
        depth: Int = 0
    ): String? {
        // Prevent infinite recursion
        if (depth > MAX_RECURSION_DEPTH || colorName in visitedColors) {
            LOG.warn("Circular reference detected or max depth reached for color: $colorName")
            return null
        }
        
        visitedColors.add(colorName)
        
        val colorValue = colors[colorName] ?: return null
        
        // If it's a reference to another color, resolve it recursively
        return when {
            colorValue.startsWith("@color/") -> {
                val referencedColorName = extractColorName(colorValue)
                resolveColorWithIndirectReferences(referencedColorName, colors, visitedColors, depth + 1)
            }
            colorValue.startsWith("@android:color/") -> {
                // Handle Android system colors
                getAndroidSystemColor(colorValue)
            }
            else -> colorValue
        }
    }
    
    private fun buildColorCacheSync(project: Project) {
        val colors = ConcurrentHashMap<String, String>()
        
        ApplicationManager.getApplication().runReadAction {
            try {
                // Search for resource files in project sources
                val projectFiles = projectSearchStrategy.findResourceFiles(project)
                projectFiles.forEach { file ->
                    if (file.name == "R.txt") {
                        // Handle R.txt files specially
                        val colorNames = resourceParser.parseRTxtFile(file)
                        colorNames.forEach { name ->
                            colors.putIfAbsent(name, "@color/$name")
                        }
                    } else {
                        val fileColors = resourceParser.parseResourceFile(file)
                        colors.putAll(fileColors)
                    }
                }
                
                // Search for resource files in build outputs
                val buildFiles = buildOutputSearchStrategy.findResourceFiles(project)
                buildFiles.forEach { file ->
                    if (file.name == "R.txt") {
                        val colorNames = resourceParser.parseRTxtFile(file)
                        colorNames.forEach { name ->
                            colors.putIfAbsent(name, "@color/$name")
                        }
                    } else {
                        val fileColors = resourceParser.parseResourceFile(file)
                        // Build output colors have priority over source colors
                        colors.putAll(fileColors)
                    }
                }
                
                // Search for resources in libraries
                val libraryResources = (librarySearchStrategy as LibraryResourceSearchStrategyImpl).findAllLibraryResources(project)
                libraryResources.forEach { entry ->
                    if (entry.path.endsWith("R.txt")) {
                        // Parse R.txt content from string
                        parseRTxtContent(entry.content).forEach { name ->
                            colors.putIfAbsent(name, "@color/$name")
                        }
                    } else if (entry.path.endsWith(".xml")) {
                        // Parse XML content from string
                        val document = parseXmlDocument(entry.content)
                        if (document != null) {
                            val fileColors = extractColorsFromDocument(document)
                            colors.putAll(fileColors)
                        }
                    }
                }
                
                LOG.info("Found ${colors.size} colors from all sources")
            } catch (e: Exception) {
                LOG.error("Error building color cache", e)
            }
        }
        
        // Update cache
        cacheManager.updateCache(project, colors)
    }
    
    private fun buildColorCacheInBackground(project: Project, indicator: ProgressIndicator) {
        val colors = ConcurrentHashMap<String, String>()
        
        try {
            indicator.text = "Searching for resource files..."
            
            // Perform file search in read action
            data class AllResources(
                val projectFiles: Collection<com.intellij.openapi.vfs.VirtualFile>,
                val buildFiles: Collection<com.intellij.openapi.vfs.VirtualFile>,
                val libraryEntries: Collection<ResourceEntry>
            )
            
            val allResources = ApplicationManager.getApplication().runReadAction<AllResources> {
                AllResources(
                    projectSearchStrategy.findResourceFiles(project),
                    buildOutputSearchStrategy.findResourceFiles(project),
                    (librarySearchStrategy as LibraryResourceSearchStrategyImpl).findAllLibraryResources(project)
                )
            }
            
            val totalItems = allResources.projectFiles.size + allResources.buildFiles.size + allResources.libraryEntries.size
            indicator.text = "Parsing $totalItems resource items..."
            var processed = 0
            
            // Parse all resources concurrently
            val futures = mutableListOf<CompletableFuture<*>>()
            
            // Process project files
            allResources.projectFiles.forEach { file ->
                futures.add(CompletableFuture.runAsync {
                    ApplicationManager.getApplication().runReadAction {
                        processFile(file, colors, indicator, processed++, totalItems)
                    }
                })
            }
            
            // Process build output files
            allResources.buildFiles.forEach { file ->
                futures.add(CompletableFuture.runAsync {
                    ApplicationManager.getApplication().runReadAction {
                        processFile(file, colors, indicator, processed++, totalItems)
                    }
                })
            }
            
            // Process library entries
            allResources.libraryEntries.forEach { entry ->
                futures.add(CompletableFuture.runAsync {
                    processLibraryEntry(entry, colors, indicator, processed++, totalItems)
                })
            }
            
            // Wait for all parsing to complete
            CompletableFuture.allOf(*futures.toTypedArray()).join()
            
            LOG.info("Found ${colors.size} colors from $totalItems items")
            
            // Update cache
            cacheManager.updateCache(project, colors)
            
        } catch (e: Exception) {
            LOG.error("Error building color cache in background", e)
        }
    }
    
    private fun getAndroidSystemColor(colorReference: String): String? {
        // Map of common Android system colors
        val androidColors = mapOf(
            "@android:color/black" to "#000000",
            "@android:color/white" to "#FFFFFF",
            "@android:color/transparent" to "#00000000",
            "@android:color/holo_blue_light" to "#FF33B5E5",
            "@android:color/holo_green_light" to "#FF99CC00",
            "@android:color/holo_orange_light" to "#FFFFBB33",
            "@android:color/holo_red_light" to "#FFFF4444",
            "@android:color/holo_blue_dark" to "#FF0099CC",
            "@android:color/holo_green_dark" to "#FF669900",
            "@android:color/holo_orange_dark" to "#FFFF8800",
            "@android:color/holo_red_dark" to "#FFCC0000",
            "@android:color/holo_purple" to "#FFAA66CC"
        )
        
        return androidColors[colorReference]
    }
    
    private fun processFile(
        file: com.intellij.openapi.vfs.VirtualFile,
        colors: ConcurrentHashMap<String, String>,
        indicator: ProgressIndicator,
        processedCount: Int,
        totalCount: Int
    ) {
        try {
            if (file.name == "R.txt") {
                val colorNames = resourceParser.parseRTxtFile(file)
                colorNames.forEach { name ->
                    colors.putIfAbsent(name, "@color/$name")
                }
            } else {
                val fileColors = resourceParser.parseResourceFile(file)
                colors.putAll(fileColors)
            }
            
            synchronized(indicator) {
                indicator.fraction = processedCount.toDouble() / totalCount
                indicator.text2 = "Processed $processedCount of $totalCount items"
            }
        } catch (e: Exception) {
            LOG.warn("Failed to parse file: ${file.path}", e)
        }
    }
    
    private fun processLibraryEntry(
        entry: ResourceEntry,
        colors: ConcurrentHashMap<String, String>,
        indicator: ProgressIndicator,
        processedCount: Int,
        totalCount: Int
    ) {
        try {
            if (entry.path.endsWith("R.txt")) {
                parseRTxtContent(entry.content).forEach { name ->
                    colors.putIfAbsent(name, "@color/$name")
                }
            } else if (entry.path.endsWith(".xml")) {
                val document = parseXmlDocument(entry.content)
                if (document != null) {
                    val fileColors = extractColorsFromDocument(document)
                    colors.putAll(fileColors)
                }
            }
            
            synchronized(indicator) {
                indicator.fraction = processedCount.toDouble() / totalCount
                indicator.text2 = "Processed $processedCount of $totalCount items"
            }
        } catch (e: Exception) {
            LOG.warn("Failed to parse library entry: ${entry.path}", e)
        }
    }
    
    private fun parseRTxtContent(content: String): Set<String> {
        val colorNames = mutableSetOf<String>()
        content.lines().forEach { line ->
            if (line.startsWith("int color ")) {
                val parts = line.split(" ")
                if (parts.size >= 4) {
                    colorNames.add(parts[2])
                }
            }
        }
        return colorNames
    }
    
    private fun parseXmlDocument(xmlContent: String): org.w3c.dom.Document? {
        return try {
            val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
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
        val colorElements = document.getElementsByTagName("color")
        
        for (i in 0 until colorElements.length) {
            val element = colorElements.item(i)
            val colorName = element.attributes?.getNamedItem("name")?.nodeValue
            val colorValue = element.textContent?.trim()
            
            if (colorName != null && colorValue != null && colorValue.isNotEmpty()) {
                when {
                    colorValue.startsWith("#") -> {
                        colors[colorName] = colorValue.uppercase()
                    }
                    colorValue.startsWith("@color/") -> {
                        colors[colorName] = colorValue
                    }
                    colorValue.startsWith("@android:color/") -> {
                        colors[colorName] = colorValue
                    }
                }
            }
        }
        
        return colors
    }
}