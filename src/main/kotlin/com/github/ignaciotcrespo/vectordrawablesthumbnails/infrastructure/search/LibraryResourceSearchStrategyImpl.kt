package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure.search

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.LibraryResourceSearchStrategy
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ResourceEntry
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.jar.JarFile
import java.util.zip.ZipFile

/**
 * Implementation for searching color resources in library dependencies.
 * Follows Single Responsibility Principle - only searches libraries.
 */
class LibraryResourceSearchStrategyImpl : LibraryResourceSearchStrategy {
    
    companion object {
        private val LOG = Logger.getInstance(LibraryResourceSearchStrategyImpl::class.java)
    }
    
    override fun findResourceFiles(project: Project): Collection<VirtualFile> {
        // This strategy returns ResourceEntry objects, not VirtualFiles
        // Implement a different method for getting library resources
        return emptyList()
    }
    
    override fun findResourcesInLibrary(libraryFile: VirtualFile): Collection<ResourceEntry> {
        val resources = mutableListOf<ResourceEntry>()
        
        try {
            when {
                libraryFile.isDirectory -> {
                    resources.addAll(searchDirectoryForResources(libraryFile))
                }
                libraryFile.extension == "aar" -> {
                    resources.addAll(parseAarFile(libraryFile))
                }
                libraryFile.extension == "jar" -> {
                    resources.addAll(parseJarFile(libraryFile))
                }
            }
        } catch (e: Exception) {
            LOG.warn("Error searching library: ${libraryFile.path}", e)
        }
        
        return resources
    }
    
    fun findAllLibraryResources(project: Project): Collection<ResourceEntry> {
        val allResources = mutableListOf<ResourceEntry>()
        
        try {
            ModuleManager.getInstance(project).modules.forEach { module ->
                val moduleRootManager = ModuleRootManager.getInstance(module)
                
                moduleRootManager.orderEntries().forEachLibrary { library ->
                    library.getFiles(com.intellij.openapi.roots.OrderRootType.CLASSES).forEach { libraryFile ->
                        if (libraryFile.isValid && libraryFile.exists()) {
                            allResources.addAll(findResourcesInLibrary(libraryFile))
                        }
                    }
                    true // Continue iteration
                }
            }
            
            LOG.info("Found ${allResources.size} resource entries in libraries")
        } catch (e: Exception) {
            LOG.error("Error searching library resources", e)
        }
        
        return allResources
    }
    
    private fun searchDirectoryForResources(directory: VirtualFile): Collection<ResourceEntry> {
        val resources = mutableListOf<ResourceEntry>()
        
        // Search for color resources in res/values directories
        val resDir = directory.findChild("res")
        if (resDir != null && resDir.isDirectory) {
            resDir.children.filter { it.name.startsWith("values") && it.isDirectory }.forEach { valuesDir ->
                valuesDir.children.filter { it.extension == "xml" }.forEach { xmlFile ->
                    try {
                        val content = String(xmlFile.contentsToByteArray())
                        resources.add(ResourceEntry(xmlFile.path, content))
                    } catch (e: Exception) {
                        LOG.debug("Failed to read file: ${xmlFile.path}", e)
                    }
                }
            }
        }
        
        // Also check for R.txt files
        val rTxtFile = directory.findFileByRelativePath("R.txt")
        if (rTxtFile != null && rTxtFile.exists()) {
            try {
                val content = String(rTxtFile.contentsToByteArray())
                resources.add(ResourceEntry(rTxtFile.path, content))
            } catch (e: Exception) {
                LOG.debug("Failed to read R.txt: ${rTxtFile.path}", e)
            }
        }
        
        return resources
    }
    
    private fun parseAarFile(aarFile: VirtualFile): Collection<ResourceEntry> {
        val resources = mutableListOf<ResourceEntry>()
        
        try {
            val path = aarFile.path.let { 
                if (it.endsWith("!/")) it.substring(0, it.length - 2) else it 
            }
            
            ZipFile(path).use { zipFile ->
                zipFile.entries().asSequence()
                    .filter { entry ->
                        !entry.isDirectory &&
                        (entry.name.matches(Regex("res/values[^/]*/.*\\.xml")) ||
                         entry.name == "R.txt")
                    }
                    .forEach { entry ->
                        try {
                            zipFile.getInputStream(entry).use { inputStream ->
                                val content = inputStream.bufferedReader().use { it.readText() }
                                resources.add(ResourceEntry(entry.name, content))
                            }
                        } catch (e: Exception) {
                            LOG.debug("Failed to read AAR entry: ${entry.name}", e)
                        }
                    }
            }
        } catch (e: Exception) {
            LOG.warn("Failed to parse AAR file: ${aarFile.path}", e)
        }
        
        return resources
    }
    
    private fun parseJarFile(jarFile: VirtualFile): Collection<ResourceEntry> {
        val resources = mutableListOf<ResourceEntry>()
        
        try {
            val path = jarFile.path.let { 
                if (it.endsWith("!/")) it.substring(0, it.length - 2) else it 
            }
            
            JarFile(path).use { jar ->
                jar.entries().asSequence()
                    .filter { entry ->
                        !entry.isDirectory &&
                        entry.name.matches(Regex(".*res/values[^/]*/.*\\.xml"))
                    }
                    .forEach { entry ->
                        try {
                            jar.getInputStream(entry).use { inputStream ->
                                val content = inputStream.bufferedReader().use { it.readText() }
                                resources.add(ResourceEntry(entry.name, content))
                            }
                        } catch (e: Exception) {
                            LOG.debug("Failed to read JAR entry: ${entry.name}", e)
                        }
                    }
            }
        } catch (e: Exception) {
            LOG.warn("Failed to parse JAR file: ${jarFile.path}", e)
        }
        
        return resources
    }
}