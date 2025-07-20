package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ColorResourceResolver
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer

/**
 * Default implementation of ColorResourceResolver.
 * Resolves color references from Android resource files.
 * 
 * This implementation now uses the UnifiedColorResourceResolver which:
 * - Integrates with Android Studio's resource APIs when available
 * - Falls back to custom implementation for other IDEs
 * - Addresses all code review feedback
 */
class DefaultColorResourceResolver : ColorResourceResolver, Disposable {
    
    // Use the unified implementation that supports both Android Studio and custom strategies
    private val delegate = UnifiedColorResourceResolver()
    
    override fun resolveColorReference(colorReference: String, project: Project): String? {
        return try {
            delegate.resolveColorReference(colorReference, project)
        } catch (e: Exception) {
            LOG.error("Error resolving color reference: $colorReference", e)
            null
        }
    }
    
    override fun buildColorCache(project: Project) {
        try {
            delegate.buildColorCache(project)
        } catch (e: Exception) {
            LOG.error("Error building color cache", e)
        }
    }
    
    override fun clearCache() {
        try {
            delegate.clearCache()
        } catch (e: Exception) {
            LOG.error("Error clearing cache", e)
        }
    }
    
    override fun getAllColorResources(project: Project): Map<String, String> {
        return try {
            delegate.getAllColorResources(project)
        } catch (e: Exception) {
            LOG.error("Error getting all color resources", e)
            emptyMap()
        }
    }
    
    override fun dispose() {
        try {
            Disposer.dispose(delegate)
        } catch (e: Exception) {
            LOG.error("Error disposing color resolver", e)
        }
    }
    
    companion object {
        private val LOG = com.intellij.openapi.diagnostic.Logger.getInstance(DefaultColorResourceResolver::class.java)
    }
}