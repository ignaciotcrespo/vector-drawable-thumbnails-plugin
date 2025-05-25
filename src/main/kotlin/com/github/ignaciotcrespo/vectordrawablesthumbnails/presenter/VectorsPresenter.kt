package com.github.ignaciotcrespo.vectordrawablesthumbnails.presenter

import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.ValidFile
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import com.github.ignaciotcrespo.vectordrawablesthumbnails.parser.IVectorDrawableParser
import com.github.ignaciotcrespo.vectordrawablesthumbnails.parser.VectorDrawableParser
import com.github.ignaciotcrespo.vectordrawablesthumbnails.scanners.IProjectFileScanner
import com.github.ignaciotcrespo.vectordrawablesthumbnails.scanners.ProjectFileScanner
import com.github.ignaciotcrespo.vectordrawablesthumbnails.ui.events.RefreshUiEvent
import com.github.ignaciotcrespo.vectordrawablesthumbnails.ui.events.UiEvent
import com.github.ignaciotcrespo.vectordrawablesthumbnails.ui.events.VectorClickedUiEvent
import com.github.ignaciotcrespo.vectordrawablesthumbnails.utils.RxUtils
import com.github.ignaciotcrespo.vectordrawablesthumbnails.utils.Utils
import com.intellij.openapi.project.Project
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

internal class VectorsPresenter(
    private val projectFileScanner: IProjectFileScanner,
    private val vectorDrawableParser: IVectorDrawableParser
) : IVectorsPresenter {
    private var sortDirection: String? = null
    private var sort: String? = null
    private var filterText: String? = null
    private val uiEvents = PublishSubject.create<UiEvent>()
    override val presenterEvents = PublishSubject.create<PresenterEvent>()
    private var state = VectorStatePresenterEvent.State.IDLE
    private val items = ArrayList<VectorItem>()

    override fun refreshPropertiesData(project: Project?, delay: Boolean) {
        project?.let {
            uiEvents.onNext(RefreshUiEvent(it, delay))
        }
    }

    private fun refresh(project: Project, delay: Boolean) {
        if (setState(VectorStatePresenterEvent.State.SEARCHING)) {
            items.clear()
            projectFileScanner.findXmlFiles(project)
                .delay(if (delay) 2 else 0, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.newThread())
                .flatMap { validFile -> vectorDrawableParser.parseVector(validFile) }
                .doOnNext { vectorItem: VectorItem? ->
                    vectorItem?.let {
                        items.add(it)
                    }
                }
                .doOnComplete { setState(VectorStatePresenterEvent.State.IDLE) }
                .doOnError {
                    // Log error appropriately
                    println("Error during refresh: ${it.message}")
                    setState(VectorStatePresenterEvent.State.IDLE)
                }
                .subscribe()
        }
    }

    private fun setState(state: VectorStatePresenterEvent.State): Boolean {
        if (this.state != state) {
            this.state = state
            presenterEvents.onNext(VectorStatePresenterEvent(state))
            return true
        }
        return false
    }

    override fun getPresenterEvents(): Observable<PresenterEvent> {
        return presenterEvents
    }

    override fun onVectorClicked(project: Project, item: VectorItem) {
        uiEvents.onNext(VectorClickedUiEvent(project, item))
    }

    override fun filter(text: String?) {
        this.filterText = text?.toLowerCase()
    }

    override fun itemsFiltered(): java.util.ArrayList<VectorItem> = when {
        filterText.isNullOrEmpty() -> ArrayList(items)
        else -> ArrayList(items.filter { it.name.toLowerCase().contains(filterText!!) }.toList())
    }.also {
        when (sortDirection) {
            "Desc" -> {
                when (sort) {
                    "By Name" -> it.sortByDescending { it.name }
                    "By Width" -> it.sortByDescending { it.viewportW }
                    "By Height" -> it.sortByDescending { it.viewportH }
                    "By Width x Height" -> it.sortByDescending { it.viewportW * it.viewportH }
                    "By File Size" -> it.sortByDescending { it.fileSize }
                }
            }
            else -> {
                when (sort) {
                    "By Name" -> it.sortBy { it.name }
                    "By Width" -> it.sortBy { it.viewportW }
                    "By Height" -> it.sortBy { it.viewportH }
                    "By Width x Height" -> it.sortBy { it.viewportW * it.viewportH }
                    "By File Size" -> it.sortBy { it.fileSize }
                }
            }
        }
    }

    override fun sortBy2(sort: String) {
        this.sort = sort
    }

    override fun sortByDirection(direction: String) {
        this.sortDirection = direction
    }

    init {
        uiEvents
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .ofType(RefreshUiEvent::class.java)
            .compose(RxUtils.avoidFastClicks())
            .retry()
            .doOnError { x: Throwable? -> println(x) }
            .subscribe { ui: RefreshUiEvent -> refresh(ui.project, ui.delay) }
        uiEvents
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .ofType(VectorClickedUiEvent::class.java)
            .compose(RxUtils.avoidFastClicks())
            .retry()
            .doOnError { x: Throwable? -> println(x) }
            .subscribe { ui: VectorClickedUiEvent -> Utils.openValidFile(ui.project, ui.item.validFile) }
    }
}