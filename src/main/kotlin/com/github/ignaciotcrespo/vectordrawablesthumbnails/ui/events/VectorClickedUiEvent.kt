package com.github.ignaciotcrespo.vectordrawablesthumbnails.ui.events

import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import com.intellij.openapi.project.Project

class VectorClickedUiEvent(val project: Project, val item: VectorItem) : UiEvent