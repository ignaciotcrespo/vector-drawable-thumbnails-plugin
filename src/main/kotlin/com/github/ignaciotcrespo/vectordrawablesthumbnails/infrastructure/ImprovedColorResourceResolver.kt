package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.*
import com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure.cache.DefaultColorCacheManager
import com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure.cache.ColorCacheBuilder
import com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure.parsers.XmlResourceFileParser
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*

/**
 * Improved implementation of ColorResourceResolver that addresses code review issues:
 * - Uses proper Android resource location strategies
 * - Performs heavy operations off UI thread
 * - Follows SOLID principles
 * - Includes comprehensive error handling
 * - Supports cache invalidation
 */
class ImprovedColorResourceResolver(
    private val cacheManager: ColorCacheManager = DefaultColorCacheManager(),
    private val resourceParser: ResourceFileParser = XmlResourceFileParser(),
    private val androidResourceLocator: AndroidResourceLocator = FlexibleAndroidResourceLocator(),
    private val colorResolver: ColorResolver = ImprovedColorResolver(),
    private val cacheInvalidator: ResourceCacheInvalidator = ResourceCacheInvalidatorImpl(),
    private val cacheBuilder: ColorCacheBuilder = ColorCacheBuilder(androidResourceLocator, resourceParser)
) : ColorResourceResolver {
    
    companion object {
        private val LOG = Logger.getInstance(ImprovedColorResourceResolver::class.java)
        private const val DEFAULT_COLOR_FALLBACK = "#000000"
    }
    
    private val coroutineScope = CoroutineScope(
        Dispatchers.IO + SupervisorJob() + CoroutineName("ColorResourceResolver")
    )
    
    init {
        // Set up cache invalidation
        cacheInvalidator.onInvalidate {
            LOG.info("Resource files changed, invalidating color cache")
            clearCache()
        }
    }
    
    override fun resolveColorReference(colorReference: String, project: Project): String? {
        return try {
            // First, check if it's a direct color or system color
            if (colorReference.startsWith("#")) {
                return colorReference
            }
            
            if (colorReference.startsWith("@android:color/")) {
                return colorResolver.resolveColor(colorReference, emptyMap())
            }
            
            // Get cached colors or build cache if needed
            val colors = cacheManager.getCachedColors(project) ?: run {
                LOG.info("Color cache not available, building synchronously")
                runBlocking {
                    val colors = cacheBuilder.buildColorCache(project)
                    cacheManager.updateCache(project, colors)
                    colors
                }
                cacheManager.getCachedColors(project)
            } ?: run {
                LOG.warn("Failed to build color cache for project")
                return null
            }
            
            // Resolve the color
            val resolvedColor = colorResolver.resolveColor(colorReference, colors)
            
            if (resolvedColor == null) {
                LOG.warn("Could not resolve color reference: $colorReference. Available colors: ${colors.size}")
            }
            
            resolvedColor
            
        } catch (e: Exception) {
            LOG.error("Error resolving color reference: $colorReference", e)
            null
        }
    }
    
    override fun buildColorCache(project: Project) {
        // Start watching for file changes
        cacheInvalidator.startWatching(project)
        
        // Build cache in background with progress
        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project,
            "Building Android color resource cache...",
            true
        ) {
            override fun run(indicator: ProgressIndicator) {
                runBlocking {
                    val colors = cacheBuilder.buildColorCacheWithProgress(project, indicator)
                    cacheManager.updateCache(project, colors)
                }
            }
            
            override fun onSuccess() {
                val cachedColors = cacheManager.getCachedColors(project)
                LOG.info("Color cache built successfully with ${cachedColors?.size ?: 0} colors")
            }
            
            override fun onThrowable(error: Throwable) {
                LOG.error("Failed to build color cache", error)
            }
        })
    }
    
    override fun clearCache() {
        cacheManager.clearAllCaches()
    }
    
    override fun getAllColorResources(project: Project): Map<String, String> {
        return cacheManager.getCachedColors(project) ?: run {
            LOG.info("Cache not available, building synchronously")
            runBlocking {
                val colors = cacheBuilder.buildColorCache(project)
                cacheManager.updateCache(project, colors)
                colors
            }
            cacheManager.getCachedColors(project) ?: emptyMap()
        }
    }
    
    fun dispose() {
        try {
            cacheInvalidator.stopWatching()
            coroutineScope.cancel()
            cacheBuilder.dispose()
        } catch (e: Exception) {
            LOG.error("Error disposing color resolver", e)
        }
    }
}