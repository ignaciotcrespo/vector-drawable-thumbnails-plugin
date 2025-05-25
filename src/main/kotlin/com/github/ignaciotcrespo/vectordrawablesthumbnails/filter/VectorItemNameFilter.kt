package com.github.ignaciotcrespo.vectordrawablesthumbnails.filter

import com.github.ignaciotcrespo.vectordrawablesthumbnails.VectorItem
import com.github.ignaciotcrespo.vectordrawablesthumbnails.interfaces.ItemFilter

class VectorItemNameFilter : ItemFilter<VectorItem> {
    override fun filter(items: List<VectorItem>, query: String?): List<VectorItem> {
        if (query.isNullOrEmpty()) {
            return items
        }
        val lowerCaseQuery = query.toLowerCase()
        return items.filter { it.name.toLowerCase().contains(lowerCaseQuery) }
    }
}
