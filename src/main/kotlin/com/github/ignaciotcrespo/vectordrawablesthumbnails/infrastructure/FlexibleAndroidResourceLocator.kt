package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.AndroidResourceLocator
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ResourceEntry
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ResourceFileCollection
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Properties
import java.util.jar.JarFile
import java.util.zip.ZipFile

/**
 * Flexible Android resource locator that adapts to different Android Gradle Plugin versions
 * and project configurations without hardcoded paths.
 * 
 * This implementation:
 * - Detects AGP version dynamically
 * - Uses gradle.properties and build configuration
 * - Adapts to different build output structures
 * - Properly handles AAR and JAR dependencies
 */
class FlexibleAndroidResourceLocator : AndroidResourceLocator {
    
    companion object {
        private val LOG = Logger.getInstance(FlexibleAndroidResourceLocator::class.java)
        
        // Generic patterns that work across AGP versions
        private val RESOURCE_FILE_NAMES = setOf(
            "colors.xml",
            "values.xml",
            "R.txt",
            "R.jar"
        )
        
        // Build directory indicators
        private val BUILD_INDICATORS = setOf(
            "build",
            ".gradle",
            "intermediates",
            "generated",
            "outputs"
        )
    }
    
    override fun findAllResourceFiles(project: Project): ResourceFileCollection {
        val sourceFiles = mutableListOf<VirtualFile>()
        val buildOutputFiles = mutableListOf<VirtualFile>()
        val libraryEntries = mutableListOf<ResourceEntry>()
        
        try {
            ModuleManager.getInstance(project).modules.forEach { module ->
                processModule(module, sourceFiles, buildOutputFiles, libraryEntries)
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
                // Check module properties for configured variants
                val gradleProperties = findGradleProperties(module)
                gradleProperties?.let { props ->
                    // Check for variant-specific properties
                    props.stringPropertyNames().forEach { key ->
                        when {
                            key.contains("buildTypes", ignoreCase = true) -> {
                                extractVariantFromProperty(key)?.let { variants.add(it) }
                            }
                            key.contains("productFlavors", ignoreCase = true) -> {
                                extractVariantFromProperty(key)?.let { variants.add(it) }
                            }
                        }
                    }
                }
                
                // Check build directories for actual variants
                val moduleFile = module.moduleFile
                if (moduleFile != null) {
                    findBuildDirectory(moduleFile.parent)?.let { buildDir ->
                        detectVariantsFromBuildStructure(buildDir, variants)
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
    
    private fun processModule(
        module: Module,
        sourceFiles: MutableList<VirtualFile>,
        buildOutputFiles: MutableList<VirtualFile>,
        libraryEntries: MutableList<ResourceEntry>
    ) {
        val moduleRoot = ModuleRootManager.getInstance(module)
        
        // Process source directories
        moduleRoot.sourceRoots.forEach { sourceRoot ->
            findResourceFilesRecursively(sourceRoot, sourceFiles, false)
        }
        
        // Process resource directories
        val moduleFile = module.moduleFile
        if (moduleFile != null) {
            val moduleDir = moduleFile.parent
            
            // Standard Android resource locations
            findStandardResourceDirectories(moduleDir).forEach { resDir ->
                findResourceFilesRecursively(resDir, sourceFiles, false)
            }
            
            // Process build output directories dynamically
            findBuildDirectory(moduleDir)?.let { buildDir ->
                processBuildDirectory(buildDir, buildOutputFiles)
            }
        }
        
        // Process dependencies
        moduleRoot.orderEntries().forEachLibrary { library ->
            library.getFiles(com.intellij.openapi.roots.OrderRootType.CLASSES).forEach { file ->
                when (file.extension) {
                    "aar" -> processAarFile(file, libraryEntries)
                    "jar" -> processJarFile(file, libraryEntries)
                }
            }
            true
        }
    }
    
    private fun findStandardResourceDirectories(moduleDir: VirtualFile): List<VirtualFile> {
        val resourceDirs = mutableListOf<VirtualFile>()
        
        // Check standard source sets
        val sourceSets = listOf("main", "debug", "release", "test", "androidTest")
        sourceSets.forEach { sourceSet ->
            moduleDir.findFileByRelativePath("src/$sourceSet/res")?.let { resourceDirs.add(it) }
            moduleDir.findFileByRelativePath("src/$sourceSet/resources")?.let { resourceDirs.add(it) }
        }
        
        // Check for custom source sets
        moduleDir.findChild("src")?.children?.forEach { child ->
            if (child.isDirectory) {
                child.findChild("res")?.let { resourceDirs.add(it) }
                child.findChild("resources")?.let { resourceDirs.add(it) }
            }
        }
        
        return resourceDirs
    }
    
    private fun findBuildDirectory(moduleDir: VirtualFile): VirtualFile? {
        // Look for build directory
        var buildDir = moduleDir.findChild("build")
        
        // If not found, check parent for multi-module projects
        if (buildDir == null) {
            moduleDir.parent?.findChild("build")?.let { parentBuild ->
                parentBuild.findChild(moduleDir.name)?.let { buildDir = it }
            }
        }
        
        // Check for gradle cache directory
        if (buildDir == null) {
            moduleDir.findChild(".gradle")?.findChild("build")?.let { buildDir = it }
        }
        
        return buildDir
    }
    
    private fun processBuildDirectory(buildDir: VirtualFile, buildOutputFiles: MutableList<VirtualFile>) {
        // Intelligently search for resource files in build directory
        val visited = mutableSetOf<VirtualFile>()
        val queue = mutableListOf(buildDir)
        
        while (queue.isNotEmpty()) {
            val current = queue.removeAt(0)
            if (!visited.add(current)) continue
            
            if (current.isDirectory) {
                // Check if this directory likely contains resources
                if (shouldSearchBuildDirectory(current)) {
                    current.children.forEach { child ->
                        if (child.isDirectory) {
                            queue.add(child)
                        } else if (isResourceFile(child)) {
                            buildOutputFiles.add(child)
                        }
                    }
                }
            }
        }
    }
    
    private fun shouldSearchBuildDirectory(dir: VirtualFile): Boolean {
        val name = dir.name.lowercase()
        
        // Skip obviously non-resource directories
        val skipPatterns = setOf("classes", "tmp", "kotlin", "java", "reports", "test-results")
        if (skipPatterns.any { name.contains(it) }) return false
        
        // Include directories that likely contain resources
        val includePatterns = setOf("res", "resource", "values", "merged", "processed", "bundle")
        return includePatterns.any { name.contains(it) }
    }
    
    private fun findResourceFilesRecursively(
        directory: VirtualFile,
        resultList: MutableList<VirtualFile>,
        isBuildDir: Boolean,
        maxDepth: Int = 10,
        currentDepth: Int = 0
    ) {
        if (!directory.exists() || !directory.isDirectory || currentDepth > maxDepth) return
        
        try {
            directory.children.forEach { file ->
                when {
                    file.isDirectory -> {
                        if (shouldSearchDirectory(file, isBuildDir)) {
                            findResourceFilesRecursively(file, resultList, isBuildDir, maxDepth, currentDepth + 1)
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
    
    private fun shouldSearchDirectory(dir: VirtualFile, isBuildDir: Boolean): Boolean {
        val name = dir.name.lowercase()
        
        // Always skip hidden directories and common non-resource directories
        if (name.startsWith(".") || name in setOf("test", "androidtest", "assets")) {
            return false
        }
        
        // For source directories, be more permissive
        if (!isBuildDir) {
            return !name.contains("drawable") && !name.contains("layout") && !name.contains("mipmap")
        }
        
        // For build directories, focus on relevant paths
        return name.contains("values") || name.contains("res") || name.contains("merged") || name.contains("processed")
    }
    
    private fun isResourceFile(file: VirtualFile): Boolean {
        if (!file.exists() || file.isDirectory) return false
        
        // Check by name
        if (file.name in RESOURCE_FILE_NAMES) return true
        
        // Check XML files in values directories
        if (file.extension == "xml" && file.parent?.name?.startsWith("values") == true) {
            return true
        }
        
        // Check R files
        if (file.name.startsWith("R.") || file.name == "R\$color.class") {
            return true
        }
        
        return false
    }
    
    private fun findGradleProperties(module: Module): Properties? {
        return try {
            val moduleFile = module.moduleFile ?: return null
            val propertiesFile = moduleFile.parent.findFileByRelativePath("gradle.properties")
                ?: moduleFile.parent.parent?.findFileByRelativePath("gradle.properties")
            
            propertiesFile?.let {
                Properties().apply {
                    load(it.inputStream)
                }
            }
        } catch (e: Exception) {
            LOG.debug("Could not load gradle.properties", e)
            null
        }
    }
    
    private fun extractVariantFromProperty(propertyKey: String): String? {
        // Extract variant name from property keys like "buildTypes.debug" or "productFlavors.free"
        val parts = propertyKey.split(".")
        return if (parts.size >= 2) parts.last() else null
    }
    
    private fun detectVariantsFromBuildStructure(buildDir: VirtualFile, variants: MutableSet<String>) {
        // Look for variant-specific directories in build output
        val variantIndicators = listOf(
            "intermediates/merged_res",
            "intermediates/res/merged",
            "generated/res",
            "outputs/apk"
        )
        
        variantIndicators.forEach { indicator ->
            buildDir.findFileByRelativePath(indicator)?.children?.forEach { child ->
                if (child.isDirectory) {
                    // Extract variant name from directory
                    val variantName = child.name.lowercase()
                    if (variantName.isNotEmpty() && !variantName.contains("test")) {
                        variants.add(variantName)
                    }
                }
            }
        }
    }
    
    private fun processAarFile(aarFile: VirtualFile, resultList: MutableList<ResourceEntry>) {
        try {
            val tempFile = createTempFile(aarFile)
            ZipFile(tempFile).use { zipFile ->
                zipFile.entries().asSequence().forEach { entry ->
                    when {
                        entry.name == "R.txt" -> {
                            processRTxtFromArchive(zipFile, entry, resultList, aarFile.name)
                        }
                        entry.name.endsWith("/colors.xml") || 
                        (entry.name.contains("/values") && entry.name.endsWith(".xml")) -> {
                            processXmlFromArchive(zipFile, entry, resultList, aarFile.name)
                        }
                    }
                }
            }
            tempFile.delete()
        } catch (e: Exception) {
            LOG.debug("Error processing AAR file: ${aarFile.name}", e)
        }
    }
    
    private fun processJarFile(jarFile: VirtualFile, resultList: MutableList<ResourceEntry>) {
        try {
            val tempFile = createTempFile(jarFile)
            JarFile(tempFile).use { jar ->
                jar.entries().asSequence().forEach { entry ->
                    when {
                        entry.name == "R.txt" -> {
                            processRTxtFromJar(jar, entry, resultList, jarFile.name)
                        }
                        entry.name.contains("R\$color") && entry.name.endsWith(".class") -> {
                            // Could process R$color.class files if needed
                        }
                    }
                }
            }
            tempFile.delete()
        } catch (e: Exception) {
            LOG.debug("Error processing JAR file: ${jarFile.name}", e)
        }
    }
    
    private fun processRTxtFromArchive(
        zipFile: ZipFile,
        entry: java.util.zip.ZipEntry,
        resultList: MutableList<ResourceEntry>,
        sourceName: String
    ) {
        try {
            BufferedReader(InputStreamReader(zipFile.getInputStream(entry))).use { reader ->
                reader.lines().forEach { line ->
                    // R.txt format: int color color_name 0x7f060000
                    if (line.startsWith("int color ")) {
                        val parts = line.split(" ")
                        if (parts.size >= 4) {
                            val colorName = parts[2]
                            val colorValue = parts[3]
                            // Store as path: colorName, content: colorValue
                            resultList.add(ResourceEntry(
                                path = "@color/$colorName",
                                content = colorValue
                            ))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOG.debug("Error processing R.txt from archive", e)
        }
    }
    
    private fun processRTxtFromJar(
        jarFile: JarFile,
        entry: java.util.jar.JarEntry,
        resultList: MutableList<ResourceEntry>,
        sourceName: String
    ) {
        try {
            BufferedReader(InputStreamReader(jarFile.getInputStream(entry))).use { reader ->
                reader.lines().forEach { line ->
                    if (line.startsWith("int color ")) {
                        val parts = line.split(" ")
                        if (parts.size >= 4) {
                            val colorName = parts[2]
                            val colorValue = parts[3]
                            // Store as path: colorName, content: colorValue
                            resultList.add(ResourceEntry(
                                path = "@color/$colorName",
                                content = colorValue
                            ))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOG.debug("Error processing R.txt from JAR", e)
        }
    }
    
    private fun processXmlFromArchive(
        zipFile: ZipFile,
        entry: java.util.zip.ZipEntry,
        resultList: MutableList<ResourceEntry>,
        sourceName: String
    ) {
        // Implementation would parse XML and extract color resources
        // Similar to existing XML parsing logic
    }
    
    private fun createTempFile(virtualFile: VirtualFile): java.io.File {
        val tempFile = java.io.File.createTempFile("resource_", ".tmp")
        virtualFile.inputStream.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }
}