package com.github.ignaciotcrespo.vectordrawablesthumbnails.view

import com.github.ignaciotcrespo.vectordrawablesthumbnails.VectorItem
import java.awt.*
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class SwingVectorDrawablesView : IVectorDrawablesView {

    val btnDonate: JButton = JButton("Donate")
    val btnRefresh: JButton = JButton("Refresh")
    val textFilter: JTextField = JTextField(20)
    val clearButton: JButton = JButton("x")
    val comboSort: JComboBox<String> = JComboBox(
        arrayOf("By Name", "By Width", "By Height", "By Width x Height", "By File Size")
    )
    val comboSortDirection: JComboBox<String> = JComboBox(arrayOf("Asc", "Desc"))
    val panelVectors: JPanel = JPanel()
    val panelFilter: JPanel = JPanel()
    private val content: JPanel = JPanel(BorderLayout())

    // Store the item click listener provided by the factory/presenter
    private var onItemClicked: ((VectorItem) -> Unit)? = null

    init {
        panelFilter.layout = FlowLayout()
        panelFilter.add(JLabel("Filter:"))
        panelFilter.add(textFilter)
        panelFilter.add(clearButton)
        panelFilter.add(JLabel("Sort by:"))
        panelFilter.add(comboSort)
        panelFilter.add(comboSortDirection)

        val topPanel = JPanel(BorderLayout())
        topPanel.add(panelFilter, BorderLayout.CENTER)
        topPanel.add(btnRefresh, BorderLayout.EAST)
        topPanel.add(btnDonate, BorderLayout.WEST)

        content.add(topPanel, BorderLayout.NORTH)
        content.add(JScrollPane(panelVectors), BorderLayout.CENTER)

        panelVectors.layout = FlowLayout(FlowLayout.LEFT)
    }

    override fun displayItems(items: List<VectorItem>, itemClickListener: (VectorItem) -> Unit) {
        clearItemsPanel() // Clear previous items
        this.onItemClicked = itemClickListener // Store the click listener

        items.forEach { item ->
            val component = ImageIcon(item.image)
            val itemPanel = JPanel() // Use itemPanel instead of 'button' for clarity
            itemPanel.layout = BorderLayout()
            itemPanel.toolTipText = "${item.name} (${item.viewportW}x${item.viewportH} - ${item.fileSize / 1024}KB)"


            val imageLabel = JLabel(component)
            imageLabel.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

            val nameLabel = JLabel(item.name).apply {
                horizontalAlignment = SwingConstants.CENTER
                font = font.deriveFont(Font.BOLD, 10f)
            }
            val detailsLabel = JLabel("${item.viewportW}x${item.viewportH} (${item.fileSize / 1024}KB)").apply {
                horizontalAlignment = SwingConstants.CENTER
                font = font.deriveFont(Font.PLAIN, 9f)
            }

            val textPanel = JPanel(GridLayout(2,1))
            textPanel.add(nameLabel)
            textPanel.add(detailsLabel)
            textPanel.border = BorderFactory.createEmptyBorder(0,5,5,5)

            itemPanel.add(imageLabel, BorderLayout.CENTER)
            itemPanel.add(textPanel, BorderLayout.SOUTH)

            itemPanel.border = BorderFactory.createEtchedBorder()
            itemPanel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

            itemPanel.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    onItemClicked?.invoke(item)
                }
            })
            panelVectors.add(itemPanel)
        }
        revalidateItemsPanel()
        repaintItemsPanel()
    }

    override fun clearItemsPanel() {
        panelVectors.removeAll()
    }

    override fun getFilterText(): String = textFilter.text

    override fun setFilterText(text: String) {
        textFilter.text = text
    }

    override fun getSelectedSortProperty(): String? = comboSort.selectedItem as? String

    override fun getSelectedSortDirection(): String? = comboSortDirection.selectedItem as? String

    override fun showLoading(isLoading: Boolean, message: String) {
        if (isLoading) {
            setRefreshButtonText(message)
        } else {
            // Assuming default refresh button text, or could be made configurable
            setRefreshButtonText("Refresh")
        }
        setFilterControlsEnabled(!isLoading)
        btnRefresh.isEnabled = !isLoading // Also disable refresh button itself during loading
    }


    override fun setRefreshButtonText(text: String) {
        btnRefresh.text = text
    }

    private fun setPanelEnabled(panel: JPanel, isEnabled: Boolean) {
        panel.isEnabled = isEnabled
        for (component in panel.components) {
            if (component is JPanel) {
                setPanelEnabled(component, isEnabled)
            } else {
                component.isEnabled = isEnabled
            }
        }
    }
    override fun setFilterControlsEnabled(enabled: Boolean) {
        setPanelEnabled(panelFilter, enabled)
    }

    override fun addRefreshButtonListener(action: () -> Unit) {
        btnRefresh.addActionListener { action() }
    }

    override fun addDonateButtonListener(action: () -> Unit) {
        btnDonate.addActionListener { action() }
    }

    override fun addFilterTextChangeListener(action: (newText: String) -> Unit) {
        textFilter.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                action(textFilter.text)
            }
            override fun removeUpdate(e: DocumentEvent?) {
                action(textFilter.text)
            }
            override fun changedUpdate(e: DocumentEvent?) {
                action(textFilter.text)
            }
        })
    }

    override fun addSortPropertyChangeListener(action: (selectedProperty: String) -> Unit) {
        comboSort.addActionListener {
            (comboSort.selectedItem as? String)?.let { action(it) }
        }
    }

    override fun addSortDirectionChangeListener(action: (selectedDirection: String) -> Unit) {
        comboSortDirection.addActionListener {
            (comboSortDirection.selectedItem as? String)?.let { action(it) }
        }
    }

    override fun addClearFilterButtonListener(action: () -> Unit) {
        clearButton.addActionListener { action() }
    }

    override fun getRootPanel(): JPanel = content

    override fun revalidateItemsPanel() {
        panelVectors.revalidate()
    }

    override fun repaintItemsPanel() {
        panelVectors.repaint()
    }
}
