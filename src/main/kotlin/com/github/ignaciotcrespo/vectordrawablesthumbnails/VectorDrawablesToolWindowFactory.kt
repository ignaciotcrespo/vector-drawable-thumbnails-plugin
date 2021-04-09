package com.github.ignaciotcrespo.vectordrawablesthumbnails

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import java.awt.GridLayout
import java.awt.event.ActionEvent
import javax.swing.*

class VectorDrawablesToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val presenter = VectorsPresenter()
        val panel = createPanel()
        val panelVectors = JPanel()
        panelVectors.layout = GridLayout(0, 3)
        val buttonRefresh = createRefreshButton(project, presenter, panel, panelVectors)

//        JLabel labelState = new JLabel("");
//        labelState.setHorizontalAlignment(JLabel.CENTER);
//        labelState.setVerticalAlignment(JLabel.CENTER);
//        panel.add(labelState);
        val vectorsContainer: JScrollPane = JBScrollPane(panelVectors)
        vectorsContainer.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        vectorsContainer.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        vectorsContainer.layout = ScrollPaneLayout()
        panel.add(vectorsContainer)
        showContent(toolWindow, panel)
        presenter.presenterEvents
            .ofType(VectorFoundPresenterEvent::class.java)
            .doOnNext { vector: VectorFoundPresenterEvent ->
                val component = ImageIcon(vector.item!!.image)
                val button = JButton(component)
                button.text = vector.item.name
                button.horizontalTextPosition = JLabel.CENTER
                button.verticalTextPosition = JLabel.BOTTOM
                button.verticalAlignment = JLabel.BOTTOM
                button.addActionListener { actionEvent: ActionEvent? ->
                    presenter.onVectorClicked(
                        project,
                        vector.item
                    )
                }
                panelVectors.add(button)
            }
            .subscribe()
        presenter.presenterEvents
            .ofType(VectorStatePresenterEvent::class.java)
            .doOnNext { event: VectorStatePresenterEvent ->
                if (event.state == VectorStatePresenterEvent.State.SEARCHING) {
                    buttonRefresh.text = "Searching, please wait..."
                } else {
                    buttonRefresh.text = "Refresh"
                }
            }
            .subscribe()
        presenter.refreshPropertiesData(project)
    }

    private fun createPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.PAGE_AXIS)
        return panel
    }

    private fun showContent(toolWindow: ToolWindow, panel: JPanel) {
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    private fun createRefreshButton(
        project: Project,
        presenter: VectorsPresenter,
        panel: JPanel,
        panelVectors: JPanel
    ): JButton {
        val button = JButton()
        button.text = "Refresh"
        button.alignmentX = JButton.CENTER_ALIGNMENT
        button.addActionListener { _: ActionEvent? ->
            panelVectors.removeAll()
            presenter.refreshPropertiesData(project)
        }
        panel.add(button)
        return button
    }
}