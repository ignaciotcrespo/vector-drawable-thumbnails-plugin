package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure.search

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ProjectResourceSearchStrategy
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope

/**
 * Implementation for searching color resources in project source files.
 * Follows Single Responsibility Principle - only searches project files.
 */
class ProjectResourceSearchStrategyImpl : ProjectResourceSearchStrategy {
    
    companion object {
        private val LOG = Logger.getInstance(ProjectResourceSearchStrategyImpl::class.java)
    }
    
    override fun findResourceFiles(project: Project): Collection<VirtualFile> {
        val resourceFiles = mutableSetOf<VirtualFile>()
        
        try {
            // Create a combined scope that includes project and all library dependencies
            val projectScope = GlobalSearchScope.projectScope(project)
            val allLibrariesScope = GlobalSearchScope.allScope(project)
            val combinedScope = projectScope.union(allLibrariesScope)
            
            // Find all colors.xml files
            val colorFiles = FilenameIndex.getVirtualFilesByName(project, "colors.xml", combinedScope)
            resourceFiles.addAll(colorFiles)
            
            // Find other value resource files that might contain colors
            val xmlFiles = FilenameIndex.getAllFilesByExt(project, "xml", combinedScope)
                .filter { file -> 
                    val path = file.path
                    (path.contains("/values/") || path.contains("/values-")) &&
                    file.name != "colors.xml" // Already added above
                }
            
            resourceFiles.addAll(xmlFiles)
            
            LOG.info("Found ${resourceFiles.size} resource files in project")
        } catch (e: Exception) {
            LOG.error("Error searching for resource files in project", e)
        }
        
        return resourceFiles
    }
}