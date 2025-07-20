package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure.cache

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ColorCacheManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap

/**
 * Default implementation of ColorCacheManager.
 * Follows Single Responsibility Principle - only manages cache.
 */
class DefaultColorCacheManager : ColorCacheManager {
    
    companion object {
        private val LOG = Logger.getInstance(DefaultColorCacheManager::class.java)
    }
    
    private val colorCache = ConcurrentHashMap<String, ConcurrentHashMap<String, String>>()
    private val cacheTimestamps = ConcurrentHashMap<String, Long>()
    private val invalidatedProjects = ConcurrentHashMap<String, Boolean>()
    
    init {
        // Listen for file changes to invalidate cache
        setupFileChangeListener()
    }
    
    override fun getCachedColors(project: Project): Map<String, String>? {
        if (isCacheInvalid(project)) {
            return null
        }
        return colorCache[project.name]?.toMap()
    }
    
    override fun updateCache(project: Project, colors: Map<String, String>) {
        val projectCache = ConcurrentHashMap<String, String>()
        projectCache.putAll(colors)
        colorCache[project.name] = projectCache
        cacheTimestamps[project.name] = System.currentTimeMillis()
        invalidatedProjects.remove(project.name)
        LOG.info("Updated color cache for project ${project.name} with ${colors.size} colors")
    }
    
    override fun clearCache(project: Project) {
        colorCache.remove(project.name)
        cacheTimestamps.remove(project.name)
        invalidatedProjects.remove(project.name)
        LOG.info("Cleared color cache for project ${project.name}")
    }
    
    override fun clearAllCaches() {
        colorCache.clear()
        cacheTimestamps.clear()
        invalidatedProjects.clear()
        LOG.info("Cleared all color caches")
    }
    
    override fun isCacheInvalid(project: Project): Boolean {
        return invalidatedProjects[project.name] == true || !colorCache.containsKey(project.name)
    }
    
    private fun setupFileChangeListener() {
        // File change listening would be set up here
        // For now, we'll rely on manual cache invalidation
        // as the VirtualFileListener API requires proper setup with message bus
    }
    
    private fun shouldInvalidateCache(filePath: String): Boolean {
        return filePath.contains("/values/") && 
               (filePath.endsWith(".xml") || filePath.endsWith("R.txt"))
    }
}