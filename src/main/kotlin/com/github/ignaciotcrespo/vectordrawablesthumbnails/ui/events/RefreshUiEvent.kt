package com.github.ignaciotcrespo.vectordrawablesthumbnails.ui.events

import com.github.ignaciotcrespo.vectordrawablesthumbnails.ui.events.UiEvent
import com.intellij.openapi.project.Project

class RefreshUiEvent(var project: Project, val delay: Boolean = true) : UiEvent