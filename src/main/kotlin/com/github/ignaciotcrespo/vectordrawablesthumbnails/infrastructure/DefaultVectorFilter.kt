package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.FilterCriteria
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.VectorFilter
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem

/**
 * Enhanced implementation of VectorFilter.
 * Supports comprehensive filtering with multiple criteria.
 */
class DefaultVectorFilter : VectorFilter {
    
    override fun filter(items: List<VectorItem>, criteria: FilterCriteria): List<VectorItem> {
        return items.filter { item ->
            matchesTextFilter(item, criteria.text) &&
            matchesSizeFilter(item, criteria.sizeRange) &&
            matchesComplexityFilter(item, criteria.complexityRange) &&
            matchesFileSizeFilter(item, criteria.fileSizeRange) &&
            matchesTagsFilter(item, criteria.tags) &&
            matchesUsageFilter(item, criteria.usageStatus) &&
            matchesAnimationFilter(item, criteria.hasAnimations)
        }
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
    
    private fun matchesComplexityFilter(item: VectorItem, complexityRange: IntRange?): Boolean {
        if (complexityRange == null) return true
        
        val complexityScore = item.analytics?.complexityScore ?: 0
        return complexityScore in complexityRange
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
} 