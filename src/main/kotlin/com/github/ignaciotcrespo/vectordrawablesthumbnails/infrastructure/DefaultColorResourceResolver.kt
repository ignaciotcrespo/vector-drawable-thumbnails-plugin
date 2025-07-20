package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ColorResourceResolver
import com.intellij.openapi.project.Project

/**
 * Default implementation of ColorResourceResolver.
 * Resolves color references from Android resource files.
 * 
 * This is now a facade that delegates to the refactored implementation
 * to maintain backward compatibility while improving the internal architecture.
 */
class DefaultColorResourceResolver : ColorResourceResolver {
    
    // Delegate to the refactored implementation
    private val delegate = RefactoredColorResourceResolver()
    
    override fun resolveColorReference(colorReference: String, project: Project): String? {
        return delegate.resolveColorReference(colorReference, project)
    }
    
    override fun buildColorCache(project: Project) {
        delegate.buildColorCache(project)
    }
    
    override fun clearCache() {
        delegate.clearCache()
    }
    
    override fun getAllColorResources(project: Project): Map<String, String> {
        return delegate.getAllColorResources(project)
    }
}