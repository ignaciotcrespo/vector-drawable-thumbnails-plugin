package com.github.ignaciotcrespo.vectordrawablesthumbnails.ui

import com.github.ignaciotcrespo.vectordrawablesthumbnails.VectorDrawablesView
import com.github.ignaciotcrespo.vectordrawablesthumbnails.application.VectorService
import com.github.ignaciotcrespo.vectordrawablesthumbnails.application.VectorServiceState
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.SortCriteria
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.SortDirection
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import com.github.ignaciotcrespo.vectordrawablesthumbnails.utils.Utils
import com.intellij.openapi.project.Project
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.awt.BorderLayout
import java.awt.Desktop
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.net.URL
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * UI Controller that manages the interaction between the view and the service.
 * Follows the Single Responsibility Principle by focusing only on UI coordination.
 * Follows the Dependency Inversion Principle by depending on abstractions.
 */
class VectorUIController(
    private val view: VectorDrawablesView,
    private val vectorService: VectorService,
    private val project: Project
) {
    
    private val disposables = CompositeDisposable()
    
    fun initialize() {
        println("VectorUIController: Initializing...")
        println("VectorUIController: btnRefresh = ${view.btnRefresh}")
        println("VectorUIController: panelVectors = ${view.panelVectors}")
        println("VectorUIController: textFilter = ${view.textFilter}")
        setupEventListeners()
        subscribeToServiceState()
        loadVectors()
        println("VectorUIController: Initialization complete")
    }
    
    fun dispose() {
        disposables.clear()
    }
    
    private fun setupEventListeners() {
        setupDonateButton()
        setupRefreshButton()
        setupFilterField()
        setupClearButton()
        setupSortControls()
    }
    
    private fun setupDonateButton() {
        view.btnDonate.addActionListener {
            try {
                Desktop.getDesktop().browse(URL("https://paypal.me/itcrespo").toURI())
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
            override fun insertUpdate(e: DocumentEvent?) = updateFilter()
            override fun removeUpdate(e: DocumentEvent?) = updateFilter()
            override fun changedUpdate(e: DocumentEvent) = updateFilter()
            
            private fun updateFilter() {
                vectorService.updateFilter(view.textFilter.text)
                updateVectorDisplay()
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
        println("Error loading vectors: ${throwable.message}")
        throwable.printStackTrace()
    }
    
    private fun updateVectorDisplay() {
        val items = vectorService.getFilteredAndSortedVectors()
        println("VectorUIController: Updating display with ${items.size} items")
        displayVectors(items)
    }
    
    private fun displayVectors(items: List<VectorItem>) {
        println("VectorUIController: Displaying ${items.size} vectors")
        view.panelVectors.removeAll()
        items.forEach { item ->
            val component = ImageIcon(item.image)
            val button = createVectorButton(component, item)
            view.panelVectors.add(button)
        }
        view.panelVectors.revalidate()
        view.panelVectors.repaint()
        println("VectorUIController: Display update complete")
    }
    
    private fun createVectorButton(icon: ImageIcon, item: VectorItem): JPanel {
        val button = JPanel()
        button.layout = BorderLayout()
        button.add(BorderLayout.NORTH, JPanel().also { jpanel ->
            jpanel.layout = BorderLayout()
            jpanel.add(BorderLayout.NORTH, JLabel(icon))
            jpanel.add(BorderLayout.SOUTH, JPanel().apply {
                layout = BorderLayout()
                add(BorderLayout.NORTH, JLabel(item.name).apply {
                    horizontalAlignment = SwingConstants.CENTER
                })
                add(BorderLayout.SOUTH, JLabel("${item.viewportW} x ${item.viewportH}").apply {
                    horizontalAlignment = SwingConstants.CENTER
                })
            })
        })
        
        button.addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent?) {
                Utils.openValidFile(project, item.validFile)
            }
            override fun mousePressed(e: MouseEvent?) {}
            override fun mouseReleased(e: MouseEvent?) {}
            override fun mouseEntered(e: MouseEvent?) {}
            override fun mouseExited(e: MouseEvent?) {}
        })
        
        return button
    }
    
    private fun mapSortStringToCriteria(sortString: String): SortCriteria {
        return when (sortString) {
            "By Name" -> SortCriteria.BY_NAME
            "By Width" -> SortCriteria.BY_WIDTH
            "By Height" -> SortCriteria.BY_HEIGHT
            "By Width x Height" -> SortCriteria.BY_AREA
            "By File Size" -> SortCriteria.BY_FILE_SIZE
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
        println("VectorUIController: Starting to load vectors...")
        val disposable = vectorService.loadVectors(project)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.computation())
            .subscribe(
                { vectorItem ->
                    // Vector item loaded successfully
                    println("VectorUIController: Loaded vector: ${vectorItem.name}")
                },
                { error ->
                    println("VectorUIController: Error loading vector: ${error.message}")
                    error.printStackTrace()
                },
                {
                    // Loading completed
                    println("VectorUIController: Vector loading completed")
                }
            )
        disposables.add(disposable)
    }
} 