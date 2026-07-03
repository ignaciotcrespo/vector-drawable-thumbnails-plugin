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
 * Enhanced to prevent IDE freezing by deferring vector loading until tool window is shown.
 */
class VectorDrawablesToolWindowFactory : ToolWindowFactory {
    
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val dependencyContainer = DependencyContainer()
        val view = VectorDrawablesView()
        val controller = VectorUIController(
            view = view,
            vectorService = dependencyContainer.vectorService,
            analyticsService = dependencyContainer.analyticsService,
            vectorDrawableRepository = dependencyContainer.vectorDrawableRepository,
            svgRepository = dependencyContainer.svgRepository,
            rasterImageRepository = dependencyContainer.rasterImageRepository,
            project = project
        )
        
        // Initialize UI components but don't load vectors yet
        controller.initializeUI()
        
        // Add listener to load vectors only when tool window is first shown
        var hasLoadedVectors = false
        toolWindow.addContentManagerListener(object : com.intellij.ui.content.ContentManagerListener {
            override fun contentAdded(event: com.intellij.ui.content.ContentManagerEvent) {
                // Load vectors when content is first added and shown
                if (!hasLoadedVectors) {
                    hasLoadedVectors = true
                    // Delay loading slightly to ensure UI is fully initialized
                    javax.swing.SwingUtilities.invokeLater {
                        controller.loadVectorsWhenReady()
                    }
                }
            }
        })
        
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