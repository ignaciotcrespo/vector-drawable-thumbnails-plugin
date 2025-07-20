package com.github.ignaciotcrespo.vectordrawablesthumbnails.config

import com.github.ignaciotcrespo.vectordrawablesthumbnails.application.VectorService
import com.github.ignaciotcrespo.vectordrawablesthumbnails.application.VectorSorterFactory
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.*
import com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure.*

/**
 * Dependency container that manages object creation and dependencies.
 * Follows the Dependency Inversion Principle by providing a centralized way to manage dependencies.
 * Follows the Single Responsibility Principle by focusing only on dependency management.
 */
class DependencyContainer {
    
    // Infrastructure layer
    private val colorResourceResolver: ColorResourceResolver by lazy { DefaultColorResourceResolver() }
    private val vectorFileSearcher: VectorFileSearcher by lazy { DefaultVectorFileSearcher() }
    private val vectorParser: VectorParser by lazy { DefaultVectorParser(colorResourceResolver) }
    private val vectorFilter: VectorFilter by lazy { DefaultVectorFilter() }
    private val vectorSorterFactory: VectorSorterFactory by lazy { DefaultVectorSorterFactory() }
    private val vectorAnalyticsService: VectorAnalyticsService by lazy { DefaultVectorAnalyticsService() }
    
    // Domain layer
    private val vectorRepository: VectorRepository by lazy { 
        DefaultVectorRepository(vectorFileSearcher, vectorParser) 
    }
    
    // Application layer
    val vectorService: VectorService by lazy { 
        VectorService(vectorRepository, vectorFilter, vectorSorterFactory) 
    }
    
    val analyticsService: VectorAnalyticsService get() = vectorAnalyticsService
    
    val colorResolver: ColorResourceResolver get() = colorResourceResolver
} 