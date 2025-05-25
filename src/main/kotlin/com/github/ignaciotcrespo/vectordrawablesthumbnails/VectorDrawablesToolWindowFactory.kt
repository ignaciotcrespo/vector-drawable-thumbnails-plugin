package com.github.ignaciotcrespo.vectordrawablesthumbnails

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.Desktop
import java.net.URL
import javax.swing.JPanel
import com.github.ignaciotcrespo.vectordrawablesthumbnails.filter.VectorItemNameFilter
import com.github.ignaciotcrespo.vectordrawablesthumbnails.sorter.VectorItemPropertySorter
import com.github.ignaciotcrespo.vectordrawablesthumbnails.view.IVectorDrawablesView
import com.github.ignaciotcrespo.vectordrawablesthumbnails.view.SwingVectorDrawablesView
import io.reactivex.schedulers.Schedulers

class VectorDrawablesToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val view: IVectorDrawablesView = SwingVectorDrawablesView()
        val presenter = VectorsPresenter(
            ProjectVectorFileProvider(),
            XmlVectorAttributeParser(),
            VdPreviewRenderer(),
            VectorItemNameFilter(),
            VectorItemPropertySorter(),
            Schedulers.io(),
            Schedulers.newThread() // Consider Schedulers.computation() for CPU-bound tasks if newThread() is too heavy
        )

        view.addDonateButtonListener {
            try {
                Desktop.getDesktop().browse(URL("https://paypal.me/itcrespo").toURI())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        view.addRefreshButtonListener {
            presenter.refreshPropertiesData(project, false)
        }

        view.addFilterTextChangeListener { newText ->
            presenter.filter(newText)
            showItemsBasedOnPresenterState(presenter, project, view)
        }

        view.addClearFilterButtonListener {
            view.setFilterText("") // This will trigger the filter text change listener
        }

        view.addSortPropertyChangeListener { selectedProperty ->
            presenter.sortBy2(selectedProperty)
            showItemsBasedOnPresenterState(presenter, project, view)
        }

        view.addSortDirectionChangeListener { selectedDirection ->
            presenter.sortByDirection(selectedDirection)
            showItemsBasedOnPresenterState(presenter, project, view)
        }

        // Subscribe to presenter events to update the view
        presenter.presenterEvents
            .ofType(VectorFoundPresenterEvent::class.java)
            .subscribe {
                // Individual item found - could update view incrementally
                // For now, full refresh on IDLE state handles this.
            }

        presenter.presenterEvents
            .ofType(VectorStatePresenterEvent::class.java)
            .subscribe { event ->
                when (event.state) {
                    VectorStatePresenterEvent.State.SEARCHING -> {
                        view.showLoading(true, "Searching, please wait...")
                    }
                    VectorStatePresenterEvent.State.IDLE -> {
                        view.showLoading(false, "Refresh") // Reset button text
                        showItemsBasedOnPresenterState(presenter, project, view)
                    }
                }
            }

        showContent(toolWindow, view.getRootPanel())
        presenter.refreshPropertiesData(project) // Initial refresh
    }

    private fun showItemsBasedOnPresenterState(
        presenter: VectorsPresenter,
        project: Project,
        view: IVectorDrawablesView
    ) {
        val itemsToDisplay = presenter.itemsFiltered()
        view.displayItems(itemsToDisplay) { clickedItem ->
            presenter.onVectorClicked(project, clickedItem)
        }
        // revalidate and repaint are called within SwingVectorDrawablesView.displayItems
    }

    private fun showContent(toolWindow: ToolWindow, panel: JPanel) {
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}