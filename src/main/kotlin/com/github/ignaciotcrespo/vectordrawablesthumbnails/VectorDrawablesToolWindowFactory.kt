package com.github.ignaciotcrespo.vectordrawablesthumbnails

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import java.awt.event.ItemEvent.SELECTED
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
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
            val button = JPanel()
            button.layout = BorderLayout()
            button.add(BorderLayout.NORTH, JLabel(component))
            button.add(BorderLayout.CENTER, JLabel(item.name).also { it.horizontalAlignment = SwingConstants.CENTER })
            button.add(
                BorderLayout.SOUTH,
                JLabel("${item.viewportW} x ${item.viewportH}").also { it.horizontalAlignment = SwingConstants.CENTER })
            button.addMouseListener(object : MouseListener {
                override fun mouseClicked(e: MouseEvent?) {
                    presenter.onVectorClicked(
                        project,
                        item
                    )
                }

                override fun mousePressed(e: MouseEvent?) {
                }

                override fun mouseReleased(e: MouseEvent?) {
                }

                override fun mouseEntered(e: MouseEvent?) {
                }

                override fun mouseExited(e: MouseEvent?) {
                }
            })
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