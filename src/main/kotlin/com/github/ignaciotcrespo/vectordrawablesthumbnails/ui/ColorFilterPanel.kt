package com.github.ignaciotcrespo.vectordrawablesthumbnails.ui

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.LineBorder

/**
 * A panel that displays color swatches for filtering vectors by color.
 * Supports multiple color selection and shows color frequency.
 */
class ColorFilterPanel : JPanel() {
    
    private val colorPanels = mutableMapOf<String, ColorSwatch>()
    private val selectedColors = mutableSetOf<String>()
    private var colorSelectionListener: ((Set<String>) -> Unit)? = null
    
    init {
        layout = FlowLayout(FlowLayout.LEFT, 5, 5)
        background = Color.WHITE
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Filter by Color"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        )
        
        // Add component listener to refresh when becoming visible
        addComponentListener(object : java.awt.event.ComponentAdapter() {
            override fun componentShown(e: java.awt.event.ComponentEvent) {
                if (components.isEmpty()) {
                    // If no components, show at least the Clear button
                    updateColors(emptyMap())
                }
            }
        })
    }
    
    /**
     * Updates the color palette with colors and their frequencies.
     */
    fun updateColors(colorFrequencies: Map<String, Int>) {
        removeAll()
        colorPanels.clear()
        
        // Add "Clear" button
        val clearButton = JButton("Clear")
        clearButton.preferredSize = Dimension(60, 30)
        clearButton.addActionListener {
            clearSelection()
        }
        add(clearButton)
        
        // Group colors by hue similarity
        val groupedColors = groupColorsByHue(colorFrequencies)
        
        // Add color swatches grouped by hue
        groupedColors.forEach { (colorHex, frequency) ->
            val swatch = ColorSwatch(colorHex, frequency)
            swatch.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    toggleColorSelection(colorHex, swatch)
                }
            })
            colorPanels[colorHex] = swatch
            
            // Restore selected state if this color was previously selected
            if (selectedColors.contains(colorHex)) {
                swatch.setSelected(true)
            }
            
            add(swatch)
        }
        
        revalidate()
        repaint()
        
        // Force immediate update if visible
        if (isShowing) {
            SwingUtilities.invokeLater {
                revalidate()
                parent?.revalidate()
            }
        }
    }
    
    /**
     * Groups colors by hue similarity and sorts by frequency within each group.
     */
    private fun groupColorsByHue(colorFrequencies: Map<String, Int>): List<Map.Entry<String, Int>> {
        // Convert colors to HSB and group by hue
        val colorWithHSB = colorFrequencies.entries.map { entry ->
            val color = parseColor(entry.key)
            val hsb = FloatArray(3)
            Color.RGBtoHSB(color.red, color.green, color.blue, hsb)
            Triple(entry, hsb[0], hsb[1]) // entry, hue, saturation
        }
        
        // Sort by hue, then by saturation (grayscale last), then by frequency
        return colorWithHSB.sortedWith(compareBy(
            { if (it.third < 0.1f) 360f else it.second * 360f }, // Grayscale colors last
            { -it.third }, // Higher saturation first
            { -it.first.value } // Higher frequency first
        )).map { it.first }
    }
    
    private fun parseColor(colorHex: String): Color {
        return try {
            when (colorHex.length) {
                7 -> Color.decode(colorHex)
                9 -> {
                    val alpha = Integer.parseInt(colorHex.substring(1, 3), 16)
                    val rgb = Integer.parseInt(colorHex.substring(3), 16)
                    Color(rgb).let { Color(it.red, it.green, it.blue, alpha) }
                }
                else -> Color.BLACK
            }
        } catch (e: Exception) {
            Color.BLACK
        }
    }
    
    /**
     * Sets the listener for color selection changes.
     */
    fun setColorSelectionListener(listener: (Set<String>) -> Unit) {
        colorSelectionListener = listener
    }
    
    private fun toggleColorSelection(colorHex: String, swatch: ColorSwatch) {
        if (selectedColors.contains(colorHex)) {
            selectedColors.remove(colorHex)
            swatch.setSelected(false)
        } else {
            selectedColors.add(colorHex)
            swatch.setSelected(true)
        }
        colorSelectionListener?.invoke(selectedColors)
    }
    
    private fun clearSelection() {
        selectedColors.clear()
        colorPanels.values.forEach { it.setSelected(false) }
        colorSelectionListener?.invoke(selectedColors)
    }
    
    /**
     * Inner class representing a single color swatch.
     */
    private class ColorSwatch(val colorHex: String, val frequency: Int) : JPanel() {
        private var isSelected = false
        private val color = try {
            // Handle both RGB (#RRGGBB) and ARGB (#AARRGGBB) formats
            when (colorHex.length) {
                7 -> Color.decode(colorHex) // #RRGGBB
                9 -> {
                    // #AARRGGBB - Extract RGB part and create color with alpha
                    val alpha = Integer.parseInt(colorHex.substring(1, 3), 16)
                    val rgb = Integer.parseInt(colorHex.substring(3), 16)
                    Color(rgb).let { Color(it.red, it.green, it.blue, alpha) }
                }
                else -> Color.BLACK
            }
        } catch (e: Exception) {
            Color.BLACK
        }
        
        init {
            preferredSize = Dimension(40, 40)
            toolTipText = "$colorHex (used ${frequency}x)"
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            border = LineBorder(Color.GRAY, 1)
        }
        
        fun setSelected(selected: Boolean) {
            isSelected = selected
            if (selected) {
                // Extremely visible selection with animation-like effect
                border = BorderFactory.createCompoundBorder(
                    LineBorder(Color(0, 120, 215), 4), // Thick bright blue
                    LineBorder(Color.WHITE, 2) // White inner border for contrast
                )
                background = Color(230, 240, 255) // Light blue background
                preferredSize = Dimension(50, 50) // Slightly larger when selected
            } else {
                border = LineBorder(Color.GRAY, 1)
                background = parent?.background ?: Color.WHITE
                preferredSize = Dimension(40, 40) // Normal size
            }
            revalidate()
            repaint()
        }
        
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2d = g as Graphics2D
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            
            // Draw color fill with margin for selected state
            val margin = if (isSelected) 8 else 2
            g2d.color = color
            g2d.fillRect(margin, margin, width - 2 * margin, height - 2 * margin)
            
            // Draw frequency label
            g2d.color = if (isLightColor(color)) Color.BLACK else Color.WHITE
            g2d.font = Font(Font.SANS_SERIF, Font.BOLD, 10)
            val frequencyText = if (frequency > 99) "99+" else frequency.toString()
            val metrics = g2d.fontMetrics
            val textX = (width - metrics.stringWidth(frequencyText)) / 2
            val textY = height - margin - 4
            g2d.drawString(frequencyText, textX, textY)
            
            // Draw very prominent checkmark if selected
            if (isSelected) {
                // Draw large checkmark with shadow
                val checkSize = width / 3
                val checkX = width - checkSize - 4
                val checkY = 4
                
                // Shadow
                g2d.color = Color(0, 0, 0, 128)
                g2d.stroke = BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                g2d.drawLine(checkX + 1, checkY + checkSize/2 + 1, checkX + checkSize/3 + 1, checkY + checkSize - 2 + 1)
                g2d.drawLine(checkX + checkSize/3 + 1, checkY + checkSize - 2 + 1, checkX + checkSize + 1, checkY + 2 + 1)
                
                // White checkmark
                g2d.color = Color.WHITE
                g2d.stroke = BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                g2d.drawLine(checkX, checkY + checkSize/2, checkX + checkSize/3, checkY + checkSize - 2)
                g2d.drawLine(checkX + checkSize/3, checkY + checkSize - 2, checkX + checkSize, checkY + 2)
            }
        }
        
        private fun isLightColor(color: Color): Boolean {
            val brightness = (color.red * 0.299 + color.green * 0.587 + color.blue * 0.114)
            return brightness > 128
        }
    }
}