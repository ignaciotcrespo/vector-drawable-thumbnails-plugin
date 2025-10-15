package com.github.ignaciotcrespo.vectordrawablesthumbnails.ui

import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.VectorAnalyticsService
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.swing.*

/**
 * Lazy-loading panel for vector items that only generates images when visible.
 * Provides memory-efficient display for large collections.
 */
class LazyVectorItemPanel(
    private val vectorItem: VectorItem,
    private val project: Project,
    private val analyticsService: VectorAnalyticsService
) : JPanel() {
    
    private lateinit var imageLabel: JLabel
    private lateinit var nameLabel: JLabel
    private lateinit var infoLabel: JLabel
    private var isImageLoaded = false
    private var isVisible = false
    
    init {
        setupPanel()
        setupComponents()
        setupMouseListeners()
        setupVisibilityTracking()
    }
    
    private fun setupPanel() {
        layout = BorderLayout()
        // Don't set background - inherit from parent to use theme colors
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBColor.border(), 1),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        )
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    }
    
    private fun setupComponents() {
        // Image placeholder
        imageLabel = JLabel("Loading...", SwingConstants.CENTER)
        imageLabel.preferredSize = Dimension(120, 120)
        // Don't set background - inherit theme colors
        imageLabel.isOpaque = false
        imageLabel.border = BorderFactory.createLineBorder(JBColor.border())

        // Vector name
        nameLabel = JLabel(vectorItem.name, SwingConstants.CENTER)
        nameLabel.font = nameLabel.font.deriveFont(Font.BOLD, 10f)

        // File info
        val sizeKB = vectorItem.fileSize / 1024
        val complexityText = vectorItem.analytics?.complexityLevel?.name?.lowercase() ?: "unknown"
        infoLabel = JLabel("${sizeKB}KB • $complexityText", SwingConstants.CENTER)
        infoLabel.font = infoLabel.font.deriveFont(9f)
        // Don't set foreground - inherit theme colors
        
        // Layout components
        add(imageLabel, BorderLayout.CENTER)
        
        val textPanel = JPanel(BorderLayout())
        textPanel.isOpaque = false
        textPanel.add(nameLabel, BorderLayout.NORTH)
        textPanel.add(infoLabel, BorderLayout.SOUTH)
        add(textPanel, BorderLayout.SOUTH)
        
        // Analytics badge if available
        vectorItem.analytics?.let { analytics ->
            add(createAnalyticsBadge(analytics), BorderLayout.NORTH)
        }
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
        
        val complexityBadge = JLabel("●")
        complexityBadge.font = complexityBadge.font.deriveFont(8f)
        complexityBadge.foreground = complexityColor
        complexityBadge.toolTipText = "Complexity: ${analytics.complexityLevel.name.lowercase()}"
        panel.add(complexityBadge)
        
        return panel
    }
    
    private fun setupVisibilityTracking() {
        // Track when component becomes visible
        addComponentListener(object : ComponentAdapter() {
            override fun componentShown(e: ComponentEvent?) {
                checkAndLoadImage()
            }
        })
        
        // Also check on hierarchy changes (when added to visible parent)
        addHierarchyListener { e ->
            if (e.changeFlags and java.awt.event.HierarchyEvent.SHOWING_CHANGED.toLong() != 0L) {
                if (isShowing) {
                    checkAndLoadImage()
                }
            }
        }
    }
    
    private fun checkAndLoadImage() {
        if (!isImageLoaded && isDisplayable && isShowing) {
            loadImageAsync()
        }
    }
    
    private fun loadImageAsync() {
        if (isImageLoaded) return
        
        // Show loading state
        SwingUtilities.invokeLater {
            imageLabel.text = "Loading..."
            imageLabel.icon = null
        }
        
        // Load image in background thread
        Thread {
            try {
                // For now, we'll use the existing image since it's already loaded
                // In a future optimization, we could modify VectorItem to support lazy loading
                val image = vectorItem.image
                SwingUtilities.invokeLater {
                    imageLabel.icon = ImageIcon(image)
                    imageLabel.text = null
                    isImageLoaded = true
                    repaint()
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    imageLabel.text = "Error"
                    imageLabel.foreground = JBColor.RED
                    repaint()
                }
            }
        }.start()
    }
    
    private fun setupMouseListeners() {
        val mouseListener = object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 1) {
                    // Single click - open file
                    com.github.ignaciotcrespo.vectordrawablesthumbnails.utils.Utils.openValidFile(project, vectorItem.validFile)
                } else if (e.clickCount == 2) {
                    // Double click - show analytics (only for Vector Drawable files, not SVG)
                    if (!vectorItem.validFile.file.name.endsWith(".svg", ignoreCase = true)) {
                        showDetailedAnalytics()
                    }
                }
            }
            
            override fun mouseEntered(e: MouseEvent) {
                // Let the UI use default hover behavior
            }

            override fun mouseExited(e: MouseEvent) {
                // Let the UI use default hover behavior
            }
        }
        
        // Add mouse listener to this panel and all child components
        addMouseListener(mouseListener)
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
        // Always show the dialog - it will load analytics on-demand if needed
        val dialog = VectorAnalyticsDialog(
            SwingUtilities.getWindowAncestor(this), 
            vectorItem, 
            analyticsService, 
            project, 
            vectorItem.analytics // Pass existing analytics if available, null if not
        )
        dialog.isVisible = true
    }
    
    override fun getPreferredSize(): Dimension {
        return Dimension(160, 180)
    }
    
    override fun getMinimumSize(): Dimension {
        return Dimension(160, 180)
    }
    
    override fun getMaximumSize(): Dimension {
        return Dimension(160, 180)
    }
    
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        
        // Trigger image loading when component is painted and visible
        if (!isImageLoaded && isShowing) {
            checkAndLoadImage()
        }
    }
} 