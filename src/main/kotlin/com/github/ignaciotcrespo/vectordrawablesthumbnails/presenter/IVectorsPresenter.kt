package com.github.ignaciotcrespo.vectordrawablesthumbnails.presenter

import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import com.intellij.openapi.project.Project
import io.reactivex.Observable

interface IVectorsPresenter {
    fun refreshPropertiesData(project: Project?, delay: Boolean = true)
    fun filter(text: String?)
    fun sortBy2(sort: String)
    fun sortByDirection(direction: String)
    fun itemsFiltered(): ArrayList<VectorItem>
    fun onVectorClicked(project: Project, item: VectorItem)
    fun getPresenterEvents(): Observable<PresenterEvent>
}
