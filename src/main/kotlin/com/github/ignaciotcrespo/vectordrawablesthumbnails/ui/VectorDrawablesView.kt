package com.github.ignaciotcrespo.vectordrawablesthumbnails.ui

import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import javax.swing.JPanel

interface VectorDrawablesView {
    fun displayItems(items: List<VectorItem>)
    fun showLoading(isLoading: Boolean)
    fun getFilterText(): String
    fun setFilterText(text: String)
    fun getSelectedSortCriteria(): String?
    fun getSelectedSortDirection(): String?
    fun addRefreshListener(listener: () -> Unit)
    fun addFilterChangeListener(listener: (filterText: String) -> Unit)
    fun addSortCriteriaListener(listener: (criteria: String) -> Unit)
    fun addSortDirectionListener(listener: (direction: String) -> Unit)
    fun addClearFilterListener(listener: () -> Unit)
    fun addDonateListener(listener: () -> Unit)
    fun addVectorClickedListener(listener: (item: VectorItem) -> Unit)
    fun getContentPanel(): JPanel
    fun enableFilterControls(enable: Boolean)
    fun setRefreshButtonText(text: String)
}
