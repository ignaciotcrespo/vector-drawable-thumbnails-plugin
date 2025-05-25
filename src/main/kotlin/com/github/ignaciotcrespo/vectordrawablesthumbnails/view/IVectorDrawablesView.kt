package com.github.ignaciotcrespo.vectordrawablesthumbnails.view

import com.github.ignaciotcrespo.vectordrawablesthumbnails.VectorItem
import java.awt.event.MouseListener
import javax.swing.JPanel

interface IVectorDrawablesView {
    fun displayItems(items: List<VectorItem>, itemClickListener: (VectorItem) -> Unit)
    fun clearItemsPanel()
    fun getFilterText(): String
    fun setFilterText(text: String)
    fun getSelectedSortProperty(): String?
    fun getSelectedSortDirection(): String?
    fun showLoading(isLoading: Boolean, message: String) // Combines setRefreshButtonText and setFilterControlsEnabled for loading state
    fun setRefreshButtonText(text: String) // Specific for non-loading text
    fun setFilterControlsEnabled(enabled: Boolean) // Specific for enabling/disabling filter panel

    fun addRefreshButtonListener(action: () -> Unit)
    fun addDonateButtonListener(action: () -> Unit)
    fun addFilterTextChangeListener(action: (newText: String) -> Unit)
    fun addSortPropertyChangeListener(action: (selectedProperty: String) -> Unit)
    fun addSortDirectionChangeListener(action: (selectedDirection: String) -> Unit)
    fun addClearFilterButtonListener(action: () -> Unit)

    // Removed addVectorItemMouseListener, item click logic will be handled within displayItems
    // via the itemClickListener lambda for better decoupling from Swing's MouseListener.

    fun getRootPanel(): JPanel
    fun revalidateItemsPanel()
    fun repaintItemsPanel()
}
