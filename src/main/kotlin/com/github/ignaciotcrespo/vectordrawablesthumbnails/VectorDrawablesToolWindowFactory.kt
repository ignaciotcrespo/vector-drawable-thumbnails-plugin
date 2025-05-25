package com.github.ignaciotcrespo.vectordrawablesthumbnails

import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import com.github.ignaciotcrespo.vectordrawablesthumbnails.parser.VectorDrawableParser
import com.github.ignaciotcrespo.vectordrawablesthumbnails.presenter.IVectorsPresenter
import com.github.ignaciotcrespo.vectordrawablesthumbnails.presenter.VectorFoundPresenterEvent
import com.github.ignaciotcrespo.vectordrawablesthumbnails.presenter.VectorStatePresenterEvent
import com.github.ignaciotcrespo.vectordrawablesthumbnails.presenter.VectorsPresenter
import com.github.ignaciotcrespo.vectordrawablesthumbnails.scanners.ProjectFileScanner
import com.github.ignaciotcrespo.vectordrawablesthumbnails.ui.SwingVectorDrawablesView
import com.github.ignaciotcrespo.vectordrawablesthumbnails.ui.VectorDrawablesView
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URL
import javax.swing.JPanel

class VectorDrawablesToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val projectFileScanner = ProjectFileScanner()
        val vectorDrawableParser = VectorDrawableParser()
        val presenter: IVectorsPresenter = VectorsPresenter(projectFileScanner, vectorDrawableParser)
        val view: VectorDrawablesView = SwingVectorDrawablesView()

        view.addDonateListener {
            try {
                Desktop.getDesktop().browse(URL("https://paypal.me/itcrespo").toURI())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        view.addRefreshListener {
            presenter.refreshPropertiesData(project, false)
        }
        view.addFilterChangeListener { currentText ->
            presenter.filter(currentText)
            showItems(presenter, project, view)
        }
        view.addClearFilterListener {
            view.setFilterText("")
        }
        view.addSortCriteriaListener { selectedSort ->
            presenter.sortBy2(selectedSort)
            showItems(presenter, project, view)
        }
        view.addSortDirectionListener { selectedDirection ->
            presenter.sortByDirection(selectedDirection)
            showItems(presenter, project, view)
        }
        view.addVectorClickedListener { item ->
            presenter.onVectorClicked(project, item)
        }

        showContent(toolWindow, view.getContentPanel())
        presenter.presenterEvents
            .ofType(VectorStatePresenterEvent::class.java)
            .doOnNext { event: VectorStatePresenterEvent ->
                if (event.state == VectorStatePresenterEvent.State.SEARCHING) {
                    view.showLoading(true)
                } else {
                    view.showLoading(false)
                    showItems(presenter, project, view)
                }
            }
            .subscribe()
        presenter.refreshPropertiesData(project)
    }

    private fun showItems(
        presenter: com.github.ignaciotcrespo.vectordrawablesthumbnails.presenter.IVectorsPresenter,
        project: com.intellij.openapi.project.Project,
        view: com.github.ignaciotcrespo.vectordrawablesthumbnails.ui.VectorDrawablesView
    ) {
        GlobalScope.launch(Dispatchers.Default) {
            val items = presenter.itemsFiltered()
            // The second showItems method was removed in a previous refactoring,
            // this call directly updates the view.
            GlobalScope.launch(Dispatchers.Main) {
                view.displayItems(items)
            }
        }
    }

    private fun showContent(toolWindow: ToolWindow, panel: JPanel) {
        val contentFactory = kotlin.runCatching { ContentFactory.getInstance() }
            .getOrNull() ?: ContentFactory.SERVICE.getInstance() // Fallback for older versions
        val content = contentFactory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }

}