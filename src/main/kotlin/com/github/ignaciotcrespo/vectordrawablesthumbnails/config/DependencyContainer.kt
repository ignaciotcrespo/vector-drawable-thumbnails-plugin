package com.github.ignaciotcrespo.vectordrawablesthumbnails.config

import com.github.ignaciotcrespo.vectordrawablesthumbnails.application.VectorService
import com.github.ignaciotcrespo.vectordrawablesthumbnails.application.VectorSorterFactory
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.*
import com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure.*

/**
 * Dependency container that manages object creation and dependencies.
 * Follows the Dependency Inversion Principle by providing a centralized way to manage dependencies.
 * Follows the Single Responsibility Principle by focusing only on dependency management.
 * Follows the Open/Closed Principle - new formats require only adding new repository instances.
 */
class DependencyContainer {

    // Infrastructure layer - shared components
    private val vectorFileSearcher: VectorFileSearcher by lazy { DefaultVectorFileSearcher() }
    private val vectorParser: VectorParser by lazy { DefaultVectorParser() }
    private val vectorFilter: VectorFilter by lazy { DefaultVectorFilter() }
    private val vectorSorterFactory: VectorSorterFactory by lazy { DefaultVectorSorterFactory() }
    private val vectorAnalyticsService: VectorAnalyticsService by lazy { DefaultVectorAnalyticsService() }

    // Domain layer - repository for each file format
    val vectorDrawableRepository: VectorRepository by lazy {
        VectorDrawableRepository(vectorFileSearcher, vectorParser)
    }

    val svgRepository: VectorRepository by lazy {
        SvgRepository(vectorFileSearcher, vectorParser)
    }

    val rasterImageRepository: VectorRepository by lazy {
        RasterImageRepository(vectorFileSearcher, vectorParser)
    }

    // All available repositories
    private val allRepositories: List<VectorRepository> by lazy {
        listOf(vectorDrawableRepository, svgRepository, rasterImageRepository)
    }

    // Application layer
    val vectorService: VectorService by lazy {
        VectorService(allRepositories, vectorFilter, vectorSorterFactory)
    }

    val analyticsService: VectorAnalyticsService get() = vectorAnalyticsService
} 