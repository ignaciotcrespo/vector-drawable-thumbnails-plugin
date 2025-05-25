package com.github.ignaciotcrespo.vectordrawablesthumbnails.interfaces

interface ItemFilter<T> {
    fun filter(items: List<T>, query: String?): List<T>
}
