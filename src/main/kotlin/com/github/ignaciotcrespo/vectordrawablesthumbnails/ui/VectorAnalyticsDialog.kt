package com.github.ignaciotcrespo.vectordrawablesthumbnails.ui

import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.Priority
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorAnalytics
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import java.awt.*
import javax.swing.*

/**
 * Dialog showing detailed analytics for a vector drawable.
 * Provides comprehensive insights and optimization suggestions.
 */
class VectorAnalyticsDialog(
    parent: Window?,
    private val vectorItem: VectorItem,
    private val analytics: VectorAnalytics
) : JDialog(parent, "Vector Analytics - ${vectorItem.name}", ModalityType.APPLICATION_MODAL) {
    
    init {
//        println("VectorAnalyticsDialog: Creating dialog for ${vectorItem.name}")
//        println("VectorAnalyticsDialog: Analytics - complexity: ${analytics.complexityLevel}, usage: ${analytics.usageStatus}")
        setupDialog()
        createContent()
        pack()
        setLocationRelativeTo(parent)
//        println("VectorAnalyticsDialog: Dialog created and positioned")
    }
    
    private fun setupDialog() {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        isResizable = true
        minimumSize = Dimension(500, 400)
    }
    
    private fun createContent() {
        layout = BorderLayout()
        
        // Header with vector preview
        add(createHeaderPanel(), BorderLayout.NORTH)
        
        // Main content with tabs
        add(createTabbedPane(), BorderLayout.CENTER)
        
        // Footer with actions
        add(createFooterPanel(), BorderLayout.SOUTH)
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
        
        val complexityLabel = JLabel("Complexity: ${analytics.complexityLevel.name.lowercase()}")
        complexityLabel.foreground = getComplexityColor(analytics.complexityLevel)
        infoPanel.add(complexityLabel)
        
        panel.add(infoPanel, BorderLayout.CENTER)
        
        return panel
    }
    
    private fun createTabbedPane(): JTabbedPane {
        val tabbedPane = JTabbedPane()
        
        tabbedPane.addTab("📊 Overview", createOverviewPanel())
        tabbedPane.addTab("🔧 Optimizations", createOptimizationsPanel())
        tabbedPane.addTab("🏷️ Tags & Usage", createTagsPanel())
        tabbedPane.addTab("📈 Performance", createPerformancePanel())
        
        return tabbedPane
    }
    
    private fun createOverviewPanel(): JPanel {
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
        val usagePanel = createUsageStatusPanel()
        panel.add(usagePanel)
        
        return panel
    }
    
    private fun createOptimizationsPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createEmptyBorder(16, 16, 16, 16)
        
        if (analytics.optimizationSuggestions.isEmpty()) {
            val noSuggestionsLabel = JLabel("No optimization suggestions available.")
            noSuggestionsLabel.horizontalAlignment = SwingConstants.CENTER
            noSuggestionsLabel.foreground = Color.GRAY
            panel.add(noSuggestionsLabel, BorderLayout.CENTER)
        } else {
            val listModel = DefaultListModel<String>()
            analytics.optimizationSuggestions.forEach { suggestion ->
                val priorityIcon = when (suggestion.priority) {
                    Priority.CRITICAL -> "🔴"
                    Priority.HIGH -> "🟠"
                    Priority.MEDIUM -> "🟡"
                    Priority.LOW -> "🟢"
                }
                listModel.addElement("$priorityIcon ${suggestion.description} (${suggestion.potentialSavings})")
            }
            
            val list = JList(listModel)
            list.selectionMode = ListSelectionModel.SINGLE_SELECTION
            list.cellRenderer = OptimizationListCellRenderer()
            
            val scrollPane = JScrollPane(list)
            panel.add(scrollPane, BorderLayout.CENTER)
            
            // Summary
            val summaryPanel = JPanel(FlowLayout(FlowLayout.LEFT))
            val summaryLabel = JLabel("${analytics.optimizationSuggestions.size} optimization suggestions found")
            summaryLabel.font = summaryLabel.font.deriveFont(Font.BOLD)
            summaryPanel.add(summaryLabel)
            panel.add(summaryPanel, BorderLayout.SOUTH)
        }
        
        return panel
    }
    
    private fun createTagsPanel(): JPanel {
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
    
    private fun createPerformancePanel(): JPanel {
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
    
    private fun createUsageStatusPanel(): JPanel {
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
            com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.UsageStatus.FREQUENTLY_USED -> Color(76, 175, 80)
            com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.UsageStatus.USED -> Color(139, 195, 74)
            com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.UsageStatus.RARELY_USED -> Color(255, 193, 7)
            com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.UsageStatus.UNUSED -> Color(158, 158, 158)
        }
    }
    
    private class OptimizationListCellRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            
            if (component is JLabel) {
                component.border = BorderFactory.createEmptyBorder(4, 8, 4, 8)
            }
            
            return component
        }
    }
} 