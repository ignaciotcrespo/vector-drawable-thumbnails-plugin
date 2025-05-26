package com.github.ignaciotcrespo.vectordrawablesthumbnails

import com.github.ignaciotcrespo.vectordrawablesthumbnails.config.DependencyContainer
import com.github.ignaciotcrespo.vectordrawablesthumbnails.ui.VectorUIController
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * Tool window factory that creates the vector drawables tool window.
 * Refactored to follow SOLID principles:
 * - Single Responsibility: Only responsible for creating the tool window
 * - Dependency Inversion: Depends on abstractions through dependency injection
 */
class VectorDrawablesToolWindowFactory : ToolWindowFactory {
    
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val dependencyContainer = DependencyContainer()
        val view = VectorDrawablesView()
        val controller = VectorUIController(
            view = view,
            vectorService = dependencyContainer.vectorService,
            project = project
        )
        
        controller.initialize()
        showContent(toolWindow, view.content)
    }

    private fun showContent(toolWindow: ToolWindow, panel: javax.swing.JPanel) {
        val contentFactory = kotlin.runCatching { ContentFactory.getInstance() }
            .getOrNull()
        val content = contentFactory?.createContent(panel, "", false)
        content?.apply {
            toolWindow.contentManager.addContent(content)
        }
    }
}