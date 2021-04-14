package com.github.ignaciotcrespo.vectordrawablesthumbnails

import com.intellij.openapi.project.Project

class RefreshUiEvent(var project: Project, val delay: Boolean = true) : UiEvent