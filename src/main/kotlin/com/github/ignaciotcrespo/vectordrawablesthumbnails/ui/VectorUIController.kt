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
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import com.github.ignaciotcrespo.vectordrawablesthumbnails.utils.PerformanceMonitor

/**
 * UI Controller that manages the interaction between the view and the service.
 * Follows the Single Responsibility Principle by focusing only on UI coordination.
 * Follows the Dependency Inversion Principle by depending on abstractions.
 * Enhanced with debouncing for smooth UI interactions.
 */
class VectorUIController(
    private val view: VectorDrawablesView,
    private val vectorService: VectorService,
    private val analyticsService: VectorAnalyticsService,
    private val project: Project
) {
    
    private val disposables = CompositeDisposable()
    
    // Debouncing for smooth UI interactions
    private val debounceExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private var filterDebounceTask: ScheduledFuture<*>? = null
    private var sliderDebounceTask: ScheduledFuture<*>? = null
    
    // Debounce delays in milliseconds
    private val FILTER_DEBOUNCE_DELAY = 300L
    private val SLIDER_DEBOUNCE_DELAY = 150L
    
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
        debounceExecutor.shutdown()
        try {
            if (!debounceExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                debounceExecutor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            debounceExecutor.shutdownNow()
        }
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
            override fun insertUpdate(e: DocumentEvent?) = debouncedUpdateFilter()
            override fun removeUpdate(e: DocumentEvent?) = debouncedUpdateFilter()
            override fun changedUpdate(e: DocumentEvent) = debouncedUpdateFilter()
            
            private fun debouncedUpdateFilter() {
                // Cancel previous task
                filterDebounceTask?.cancel(false)
                
                // Schedule new task
                filterDebounceTask = debounceExecutor.schedule({
                    SwingUtilities.invokeLater {
                        vectorService.updateFilter(view.textFilter.text)
                        updateVectorDisplay()
                    }
                }, FILTER_DEBOUNCE_DELAY, TimeUnit.MILLISECONDS)
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
        // Complexity filter - immediate update for combo boxes
        view.comboComplexityFilter?.addActionListener {
            updateAdvancedFilter()
        }
        
        // Usage filter - immediate update for combo boxes
        view.comboUsageFilter?.addActionListener {
            updateAdvancedFilter()
        }
        
        // File size slider - debounced for smooth dragging
        view.sliderFileSizeMax?.addChangeListener { e ->
            val slider = e.source as JSlider
            
            // Update the label immediately for visual feedback
            updateSliderLabel(slider.value)
            
            // Only trigger filtering when user stops dragging or on final value
            if (!slider.valueIsAdjusting) {
                // Immediate update when user releases slider
                updateAdvancedFilter()
            } else {
                // Debounced update while dragging for smooth experience
                debouncedSliderUpdate()
            }
        }
        
        // Tags filter - debounced for smooth typing
        view.textTagsFilter?.document?.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = debouncedUpdateAdvancedFilter()
            override fun removeUpdate(e: DocumentEvent?) = debouncedUpdateAdvancedFilter()
            override fun changedUpdate(e: DocumentEvent) = debouncedUpdateAdvancedFilter()
        })
        
        // Checkboxes - immediate update
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
        PerformanceMonitor.measure("Advanced Filter Update") {
            val criteria = buildFilterCriteria()
            println("VectorUIController: Applying advanced filter - complexityLevel: ${criteria.complexityLevel}, usageStatus: ${criteria.usageStatus}, hasOptimizationSuggestions: ${criteria.hasOptimizationSuggestions}")
            vectorService.updateAdvancedFilter(criteria)
            updateVectorDisplay()
        }
    }
    
    private fun buildFilterCriteria(): com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.FilterCriteria {
        val textFilter = view.textFilter.text?.takeIf { it.isNotBlank() }
        
        // Complexity filter
        val complexitySelection = view.comboComplexityFilter?.selectedItem?.toString()
        println("VectorUIController: Complexity selection: '$complexitySelection'")
        val complexityLevel = when (complexitySelection) {
            "Simple" -> com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ComplexityLevel.SIMPLE
            "Moderate" -> com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ComplexityLevel.MODERATE
            "Complex" -> com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ComplexityLevel.COMPLEX
            "Very Complex" -> com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ComplexityLevel.VERY_COMPLEX
            else -> null
        }
        
        // Usage filter
        val usageSelection = view.comboUsageFilter?.selectedItem?.toString()
        println("VectorUIController: Usage selection: '$usageSelection'")
        val usageStatus = when (usageSelection) {
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
        
        // Optimization suggestions filter - check if vectors have actual optimization suggestions
        val hasOptimizationSuggestions = if (view.checkShowOptimizable?.isSelected == true) true else null
        
        val criteria = com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.FilterCriteria(
            text = textFilter,
            fileSizeRange = fileSizeRange,
            complexityLevel = complexityLevel,
            tags = tags,
            usageStatus = usageStatus,
            hasAnimations = hasAnimations,
            hasOptimizationSuggestions = hasOptimizationSuggestions
        )
        
        println("VectorUIController: Built filter criteria - $criteria")
        return criteria
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
        // Run display update on background thread to avoid blocking UI
        SwingUtilities.invokeLater {
            val items = vectorService.getFilteredAndSortedVectors()
            
            // Update result count immediately
            view.labelResultCount?.text = "${items.size} vectors"
            
            // Only update display if there are reasonable number of items or if forced
            if (items.size <= 1000) {
                displayVectors(items)
            } else {
                // For very large result sets, show a message and limit display
                val limitedItems = items.take(500)
                view.labelResultCount?.text = "${items.size} vectors (showing first 500)"
                displayVectors(limitedItems)
            }
        }
    }
    
    private fun displayVectors(items: List<VectorItem>) {
        println("VectorUIController: Displaying ${items.size} vectors")
        
        // Clear existing components efficiently
        view.panelVectors.removeAll()
        
        // Set up grid layout for better organization
        val columns = calculateOptimalColumns(items.size)
        view.panelVectors.layout = GridLayout(0, columns, 8, 8)
        
        // Batch process vector panels to avoid UI freezing
        val batchSize = 50
        var processedCount = 0
        
        items.chunked(batchSize).forEach { batch ->
            SwingUtilities.invokeLater {
                batch.forEach { item ->
                    // Analytics should already be generated and persisted
                    if (item.analytics == null) {
                        println("VectorUIController: WARNING - No analytics for ${item.name}, generating on-demand")
                        val analytics = analyticsService.analyzeVector(item)
                        vectorService.updateVectorAnalytics(item, analytics)
                        println("VectorUIController: Generated analytics for ${item.name} - complexity: ${analytics.complexityScore}")
                    }
                    
                    val vectorPanel = VectorItemPanel(item, project)
                    view.panelVectors.add(vectorPanel)
                }
                
                processedCount += batch.size
                
                // Update UI after each batch
                view.panelVectors.revalidate()
                view.panelVectors.repaint()
                
                // Update progress if needed
                if (processedCount >= items.size) {
                    println("VectorUIController: Display update complete - ${items.size} vectors")
                }
            }
        }
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
        println("VectorUIController: Starting to load vectors...")
        
        // Run in background task with progress indicator
        com.intellij.openapi.progress.ProgressManager.getInstance().run(
            object : com.intellij.openapi.progress.Task.Backgroundable(project, "Loading Vector Drawables", true) {
                override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) {
                    indicator.text = "Searching for vector drawable files..."
                    indicator.isIndeterminate = false
                    indicator.fraction = 0.0
                    
                    val disposable = vectorService.loadVectors(project)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.computation())
                        .doOnNext { vectorItem ->
                            // Update progress
                            SwingUtilities.invokeLater {
                                indicator.text2 = "Processing: ${vectorItem.name}"
                            }
                        }
                        .subscribe(
                            { vectorItem ->
                                // Check for cancellation
                                if (indicator.isCanceled) return@subscribe
                                
                                // Vector item loaded successfully - generate analytics immediately
                                println("VectorUIController: Loaded vector: ${vectorItem.name}")
                                if (vectorItem.analytics == null) {
                                    val analytics = analyticsService.analyzeVector(vectorItem)
                                    vectorService.updateVectorAnalytics(vectorItem, analytics)
                                    println("VectorUIController: Generated analytics for ${vectorItem.name} - complexity: ${analytics.complexityScore}")
                                }
                            },
                            { error ->
                                if (!indicator.isCanceled) {
                                    println("VectorUIController: Error loading vector: ${error.message}")
                                    error.printStackTrace()
                                }
                            },
                            {
                                if (!indicator.isCanceled) {
                                    // Loading completed - generate usage analysis for all vectors
                                    println("VectorUIController: Vector loading completed, generating usage analytics...")
                                    indicator.text = "Analyzing vector usage..."
                                    indicator.fraction = 0.8
                                    
                                    SwingUtilities.invokeLater {
                                        generateUsageAnalyticsForAllVectors()
                                        updateVectorDisplay()
                                        indicator.fraction = 1.0
                                    }
                                }
                            }
                        )
                    disposables.add(disposable)
                    
                    // Wait for completion or cancellation
                    while (!indicator.isCanceled && !disposable.isDisposed) {
                        try {
                            Thread.sleep(100)
                        } catch (e: InterruptedException) {
                            break
                        }
                    }
                    
                    if (indicator.isCanceled) {
                        disposable.dispose()
                    }
                }
            }
        )
    }
    
    private fun generateUsageAnalyticsForAllVectors() {
        val vectors = vectorService.getAllVectors() // Get all vectors, not filtered ones
        println("VectorUIController: Generating usage analytics for ${vectors.size} vectors")
        
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
                println("VectorUIController: Updated usage for ${vector.name} - status: ${updatedAnalytics.usageStatus}")
            }
        }
        
        println("VectorUIController: Usage analytics generation completed")
    }
    
    private fun debouncedSliderUpdate() {
        // Cancel previous task
        sliderDebounceTask?.cancel(false)
        
        // Schedule new task with shorter delay for slider
        sliderDebounceTask = debounceExecutor.schedule({
            SwingUtilities.invokeLater {
                updateAdvancedFilter()
            }
        }, SLIDER_DEBOUNCE_DELAY, TimeUnit.MILLISECONDS)
    }
    
    private fun debouncedUpdateAdvancedFilter() {
        // Cancel previous task
        filterDebounceTask?.cancel(false)
        
        // Schedule new task
        filterDebounceTask = debounceExecutor.schedule({
            SwingUtilities.invokeLater {
                updateAdvancedFilter()
            }
        }, FILTER_DEBOUNCE_DELAY, TimeUnit.MILLISECONDS)
    }
    
    private fun updateSliderLabel(value: Int) {
        // Update slider tooltip or label for immediate visual feedback
        view.sliderFileSizeMax?.toolTipText = if (value >= 50) "No limit" else "${value}KB max"
    }
} 