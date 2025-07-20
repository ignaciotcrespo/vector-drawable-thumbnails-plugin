package com.github.ignaciotcrespo.vectordrawablesthumbnails.domain

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Interface for locating Android resource files across different build configurations.
 * Follows Single Responsibility Principle - only locates resource files.
 */
interface AndroidResourceLocator {
    /**
     * Finds all Android resource files in the project.
     * Includes source files, build outputs, and library resources.
     */
    fun findAllResourceFiles(project: Project): ResourceFileCollection
    
    /**
     * Gets supported build variants for the project.
     */
    fun getSupportedBuildVariants(project: Project): List<String>
}

/**
 * Collection of resource files organized by source.
 */
data class ResourceFileCollection(
    val sourceFiles: List<VirtualFile>,
    val buildOutputFiles: List<VirtualFile>,
    val libraryEntries: List<ResourceEntry>,
    val buildVariant: String? = null
)