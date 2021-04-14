package com.github.ignaciotcrespo.vectordrawablesthumbnails

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

class VectorDrawablesToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val presenter = VectorsPresenter()
        val view = VectorDrawablesView()

        view.btnRefresh.addActionListener {
            view.panelVectors.removeAll()
            presenter.refreshPropertiesData(project)
        }

        showContent(toolWindow, view.content)
        presenter.presenterEvents
            .ofType(VectorFoundPresenterEvent::class.java)
            .doOnNext { vector: VectorFoundPresenterEvent ->
                val component = ImageIcon(vector.item!!.image)
                val button = JButton(component)
                button.text = vector.item.name
                button.horizontalTextPosition = JLabel.CENTER
                button.verticalTextPosition = JLabel.BOTTOM
                button.verticalAlignment = JLabel.BOTTOM
                button.addActionListener {
                    presenter.onVectorClicked(
                        project,
                        vector.item
                    )
                }
                view.panelVectors.add(button)
            }
            .subscribe()
        presenter.presenterEvents
            .ofType(VectorStatePresenterEvent::class.java)
            .doOnNext { event: VectorStatePresenterEvent ->
                if (event.state == VectorStatePresenterEvent.State.SEARCHING) {
                    view.btnRefresh.text = "Searching, please wait..."
                } else {
                    view.btnRefresh.text = "Refresh"
                }
            }
            .subscribe()
        presenter.refreshPropertiesData(project)
    }

    private fun showContent(toolWindow: ToolWindow, panel: JPanel) {
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }

}