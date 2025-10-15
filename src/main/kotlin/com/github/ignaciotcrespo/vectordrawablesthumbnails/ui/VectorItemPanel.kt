package com.github.ignaciotcrespo.vectordrawablesthumbnails.ui

import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.Priority
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import com.github.ignaciotcrespo.vectordrawablesthumbnails.utils.Utils
import com.intellij.openapi.project.Project
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

/**
 * Enhanced panel for displaying vector items with analytics information.
 * Provides a rich, professional display with hover effects and detailed info.
 */
class VectorItemPanel(
    private val vectorItem: VectorItem,
    private val project: Project
) : JPanel() {
    
    private var isHovered = false
    private val baseColor = Color(245, 245, 245)
    private val hoverColor = Color(230, 240, 250)
    private val borderColor = Color(200, 200, 200)
    
    init {
        setupPanel()
        setupMouseListeners()
    }
    
    private fun setupPanel() {
        layout = BorderLayout()
        background = baseColor
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 1),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        )
        
        // Main content
        add(createMainContent(), BorderLayout.CENTER)
        
        // Analytics badge
        vectorItem.analytics?.let { analytics ->
            add(createAnalyticsBadge(analytics), BorderLayout.NORTH)
        }
        
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    }
    
    private fun createMainContent(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.isOpaque = false
        
        // Vector image
        val imageLabel = JLabel(ImageIcon(vectorItem.image))
        imageLabel.horizontalAlignment = SwingConstants.CENTER
        panel.add(imageLabel, BorderLayout.CENTER)
        
        // Info panel
        val infoPanel = createInfoPanel()
        panel.add(infoPanel, BorderLayout.SOUTH)
        
        return panel
    }
    
    private fun createInfoPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.isOpaque = false
        
        // Name
        val nameLabel = JLabel(vectorItem.name)
        nameLabel.font = nameLabel.font.deriveFont(Font.BOLD, 12f)
        nameLabel.horizontalAlignment = SwingConstants.CENTER
        nameLabel.alignmentX = Component.CENTER_ALIGNMENT
        panel.add(nameLabel)
        
        // Size info
        val sizeLabel = JLabel(vectorItem.displaySize)
        sizeLabel.font = sizeLabel.font.deriveFont(10f)
        sizeLabel.foreground = Color.GRAY
        sizeLabel.horizontalAlignment = SwingConstants.CENTER
        sizeLabel.alignmentX = Component.CENTER_ALIGNMENT
        panel.add(sizeLabel)
        
        // File size
        val fileSizeLabel = JLabel(vectorItem.fileSizeFormatted)
        fileSizeLabel.font = fileSizeLabel.font.deriveFont(9f)
        fileSizeLabel.foreground = Color.GRAY
        fileSizeLabel.horizontalAlignment = SwingConstants.CENTER
        fileSizeLabel.alignmentX = Component.CENTER_ALIGNMENT
        panel.add(fileSizeLabel)
        
        // Tags (if available)
        vectorItem.analytics?.tags?.take(2)?.let { tags ->
            if (tags.isNotEmpty()) {
                val tagsLabel = JLabel(tags.joinToString(", "))
                tagsLabel.font = tagsLabel.font.deriveFont(8f)
                tagsLabel.foreground = Color(100, 100, 150)
                tagsLabel.horizontalAlignment = SwingConstants.CENTER
                tagsLabel.alignmentX = Component.CENTER_ALIGNMENT
                panel.add(tagsLabel)
            }
        }
        
        return panel
    }
    
    private fun createAnalyticsBadge(analytics: com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorAnalytics): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.RIGHT, 2, 2))
        panel.isOpaque = false
        
        // Complexity indicator
        val complexityColor = when (analytics.complexityLevel) {
            com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ComplexityLevel.SIMPLE -> Color(76, 175, 80)
            com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ComplexityLevel.MODERATE -> Color(255, 193, 7)
            com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ComplexityLevel.COMPLEX -> Color(255, 152, 0)
            com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ComplexityLevel.VERY_COMPLEX -> Color(244, 67, 54)
        }
        
        val complexityBadge = createBadge("●", complexityColor)
        complexityBadge.toolTipText = "Complexity: ${analytics.complexityLevel.name.lowercase()}"
        panel.add(complexityBadge)
        
        // Usage indicator
        val usageColor = when (analytics.usageStatus) {
            com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.UsageStatus.FREQUENTLY_USED -> Color(76, 175, 80)
            com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.UsageStatus.USED -> Color(139, 195, 74)
            com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.UsageStatus.RARELY_USED -> Color(255, 193, 7)
            com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.UsageStatus.UNUSED -> Color(158, 158, 158)
        }
        
        val usageBadge = createBadge("◆", usageColor)
        usageBadge.toolTipText = "Usage: ${analytics.usageStatus.name.lowercase().replace('_', ' ')}"
        panel.add(usageBadge)
        
        // Optimization indicator
        val highPriorityOptimizations = analytics.optimizationSuggestions.count { it.priority == Priority.HIGH || it.priority == Priority.CRITICAL }
        if (highPriorityOptimizations > 0) {
            val optimizationBadge = createBadge("⚠", Color(255, 152, 0))
            optimizationBadge.toolTipText = "$highPriorityOptimizations optimization suggestions"
            panel.add(optimizationBadge)
        }
        
        // Animation indicator
        if (analytics.hasAnimations) {
            val animationBadge = createBadge("▶", Color(33, 150, 243))
            animationBadge.toolTipText = "Contains animations"
            panel.add(animationBadge)
        }
        
        return panel
    }
    
    private fun createBadge(text: String, color: Color): JLabel {
        val badge = JLabel(text)
        badge.font = badge.font.deriveFont(10f)
        badge.foreground = color
        return badge
    }
    
    private fun setupMouseListeners() {
        val mouseListener = object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
//                println("VectorItemPanel: Mouse clicked on ${vectorItem.name}, clickCount=${e.clickCount}, analytics=${vectorItem.analytics != null}")

                if (e.clickCount == 1) {
//                    println("VectorItemPanel: Single click - opening file")
                    Utils.openValidFile(project, vectorItem.validFile)
                } else if (e.clickCount == 2) {
//                    println("VectorItemPanel: Double click - showing analytics")
                    // Only show analytics for Vector Drawable files (.xml), not SVG files
                    if (!vectorItem.validFile.file.name.endsWith(".svg", ignoreCase = true)) {
                        showDetailedAnalytics()
                    }
                }
            }
            
            override fun mouseEntered(e: MouseEvent) {
                isHovered = true
                background = hoverColor
                repaint()
            }
            
            override fun mouseExited(e: MouseEvent) {
                isHovered = false
                background = baseColor
                repaint()
            }
        }
        
        // Add mouse listener to this panel
        addMouseListener(mouseListener)
        
        // Add mouse listeners to all child components recursively
        addMouseListenersToAllComponents(this, mouseListener)
    }
    
    private fun addMouseListenersToAllComponents(component: Component, mouseListener: MouseAdapter) {
        if (component is Container) {
            for (child in component.components) {
                child.addMouseListener(mouseListener)
                if (child is Container) {
                    addMouseListenersToAllComponents(child, mouseListener)
                }
            }
        }
    }
    
    private fun showDetailedAnalytics() {
//        println("VectorItemPanel: showDetailedAnalytics called for ${vectorItem.name}")
        vectorItem.analytics?.let { analytics ->
//            println("VectorItemPanel: Analytics found, creating dialog")
            val dialog = VectorAnalyticsDialog(SwingUtilities.getWindowAncestor(this), vectorItem, analytics)
            dialog.isVisible = true
        } ?: run {
//            println("VectorItemPanel: No analytics available for ${vectorItem.name}")
            JOptionPane.showMessageDialog(
                this,
                "Analytics not available for this vector.",
                "No Analytics",
                JOptionPane.INFORMATION_MESSAGE
            )
        }
    }
    
    override fun getPreferredSize(): Dimension {
        return Dimension(180, 200)
    }
} 