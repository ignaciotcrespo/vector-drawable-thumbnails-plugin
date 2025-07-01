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
import java.net.URI
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
    
    // Color filter state
    private var currentSelectedColors: Set<String> = emptySet()
    
    // Debounce delays in milliseconds
    private val FILTER_DEBOUNCE_DELAY = 300L
    private val SLIDER_DEBOUNCE_DELAY = 150L
    
    // Add pagination display
    private var paginatedDisplay: PaginatedVectorDisplay? = null
    
    fun initialize() {
        initializeUI()
        loadVectorsWhenReady()
    }
    
    /**
     * Initialize UI components without loading vectors.
     * This prevents IDE freezing on startup.
     */
    fun initializeUI() {
//        println("VectorUIController: Initializing UI...")
//        println("VectorUIController: btnRefresh = ${view.btnRefresh}")
//        println("VectorUIController: panelVectors = ${view.panelVectors}")
//        println("VectorUIController: textFilter = ${view.textFilter}")
        setupPaginatedDisplay()
        setupEventListeners()
        subscribeToServiceState()
//        println("VectorUIController: UI initialization complete")
    }
    
    /**
     * Load vectors when the tool window is ready and visible.
     * This is called separately from UI initialization to prevent startup freezing.
     */
    fun loadVectorsWhenReady() {
        println("VectorUIController: Loading vectors when ready...")
        loadVectors()
    }
    
    fun dispose() {
        disposables.clear()
        paginatedDisplay?.dispose()
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
        setupColorFilter()
    }
    
    private fun setupDonateButton() {
        view.btnDonate.addActionListener {
            try {
                Desktop.getDesktop().browse(URI("https://paypal.me/itcrespo"))
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
    
    private fun setupColorFilter() {
        view.colorFilterPanel?.setColorSelectionListener { selectedColors ->
            currentSelectedColors = selectedColors
            updateAdvancedFilter()
        }
        
        // Initialize with empty color palette
        view.colorFilterPanel?.updateColors(emptyMap())
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
        
        // Color filter
        val selectedColors = currentSelectedColors
        
        val criteria = com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.FilterCriteria(
            text = textFilter,
            fileSizeRange = fileSizeRange,
            complexityLevel = complexityLevel,
            tags = tags,
            usageStatus = usageStatus,
            hasAnimations = hasAnimations,
            colors = selectedColors,
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
            
            // Always display all items - no artificial limits
            displayVectors(items)
        }
    }
    
    private fun displayVectors(items: List<VectorItem>) {
        println("VectorUIController: Displaying ${items.size} vectors with pagination")
        
        // Update result count in the main view
        view.labelResultCount?.text = "${items.size} vectors"
        
        // Calculate color frequencies from all vectors (not just displayed ones)
        updateColorPalette()
        
        // Use paginated display for efficient loading
        paginatedDisplay?.setItems(items)
        
        println("VectorUIController: Paginated display updated with ${items.size} vectors")
    }
    
    private fun updateColorPalette() {
        // Get all vectors (not filtered) to show all available colors
        val allVectors = vectorService.getAllVectors()
        val colorFrequencies = mutableMapOf<String, Int>()
        
        // Count color occurrences across all vectors
        allVectors.forEach { vector ->
            vector.analytics?.colors?.forEach { color ->
                colorFrequencies[color] = colorFrequencies.getOrDefault(color, 0) + 1
            }
        }
        
        // Update the color filter panel
        view.colorFilterPanel?.updateColors(colorFrequencies)
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
        println("VectorUIController: Starting ultra-fast vector loading...")
        
        // Show loading state immediately
        SwingUtilities.invokeLater {
            view.btnRefresh.text = "Loading..."
            view.panelFilter.enableAll(false)
            paginatedDisplay?.setItems(emptyList())
        }
        
        // Ultra-fast loading: Show vectors immediately without ANY analytics
        Thread {
            try {
                println("VectorUIController: Ultra-fast loading - no analytics, no blocking operations")
                
                // Load vectors with minimal processing
                val loadingDisposable = vectorService.loadVectors(project)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.computation())
                    .subscribe(
                        { vectorItem ->
                            // Just load the vector, absolutely no processing
                            // Don't even print to avoid I/O overhead
                        },
                        { error ->
                            println("VectorUIController: Error loading vectors: ${error.message}")
                            SwingUtilities.invokeLater {
                                view.btnRefresh.text = "Refresh"
                                view.panelFilter.enableAll(true)
                            }
                        },
                        {
                            // Vectors loaded - show immediately without any analytics
                            SwingUtilities.invokeLater {
                                println("VectorUIController: Vectors loaded - showing immediately without analytics")
                                view.btnRefresh.text = "Refresh"
                                view.panelFilter.enableAll(true)
                                updateVectorDisplay() // Show vectors immediately
                                
                                // Start optional analytics in background (completely separate)
                                startOptionalAnalytics()
                            }
                        }
                    )
                
                disposables.add(loadingDisposable)
                
            } catch (e: Exception) {
                println("VectorUIController: Exception in vector loading: ${e.message}")
                SwingUtilities.invokeLater {
                    view.btnRefresh.text = "Refresh"
                    view.panelFilter.enableAll(true)
                }
            }
        }.start()
    }
    
    private fun startOptionalAnalytics() {
        println("VectorUIController: Starting optional analytics in background (non-blocking)")
        
        // Run analytics completely in background with maximum yielding
        Thread {
            try {
                // Wait a bit to let UI settle
                Thread.sleep(500)
                
                val vectors = vectorService.getAllVectors()
                println("VectorUIController: Starting background analytics for ${vectors.size} vectors")
                
                var processedCount = 0
                val batchSize = 3 // Very small batches
                val totalVectors = vectors.size
                
                // Process vectors in very small batches with maximum yielding
                vectors.chunked(batchSize).forEachIndexed { batchIndex, batch ->
                    // Process batch
                    batch.forEach { vector ->
                        try {
                            // Generate analytics only if not already present
                            if (vector.analytics == null) {
                                val analytics = analyticsService.analyzeVector(vector)
                                vectorService.updateVectorAnalytics(vector, analytics)
                            }
                            processedCount++
                        } catch (e: Exception) {
                            // Silently ignore errors to prevent console spam
                        }
                    }
                    
                    // Update UI progress very occasionally to avoid overwhelming
                    if (batchIndex % 10 == 0) {
                        SwingUtilities.invokeLater {
                            val progress = (processedCount * 100) / totalVectors
                            // Only update button text, don't update display to avoid UI work
                            if (progress < 100) {
                                view.btnRefresh.text = "Background: $progress%"
                            }
                        }
                    }
                    
                    // Maximum yielding to prevent any UI blocking
                    Thread.sleep(50) // Longer delay
                    Thread.yield()
                    
                    // Additional yield every few batches
                    if (batchIndex % 5 == 0) {
                        Thread.sleep(100)
                    }
                }
                
                // Skip usage analytics entirely for now - too expensive
                SwingUtilities.invokeLater {
                    view.btnRefresh.text = "Refresh"
                    println("VectorUIController: Background analytics completed (usage analysis skipped)")
                }
                
            } catch (e: Exception) {
                println("VectorUIController: Exception in background analytics: ${e.message}")
                SwingUtilities.invokeLater {
                    view.btnRefresh.text = "Refresh"
                }
            }
        }.start()
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
    
    private fun setupPaginatedDisplay() {
        // Replace the old panel with paginated display
        paginatedDisplay = PaginatedVectorDisplay(project, analyticsService, pageSize = 50)
        
        // Replace the content of the existing panelVectors
        view.panelVectors.removeAll()
        view.panelVectors.layout = BorderLayout()
        view.panelVectors.add(paginatedDisplay!!, BorderLayout.CENTER)
        view.panelVectors.revalidate()
        view.panelVectors.repaint()
    }
} 