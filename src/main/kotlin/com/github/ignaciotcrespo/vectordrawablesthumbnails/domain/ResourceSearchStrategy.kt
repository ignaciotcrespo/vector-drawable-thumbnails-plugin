package com.github.ignaciotcrespo.vectordrawablesthumbnails.domain

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Interface for different strategies to search for resource files.
 * Follows Strategy pattern and Single Responsibility Principle.
 */
interface ResourceSearchStrategy {
    /**
     * Searches for resource files containing color definitions.
     * 
     * @param project The project to search in
     * @return Collection of files that may contain color resources
     */
    fun findResourceFiles(project: Project): Collection<VirtualFile>
}

/**
 * Strategy for searching resources in project source files.
 */
interface ProjectResourceSearchStrategy : ResourceSearchStrategy

/**
 * Strategy for searching resources in Android build outputs.
 */
interface BuildOutputResourceSearchStrategy : ResourceSearchStrategy {
    /**
     * Searches for resource files in module build outputs.
     * 
     * @param module The module to search in
     * @return Collection of files that may contain color resources
     */
    fun findResourceFilesInModule(module: Module): Collection<VirtualFile>
}

/**
 * Strategy for searching resources in library dependencies.
 */
interface LibraryResourceSearchStrategy : ResourceSearchStrategy {
    /**
     * Searches for resource files in a library file (AAR/JAR).
     * 
     * @param libraryFile The library file to search in
     * @return Collection of resource entries found
     */
    fun findResourcesInLibrary(libraryFile: VirtualFile): Collection<ResourceEntry>
}

/**
 * Represents a resource entry found in a library.
 */
data class ResourceEntry(
    val path: String,
    val content: String
)