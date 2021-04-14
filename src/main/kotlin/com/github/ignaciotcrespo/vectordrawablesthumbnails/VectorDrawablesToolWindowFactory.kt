package com.github.ignaciotcrespo.vectordrawablesthumbnails

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.event.ItemEvent.SELECTED
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class VectorDrawablesToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val presenter = VectorsPresenter()
        val view = VectorDrawablesView()

        view.btnRefresh.addActionListener {
            view.panelVectors.removeAll()
            presenter.refreshPropertiesData(project, false)
        }
        view.textFilter.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                presenter.filter(view.textFilter.text)
                view.panelVectors.removeAll()
                showItems(presenter, project, view)
            }

            override fun removeUpdate(e: DocumentEvent?) {
                presenter.filter(view.textFilter.text)
                view.panelVectors.removeAll()
                showItems(presenter, project, view)
            }

            override fun changedUpdate(e: DocumentEvent) {
                presenter.filter(view.textFilter.text)
                view.panelVectors.removeAll()
                showItems(presenter, project, view)
            }

        })
        view.clearButton.addActionListener {
            view.textFilter.text = ""
        }
        view.radioSortName.addItemListener {
            if (it.stateChange == SELECTED) {
                presenter.sortBy(SortByItem.NAME)
                view.panelVectors.removeAll()
                showItems(presenter, project, view)
            }
        }
        view.radioSortUnsorted.addItemListener {
            if (it.stateChange == SELECTED) {
                presenter.sortBy(SortByItem.UNSORTED)
                view.panelVectors.removeAll()
                showItems(presenter, project, view)
            }
        }

        showContent(toolWindow, view.content)
        presenter.presenterEvents
            .ofType(VectorFoundPresenterEvent::class.java)
            .doOnNext { vector: VectorFoundPresenterEvent ->
            }
            .doOnComplete {
            }
            .subscribe()
        presenter.presenterEvents
            .ofType(VectorStatePresenterEvent::class.java)
            .doOnNext { event: VectorStatePresenterEvent ->
                if (event.state == VectorStatePresenterEvent.State.SEARCHING) {
                    view.btnRefresh.text = "Searching, please wait..."
                } else {
                    showItems(presenter, project, view)
                    view.btnRefresh.text = "Refresh"
                }
            }
            .subscribe()
        presenter.refreshPropertiesData(project)
    }

    private fun showItems(
        presenter: VectorsPresenter,
        project: Project,
        view: VectorDrawablesView
    ) {
        presenter.itemsFiltered().forEach { item ->
            val component = ImageIcon(item.image)
            val button = JButton(component)
            button.text = item.name
            button.horizontalTextPosition = JLabel.CENTER
            button.verticalTextPosition = JLabel.BOTTOM
            button.verticalAlignment = JLabel.BOTTOM
            button.addActionListener {
                presenter.onVectorClicked(
                    project,
                    item
                )
            }
            view.panelVectors.add(button)
        }
        view.panelVectors.revalidate()
    }

    private fun showContent(toolWindow: ToolWindow, panel: JPanel) {
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }

}