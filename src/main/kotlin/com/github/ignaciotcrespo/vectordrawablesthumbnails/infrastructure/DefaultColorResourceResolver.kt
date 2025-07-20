package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ColorResourceResolver
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.StringReader
import java.util.concurrent.ConcurrentHashMap
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
            // For AAR files, look for resources in the expanded directory structure
            if (libraryFile.isDirectory) {
                // Search for color resources in res/values directories
                val resDir = libraryFile.findChild("res")
                if (resDir != null && resDir.isDirectory) {
                    resDir.children.filter { it.name.startsWith("values") && it.isDirectory }.forEach { valuesDir ->
                        valuesDir.children.filter { it.extension == "xml" }.forEach { xmlFile ->
                            parseColorFile(xmlFile, cache)
                        }
                    }
                }
            } else if (libraryFile.extension == "jar" || libraryFile.extension == "aar") {
                // For JAR/AAR files, we need to check if they contain Android resources
                // This is more complex as it requires extracting the archive
                // For now, we'll skip this case, but it could be implemented in the future
                // using JarFile or similar APIs to read the contents
            }
        } catch (e: Exception) {
            // Ignore errors when processing library files
        }
    }
}