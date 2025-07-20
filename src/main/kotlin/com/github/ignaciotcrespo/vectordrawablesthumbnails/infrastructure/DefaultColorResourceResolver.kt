package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ColorResourceResolver
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
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
        
        return projectCache?.get(colorName)
    }
    
    override fun buildColorCache(project: Project) {
        val projectCache = ConcurrentHashMap<String, String>()
        
        ApplicationManager.getApplication().runReadAction {
            try {
                // Find all colors.xml files in the project
                val scope = GlobalSearchScope.projectScope(project)
                val colorFiles = FilenameIndex.getVirtualFilesByName(project, "colors.xml", scope)
                
                // Process each colors.xml file
                colorFiles.forEach { file ->
                    parseColorFile(file, projectCache)
                }
                
                // Also look for color definitions in other resource files
                val xmlFiles = FilenameIndex.getAllFilesByExt(project, "xml", scope)
                    .filter { file -> 
                        val path = file.path
                        path.contains("/values/") || path.contains("/values-")
                    }
                
                xmlFiles.forEach { file ->
                    if (file.name != "colors.xml") {
                        parseColorFile(file, projectCache)
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
                        // Handle color references (e.g., @color/other_color)
                        if (colorValue.startsWith("@color/")) {
                            // This is a reference to another color, we'll resolve it later
                            continue
                        }
                        
                        // Store direct color values
                        if (colorValue.startsWith("#")) {
                            cache[colorName] = colorValue.uppercase()
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
}