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
            ".gradle/build",
            // Specific paths for merged resources in different AGP versions
            "build/intermediates/incremental/merge*/merged.dir/values",
            "build/intermediates/res/merged/*/values", 
            "build/intermediates/merged_res/*/values",
            "build/intermediates/incremental/mergeDebugResources/merged.dir/values",
            "build/intermediates/incremental/mergeReleaseResources/merged.dir/values"
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
            if (pattern.contains("*")) {
                // Handle glob patterns
                val parts = pattern.split("*")
                if (parts.size == 2) {
                    val prefix = parts[0]
                    val suffix = parts[1]
                    val parentDir = moduleDir.findFileByRelativePath(prefix.substringBeforeLast("/"))
                    if (parentDir != null && parentDir.exists()) {
                        parentDir.children.forEach { child ->
                            if (child.isDirectory) {
                                val targetPath = suffix.removePrefix("/")
                                val targetDir = if (targetPath.isNotEmpty()) {
                                    child.findFileByRelativePath(targetPath)
                                } else {
                                    child
                                }
                                if (targetDir != null && targetDir.exists()) {
                                    findResourceFilesInDirectory(targetDir, resultList, true)
                                }
                            }
                        }
                    }
                }
            } else {
                // Handle exact paths
                val buildDir = moduleDir.findFileByRelativePath(pattern)
                if (buildDir != null && buildDir.exists()) {
                    findResourceFilesInDirectory(buildDir, resultList, true)
                }
            }
        }
        
        // Also look for app-specific build directories
        moduleDir.children.filter { it.name == "app" || it.name.endsWith("-app") }.forEach { appDir ->
            BUILD_DIR_PATTERNS.forEach { pattern ->
                val buildDir = appDir.findFileByRelativePath(pattern)
                if (buildDir != null && buildDir.exists()) {
                    findResourceFilesInDirectory(buildDir, resultList, true)
                }
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
            when (archiveFile.extension) {
                "aar" -> extractFromAar(archiveFile, resultList)
                "jar" -> extractFromJar(archiveFile, resultList)
            }
        } catch (e: Exception) {
            LOG.debug("Error extracting resources from archive: ${archiveFile.path}", e)
        }
    }
    
    private fun extractFromAar(aarFile: VirtualFile, resultList: MutableList<ResourceEntry>) {
        try {
            // AAR files are ZIP archives containing res/values/ resources
            val jarFileSystem = com.intellij.openapi.vfs.VfsUtil.findFileByURL(
                java.net.URL("jar:${aarFile.url}!/")
            )
            
            if (jarFileSystem != null) {
                // Look for res/values directories
                val resDir = jarFileSystem.findFileByRelativePath("res")
                if (resDir != null && resDir.exists()) {
                    resDir.children.filter { it.name.startsWith("values") }.forEach { valuesDir ->
                        valuesDir.children.filter { it.extension == "xml" }.forEach { xmlFile ->
                            try {
                                val content = String(xmlFile.contentsToByteArray())
                                resultList.add(ResourceEntry(
                                    path = "aar:${aarFile.name}!/${xmlFile.path}",
                                    content = content
                                ))
                            } catch (e: Exception) {
                                LOG.debug("Error reading AAR resource: ${xmlFile.path}", e)
                            }
                        }
                    }
                }
                
                // Also check for R.txt
                val rTxtFile = jarFileSystem.findFileByRelativePath("R.txt")
                if (rTxtFile != null && rTxtFile.exists()) {
                    try {
                        val content = String(rTxtFile.contentsToByteArray())
                        resultList.add(ResourceEntry(
                            path = "aar:${aarFile.name}!/R.txt",
                            content = content
                        ))
                    } catch (e: Exception) {
                        LOG.debug("Error reading AAR R.txt", e)
                    }
                }
            }
        } catch (e: Exception) {
            LOG.debug("Error extracting from AAR: ${aarFile.path}", e)
        }
    }
    
    private fun extractFromJar(jarFile: VirtualFile, resultList: MutableList<ResourceEntry>) {
        try {
            // JAR files might contain Android resources in META-INF
            val jarFileSystem = com.intellij.openapi.vfs.VfsUtil.findFileByURL(
                java.net.URL("jar:${jarFile.url}!/")
            )
            
            if (jarFileSystem != null) {
                // Look for Android resources in META-INF
                val metaInf = jarFileSystem.findFileByRelativePath("META-INF")
                if (metaInf != null && metaInf.exists()) {
                    metaInf.children.filter { 
                        it.name.endsWith("_colors.xml") || it.name == "R.txt"
                    }.forEach { resourceFile ->
                        try {
                            val content = String(resourceFile.contentsToByteArray())
                            resultList.add(ResourceEntry(
                                path = "jar:${jarFile.name}!/${resourceFile.path}",
                                content = content
                            ))
                        } catch (e: Exception) {
                            LOG.debug("Error reading JAR resource: ${resourceFile.path}", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOG.debug("Error extracting from JAR: ${jarFile.path}", e)
        }
    }
}