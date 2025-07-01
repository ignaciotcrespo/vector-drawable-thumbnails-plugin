package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.*
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.StringReader
import java.util.concurrent.ConcurrentHashMap
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Default implementation of VectorAnalyticsService.
 * Provides comprehensive analysis of vector drawables with caching for performance.
 */
class DefaultVectorAnalyticsService : VectorAnalyticsService {
    
    // Cache for analytics to avoid recomputation
    private val analyticsCache = ConcurrentHashMap<String, VectorAnalytics>()
    private val usageCache = ConcurrentHashMap<String, Map<VectorItem, UsageStatus>>()
    
    override fun analyzeVector(vectorItem: VectorItem): VectorAnalytics {
        val cacheKey = generateCacheKey(vectorItem)
        
        // Check cache first
        analyticsCache[cacheKey]?.let { return it }
        
        // Read XML content once and reuse
        val xmlContent = vectorItem.validFile.file.readText()
        
        // Parse document once for efficiency
        val document = parseXmlDocument(xmlContent)
        
        // Calculate all metrics efficiently
        val pathCount = countPaths(document)
        val complexityScore = calculateComplexityScoreOptimized(vectorItem, xmlContent, document)
        val complexityLevel = determineComplexityLevel(pathCount)
        val estimatedRenderTime = estimateRenderTimeOptimized(complexityScore, vectorItem)
        val optimizationSuggestions = generateOptimizationSuggestionsOptimized(vectorItem, xmlContent)
        val tags = extractTagsOptimized(vectorItem)
        val hasAnimations = detectAnimations(document)
        val colors = extractColors(document)
        
        val analytics = VectorAnalytics(
            complexityScore = complexityScore,
            complexityLevel = complexityLevel,
            pathCount = pathCount,
            estimatedRenderTime = estimatedRenderTime,
            optimizationSuggestions = optimizationSuggestions,
            usageCount = 0, // Will be updated by usage analysis
            usageStatus = UsageStatus.UNUSED, // Will be updated by usage analysis
            tags = tags,
            hasAnimations = hasAnimations,
            colorCount = colors.size,
            colors = colors,
            aspectRatio = vectorItem.aspectRatio
        )
        
        // Cache the result
        analyticsCache[cacheKey] = analytics
        return analytics
    }
    
    override fun analyzeUsage(project: Project, vectors: List<VectorItem>): Map<VectorItem, UsageStatus> {
        val projectCacheKey = "${project.name}:${vectors.size}:${vectors.hashCode()}"
        
        // Check cache first
        usageCache[projectCacheKey]?.let { return it }
        
        val usageMap = mutableMapOf<VectorItem, UsageStatus>()
        
        // For small batches, process more efficiently
        if (vectors.size <= 10) {
            // Process small batches with optimized search
            vectors.forEachIndexed { index, vector ->
                val usageCount = findUsageInProjectOptimized(project, vector)
                val status = when {
                    usageCount == 0 -> UsageStatus.UNUSED
                    usageCount >= 10 -> UsageStatus.FREQUENTLY_USED
                    usageCount >= 3 -> UsageStatus.USED
                    else -> UsageStatus.RARELY_USED
                }
                usageMap[vector] = status
                
                // Yield every few vectors to prevent blocking
                if (index % 3 == 0) {
                    Thread.yield()
                }
            }
        } else {
            // For larger batches, use the original method
            vectors.forEach { vector ->
                val usageCount = findUsageInProject(project, vector)
                val status = when {
                    usageCount == 0 -> UsageStatus.UNUSED
                    usageCount >= 10 -> UsageStatus.FREQUENTLY_USED
                    usageCount >= 3 -> UsageStatus.USED
                    else -> UsageStatus.RARELY_USED
                }
                usageMap[vector] = status
            }
        }
        
        // Only cache larger results to avoid memory bloat
        if (vectors.size >= 50) {
            usageCache[projectCacheKey] = usageMap
        }
        
        return usageMap
    }
    
