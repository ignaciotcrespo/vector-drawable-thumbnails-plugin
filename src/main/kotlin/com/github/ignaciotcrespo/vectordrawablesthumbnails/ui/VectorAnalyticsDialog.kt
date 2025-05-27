package com.github.ignaciotcrespo.vectordrawablesthumbnails.ui

import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.Priority
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorAnalytics
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.VectorAnalyticsService
import com.intellij.openapi.project.Project
import java.awt.*
import javax.swing.*

/**
 * Dialog showing detailed analytics for a vector drawable.
 * Provides comprehensive insights and optimization suggestions.
 * Can load analytics on-demand if not already available.
 */
class VectorAnalyticsDialog : JDialog {
    
    private val vectorItem: VectorItem
    private val analyticsService: VectorAnalyticsService?
    private val project: Project?
    private var analytics: VectorAnalytics?
    private lateinit var contentPanel: JPanel
    private lateinit var loadingPanel: JPanel
    private lateinit var progressBar: JProgressBar
    private lateinit var loadingLabel: JLabel
    
    // New constructor with on-demand analytics loading
    constructor(
        parent: Window?,
        vectorItem: VectorItem,
        analyticsService: VectorAnalyticsService,
        project: Project,
        initialAnalytics: VectorAnalytics? = null
    ) : super(parent, "Vector Analytics - ${vectorItem.name}", ModalityType.APPLICATION_MODAL) {
        this.vectorItem = vectorItem
        this.analyticsService = analyticsService
        this.project = project
        this.analytics = initialAnalytics
        
        setupDialog()
        if (analytics != null) {
            createContentWithAnalytics()
        } else {
            createLoadingContent()
            loadAnalyticsAsync()
        }
        pack()
        setLocationRelativeTo(parent)
    }
    
    // Backward-compatible constructor for existing code
    constructor(
        parent: Window?,
        vectorItem: VectorItem,
        analytics: VectorAnalytics
    ) : super(parent, "Vector Analytics - ${vectorItem.name}", ModalityType.APPLICATION_MODAL) {
        this.vectorItem = vectorItem
        this.analyticsService = null
        this.project = null
        this.analytics = analytics
        
        setupDialog()
        createContentWithAnalytics()
        pack()
        setLocationRelativeTo(parent)
    }
    
    private fun setupDialog() {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        isResizable = true
        minimumSize = Dimension(500, 400)
    }
    
    private fun createLoadingContent() {
        layout = BorderLayout()
        
        // Header with vector preview (always available)
        add(createHeaderPanel(), BorderLayout.NORTH)
        
        // Loading panel
        loadingPanel = JPanel()
        loadingPanel.layout = BoxLayout(loadingPanel, BoxLayout.Y_AXIS)
        loadingPanel.border = BorderFactory.createEmptyBorder(50, 50, 50, 50)
        
        loadingLabel = JLabel("Loading analytics...", SwingConstants.CENTER)
        loadingLabel.font = loadingLabel.font.deriveFont(Font.BOLD, 16f)
        loadingLabel.alignmentX = Component.CENTER_ALIGNMENT
        
        progressBar = JProgressBar()
        progressBar.isIndeterminate = true
        progressBar.alignmentX = Component.CENTER_ALIGNMENT
        progressBar.preferredSize = Dimension(300, 20)
        
        val statusLabel = JLabel("Analyzing vector complexity, usage, and optimization opportunities...", SwingConstants.CENTER)
        statusLabel.font = statusLabel.font.deriveFont(12f)
        statusLabel.foreground = Color.GRAY
        statusLabel.alignmentX = Component.CENTER_ALIGNMENT
        
        loadingPanel.add(Box.createVerticalGlue())
        loadingPanel.add(loadingLabel)
        loadingPanel.add(Box.createVerticalStrut(20))
        loadingPanel.add(progressBar)
        loadingPanel.add(Box.createVerticalStrut(10))
        loadingPanel.add(statusLabel)
        loadingPanel.add(Box.createVerticalGlue())
        
        add(loadingPanel, BorderLayout.CENTER)
        
        // Footer with close button
        add(createFooterPanel(), BorderLayout.SOUTH)
    }
    
    private fun createContentWithAnalytics() {
        layout = BorderLayout()
        
        // Header with vector preview
        add(createHeaderPanel(), BorderLayout.NORTH)
        
        // Main content with tabs
        add(createTabbedPane(), BorderLayout.CENTER)
        
        // Footer with actions
        add(createFooterPanel(), BorderLayout.SOUTH)
    }
    
