package com.github.ignaciotcrespo.vectordrawablesthumbnails.interfaces

interface ItemSorter<T> {
    fun sort(items: List<T>, sortProperty: String?, direction: String?): List<T>
}