    override fun generateOptimizationSuggestions(vectorItem: VectorItem): List<OptimizationSuggestion> {
        val suggestions = mutableListOf<OptimizationSuggestion>()
        
        // File size suggestions
        if (vectorItem.fileSize > 5 * 1024) { // > 5KB
            suggestions.add(
                OptimizationSuggestion(
                    type = OptimizationType.REDUCE_PRECISION,
                    description = "Reduce decimal precision in path data",
                    potentialSavings = "10-20% file size reduction",
                    priority = Priority.MEDIUM
                )
            )
        }
        
        if (vectorItem.fileSize > 10 * 1024) { // > 10KB
            suggestions.add(
                OptimizationSuggestion(
                    type = OptimizationType.SIMPLIFY_CURVES,
                    description = "Simplify complex curves and paths",
                    potentialSavings = "15-30% file size reduction",
                    priority = Priority.HIGH
                )
            )
        }
        
        // Complexity suggestions
        val xmlContent = vectorItem.validFile.file.readText()
        if (xmlContent.contains("transform=")) {
            suggestions.add(
                OptimizationSuggestion(
                    type = OptimizationType.REMOVE_REDUNDANT_GROUPS,
                    description = "Remove unnecessary group transformations",
                    potentialSavings = "5-15% file size reduction",
                    priority = Priority.LOW
                )
            )
        }
        
        return suggestions
    }
    
    override fun calculateComplexityScore(vectorItem: VectorItem): Int {
        val xmlContent = vectorItem.validFile.file.readText()
        val document = parseXmlDocument(xmlContent)
        return calculateComplexityScoreOptimized(vectorItem, xmlContent, document)
    }
    
    override fun estimateRenderTime(vectorItem: VectorItem): Long {
        val complexityScore = calculateComplexityScore(vectorItem)
        return estimateRenderTimeOptimized(complexityScore, vectorItem)
    }
    
    override fun extractTags(vectorItem: VectorItem): List<String> {
        return extractTagsOptimized(vectorItem)
    }
    
