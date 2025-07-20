package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ColorResourceResolver
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.StringReader
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarFile
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Default implementation of ColorResourceResolver.
 * Resolves color references from Android resource files.
 */
class DefaultColorResourceResolver : ColorResourceResolver {
    
    // Cache for color resources per project
    private val colorCache = ConcurrentHashMap<String, ConcurrentHashMap<String, String>>()
    
    override fun resolveColorReference(colorReference: String, project: Project): String? {
        // Extract color name from reference (e.g., "@color/colorPrimary" -> "colorPrimary")
        val colorName = colorReference.removePrefix("@color/").trim()
        
        // Get project cache
        val projectCache = colorCache[project.name] ?: run {
            // Build cache if not exists
            buildColorCache(project)
            colorCache[project.name]
        }
        
        // Resolve color with support for indirect references
        return resolveColorWithIndirectReferences(colorName, projectCache ?: ConcurrentHashMap())
    }
    
    private fun resolveColorWithIndirectReferences(
        colorName: String,
        cache: ConcurrentHashMap<String, String>,
        visitedColors: MutableSet<String> = mutableSetOf()
    ): String? {
        // Prevent infinite recursion
        if (colorName in visitedColors) {
            return null
        }
        visitedColors.add(colorName)
        
        val colorValue = cache[colorName] ?: return null
        
        // If it's a reference to another color, resolve it recursively
        if (colorValue.startsWith("@color/")) {
            val referencedColorName = colorValue.removePrefix("@color/").trim()
            return resolveColorWithIndirectReferences(referencedColorName, cache, visitedColors)
        }
        
        // Return direct color value
        return colorValue
    }
    
    override fun buildColorCache(project: Project) {
        val projectCache = ConcurrentHashMap<String, String>()
        
        ApplicationManager.getApplication().runReadAction {
            try {
                // Create a combined scope that includes project and all library dependencies
                val projectScope = GlobalSearchScope.projectScope(project)
                val allLibrariesScope = GlobalSearchScope.allScope(project)
                val combinedScope = projectScope.union(allLibrariesScope)
                
                // Find all colors.xml files in project and dependencies
                val colorFiles = FilenameIndex.getVirtualFilesByName(project, "colors.xml", combinedScope)
                
                // Process each colors.xml file
                colorFiles.forEach { file ->
                    parseColorFile(file, projectCache)
                }
                
                // Also look for color definitions in other resource files
                val xmlFiles = FilenameIndex.getAllFilesByExt(project, "xml", combinedScope)
                    .filter { file -> 
                        val path = file.path
                        path.contains("/values/") || path.contains("/values-")
                    }
                
                xmlFiles.forEach { file ->
                    if (file.name != "colors.xml") {
                        parseColorFile(file, projectCache)
                    }
                }
                
                // Additionally, check module dependencies for compiled resources
                ModuleManager.getInstance(project).modules.forEach { module ->
                    val moduleRootManager = ModuleRootManager.getInstance(module)
                    
                    // Look for Android build outputs in the module
                    searchAndroidBuildOutputs(module, projectCache)
                    
                    // Look for resources in library entries
                    moduleRootManager.orderEntries().forEachLibrary { library ->
                        library.getFiles(com.intellij.openapi.roots.OrderRootType.CLASSES).forEach { libraryFile ->
                            // Check for resource files in AAR/JAR files
                            if (libraryFile.isValid && libraryFile.exists()) {
                                searchColorResourcesInLibrary(libraryFile, projectCache)
                            }
                        }
                        true // Continue iteration
                    }
                }
                
            } catch (e: Exception) {
                println("Error building color cache: ${e.message}")
            }
        }
        
        colorCache[project.name] = projectCache
    }
    
    override fun clearCache() {
        colorCache.clear()
    }
    
    override fun getAllColorResources(project: Project): Map<String, String> {
        val projectCache = colorCache[project.name] ?: run {
            buildColorCache(project)
            colorCache[project.name]
        }
        
        return projectCache?.toMap() ?: emptyMap()
    }
    
