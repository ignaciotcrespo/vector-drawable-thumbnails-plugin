package com.github.ignaciotcrespo.vectordrawablesthumbnails.ui

import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.components.JBScrollPane
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class SwingVectorDrawablesView : VectorDrawablesView {

    private val btnDonate = JButton("Donate")
    private val btnRefresh = JButton("Refresh")
    private val textFilter = JTextField(20)
    private val clearButton = JButton("x")
    private val comboSort = JComboBox<String>()
    private val comboSortDirection = JComboBox<String>()
    private val panelFilter = JPanel(FlowLayout(FlowLayout.LEFT))
    private val panelVectors = JPanel()
    private val content = JPanel(BorderLayout())

    private var vectorClickedListener: ((item: VectorItem) -> Unit)? = null

    init {
        panelFilter.add(JLabel("Filter by name:"))
        panelFilter.add(textFilter)
        panelFilter.add(clearButton)
        panelFilter.add(JLabel("Sort by:"))
        panelFilter.add(comboSort)
        panelFilter.add(comboSortDirection)
        panelFilter.add(btnRefresh)
        panelFilter.add(btnDonate)

        content.add(panelFilter, BorderLayout.NORTH)
        content.add(JBScrollPane(panelVectors), BorderLayout.CENTER)

        panelVectors.layout = BoxLayout(panelVectors, BoxLayout.Y_AXIS)
        panelVectors.background = Color.WHITE
        panelVectors.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        comboSort.addItem("Name")
        comboSort.addItem("Module")
        comboSort.addItem("Size")
        comboSort.addItem("Category")
        comboSort.addItem("Added date")

        comboSortDirection.addItem("Ascending")
        comboSortDirection.addItem("Descending")
    }

    override fun displayItems(items: List<VectorItem>) {
        panelVectors.removeAll()
        if (items.isNotEmpty()) {
            val panelWidth = panelVectors.width - 20 // Account for padding
            val columns = maxOf(1, panelWidth / (items[0].viewportW + 20)) // +20 for padding between items
            val rows = (items.size + columns - 1) / columns

            panelVectors.layout = GridLayout(rows, columns, 10, 10) // 10px hgap and vgap

            items.forEach { item ->
                val icon = ImageIcon(item.image)
                val label = JLabel(icon, SwingConstants.CENTER)
                val text = JLabel(item.name, SwingConstants.CENTER)
                text.font = Font("Arial", Font.PLAIN, 10)

                val panel = JPanel()
                panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
                panel.add(label)
                panel.add(Box.createRigidArea(Dimension(0, 5))) // spacer
                panel.add(text)
                panel.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
                panel.isOpaque = false
                panel.toolTipText = "Click to open ${item.name}"

                panel.addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent?) {
                        vectorClickedListener?.invoke(item)
                    }
                })
                panelVectors.add(panel)
            }
        }
        panelVectors.revalidate()
        panelVectors.repaint()
    }

    override fun showLoading(isLoading: Boolean) {
        setRefreshButtonText(if (isLoading) "Searching, please wait..." else "Refresh")
        enableFilterControls(!isLoading)
    }

    override fun getFilterText(): String = textFilter.text

    override fun setFilterText(text: String) {
        textFilter.text = text
    }

    override fun getSelectedSortCriteria(): String? = comboSort.selectedItem?.toString()

    override fun getSelectedSortDirection(): String? = comboSortDirection.selectedItem?.toString()

    override fun addRefreshListener(listener: () -> Unit) {
        btnRefresh.addActionListener { listener() }
    }

    override fun addFilterChangeListener(listener: (filterText: String) -> Unit) {
        textFilter.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                listener(textFilter.text)
            }

            override fun removeUpdate(e: DocumentEvent?) {
                listener(textFilter.text)
            }

            override fun changedUpdate(e: DocumentEvent?) {
                listener(textFilter.text)
            }
        })
    }

    override fun addSortCriteriaListener(listener: (criteria: String) -> Unit) {
        comboSort.addActionListener { listener(comboSort.selectedItem.toString()) }
    }

    override fun addSortDirectionListener(listener: (direction: String) -> Unit) {
        comboSortDirection.addActionListener { listener(comboSortDirection.selectedItem.toString()) }
    }

    override fun addClearFilterListener(listener: () -> Unit) {
        clearButton.addActionListener { listener() }
    }

    override fun addDonateListener(listener: () -> Unit) {
        btnDonate.addActionListener { listener() }
    }

    override fun addVectorClickedListener(listener: (item: VectorItem) -> Unit) {
        this.vectorClickedListener = listener
    }

    override fun getContentPanel(): JPanel = content

    override fun enableFilterControls(enable: Boolean) {
        enableAll(panelFilter, enable)
    }

    override fun setRefreshButtonText(text: String) {
        btnRefresh.text = text
    }

    private fun enableAll(component: Component, enabled: Boolean) {
        component.isEnabled = enabled
        if (component is Container) {
            for (child in component.components) {
                enableAll(child, enabled)
            }
        }
    }
}
