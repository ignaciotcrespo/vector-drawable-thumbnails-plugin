package com.github.ignaciotcrespo.vectordrawablethumbnailsplugin.services

import com.github.ignaciotcrespo.vectordrawablethumbnailsplugin.MyBundle
import com.intellij.openapi.project.Project

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
