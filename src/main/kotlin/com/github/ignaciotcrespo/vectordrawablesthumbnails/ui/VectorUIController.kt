package com.github.ignaciotcrespo.vectordrawablesthumbnails.ui

import com.github.ignaciotcrespo.vectordrawablesthumbnails.VectorDrawablesView
import com.github.ignaciotcrespo.vectordrawablesthumbnails.application.VectorService
import com.github.ignaciotcrespo.vectordrawablesthumbnails.application.VectorServiceState
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.SortCriteria
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.SortDirection
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.VectorAnalyticsService
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import com.github.ignaciotcrespo.vectordrawablesthumbnails.utils.Utils
import com.intellij.openapi.project.Project
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.awt.BorderLayout
import java.awt.Desktop
import java.awt.GridLayout
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.net.URL
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * UI Controller that manages the interaction between the view and the service.
 * Follows the Single Responsibility Principle by focusing only on UI coordination.
 * Follows the Dependency Inversion Principle by depending on abstractions.
 */
class VectorUIController(
    private val view: VectorDrawablesView,
    private val vectorService: VectorService,
    private val analyticsService: VectorAnalyticsService,
    private val project: Project
) {
    
    private val disposables = CompositeDisposable()
    
    fun initialize() {
//        println("VectorUIController: Initializing...")
//        println("VectorUIController: btnRefresh = ${view.btnRefresh}")
//        println("VectorUIController: panelVectors = ${view.panelVectors}")
//        println("VectorUIController: textFilter = ${view.textFilter}")
        setupEventListeners()
        subscribeToServiceState()
        loadVectors()
//        println("VectorUIController: Initialization complete")
    }
    
    fun dispose() {
        disposables.clear()
    }
    
    private fun setupEventListeners() {
        setupDonateButton()
        setupRefreshButton()
        setupFilterField()
        setupClearButton()
        setupSortControls()
        setupAdvancedFilters()
        setupPresetButtons()
    }
    
    private fun setupDonateButton() {
        view.btnDonate.addActionListener {
            try {
                Desktop.getDesktop().browse(URL("https://paypal.me/itcrespo").toURI())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun setupRefreshButton() {
        view.btnRefresh.addActionListener {
            loadVectors()
        }
    }
    
    private fun setupFilterField() {
        view.textFilter.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = updateFilter()
            override fun removeUpdate(e: DocumentEvent?) = updateFilter()
            override fun changedUpdate(e: DocumentEvent) = updateFilter()
            
            private fun updateFilter() {
                vectorService.updateFilter(view.textFilter.text)
                updateVectorDisplay()
            }
        })
    }
    
    private fun setupClearButton() {
        view.clearButton.addActionListener {
            view.textFilter.text = ""
        }
    }
    
    private fun setupSortControls() {
        view.comboSort.addActionListener {
            val criteria = mapSortStringToCriteria(view.comboSort.selectedItem?.toString() ?: "")
            val direction = vectorService.getCurrentSortDirection()
            vectorService.updateSort(criteria, direction)
            updateVectorDisplay()
        }
        
        view.comboSortDirection.addActionListener {
            val direction = mapSortDirectionString(view.comboSortDirection.selectedItem?.toString() ?: "")
            val criteria = vectorService.getCurrentSortCriteria()
            vectorService.updateSort(criteria, direction)
            updateVectorDisplay()
        }
    }
    
    private fun setupAdvancedFilters() {
        // Complexity filter
        view.comboComplexityFilter?.addActionListener {
            updateAdvancedFilter()
        }
        
        // Usage filter
        view.comboUsageFilter?.addActionListener {
            updateAdvancedFilter()
        }
        
        // File size slider
        view.sliderFileSizeMax?.addChangeListener {
            updateAdvancedFilter()
        }
        
        // Tags filter
        view.textTagsFilter?.document?.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = updateAdvancedFilter()
            override fun removeUpdate(e: DocumentEvent?) = updateAdvancedFilter()
            override fun changedUpdate(e: DocumentEvent) = updateAdvancedFilter()
        })
        
        // Checkboxes
        view.checkShowAnimated?.addActionListener { updateAdvancedFilter() }
        view.checkShowOptimizable?.addActionListener { updateAdvancedFilter() }
        
        // Reset filters button
        view.btnResetFilters?.addActionListener {
            resetAllFilters()
        }
    }
    
    private fun setupPresetButtons() {
        view.btnPresetUnused?.addActionListener {
            applyPresetFilter("unused")
        }
        
        view.btnPresetComplex?.addActionListener {
            applyPresetFilter("complex")
        }
        
        view.btnPresetOptimizable?.addActionListener {
            applyPresetFilter("optimizable")
        }
    }
    
    private fun updateAdvancedFilter() {
        val criteria = buildFilterCriteria()
        vectorService.updateAdvancedFilter(criteria)
        updateVectorDisplay()
    }
    
    private fun buildFilterCriteria(): com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.FilterCriteria {
        val textFilter = view.textFilter.text?.takeIf { it.isNotBlank() }
        
        // Complexity filter
        val complexityLevel = when (view.comboComplexityFilter?.selectedItem?.toString()) {
            "Simple" -> com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ComplexityLevel.SIMPLE
            "Moderate" -> com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ComplexityLevel.MODERATE
            "Complex" -> com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ComplexityLevel.COMPLEX
            "Very Complex" -> com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ComplexityLevel.VERY_COMPLEX
            else -> null
        }
        
        // Usage filter
        val usageStatus = when (view.comboUsageFilter?.selectedItem?.toString()) {
            "Unused" -> com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.UsageStatus.UNUSED
            "Rarely Used" -> com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.UsageStatus.RARELY_USED
            "Used" -> com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.UsageStatus.USED
            "Frequently Used" -> com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.UsageStatus.FREQUENTLY_USED
            else -> null
        }
        
        // File size filter
        val maxFileSize = view.sliderFileSizeMax?.value?.let { it * 1024L } // Convert KB to bytes
        val fileSizeRange = if (maxFileSize != null && maxFileSize < 50 * 1024) {
            0L..maxFileSize
        } else null
        
        // Tags filter
        val tags = view.textTagsFilter?.text?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        
        // Animation filter
        val hasAnimations = if (view.checkShowAnimated?.isSelected == true) true else null
        
        // Complexity range for optimizable filter
        val complexityRange = if (view.checkShowOptimizable?.isSelected == true) {
            // Show vectors with complexity > 20 (likely to have optimization suggestions)
            20..100
        } else null
        
        return com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.FilterCriteria(
            text = textFilter,
            fileSizeRange = fileSizeRange,
            complexityRange = complexityRange,
            tags = tags,
            usageStatus = usageStatus,
            hasAnimations = hasAnimations
        )
    }
    
    private fun resetAllFilters() {
        // Reset UI components
        view.textFilter.text = ""
        view.textTagsFilter?.text = ""
        view.comboComplexityFilter?.selectedItem = "All"
        view.comboUsageFilter?.selectedItem = "All"
        view.sliderFileSizeMax?.value = 50
        view.checkShowAnimated?.isSelected = false
        view.checkShowOptimizable?.isSelected = false
        
        // Update filters
        vectorService.updateFilter(null)
        vectorService.updateAdvancedFilter(com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.FilterCriteria())
        updateVectorDisplay()
    }
    
    private fun applyPresetFilter(preset: String) {
        resetAllFilters()
        
        when (preset) {
            "unused" -> {
                view.comboUsageFilter?.selectedItem = "Unused"
            }
            "complex" -> {
                view.comboComplexityFilter?.selectedItem = "Complex"
                view.comboSort.selectedItem = "By Complexity"
                view.comboSortDirection.selectedItem = "Desc"
            }
            "optimizable" -> {
                view.checkShowOptimizable?.isSelected = true
                view.comboSort.selectedItem = "By Complexity"
                view.comboSortDirection.selectedItem = "Desc"
            }
        }
        
        updateAdvancedFilter()
        updateSortFromUI()
    }
    
    private fun updateSortFromUI() {
        val criteria = mapSortStringToCriteria(view.comboSort.selectedItem?.toString() ?: "")
        val direction = mapSortDirectionString(view.comboSortDirection.selectedItem?.toString() ?: "")
        vectorService.updateSort(criteria, direction)
        updateVectorDisplay()
    }
    
    private fun subscribeToServiceState() {
        val disposable = vectorService.stateObservable
            .subscribeOn(Schedulers.io())
            .subscribe { state ->
                SwingUtilities.invokeLater {
                    when (state) {
                        is VectorServiceState.Loading -> handleLoadingState()
                        is VectorServiceState.Loaded -> handleLoadedState()
                        is VectorServiceState.Error -> handleErrorState(state.throwable)
                    }
                }
            }
        disposables.add(disposable)
    }
    
    private fun handleLoadingState() {
        view.btnRefresh.text = "Searching, please wait..."
        view.panelFilter.enableAll(false)
    }
    
    private fun handleLoadedState() {
        view.btnRefresh.text = "Refresh"
        view.panelFilter.enableAll(true)
        updateVectorDisplay()
    }
    
    private fun handleErrorState(throwable: Throwable) {
        view.btnRefresh.text = "Refresh"
        view.panelFilter.enableAll(true)
        // Could show error dialog here
//        println("Error loading vectors: ${throwable.message}")
        throwable.printStackTrace()
    }
    
    private fun updateVectorDisplay() {
        val items = vectorService.getFilteredAndSortedVectors()
//        println("VectorUIController: Updating display with ${items.size} items")
        
        // Update result count
        view.labelResultCount?.text = "${items.size} vectors"
        
        displayVectors(items)
    }
    
    private fun displayVectors(items: List<VectorItem>) {
//        println("VectorUIController: Displaying ${items.size} vectors")
        view.panelVectors.removeAll()
        
        // Set up grid layout for better organization
        val columns = calculateOptimalColumns(items.size)
        view.panelVectors.layout = GridLayout(0, columns, 8, 8)
        
        items.forEach { item ->
            // Generate analytics if not present
            val itemWithAnalytics = if (item.analytics == null) {
//                println("VectorUIController: Generating analytics for ${item.name}")
                val analytics = analyticsService.analyzeVector(item)
                item.copy(analytics = analytics)
            } else {
                item
            }
            
            val vectorPanel = VectorItemPanel(itemWithAnalytics, project)
            view.panelVectors.add(vectorPanel)
        }
        
        view.panelVectors.revalidate()
        view.panelVectors.repaint()
//        println("VectorUIController: Display update complete")
    }
    
    private fun calculateOptimalColumns(itemCount: Int): Int {
        return when {
            itemCount <= 4 -> 2
            itemCount <= 9 -> 3
            itemCount <= 16 -> 4
            itemCount <= 25 -> 5
            else -> 6
        }
    }
    
    private fun mapSortStringToCriteria(sortString: String): SortCriteria {
        return when (sortString) {
            "By Name" -> SortCriteria.BY_NAME
            "By Width" -> SortCriteria.BY_WIDTH
            "By Height" -> SortCriteria.BY_HEIGHT
            "By Width x Height" -> SortCriteria.BY_AREA
            "By File Size" -> SortCriteria.BY_FILE_SIZE
            "By Complexity" -> SortCriteria.BY_COMPLEXITY
            "By Usage Count" -> SortCriteria.BY_USAGE_COUNT
            "By Tags" -> SortCriteria.BY_TAGS
            else -> SortCriteria.BY_NAME
        }
    }
    
    private fun mapSortDirectionString(directionString: String): SortDirection {
        return when (directionString) {
            "Desc" -> SortDirection.DESC
            else -> SortDirection.ASC
        }
    }
    
    private fun JPanel.enableAll(isEnabled: Boolean) {
        this.isEnabled = isEnabled
        for (component in this.components) {
            if (component is JPanel) {
                component.enableAll(isEnabled)
            }
            component.isEnabled = isEnabled
        }
    }
    
    private fun loadVectors() {
//        println("VectorUIController: Starting to load vectors...")
        val disposable = vectorService.loadVectors(project)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.computation())
            .subscribe(
                { vectorItem ->
                    // Vector item loaded successfully
//                    println("VectorUIController: Loaded vector: ${vectorItem.name}")
                },
                { error ->
//                    println("VectorUIController: Error loading vector: ${error.message}")
                    error.printStackTrace()
                },
                {
                    // Loading completed - generate analytics for all vectors
//                    println("VectorUIController: Vector loading completed, generating analytics...")
                    SwingUtilities.invokeLater {
                        generateAnalyticsForAllVectors()
                    }
                }
            )
        disposables.add(disposable)
    }
    
    private fun generateAnalyticsForAllVectors() {
        val vectors = vectorService.getFilteredAndSortedVectors()
//        println("VectorUIController: Generating analytics for ${vectors.size} vectors")
        
        // Generate usage analysis for all vectors
        val usageMap = analyticsService.analyzeUsage(project, vectors)
        
        // Update vectors with usage information
        vectors.forEach { vector ->
            if (vector.analytics != null) {
                val updatedAnalytics = vector.analytics.copy(
                    usageStatus = usageMap[vector] ?: vector.analytics.usageStatus,
                    usageCount = when (usageMap[vector]) {
                        com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.UsageStatus.FREQUENTLY_USED -> 10
                        com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.UsageStatus.USED -> 5
                        com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.UsageStatus.RARELY_USED -> 2
                        else -> 0
                    }
                )
                // Update the vector in the repository
                vectorService.updateVectorAnalytics(vector, updatedAnalytics)
            }
        }
        
//        println("VectorUIController: Analytics generation completed")
    }
} 