    private fun parseXmlDocument(xmlContent: String): Document? {
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val inputSource = InputSource(StringReader(xmlContent))
            builder.parse(inputSource)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun countPaths(document: Document?): Int {
        return try {
            document?.getElementsByTagName("path")?.length ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    private fun determineComplexityLevel(pathCount: Int): ComplexityLevel {
        return when {
            pathCount <= 2 -> ComplexityLevel.SIMPLE
            pathCount <= 5 -> ComplexityLevel.MODERATE
            pathCount <= 10 -> ComplexityLevel.COMPLEX
            else -> ComplexityLevel.VERY_COMPLEX
        }
    }
    
    private fun detectAnimations(document: Document?): Boolean {
        return try {
            val animatedVectorTags = document?.getElementsByTagName("animated-vector")?.length ?: 0
            val animationTags = document?.getElementsByTagName("animation")?.length ?: 0
            val objectAnimatorTags = document?.getElementsByTagName("objectAnimator")?.length ?: 0
            
            animatedVectorTags > 0 || animationTags > 0 || objectAnimatorTags > 0
        } catch (e: Exception) {
            false
        }
    }
    
    private fun extractColors(document: Document?): Set<String> {
        return try {
            val colorSet = mutableSetOf<String>()
            
            // Extract fill colors
            val pathElements = document?.getElementsByTagName("path")
            if (pathElements != null) {
                for (i in 0 until pathElements.length) {
                    val element = pathElements.item(i)
                    val fillColor = element.attributes?.getNamedItem("android:fillColor")?.nodeValue
                    if (fillColor != null && fillColor.startsWith("#")) {
                        colorSet.add(fillColor.uppercase())
                    }
                }
            }
            
            // Extract stroke colors
            if (pathElements != null) {
                for (i in 0 until pathElements.length) {
                    val element = pathElements.item(i)
                    val strokeColor = element.attributes?.getNamedItem("android:strokeColor")?.nodeValue
                    if (strokeColor != null && strokeColor.startsWith("#")) {
                        colorSet.add(strokeColor.uppercase())
                    }
                }
            }
            
            if (colorSet.isEmpty()) {
                setOf("#000000") // Default black if no colors found
            } else {
                colorSet
            }
        } catch (e: Exception) {
            setOf("#000000")
        }
    }
    
    private fun findUsageInProject(project: Project, vector: VectorItem): Int {
        return try {
            val vectorName = vector.name.removeSuffix(".xml")
            val scope = GlobalSearchScope.projectScope(project)
            
            // Use more efficient search approach with proper read access
            var usageCount = 0
            
            // Search using IntelliJ's built-in search capabilities
            val searchPattern = "@drawable/$vectorName"
            val alternatePattern = "drawable/$vectorName"
            
            // Get layout files more efficiently with read access
            val layoutFiles = ApplicationManager.getApplication().runReadAction<List<com.intellij.openapi.vfs.VirtualFile>> {
                FilenameIndex.getAllFilesByExt(project, "xml", scope)
                    .filter { file -> 
                        // Filter to only layout-related directories to reduce search scope
                        val path = file.path
                        path.contains("/layout/") || path.contains("/layout-") || 
                        path.contains("/menu/") || path.contains("/drawable/")
                    }
            }
            
            // Batch process files to reduce I/O overhead
            layoutFiles.chunked(50).forEach { batch ->
                batch.forEach { file ->
                    try {
                        // Use more efficient content reading
                        val content = String(file.contentsToByteArray())
                        if (content.contains(searchPattern) || content.contains(alternatePattern)) {
                            usageCount++
                        }
                    } catch (e: Exception) {
                        // Ignore files that can't be read
                    }
                }
                
                // Allow other threads to work
                Thread.yield()
            }
            
            usageCount
        } catch (e: Exception) {
            println("Error finding usage for ${vector.name}: ${e.message}")
            0
        }
    }
    
    private fun findUsageInProjectOptimized(project: Project, vector: VectorItem): Int {
        return try {
            val vectorName = vector.name.removeSuffix(".xml")
            val scope = GlobalSearchScope.projectScope(project)
            
            var usageCount = 0
            
            // Search patterns
            val searchPattern = "@drawable/$vectorName"
            val alternatePattern = "drawable/$vectorName"
            
            // Get layout files with smaller batch size for responsiveness - FIXED WITH READ ACCESS
            val layoutFiles = ApplicationManager.getApplication().runReadAction<List<com.intellij.openapi.vfs.VirtualFile>> {
                FilenameIndex.getAllFilesByExt(project, "xml", scope)
                    .filter { file -> 
                        val path = file.path
                        path.contains("/layout/") || path.contains("/layout-") || 
                        path.contains("/menu/") || path.contains("/drawable/")
                    }
            }
            
            // Process in smaller batches with more frequent yielding
            layoutFiles.chunked(20).forEach { batch ->
                batch.forEach { file ->
                    try {
                        val content = String(file.contentsToByteArray())
                        if (content.contains(searchPattern) || content.contains(alternatePattern)) {
                            usageCount++
                        }
                    } catch (e: Exception) {
                        // Ignore files that can't be read
                    }
                }
                
                // More frequent yielding for better responsiveness
                Thread.yield()
                Thread.sleep(5) // Small delay to prevent overwhelming
            }
            
            usageCount
        } catch (e: Exception) {
            println("Error finding usage for ${vector.name}: ${e.message}")
            0
        }
    }
    
    private fun generateCacheKey(vectorItem: VectorItem): String {
        return "${vectorItem.validFile.file.path}:${vectorItem.validFile.file.lastModified()}:${vectorItem.fileSize}"
    }
    
    fun clearCache() {
        analyticsCache.clear()
        usageCache.clear()
    }
    
    private fun calculateComplexityScoreOptimized(vectorItem: VectorItem, xmlContent: String, document: Document?): Int {
        var score = 0
        
        // Base score from path count (already calculated)
        val pathCount = countPaths(document)
        score += pathCount * 2
        
        // Additional complexity factors using pre-loaded content
        if (xmlContent.contains("gradient")) score += 10
        if (xmlContent.contains("clip-path")) score += 5
        if (xmlContent.contains("transform")) score += 3
        if (xmlContent.contains("animate")) score += 15
        
        // File size factor
        score += (vectorItem.fileSize / 1024).toInt() // 1 point per KB
        
        return minOf(score, 100) // Cap at 100
    }
    
    private fun estimateRenderTimeOptimized(complexityScore: Int, vectorItem: VectorItem): Long {
        val baseTime = 100L // microseconds
        
        // Estimate based on complexity and size (avoid recalculating complexity)
        return baseTime + (complexityScore * 10) + (vectorItem.viewportW * vectorItem.viewportH / 1000)
    }
    
    private fun generateOptimizationSuggestionsOptimized(vectorItem: VectorItem, xmlContent: String): List<OptimizationSuggestion> {
        val suggestions = mutableListOf<OptimizationSuggestion>()
        
        // File size suggestions
        if (vectorItem.fileSize > 5 * 1024) { // > 5KB
            suggestions.add(
                OptimizationSuggestion(
                    type = OptimizationType.REDUCE_PRECISION,
                    description = "Reduce decimal precision in path data",
                    potentialSavings = "10-20% file size reduction",
                    priority = Priority.MEDIUM
                )
            )
        }
        
        if (vectorItem.fileSize > 10 * 1024) { // > 10KB
            suggestions.add(
                OptimizationSuggestion(
                    type = OptimizationType.SIMPLIFY_CURVES,
                    description = "Simplify complex curves and paths",
                    potentialSavings = "15-30% file size reduction",
                    priority = Priority.HIGH
                )
            )
        }
        
        // Complexity suggestions using pre-loaded content
        if (xmlContent.contains("transform=")) {
            suggestions.add(
                OptimizationSuggestion(
                    type = OptimizationType.REMOVE_REDUNDANT_GROUPS,
                    description = "Remove unnecessary group transformations",
                    potentialSavings = "5-15% file size reduction",
                    priority = Priority.LOW
                )
            )
        }
        
        return suggestions
    }
    
    private fun extractTagsOptimized(vectorItem: VectorItem): List<String> {
        val tags = mutableListOf<String>()
        val fileName = vectorItem.name.lowercase()
        
        // Extract semantic meaning from filename (optimized with when expressions)
        when {
            fileName.contains("ic_") -> tags.add("icon")
            fileName.contains("btn_") -> tags.add("button")
            fileName.contains("bg_") -> tags.add("background")
        }
        
        // Common icon categories (combined checks for efficiency)
        when {
            fileName.contains("home") || fileName.contains("menu") || 
            fileName.contains("back") || fileName.contains("arrow") -> tags.add("navigation")
            fileName.contains("search") || fileName.contains("add") || 
            fileName.contains("plus") || fileName.contains("delete") || 
            fileName.contains("remove") || fileName.contains("edit") -> tags.add("action")
            fileName.contains("share") || fileName.contains("heart") || 
            fileName.contains("like") || fileName.contains("star") || 
            fileName.contains("favorite") -> tags.add("social")
        }
        
        // Size categories
        when {
            vectorItem.isSquare -> tags.add("square")
            vectorItem.aspectRatio > 1.5 -> tags.add("wide")
            vectorItem.aspectRatio < 0.67 -> tags.add("tall")
        }
        
        // Complexity tags
        when {
            vectorItem.fileSize > 5 * 1024 -> tags.add("complex")
            vectorItem.fileSize < 1024 -> tags.add("simple")
        }
        
        return tags.distinct()
    }
} 