    private fun parseColorFile(file: VirtualFile, cache: ConcurrentHashMap<String, String>) {
        try {
            val content = String(file.contentsToByteArray())
            val document = parseXmlDocument(content)
            
            if (document != null) {
                // Extract color elements
                val colorElements = document.getElementsByTagName("color")
                
                for (i in 0 until colorElements.length) {
                    val element = colorElements.item(i)
                    val colorName = element.attributes?.getNamedItem("name")?.nodeValue
                    val colorValue = element.textContent?.trim()
                    
                    if (colorName != null && colorValue != null) {
                        // Store both direct color values and color references
                        when {
                            colorValue.startsWith("#") -> {
                                // Direct hex color value
                                cache[colorName] = colorValue.uppercase()
                            }
                            colorValue.startsWith("@color/") -> {
                                // Store color reference for later resolution
                                cache[colorName] = colorValue
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore parsing errors for individual files
        }
    }
    
    private fun parseXmlDocument(xmlContent: String): Document? {
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val inputSource = InputSource(StringReader(xmlContent))
            builder.parse(inputSource)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun searchColorResourcesInLibrary(libraryFile: VirtualFile, cache: ConcurrentHashMap<String, String>) {
        try {
            when {
                libraryFile.isDirectory -> {
                    // Search for color resources in res/values directories
                    val resDir = libraryFile.findChild("res")
                    if (resDir != null && resDir.isDirectory) {
                        resDir.children.filter { it.name.startsWith("values") && it.isDirectory }.forEach { valuesDir ->
                            valuesDir.children.filter { it.extension == "xml" }.forEach { xmlFile ->
                                parseColorFile(xmlFile, cache)
                            }
                        }
                    }
                    
                    // Also check for R.txt files in AAR exploded directories
                    val rTxtFile = libraryFile.findFileByRelativePath("R.txt")
                    if (rTxtFile != null && rTxtFile.exists()) {
                        parseRTxtFile(rTxtFile, cache)
                    }
                }
                libraryFile.extension == "aar" -> {
                    // Handle compressed AAR files
                    parseAarFile(libraryFile, cache)
                }
                libraryFile.extension == "jar" -> {
                    // Some JARs might contain resources, check them
                    parseJarFile(libraryFile, cache)
                }
            }
        } catch (e: Exception) {
            // Ignore errors when processing library files
        }
    }
    
    private fun searchAndroidBuildOutputs(module: com.intellij.openapi.module.Module, cache: ConcurrentHashMap<String, String>) {
        try {
            val moduleFile = module.moduleFile
            if (moduleFile != null) {
                val moduleDir = moduleFile.parent
                
                // Common Android build output directories
                val buildPaths = listOf(
                    // Merged resources from all sources
                    "build/intermediates/incremental/debug/mergeDebugResources/merged.dir/values",
                    "build/intermediates/incremental/release/mergeReleaseResources/merged.dir/values",
                    "build/intermediates/merged_res/debug/values",
                    "build/intermediates/merged_res/release/values",
                    "build/intermediates/res/merged/debug/values",
                    "build/intermediates/res/merged/release/values",
                    // R.txt files with resource definitions
                    "build/intermediates/runtime_symbol_list/debug/R.txt",
                    "build/intermediates/runtime_symbol_list/release/R.txt",
                    "build/intermediates/symbols/debug/R.txt",
                    "build/intermediates/symbols/release/R.txt",
                    // Additional merged manifest and resources
                    "build/intermediates/packaged_res/debug/values",
                    "build/intermediates/packaged_res/release/values",
                    "build/intermediates/merged-not-compiled-resources/debug/values",
                    "build/intermediates/merged-not-compiled-resources/release/values"
                )
                
                buildPaths.forEach { path ->
                    val file = moduleDir.findFileByRelativePath(path)
                    when {
                        file != null && file.exists() && file.name == "R.txt" -> {
                            parseRTxtFile(file, cache)
                        }
                        file != null && file.exists() && file.isDirectory -> {
                            // Look for values XML files
                            file.children.filter { it.extension == "xml" }.forEach { xmlFile ->
                                parseColorFile(xmlFile, cache)
                            }
                        }
                    }
                }
                
                // Also check for merged values files that contain actual color values
                val mergedValuesPaths = listOf(
                    "build/intermediates/incremental/debug/mergeDebugResources/merged.dir/values/values.xml",
                    "build/intermediates/incremental/release/mergeReleaseResources/merged.dir/values/values.xml",
                    "build/intermediates/res/merged/debug/values/values.xml",
                    "build/intermediates/res/merged/release/values/values.xml"
                )
                
                mergedValuesPaths.forEach { path ->
                    val mergedFile = moduleDir.findFileByRelativePath(path)
                    if (mergedFile != null && mergedFile.exists()) {
                        parseColorFile(mergedFile, cache)
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore errors when searching build outputs
        }
    }
    
    private fun parseRTxtFile(rTxtFile: VirtualFile, cache: ConcurrentHashMap<String, String>) {
        try {
            rTxtFile.inputStream.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.lines().forEach { line ->
                        // R.txt format: int color colorName 0x7f060001
                        if (line.startsWith("int color ")) {
                            val parts = line.split(" ")
                            if (parts.size >= 4) {
                                val colorName = parts[2]
                                val hexValue = parts[3]
                                // Convert Android resource ID to hex color if possible
                                // Note: This requires additional mapping from resource IDs to actual values
                                // For now, we'll just mark that this color exists
                                if (!cache.containsKey(colorName)) {
                                    // Mark as unresolved reference for now
                                    cache[colorName] = "@color/$colorName"
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore errors when parsing R.txt
        }
    }
    
    private fun parseAarFile(aarFile: VirtualFile, cache: ConcurrentHashMap<String, String>) {
        try {
            val path = aarFile.path
            if (path.endsWith("!/")) {
                // This is a JAR URL, extract the actual file path
                val actualPath = path.substring(0, path.length - 2)
                parseAarFileFromPath(actualPath, cache)
            } else {
                parseAarFileFromPath(path, cache)
            }
        } catch (e: Exception) {
            // Ignore errors
        }
    }
    
    private fun parseAarFileFromPath(aarPath: String, cache: ConcurrentHashMap<String, String>) {
        try {
            ZipFile(aarPath).use { zipFile ->
                // Look for res/values*/colors.xml entries
                zipFile.entries().asSequence()
                    .filter { entry ->
                        !entry.isDirectory &&
                        (entry.name.matches(Regex("res/values[^/]*/.*\\.xml")) ||
                         entry.name == "R.txt")
                    }
                    .forEach { entry ->
                        when {
                            entry.name == "R.txt" -> {
                                zipFile.getInputStream(entry).use { inputStream ->
                                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                                        reader.lines().forEach { line ->
                                            if (line.startsWith("int color ")) {
                                                val parts = line.split(" ")
                                                if (parts.size >= 4) {
                                                    val colorName = parts[2]
                                                    if (!cache.containsKey(colorName)) {
                                                        cache[colorName] = "@color/$colorName"
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            entry.name.endsWith(".xml") -> {
                                zipFile.getInputStream(entry).use { inputStream ->
                                    val content = inputStream.bufferedReader().use { it.readText() }
                                    val document = parseXmlDocument(content)
                                    if (document != null) {
                                        extractColorsFromDocument(document, cache)
                                    }
                                }
                            }
                        }
                    }
            }
        } catch (e: Exception) {
            // Ignore errors when parsing AAR files
        }
    }
    
    private fun parseJarFile(jarFile: VirtualFile, cache: ConcurrentHashMap<String, String>) {
        try {
            val path = jarFile.path
            if (path.endsWith("!/")) {
                val actualPath = path.substring(0, path.length - 2)
                parseJarFileFromPath(actualPath, cache)
            } else {
                parseJarFileFromPath(path, cache)
            }
        } catch (e: Exception) {
            // Ignore errors
        }
    }
    
    private fun parseJarFileFromPath(jarPath: String, cache: ConcurrentHashMap<String, String>) {
        try {
            JarFile(jarPath).use { jarFile ->
                // Look for Android resource files in JAR
                jarFile.entries().asSequence()
                    .filter { entry ->
                        !entry.isDirectory &&
                        entry.name.matches(Regex(".*res/values[^/]*/.*\\.xml"))
                    }
                    .forEach { entry ->
                        jarFile.getInputStream(entry).use { inputStream ->
                            val content = inputStream.bufferedReader().use { it.readText() }
                            val document = parseXmlDocument(content)
                            if (document != null) {
                                extractColorsFromDocument(document, cache)
                            }
                        }
                    }
            }
        } catch (e: Exception) {
            // Ignore errors when parsing JAR files
        }
    }
    
    private fun extractColorsFromDocument(document: Document, cache: ConcurrentHashMap<String, String>) {
        val colorElements = document.getElementsByTagName("color")
        for (i in 0 until colorElements.length) {
            val element = colorElements.item(i)
            val colorName = element.attributes?.getNamedItem("name")?.nodeValue
            val colorValue = element.textContent?.trim()
            
            if (colorName != null && colorValue != null) {
                when {
                    colorValue.startsWith("#") -> {
                        cache[colorName] = colorValue.uppercase()
                    }
                    colorValue.startsWith("@color/") -> {
                        cache[colorName] = colorValue
                    }
                }
            }
        }
    }
}