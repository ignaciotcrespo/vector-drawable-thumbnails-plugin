package com.github.ignaciotcrespo.vectordrawablesthumbnails.ui

import java.awt.*
import javax.swing.JPanel

/**
 * A responsive grid layout that automatically calculates columns based on container width.
 * Prevents horizontal scrolling by ensuring items wrap to new rows.
 */
class ResponsiveGridLayout(
    private val itemWidth: Int = 160,
    private val itemHeight: Int = 180,
    private val hgap: Int = 8,
    private val vgap: Int = 8
) : LayoutManager {
    
    override fun addLayoutComponent(name: String?, comp: Component?) {
        // No-op
    }
    
    override fun removeLayoutComponent(comp: Component?) {
        // No-op
    }
    
    override fun preferredLayoutSize(parent: Container): Dimension {
        val insets = parent.insets
        val componentCount = parent.componentCount
        
        if (componentCount == 0) {
            return Dimension(insets.left + insets.right, insets.top + insets.bottom)
        }
        
        // Try to get the viewport width from the scroll pane ancestor
        val scrollPane = javax.swing.SwingUtilities.getAncestorOfClass(javax.swing.JScrollPane::class.java, parent) as? javax.swing.JScrollPane
        val viewportWidth = scrollPane?.viewport?.width ?: 0
        
        // Use viewport width if available and valid, otherwise use parent width or default
        val availableWidth = when {
            viewportWidth > 0 -> viewportWidth - insets.left - insets.right
            parent.width > 0 -> parent.width - insets.left - insets.right
            else -> 800 // Default width for initial layout
        }
        
        val columns = calculateColumns(availableWidth)
        val rows = calculateRows(componentCount, columns)
        
        // Always calculate width based on actual content, not parent width
        // This ensures proper scrolling behavior
        val width = columns * itemWidth + (columns - 1) * hgap + insets.left + insets.right
        
        val height = rows * itemHeight + (rows - 1) * vgap + insets.top + insets.bottom
        
        return Dimension(width, height)
    }
    
    override fun minimumLayoutSize(parent: Container): Dimension {
        return Dimension(itemWidth + parent.insets.left + parent.insets.right,
                        itemHeight + parent.insets.top + parent.insets.bottom)
    }
    
    override fun layoutContainer(parent: Container) {
        val insets = parent.insets
        
        // Try to get the viewport width from the scroll pane ancestor
        val scrollPane = javax.swing.SwingUtilities.getAncestorOfClass(javax.swing.JScrollPane::class.java, parent) as? javax.swing.JScrollPane
        val viewportWidth = scrollPane?.viewport?.width ?: 0
        
        // Use viewport width if available and valid, otherwise use parent width
        val availableWidth = if (viewportWidth > 0) {
            viewportWidth - insets.left - insets.right
        } else {
            parent.width - insets.left - insets.right
        }
        
        val columns = calculateColumns(availableWidth)
        
        var x = insets.left
        var y = insets.top
        var currentColumn = 0
        
        for (i in 0 until parent.componentCount) {
            val component = parent.getComponent(i)
            
            component.setBounds(x, y, itemWidth, itemHeight)
            
            currentColumn++
            if (currentColumn >= columns) {
                // Move to next row
                currentColumn = 0
                x = insets.left
                y += itemHeight + vgap
            } else {
                // Move to next column
                x += itemWidth + hgap
            }
        }
        
        // Force parent to use our preferred size for proper scrolling
        parent.preferredSize = preferredLayoutSize(parent)
    }
    
    private fun calculateColumns(availableWidth: Int): Int {
        if (availableWidth <= 0) return 1
        
        // Calculate how many items can fit in the available width
        val columns = (availableWidth + hgap) / (itemWidth + hgap)
        return maxOf(1, columns) // At least 1 column
    }
    
    private fun calculateRows(itemCount: Int, columns: Int): Int {
        if (itemCount == 0) return 0
        return (itemCount + columns - 1) / columns
    }
} 