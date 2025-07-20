package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.AndroidResourceLocator
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ResourceEntry
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ResourceFileCollection
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

/**
 * Implementation of AndroidResourceLocator that finds resource files
 * across different Android project configurations.
 */
class AndroidResourceLocatorImpl : AndroidResourceLocator {
    
    companion object {
        private val LOG = Logger.getInstance(AndroidResourceLocatorImpl::class.java)
        
        // Resource file patterns
        private val RESOURCE_FILE_PATTERNS = listOf(
            "**/values/colors.xml",
            "**/values/values.xml",
            "**/values-*/colors.xml",
            "**/values-*/values.xml",
            "**/R.txt"
        )
        
        // Build directory patterns for different AGP versions
        private val BUILD_DIR_PATTERNS = listOf(
            "build/intermediates",
            "build/generated",
            "build/outputs",
            ".gradle/build"
        )
    }
    
    override fun findAllResourceFiles(project: Project): ResourceFileCollection {
        val sourceFiles = mutableListOf<VirtualFile>()
        val buildOutputFiles = mutableListOf<VirtualFile>()
        val libraryEntries = mutableListOf<ResourceEntry>()
        
        try {
            // Find source resource files
            ModuleManager.getInstance(project).modules.forEach { module ->
                val moduleRoot = ModuleRootManager.getInstance(module)
                
                // Search in source directories
                moduleRoot.sourceRoots.forEach { sourceRoot ->
                    findResourceFilesInDirectory(sourceRoot, sourceFiles, false)
                }
                
                // Search in resource directories (for Android modules)
                val moduleFile = module.moduleFile
                if (moduleFile != null) {
                    val moduleDir = moduleFile.parent
                    
                    // Check common Android resource locations
                    listOf("src/main/res", "src/debug/res", "src/release/res").forEach { resPath ->
                        moduleDir.findFileByRelativePath(resPath)?.let { resDir ->
                            findResourceFilesInDirectory(resDir, sourceFiles, false)
                        }
                    }
                    
                    // Search in build directories
                    findBuildOutputFiles(moduleDir, buildOutputFiles)
                }
                
                // Search in dependencies
                moduleRoot.orderEntries().forEachLibrary { library ->
                    library.getFiles(com.intellij.openapi.roots.OrderRootType.CLASSES).forEach { file ->
                        if (file.extension == "aar" || file.extension == "jar") {
                            extractResourcesFromArchive(file, libraryEntries)
                        }
                    }
                    true
                }
            }
            
            LOG.info("Found ${sourceFiles.size} source files, ${buildOutputFiles.size} build files, ${libraryEntries.size} library entries")
            
        } catch (e: Exception) {
            LOG.error("Error finding resource files", e)
        }
        
        return ResourceFileCollection(sourceFiles, buildOutputFiles, libraryEntries)
    }
    
    override fun getSupportedBuildVariants(project: Project): List<String> {
        val variants = mutableSetOf<String>()
        
        try {
            ModuleManager.getInstance(project).modules.forEach { module ->
                val moduleFile = module.moduleFile
                if (moduleFile != null) {
                    val buildDir = moduleFile.parent.findFileByRelativePath("build")
                    if (buildDir != null && buildDir.exists()) {
                        // Look for build variant directories
                        buildDir.children.forEach { child ->
                            if (child.isDirectory && child.name == "intermediates") {
                                child.children.forEach { intermediate ->
                                    // Extract variant names from directory structure
                                    val name = intermediate.name
                                    if (name.contains("debug", ignoreCase = true)) {
                                        variants.add("debug")
                                    }
                                    if (name.contains("release", ignoreCase = true)) {
                                        variants.add("release")
                                    }
                                    // Check for custom variants
                                    if (name.matches(Regex("merge(.+)Resources"))) {
                                        val variant = name.substringAfter("merge").substringBefore("Resources")
                                            .lowercase().trim()
                                        if (variant.isNotEmpty()) {
                                            variants.add(variant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOG.warn("Error detecting build variants", e)
        }
        
        // Always include default variants
        if (variants.isEmpty()) {
            variants.addAll(listOf("debug", "release"))
        }
        
        return variants.toList().sorted()
    }
    
    private fun findResourceFilesInDirectory(
        directory: VirtualFile,
        resultList: MutableList<VirtualFile>,
        isBuildDir: Boolean
    ) {
        if (!directory.exists() || !directory.isDirectory) return
        
        try {
            directory.children.forEach { file ->
                when {
                    file.isDirectory -> {
                        // Recursively search subdirectories
                        if (shouldSearchDirectory(file, isBuildDir)) {
                            findResourceFilesInDirectory(file, resultList, isBuildDir)
                        }
                    }
                    isResourceFile(file) -> {
                        resultList.add(file)
                    }
                }
            }
        } catch (e: Exception) {
            LOG.debug("Error searching directory: ${directory.path}", e)
        }
    }
    
    private fun findBuildOutputFiles(moduleDir: VirtualFile, resultList: MutableList<VirtualFile>) {
        BUILD_DIR_PATTERNS.forEach { pattern ->
            val buildDir = moduleDir.findFileByRelativePath(pattern)
            if (buildDir != null && buildDir.exists()) {
                findResourceFilesInDirectory(buildDir, resultList, true)
            }
        }
    }
    
    private fun shouldSearchDirectory(dir: VirtualFile, isBuildDir: Boolean): Boolean {
        val name = dir.name.lowercase()
        
        // Skip certain directories to improve performance
        val skipDirs = setOf("test", "androidtest", "assets", "raw", "drawable", "layout", "mipmap")
        if (name in skipDirs) return false
        
        // In build directories, focus on specific paths
        if (isBuildDir) {
            return name.contains("values") || 
                   name.contains("merged") || 
                   name.contains("symbol") ||
                   name.contains("runtime")
        }
        
        return true
    }
    
    private fun isResourceFile(file: VirtualFile): Boolean {
        if (!file.exists() || file.isDirectory) return false
        
        return when {
            file.name == "R.txt" -> true
            file.name == "colors.xml" -> true
            file.name == "values.xml" -> true
            file.extension == "xml" && file.parent?.name?.startsWith("values") == true -> true
            else -> false
        }
    }
    
    private fun extractResourcesFromArchive(
        archiveFile: VirtualFile,
        resultList: MutableList<ResourceEntry>
    ) {
        try {
            // For AAR files, resources are in res/values/
            if (archiveFile.extension == "aar") {
                val jarPath = archiveFile.path
                // Note: In a real implementation, we would extract and read AAR contents
                // For now, we'll log this as a TODO
                LOG.debug("TODO: Extract resources from AAR: $jarPath")
            }
        } catch (e: Exception) {
            LOG.debug("Error extracting resources from archive: ${archiveFile.path}", e)
        }
    }
}