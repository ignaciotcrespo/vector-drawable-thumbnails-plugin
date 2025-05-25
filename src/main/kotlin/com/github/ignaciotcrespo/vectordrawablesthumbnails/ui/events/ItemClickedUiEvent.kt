package com.github.ignaciotcrespo.vectordrawablesthumbnails.ui.events

import com.github.ignaciotcrespo.vectordrawablesthumbnails.ui.events.UiEvent
import com.intellij.openapi.project.Project

internal class ItemClickedUiEvent(val row: Int, val project: Project) : UiEvent