    private fun loadAnalyticsAsync() {
        Thread {
            try {
                // Check if we have the required services
                if (analyticsService == null || project == null) {
                    SwingUtilities.invokeLater {
                        showErrorContent("Analytics service not available")
                    }
                    return@Thread
                }
                
                SwingUtilities.invokeLater {
                    loadingLabel.text = "Analyzing vector structure..."
                }
                
                // Generate analytics with progress updates
                val generatedAnalytics = analyticsService.analyzeVector(vectorItem)
                
                SwingUtilities.invokeLater {
                    loadingLabel.text = "Analyzing usage patterns..."
                }
                
                // Analyze usage (this is the expensive part)
                val usageAnalytics = analyticsService.analyzeUsage(project, listOf(vectorItem))
                val usageStatus = usageAnalytics[vectorItem] ?: com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.UsageStatus.UNUSED
                val usageCount = when (usageStatus) {
                    com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.UsageStatus.UNUSED -> 0
                    com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.UsageStatus.RARELY_USED -> 1
                    com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.UsageStatus.USED -> 5
                    com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.UsageStatus.FREQUENTLY_USED -> 10
                }
                
                // Update analytics with usage information
                analytics = generatedAnalytics.copy(
                    usageStatus = usageStatus,
                    usageCount = usageCount
                )
                
                SwingUtilities.invokeLater {
                    loadingLabel.text = "Finalizing analytics..."
                    
                    // Replace loading content with actual analytics
                    remove(loadingPanel)
                    add(createTabbedPane(), BorderLayout.CENTER)
                    revalidate()
                    repaint()
                    pack()
                }
                
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    showErrorContent("Failed to load analytics: ${e.message}")
                }
            }
        }.start()
    }
    
    private fun showErrorContent(errorMessage: String) {
        remove(loadingPanel)
        
        val errorPanel = JPanel()
        errorPanel.layout = BoxLayout(errorPanel, BoxLayout.Y_AXIS)
        errorPanel.border = BorderFactory.createEmptyBorder(50, 50, 50, 50)
        
        val errorLabel = JLabel("Error loading analytics", SwingConstants.CENTER)
        errorLabel.font = errorLabel.font.deriveFont(Font.BOLD, 16f)
        errorLabel.foreground = Color.RED
        errorLabel.alignmentX = Component.CENTER_ALIGNMENT
        
        val messageLabel = JLabel(errorMessage, SwingConstants.CENTER)
        messageLabel.font = messageLabel.font.deriveFont(12f)
        messageLabel.foreground = Color.GRAY
        messageLabel.alignmentX = Component.CENTER_ALIGNMENT
        
        errorPanel.add(Box.createVerticalGlue())
        errorPanel.add(errorLabel)
        errorPanel.add(Box.createVerticalStrut(10))
        errorPanel.add(messageLabel)
        errorPanel.add(Box.createVerticalGlue())
        
        add(errorPanel, BorderLayout.CENTER)
        revalidate()
        repaint()
    }
    
    private fun createHeaderPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createEmptyBorder(16, 16, 16, 16)
        panel.background = Color(250, 250, 250)
        
        // Vector preview
        val imageLabel = JLabel(ImageIcon(vectorItem.image))
        imageLabel.border = BorderFactory.createLineBorder(Color.LIGHT_GRAY)
        panel.add(imageLabel, BorderLayout.WEST)
        
        // Basic info
        val infoPanel = JPanel()
        infoPanel.layout = BoxLayout(infoPanel, BoxLayout.Y_AXIS)
        infoPanel.border = BorderFactory.createEmptyBorder(0, 16, 0, 0)
        infoPanel.isOpaque = false
        
        val nameLabel = JLabel(vectorItem.name)
        nameLabel.font = nameLabel.font.deriveFont(Font.BOLD, 16f)
        infoPanel.add(nameLabel)
        
        infoPanel.add(Box.createVerticalStrut(8))
        
        val sizeLabel = JLabel("Size: ${vectorItem.displaySize}")
        infoPanel.add(sizeLabel)
        
        val fileSizeLabel = JLabel("File Size: ${vectorItem.fileSizeFormatted}")
        infoPanel.add(fileSizeLabel)
        
        // Only show complexity if analytics are available
        analytics?.let { analytics ->
            val complexityLabel = JLabel("Complexity: ${analytics.complexityLevel.name.lowercase()}")
            complexityLabel.foreground = getComplexityColor(analytics.complexityLevel)
            infoPanel.add(complexityLabel)
        }
        
        panel.add(infoPanel, BorderLayout.CENTER)
        
        return panel
    }
    
    private fun createTabbedPane(): JTabbedPane {
        val tabbedPane = JTabbedPane()
        
        analytics?.let { analytics ->
            tabbedPane.addTab("📊 Overview", createOverviewPanel(analytics))
            tabbedPane.addTab("🔧 Optimizations", createOptimizationsPanel(analytics))
            tabbedPane.addTab("🏷️ Tags & Usage", createTagsPanel(analytics))
            tabbedPane.addTab("📈 Performance", createPerformancePanel(analytics))
        }
        
        return tabbedPane
    }
    
    private fun createOverviewPanel(analytics: VectorAnalytics): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = BorderFactory.createEmptyBorder(16, 16, 16, 16)
        
        // Metrics grid
        val metricsPanel = JPanel(GridLayout(0, 2, 16, 8))
        
        metricsPanel.add(createMetricPanel("Complexity Score", "${analytics.complexityScore}/100"))
        metricsPanel.add(createMetricPanel("Path Count", analytics.pathCount.toString()))
        metricsPanel.add(createMetricPanel("Color Count", analytics.colorCount.toString()))
        metricsPanel.add(createMetricPanel("Usage Count", analytics.usageCount.toString()))
        metricsPanel.add(createMetricPanel("Aspect Ratio", "%.2f".format(analytics.aspectRatio)))
        metricsPanel.add(createMetricPanel("Has Animations", if (analytics.hasAnimations) "Yes" else "No"))
        
        panel.add(metricsPanel)
        
        // Usage status
        panel.add(Box.createVerticalStrut(16))
        val usagePanel = createUsageStatusPanel(analytics)
        panel.add(usagePanel)
        
        return panel
    }
    
    private fun createOptimizationsPanel(analytics: VectorAnalytics): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = BorderFactory.createEmptyBorder(16, 16, 16, 16)
        
        if (analytics.optimizationSuggestions.isEmpty()) {
            val noSuggestionsLabel = JLabel("No optimization suggestions available")
            noSuggestionsLabel.foreground = Color.GRAY
            noSuggestionsLabel.horizontalAlignment = SwingConstants.CENTER
            panel.add(noSuggestionsLabel)
        } else {
            analytics.optimizationSuggestions.forEach { suggestion ->
                val suggestionPanel = createOptimizationSuggestionPanel(suggestion)
                panel.add(suggestionPanel)
                panel.add(Box.createVerticalStrut(8))
            }
        }
        
        return panel
    }
    
    private fun createOptimizationSuggestionPanel(suggestion: com.github.ignaciotcrespo.vectordrawablesthumbnails.model.OptimizationSuggestion): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(getPriorityColor(suggestion.priority)),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        )
        
        val titleLabel = JLabel(suggestion.type.name.lowercase().replace('_', ' '))
        titleLabel.font = titleLabel.font.deriveFont(Font.BOLD)
        panel.add(titleLabel, BorderLayout.NORTH)
        
        val descriptionLabel = JLabel("<html>${suggestion.description}</html>")
        panel.add(descriptionLabel, BorderLayout.CENTER)
        
        val savingsLabel = JLabel(suggestion.potentialSavings)
        savingsLabel.foreground = Color(0, 150, 0)
        savingsLabel.font = savingsLabel.font.deriveFont(Font.BOLD, 10f)
        panel.add(savingsLabel, BorderLayout.SOUTH)
        
        return panel
    }
    
    private fun createTagsPanel(analytics: VectorAnalytics): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createEmptyBorder(16, 16, 16, 16)
        
        // Tags section
        val tagsPanel = JPanel()
        tagsPanel.layout = BoxLayout(tagsPanel, BoxLayout.Y_AXIS)
        
        val tagsLabel = JLabel("Tags:")
        tagsLabel.font = tagsLabel.font.deriveFont(Font.BOLD)
        tagsPanel.add(tagsLabel)
        
        tagsPanel.add(Box.createVerticalStrut(8))
        
        if (analytics.tags.isEmpty()) {
            val noTagsLabel = JLabel("No tags available")
            noTagsLabel.foreground = Color.GRAY
            tagsPanel.add(noTagsLabel)
        } else {
            val tagsFlowPanel = JPanel(FlowLayout(FlowLayout.LEFT))
            analytics.tags.forEach { tag ->
                val tagLabel = createTagLabel(tag)
                tagsFlowPanel.add(tagLabel)
            }
            tagsPanel.add(tagsFlowPanel)
        }
        
        panel.add(tagsPanel, BorderLayout.NORTH)
        
        // Usage details
        val usagePanel = JPanel()
        usagePanel.layout = BoxLayout(usagePanel, BoxLayout.Y_AXIS)
        usagePanel.border = BorderFactory.createTitledBorder("Usage Details")
        
        val usageStatusLabel = JLabel("Status: ${analytics.usageStatus.name.lowercase().replace('_', ' ')}")
        usagePanel.add(usageStatusLabel)
        
        val usageCountLabel = JLabel("Found in ${analytics.usageCount} files")
        usagePanel.add(usageCountLabel)
        
        panel.add(usagePanel, BorderLayout.CENTER)
        
        return panel
    }
    
    private fun createPerformancePanel(analytics: VectorAnalytics): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = BorderFactory.createEmptyBorder(16, 16, 16, 16)
        
        // Performance metrics
        val performancePanel = JPanel(GridLayout(0, 1, 0, 8))
        
        val renderTimeLabel = JLabel("Estimated Render Time: ${analytics.estimatedRenderTime}μs")
        performancePanel.add(renderTimeLabel)
        
        val complexityBar = createProgressBar("Complexity", analytics.complexityScore, 100)
        performancePanel.add(complexityBar)
        
        val sizeBar = createProgressBar("File Size", vectorItem.fileSize.toInt(), 20 * 1024) // Max 20KB
        performancePanel.add(sizeBar)
        
        panel.add(performancePanel)
        
        return panel
    }
    
    private fun createFooterPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.RIGHT))
        panel.border = BorderFactory.createEmptyBorder(8, 16, 16, 16)
        
        val closeButton = JButton("Close")
        closeButton.addActionListener { dispose() }
        panel.add(closeButton)
        
        return panel
    }
    
    private fun createMetricPanel(label: String, value: String): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        )
        
        val labelComponent = JLabel(label)
        labelComponent.font = labelComponent.font.deriveFont(Font.BOLD, 10f)
        labelComponent.foreground = Color.GRAY
        panel.add(labelComponent, BorderLayout.NORTH)
        
        val valueComponent = JLabel(value)
        valueComponent.font = valueComponent.font.deriveFont(Font.BOLD, 14f)
        panel.add(valueComponent, BorderLayout.CENTER)
        
        return panel
    }
    
    private fun createUsageStatusPanel(analytics: VectorAnalytics): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("Usage Status")
        
        val statusLabel = JLabel(analytics.usageStatus.name.lowercase().replace('_', ' '))
        statusLabel.font = statusLabel.font.deriveFont(Font.BOLD, 14f)
        statusLabel.foreground = getUsageColor(analytics.usageStatus)
        statusLabel.horizontalAlignment = SwingConstants.CENTER
        
        panel.add(statusLabel, BorderLayout.CENTER)
        
        return panel
    }
    
    private fun createTagLabel(tag: String): JLabel {
        val label = JLabel(tag)
        label.font = label.font.deriveFont(10f)
        label.foreground = Color.WHITE
        label.background = Color(100, 150, 200)
        label.isOpaque = true
        label.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color(80, 130, 180)),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)
        )
        return label
    }
    
    private fun createProgressBar(label: String, value: Int, max: Int): JPanel {
        val panel = JPanel(BorderLayout())
        
        val labelComponent = JLabel(label)
        panel.add(labelComponent, BorderLayout.WEST)
        
        val progressBar = JProgressBar(0, max)
        progressBar.value = value
        progressBar.isStringPainted = true
        progressBar.string = "$value / $max"
        panel.add(progressBar, BorderLayout.CENTER)
        
        return panel
    }
    
    private fun getComplexityColor(level: com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ComplexityLevel): Color {
        return when (level) {
            com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ComplexityLevel.SIMPLE -> Color(76, 175, 80)
            com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ComplexityLevel.MODERATE -> Color(255, 193, 7)
            com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ComplexityLevel.COMPLEX -> Color(255, 152, 0)
            com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ComplexityLevel.VERY_COMPLEX -> Color(244, 67, 54)
        }
    }
    
    private fun getUsageColor(status: com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.UsageStatus): Color {
        return when (status) {
            com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.UsageStatus.UNUSED -> Color(244, 67, 54)
            com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.UsageStatus.RARELY_USED -> Color(255, 152, 0)
            com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.UsageStatus.USED -> Color(255, 193, 7)
            com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.UsageStatus.FREQUENTLY_USED -> Color(76, 175, 80)
        }
    }
    
    private fun getPriorityColor(priority: Priority): Color {
        return when (priority) {
            Priority.LOW -> Color(76, 175, 80)
            Priority.MEDIUM -> Color(255, 193, 7)
            Priority.HIGH -> Color(255, 152, 0)
            Priority.CRITICAL -> Color(244, 67, 54)
        }
    }
} 