package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure.search

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.BuildOutputResourceSearchStrategy
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Implementation for searching color resources in Android build outputs.
 * Follows Single Responsibility Principle - only searches build outputs.
 */
class BuildOutputResourceSearchStrategyImpl : BuildOutputResourceSearchStrategy {
    
    companion object {
        private val LOG = Logger.getInstance(BuildOutputResourceSearchStrategyImpl::class.java)
        
        // Common Android build output directories for different AGP versions
        private val BUILD_OUTPUT_PATHS = listOf(
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
            "build/intermediates/merged-not-compiled-resources/release/values",
            // Merged values files
            "build/intermediates/incremental/debug/mergeDebugResources/merged.dir/values/values.xml",
            "build/intermediates/incremental/release/mergeReleaseResources/merged.dir/values/values.xml",
            "build/intermediates/res/merged/debug/values/values.xml",
            "build/intermediates/res/merged/release/values/values.xml"
        )
    }
    
    override fun findResourceFiles(project: Project): Collection<VirtualFile> {
        val resourceFiles = mutableSetOf<VirtualFile>()
        
        try {
            ModuleManager.getInstance(project).modules.forEach { module ->
                resourceFiles.addAll(findResourceFilesInModule(module))
            }
            
            LOG.info("Found ${resourceFiles.size} resource files in build outputs")
        } catch (e: Exception) {
            LOG.error("Error searching for resource files in build outputs", e)
        }
        
        return resourceFiles
    }
    
    override fun findResourceFilesInModule(module: Module): Collection<VirtualFile> {
        val resourceFiles = mutableSetOf<VirtualFile>()
        
        try {
            val moduleFile = module.moduleFile
            if (moduleFile != null) {
                val moduleDir = moduleFile.parent
                
                BUILD_OUTPUT_PATHS.forEach { path ->
                    val file = moduleDir.findFileByRelativePath(path)
                    when {
                        file != null && file.exists() && (file.name == "R.txt" || file.extension == "xml") -> {
                            resourceFiles.add(file)
                        }
                        file != null && file.exists() && file.isDirectory -> {
                            // Look for XML files in the directory
                            file.children.filter { it.extension == "xml" }.forEach { xmlFile ->
                                resourceFiles.add(xmlFile)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOG.warn("Error searching build outputs for module: ${module.name}", e)
        }
        
        return resourceFiles
    }
}