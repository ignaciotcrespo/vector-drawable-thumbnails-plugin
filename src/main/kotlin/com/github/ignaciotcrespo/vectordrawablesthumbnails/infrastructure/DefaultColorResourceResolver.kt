package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ColorResourceResolver
import com.intellij.openapi.project.Project

/**
 * Default implementation of ColorResourceResolver.
 * Resolves color references from Android resource files.
 * 
 * This is now a facade that delegates to the improved implementation
 * to maintain backward compatibility while addressing code review issues.
 */
class DefaultColorResourceResolver : ColorResourceResolver {
    
    // Delegate to the improved implementation that addresses all code review issues
    private val delegate = ImprovedColorResourceResolver()
    
    override fun resolveColorReference(colorReference: String, project: Project): String? {
        return try {
            delegate.resolveColorReference(colorReference, project) ?: run {
                // Fallback to black for better user experience
                LOG.debug("Color reference not found: $colorReference, using fallback")
                "#000000"
            }
        } catch (e: Exception) {
            LOG.error("Error resolving color reference: $colorReference", e)
            "#000000"
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
    
    companion object {
        private val LOG = com.intellij.openapi.diagnostic.Logger.getInstance(DefaultColorResourceResolver::class.java)
    }
}