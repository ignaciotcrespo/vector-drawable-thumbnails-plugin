package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.*
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.*
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
        
        val xmlContent = vectorItem.validFile.file.readText()
        val document = parseXmlDocument(xmlContent)
        
        val pathCount = countPaths(document)
        val complexityScore = calculateComplexityScore(vectorItem)
        val complexityLevel = determineComplexityLevel(pathCount)
        val estimatedRenderTime = estimateRenderTime(vectorItem)
        val optimizationSuggestions = generateOptimizationSuggestions(vectorItem)
        val tags = extractTags(vectorItem)
        val hasAnimations = detectAnimations(document)
        val colorCount = countColors(document)
        
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
            colorCount = colorCount,
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
        
        // Cache the result
        usageCache[projectCacheKey] = usageMap
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
        
        var score = 0
        
        // Base score from path count
        val pathCount = countPaths(document)
        score += pathCount * 2
        
        // Additional complexity factors
        if (xmlContent.contains("gradient")) score += 10
        if (xmlContent.contains("clip-path")) score += 5
        if (xmlContent.contains("transform")) score += 3
        if (xmlContent.contains("animate")) score += 15
        
        // File size factor
        score += (vectorItem.fileSize / 1024).toInt() // 1 point per KB
        
        return minOf(score, 100) // Cap at 100
    }
    
    override fun estimateRenderTime(vectorItem: VectorItem): Long {
        val complexityScore = calculateComplexityScore(vectorItem)
        val baseTime = 100L // microseconds
        
        // Estimate based on complexity and size
        return baseTime + (complexityScore * 10) + (vectorItem.viewportW * vectorItem.viewportH / 1000)
    }
    
    override fun extractTags(vectorItem: VectorItem): List<String> {
        val tags = mutableListOf<String>()
        val fileName = vectorItem.name.lowercase()
        
        // Extract semantic meaning from filename
        when {
            fileName.contains("ic_") -> tags.add("icon")
            fileName.contains("btn_") -> tags.add("button")
            fileName.contains("bg_") -> tags.add("background")
        }
        
        // Common icon categories
        when {
            fileName.contains("home") -> tags.add("navigation")
            fileName.contains("menu") -> tags.add("navigation")
            fileName.contains("back") || fileName.contains("arrow") -> tags.add("navigation")
            fileName.contains("search") -> tags.add("action")
            fileName.contains("add") || fileName.contains("plus") -> tags.add("action")
            fileName.contains("delete") || fileName.contains("remove") -> tags.add("action")
            fileName.contains("edit") -> tags.add("action")
            fileName.contains("share") -> tags.add("social")
            fileName.contains("heart") || fileName.contains("like") -> tags.add("social")
            fileName.contains("star") || fileName.contains("favorite") -> tags.add("social")
        }
        
        // Size categories
        when {
            vectorItem.isSquare -> tags.add("square")
            vectorItem.aspectRatio > 1.5 -> tags.add("wide")
            vectorItem.aspectRatio < 0.67 -> tags.add("tall")
        }
        
        // Complexity tags
        if (vectorItem.fileSize > 5 * 1024) tags.add("complex")
        if (vectorItem.fileSize < 1024) tags.add("simple")
        
        return tags.distinct()
    }
    
    private fun parseXmlDocument(xmlContent: String): Document? {
        return try {
            val dbf = DocumentBuilderFactory.newInstance()
            val db = dbf.newDocumentBuilder()
            db.parse(InputSource(StringReader(xmlContent)))
        } catch (e: Exception) {
            null
        }
    }
    
    private fun countPaths(document: Document?): Int {
        return document?.getElementsByTagName("path")?.length ?: 0
    }
    
    private fun determineComplexityLevel(pathCount: Int): ComplexityLevel {
        return when {
            pathCount <= 5 -> ComplexityLevel.SIMPLE
            pathCount <= 15 -> ComplexityLevel.MODERATE
            pathCount <= 30 -> ComplexityLevel.COMPLEX
            else -> ComplexityLevel.VERY_COMPLEX
        }
    }
    
    private fun detectAnimations(document: Document?): Boolean {
        return document?.let { doc ->
            doc.getElementsByTagName("animate").length > 0 ||
            doc.getElementsByTagName("animateTransform").length > 0 ||
            doc.getElementsByTagName("animateColor").length > 0
        } ?: false
    }
    
    private fun countColors(document: Document?): Int {
        val colors = mutableSetOf<String>()
        document?.let { doc ->
            val elements = doc.getElementsByTagName("*")
            for (i in 0 until elements.length) {
                val element = elements.item(i)
                val fillColor = element.attributes?.getNamedItem("android:fillColor")?.nodeValue
                val strokeColor = element.attributes?.getNamedItem("android:strokeColor")?.nodeValue
                
                fillColor?.let { colors.add(it) }
                strokeColor?.let { colors.add(it) }
            }
        }
        return maxOf(colors.size, 1)
    }
    
    private fun findUsageInProject(project: Project, vector: VectorItem): Int {
        try {
            val vectorName = vector.name.removeSuffix(".xml")
            val scope = GlobalSearchScope.projectScope(project)
            
            // Use more efficient search approach
            var usageCount = 0
            
            // Search using IntelliJ's built-in search capabilities
            val searchPattern = "@drawable/$vectorName"
            val alternatePattern = "drawable/$vectorName"
            
            // Get layout files more efficiently
            val layoutFiles = FilenameIndex.getAllFilesByExt(project, "xml", scope)
                .filter { file -> 
                    // Filter to only layout-related directories to reduce search scope
                    val path = file.path
                    path.contains("/layout/") || path.contains("/layout-") || 
                    path.contains("/menu/") || path.contains("/drawable/")
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
            
            return usageCount
        } catch (e: Exception) {
            return 0
        }
    }
    
    private fun generateCacheKey(vectorItem: VectorItem): String {
        return "${vectorItem.validFile.file.path}:${vectorItem.validFile.file.lastModified()}:${vectorItem.fileSize}"
    }
    
    fun clearCache() {
        analyticsCache.clear()
        usageCache.clear()
    }
} 