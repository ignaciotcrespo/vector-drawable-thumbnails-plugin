package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ColorMatchMode
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.FilterCriteria
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.VectorFilter
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem

/**
 * Enhanced implementation of VectorFilter.
 * Supports comprehensive filtering with multiple criteria.
 */
class DefaultVectorFilter : VectorFilter {
    
    override fun filter(items: List<VectorItem>, criteria: FilterCriteria): List<VectorItem> {
        println("DefaultVectorFilter: Filtering ${items.size} vectors with criteria: $criteria")
        
        val filtered = items.filter { item ->
            val textMatch = matchesTextFilter(item, criteria.text)
            val sizeMatch = matchesSizeFilter(item, criteria.sizeRange)
            val complexityMatch = matchesComplexityFilter(item, criteria.complexityLevel)
            val fileSizeMatch = matchesFileSizeFilter(item, criteria.fileSizeRange)
            val tagsMatch = matchesTagsFilter(item, criteria.tags)
            val usageMatch = matchesUsageFilter(item, criteria.usageStatus)
            val animationMatch = matchesAnimationFilter(item, criteria.hasAnimations)
            val optimizationMatch = matchesOptimizationSuggestionsFilter(item, criteria.hasOptimizationSuggestions)
            val colorMatch = matchesColorFilter(item, criteria.colors, criteria.colorMatchMode)
            
            val matches = textMatch && sizeMatch && complexityMatch && fileSizeMatch && 
                         tagsMatch && usageMatch && animationMatch && optimizationMatch && colorMatch
            
            if (!matches && (criteria.complexityLevel != null || criteria.usageStatus != null)) {
                println("DefaultVectorFilter: ${item.name} filtered out - complexity: ${item.analytics?.complexityLevel} (want: ${criteria.complexityLevel}), usage: ${item.analytics?.usageStatus} (want: ${criteria.usageStatus})")
            }
            
            matches
        }
        
        println("DefaultVectorFilter: Filtered result: ${filtered.size} vectors")
        return filtered
    }
    
    private fun matchesTextFilter(item: VectorItem, text: String?): Boolean {
        if (text.isNullOrBlank()) return true
        
        val searchText = text.lowercase()
        return item.name.lowercase().contains(searchText) ||
               item.category?.lowercase()?.contains(searchText) == true ||
               item.description?.lowercase()?.contains(searchText) == true ||
               item.analytics?.tags?.any { it.lowercase().contains(searchText) } == true
    }
    
    private fun matchesSizeFilter(item: VectorItem, sizeRange: IntRange?): Boolean {
        if (sizeRange == null) return true
        
        val maxDimension = maxOf(item.viewportW, item.viewportH)
        return maxDimension in sizeRange
    }
    
    private fun matchesComplexityFilter(item: VectorItem, complexityLevel: com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ComplexityLevel?): Boolean {
        if (complexityLevel == null) return true
        
        return item.analytics?.complexityLevel == complexityLevel
    }
    
    private fun matchesFileSizeFilter(item: VectorItem, fileSizeRange: LongRange?): Boolean {
        if (fileSizeRange == null) return true
        
        return item.fileSize in fileSizeRange
    }
    
    private fun matchesTagsFilter(item: VectorItem, tags: List<String>): Boolean {
        if (tags.isEmpty()) return true
        
        val itemTags = item.analytics?.tags ?: emptyList()
        return tags.any { tag -> 
            itemTags.any { itemTag -> 
                itemTag.lowercase().contains(tag.lowercase()) 
            }
        }
    }
    
    private fun matchesUsageFilter(item: VectorItem, usageStatus: com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.UsageStatus?): Boolean {
        if (usageStatus == null) return true
        
        return item.analytics?.usageStatus == usageStatus
    }
    
    private fun matchesAnimationFilter(item: VectorItem, hasAnimations: Boolean?): Boolean {
        if (hasAnimations == null) return true
        
        return item.analytics?.hasAnimations == hasAnimations
    }
    
    private fun matchesOptimizationSuggestionsFilter(item: VectorItem, hasOptimizationSuggestions: Boolean?): Boolean {
        if (hasOptimizationSuggestions == null) return true
        
        return item.analytics?.hasOptimizationSuggestions == hasOptimizationSuggestions
    }
    
    private fun matchesColorFilter(item: VectorItem, colors: Set<String>, matchMode: ColorMatchMode): Boolean {
        if (colors.isEmpty()) return true
        
        val itemColors = item.analytics?.colors ?: emptySet()
        if (itemColors.isEmpty()) return false
        
        return when (matchMode) {
            ColorMatchMode.ANY -> colors.any { color -> itemColors.contains(color) }
            ColorMatchMode.ALL -> colors.all { color -> itemColors.contains(color) }
        }
    }
} 