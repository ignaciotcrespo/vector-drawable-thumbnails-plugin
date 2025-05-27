package com.github.ignaciotcrespo.vectordrawablesthumbnails.ui

import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.VectorAnalyticsService
import com.intellij.openapi.project.Project
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import javax.swing.*

/**
 * Paginated display system for vector items with lazy loading.
 * Loads first page immediately, then loads additional pages in background.
 */
class PaginatedVectorDisplay(
    private val project: Project,
    private val analyticsService: VectorAnalyticsService,
    private val pageSize: Int = 100 // Items per page
) : JPanel() {
    
    private val vectorPanel = JPanel()
    private val paginationPanel = JPanel()
    private val statusLabel = JLabel()
    
    private var allItems: List<VectorItem> = emptyList()
    private var currentPage = 0
    private var totalPages = 0
    private var isLoading = false
    
    // Background executor for loading additional pages
    private val backgroundExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    
    // Viewport monitoring thread
    private var viewportMonitoringThread: Thread? = null
    
    // Pagination controls
    private val firstButton = JButton("⏮")
    private val prevButton = JButton("◀")
    private val nextButton = JButton("▶")
    private val lastButton = JButton("⏭")
    private val pageLabel = JLabel()
    private val pageSizeCombo = JComboBox(arrayOf(50, 100, 200, 500))
    
    init {
        setupLayout()
        setupPaginationControls()
        setupVectorPanel()
    }
    
    private fun setupLayout() {
        layout = BorderLayout()
        
        // Main vector display area with scroll
        val scrollPane = JScrollPane(vectorPanel)
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        add(scrollPane, BorderLayout.CENTER)
        
        // Bottom panel with pagination and status
        val bottomPanel = JPanel(BorderLayout())
        bottomPanel.add(paginationPanel, BorderLayout.CENTER)
        bottomPanel.add(statusLabel, BorderLayout.EAST)
        add(bottomPanel, BorderLayout.SOUTH)
    }
    
    private fun setupPaginationControls() {
        paginationPanel.layout = FlowLayout(FlowLayout.CENTER)
        
        // Page navigation buttons
        firstButton.addActionListener { goToPage(0) }
        prevButton.addActionListener { goToPage(currentPage - 1) }
        nextButton.addActionListener { goToPage(currentPage + 1) }
        lastButton.addActionListener { goToPage(totalPages - 1) }
        
        // Page size selector
        pageSizeCombo.selectedItem = pageSize
        pageSizeCombo.addActionListener {
            val newPageSize = pageSizeCombo.selectedItem as Int
            if (newPageSize != pageSize) {
                updatePageSize(newPageSize)
            }
        }
        
        paginationPanel.add(firstButton)
        paginationPanel.add(prevButton)
        paginationPanel.add(pageLabel)
        paginationPanel.add(nextButton)
        paginationPanel.add(lastButton)
        paginationPanel.add(JLabel("  Items per page:"))
        paginationPanel.add(pageSizeCombo)
        
        updatePaginationControls()
    }
    
    private fun setupVectorPanel() {
        vectorPanel.background = Color.WHITE
        // Layout will be set dynamically based on content
    }
    
    /**
     * Sets the items to display with immediate first page load and background loading for rest.
     */
    fun setItems(items: List<VectorItem>) {
        allItems = items
        totalPages = if (items.isEmpty()) 0 else (items.size + pageSize - 1) / pageSize
        currentPage = 0
        
        updateStatusLabel()
        updatePaginationControls()
        
        if (items.isNotEmpty()) {
            // Load first page immediately for quick response
            loadPageImmediate(0)
            
            // Disable background loading for now to prevent performance issues
            // if (totalPages > 1) {
            //     startBackgroundLoading()
            // }
        } else {
            clearVectorPanel()
        }
    }
    
    private fun loadPageImmediate(page: Int) {
        if (page < 0 || page >= totalPages) return
        
        currentPage = page
        val startIndex = page * pageSize
        val endIndex = minOf(startIndex + pageSize, allItems.size)
        val pageItems = allItems.subList(startIndex, endIndex)
        
        displayPageItems(pageItems)
        updatePaginationControls()
        updateStatusLabel()
        
        println("PaginatedVectorDisplay: Loaded page ${page + 1}/$totalPages immediately (${pageItems.size} items)")
    }
    
    private fun displayPageItems(items: List<VectorItem>) {
        SwingUtilities.invokeLater {
            vectorPanel.removeAll()
            
            // Use responsive grid layout that prevents horizontal scrolling
            vectorPanel.layout = ResponsiveGridLayout(160, 180, 8, 8)
            
            // Add viewport-aware lazy placeholders
            items.forEach { item ->
                val placeholder = createViewportLazyPlaceholder(item)
                vectorPanel.add(placeholder)
            }
            
            vectorPanel.revalidate()
            vectorPanel.repaint()
            
            // Start viewport monitoring after a short delay to let layout settle
            SwingUtilities.invokeLater {
                startViewportMonitoring()
            }
        }
    }
    
    private fun createViewportLazyPlaceholder(item: VectorItem): JPanel {
        val placeholder = JPanel(BorderLayout())
        placeholder.background = Color.WHITE
        placeholder.border = BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1)
        placeholder.preferredSize = Dimension(160, 180)
        placeholder.minimumSize = Dimension(160, 180)
        placeholder.maximumSize = Dimension(160, 180)
        
        // Just show the name immediately - no other processing
        val nameLabel = JLabel(item.name, SwingConstants.CENTER)
        nameLabel.font = nameLabel.font.deriveFont(Font.BOLD, 10f)
        placeholder.add(nameLabel, BorderLayout.CENTER)
        
        // Add a simple loading indicator
        val loadingLabel = JLabel("Loading...", SwingConstants.CENTER)
        loadingLabel.font = loadingLabel.font.deriveFont(9f)
        loadingLabel.foreground = Color.GRAY
        placeholder.add(loadingLabel, BorderLayout.SOUTH)
        
        // Store item reference for viewport loading
        placeholder.putClientProperty("vectorItem", item)
        placeholder.putClientProperty("isLoaded", false)
        placeholder.putClientProperty("isLoading", false)
        
        // Handle double-click for priority analytics loading
        placeholder.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 1) {
                    // Single click - open file (works even with placeholder)
                    com.github.ignaciotcrespo.vectordrawablesthumbnails.utils.Utils.openValidFile(project, item.validFile)
                } else if (e.clickCount == 2) {
                    // Double click - show analytics dialog immediately (it will load analytics on-demand)
                    showAnalyticsDialog(item)
                }
            }
            
            override fun mouseEntered(e: java.awt.event.MouseEvent) {
                placeholder.background = Color(240, 240, 240)
                placeholder.repaint()
            }
            
            override fun mouseExited(e: java.awt.event.MouseEvent) {
                placeholder.background = Color.WHITE
                placeholder.repaint()
            }
        })
        
        placeholder.cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
        return placeholder
    }
    
    private fun startViewportMonitoring() {
        // Stop any existing monitoring thread
        viewportMonitoringThread?.interrupt()
        
        // Use a background thread to monitor viewport and load visible items
        viewportMonitoringThread = Thread {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    SwingUtilities.invokeAndWait {
                        loadVisiblePlaceholders()
                    }
                    
                    // Check every 200ms for smooth scrolling experience
                    Thread.sleep(200)
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    // Continue monitoring even if there are errors
                }
            }
        }
        viewportMonitoringThread?.start()
    }
    
    private fun loadVisiblePlaceholders() {
        val scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane::class.java, vectorPanel) as? JScrollPane
        if (scrollPane == null) return
        
        val viewport = scrollPane.viewport
        val viewRect = viewport.viewRect
        
        // Add some buffer for smoother experience
        val bufferedRect = Rectangle(
            viewRect.x,
            maxOf(0, viewRect.y - 200), // Load 200px above visible area
            viewRect.width,
            viewRect.height + 400 // Load 200px below visible area
        )
        
        // Check each placeholder
        for (component in vectorPanel.components) {
            if (component is JPanel) {
                val isLoaded = component.getClientProperty("isLoaded") as? Boolean ?: false
                val isLoading = component.getClientProperty("isLoading") as? Boolean ?: false
                
                if (!isLoaded && !isLoading) {
                    val bounds = component.bounds
                    if (bufferedRect.intersects(bounds)) {
                        val item = component.getClientProperty("vectorItem") as? VectorItem
                        if (item != null) {
                            loadPlaceholderAsync(component, item, false) // Normal priority
                        }
                    }
                }
            }
        }
    }
    
    private fun loadPlaceholderAsync(placeholder: JPanel, item: VectorItem, isPriority: Boolean = false) {
        // Prevent multiple loading attempts
        val isLoading = placeholder.getClientProperty("isLoading") as? Boolean ?: false
        if (isLoading) return
        
        placeholder.putClientProperty("isLoading", true)
        
        // Show loading state
        SwingUtilities.invokeLater {
            val loadingLabel = placeholder.components.find { it is JLabel && (it as JLabel).text == "Loading..." } as? JLabel
            if (loadingLabel != null) {
                loadingLabel.text = if (isPriority) "Priority loading..." else "Loading..."
                loadingLabel.foreground = if (isPriority) Color.BLUE else Color.GRAY
            }
        }
        
        // Create full panel in background
        val thread = Thread {
            try {
                val fullPanel = LazyVectorItemPanel(item, project, analyticsService)
                
                SwingUtilities.invokeLater {
                    replacePlaceholderWithPanel(placeholder, fullPanel)
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    showErrorPlaceholder(placeholder, "Load failed")
                }
            }
        }
        
        // Set thread priority based on loading type
        thread.priority = if (isPriority) Thread.MAX_PRIORITY else Thread.NORM_PRIORITY
        thread.start()
    }
    
    private fun replacePlaceholderWithPanel(placeholder: JPanel, fullPanel: LazyVectorItemPanel) {
        val parent = placeholder.parent
        if (parent != null) {
            val index = parent.components.indexOf(placeholder)
            if (index >= 0) {
                parent.remove(placeholder)
                parent.add(fullPanel, index)
                parent.revalidate()
                parent.repaint()
            }
        }
    }
    
    private fun showErrorPlaceholder(placeholder: JPanel, errorMessage: String) {
        placeholder.removeAll()
        val errorLabel = JLabel(errorMessage, SwingConstants.CENTER)
        errorLabel.foreground = Color.RED
        errorLabel.font = errorLabel.font.deriveFont(9f)
        placeholder.add(errorLabel, BorderLayout.CENTER)
        placeholder.putClientProperty("isLoaded", true) // Mark as "loaded" to prevent retries
        placeholder.putClientProperty("isLoading", false)
        placeholder.revalidate()
        placeholder.repaint()
    }
    
    private fun showAnalyticsDialog(item: VectorItem) {
        // Show the analytics dialog immediately - it will load analytics on-demand if needed
        SwingUtilities.invokeLater {
            val dialog = VectorAnalyticsDialog(
                SwingUtilities.getWindowAncestor(this), 
                item, 
                analyticsService, 
                project, 
                item.analytics // Pass existing analytics if available, null if not
            )
            dialog.isVisible = true
        }
    }
    
    private fun goToPage(page: Int) {
        if (page < 0 || page >= totalPages || page == currentPage || isLoading) return
        
        loadPageImmediate(page)
    }
    
    private fun updatePageSize(newPageSize: Int) {
        val currentItem = if (allItems.isNotEmpty() && currentPage >= 0) {
            allItems.getOrNull(currentPage * pageSize)
        } else null
        
        // Recalculate pagination with new page size
        val newTotalPages = if (allItems.isEmpty()) 0 else (allItems.size + newPageSize - 1) / newPageSize
        
        // Find which page the current first item would be on
        val newCurrentPage = if (currentItem != null) {
            val itemIndex = allItems.indexOf(currentItem)
            if (itemIndex >= 0) itemIndex / newPageSize else 0
        } else 0
        
        totalPages = newTotalPages
        currentPage = newCurrentPage.coerceIn(0, maxOf(0, totalPages - 1))
        
        updatePaginationControls()
        updateStatusLabel()
        
        if (allItems.isNotEmpty()) {
            loadPageImmediate(currentPage)
        }
    }
    
    private fun updatePaginationControls() {
        firstButton.isEnabled = currentPage > 0
        prevButton.isEnabled = currentPage > 0
        nextButton.isEnabled = currentPage < totalPages - 1
        lastButton.isEnabled = currentPage < totalPages - 1
        
        pageLabel.text = if (totalPages > 0) {
            "Page ${currentPage + 1} of $totalPages"
        } else {
            "No pages"
        }
    }
    
    private fun updateStatusLabel(customMessage: String? = null) {
        val message = customMessage ?: run {
            val startIndex = currentPage * pageSize + 1
            val endIndex = minOf((currentPage + 1) * pageSize, allItems.size)
            
            when {
                allItems.isEmpty() -> "No vectors"
                totalPages == 1 -> "${allItems.size} vectors"
                else -> "Showing $startIndex-$endIndex of ${allItems.size} vectors"
            }
        }
        
        statusLabel.text = message
    }
    
    private fun clearVectorPanel() {
        SwingUtilities.invokeLater {
            vectorPanel.removeAll()
            vectorPanel.revalidate()
            vectorPanel.repaint()
        }
    }
    
    /**
     * Gets current pagination statistics.
     */
    fun getPaginationStats(): PaginationStats {
        return PaginationStats(
            totalItems = allItems.size,
            currentPage = currentPage + 1,
            totalPages = totalPages,
            pageSize = pageSize,
            itemsOnCurrentPage = if (totalPages > 0) {
                val startIndex = currentPage * pageSize
                val endIndex = minOf(startIndex + pageSize, allItems.size)
                endIndex - startIndex
            } else 0
        )
    }
    
    fun dispose() {
        // Stop viewport monitoring thread
        viewportMonitoringThread?.interrupt()
        viewportMonitoringThread = null
        
        // Shutdown background executor
        backgroundExecutor.shutdown()
    }
}

/**
 * Statistics about the current pagination state.
 */
data class PaginationStats(
    val totalItems: Int,
    val currentPage: Int,
    val totalPages: Int,
    val pageSize: Int,
    val itemsOnCurrentPage: Int
) 