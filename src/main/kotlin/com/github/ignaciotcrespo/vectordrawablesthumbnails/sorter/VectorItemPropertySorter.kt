package com.github.ignaciotcrespo.vectordrawablesthumbnails.sorter

import com.github.ignaciotcrespo.vectordrawablesthumbnails.VectorItem
import com.github.ignaciotcrespo.vectordrawablesthumbnails.interfaces.ItemSorter

class VectorItemPropertySorter : ItemSorter<VectorItem> {
    override fun sort(items: List<VectorItem>, sortProperty: String?, direction: String?): List<VectorItem> {
        val sortedItems = ArrayList(items) // Work on a mutable copy
        when (direction) {
            "Desc" -> {
                when (sortProperty) {
                    "By Name" -> sortedItems.sortByDescending { it.name }
                    "By Width" -> sortedItems.sortByDescending { it.viewportW }
                    "By Height" -> sortedItems.sortByDescending { it.viewportH }
                    "By Width x Height" -> sortedItems.sortByDescending { it.viewportW * it.viewportH }
                    "By File Size" -> sortedItems.sortByDescending { it.fileSize }
                }
            }
            else -> { // Default to Ascending
                when (sortProperty) {
                    "By Name" -> sortedItems.sortBy { it.name }
                    "By Width" -> sortedItems.sortBy { it.viewportW }
                    "By Height" -> sortedItems.sortBy { it.viewportH }
                    "By Width x Height" -> sortedItems.sortBy { it.viewportW * it.viewportH }
                    "By File Size" -> sortedItems.sortBy { it.fileSize }
                }
            }
        }
        return sortedItems
    }
}
