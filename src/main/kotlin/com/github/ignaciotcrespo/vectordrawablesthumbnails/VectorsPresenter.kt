package com.github.ignaciotcrespo.vectordrawablesthumbnails

import com.intellij.openapi.project.Project
import io.reactivex.Emitter
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.io.FileInputStream
import java.util.concurrent.TimeUnit

import com.github.ignaciotcrespo.vectordrawablesthumbnails.interfaces.ItemFilter
import com.github.ignaciotcrespo.vectordrawablesthumbnails.interfaces.ItemSorter

internal class VectorsPresenter(
    private val fileProvider: VectorFileProvider,
    private val attributeParser: VectorAttributeParser,
    private val imageRenderer: VectorImageRenderer,
    private val itemFilter: ItemFilter<VectorItem>,
    private val itemSorter: ItemSorter<VectorItem>,
    private val ioScheduler: Scheduler,
    private val processingScheduler: Scheduler
) {
    private var sortDirection: String? = null
    private var sort: String? = null
    private var filterText: String? = null
    private val uiEvents = PublishSubject.create<UiEvent>()
    val presenterEvents = PublishSubject.create<PresenterEvent>()
    private var state = VectorStatePresenterEvent.State.IDLE
    private val items = ArrayList<VectorItem>()

    fun refreshPropertiesData(project: Project?, delay: Boolean = true) {
        uiEvents.onNext(RefreshUiEvent(project!!, delay))
    }

    private fun refresh(project: Project, delay: Boolean = true) {
        if (setState(VectorStatePresenterEvent.State.SEARCHING)) {
            items.clear()
            fileProvider.getValidFilesObservable(project)
                .delay(if (delay) 2 else 0, TimeUnit.SECONDS) // Delay is on the computation scheduler by default
                .subscribeOn(ioScheduler)
                .observeOn(processingScheduler)
                .flatMap { f: ValidFile -> getItemsObservable(f) } // This will run on processingScheduler
                .doOnNext { vectorItem: VectorItem? ->
                    if (vectorItem != null) {
                        items.add(vectorItem)
                        presenterEvents.onNext(VectorFoundPresenterEvent(vectorItem))
                    }
                }
                .doOnComplete { setState(VectorStatePresenterEvent.State.IDLE) }
                .doOnError { setState(VectorStatePresenterEvent.State.IDLE) } // TODO: Consider specific error state
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

    private fun getItemsObservable(validFile: ValidFile): Observable<VectorItem?> {
        // This observable's creation logic will run on the scheduler specified by observeOn in refresh()
        return Observable.create { emitter: ObservableEmitter<VectorItem?> ->
            try {
                val xmlContent = FileInputStream(validFile.file).bufferedReader().use { it.readText() }
                val attributes = attributeParser.parse(xmlContent, validFile.file.name, validFile.file.length(), validFile)

                if (attributes != null) {
                    val image = imageRenderer.render(attributes)
                    if (image != null) {
                        emitter.onNext(
                            VectorItem(
                                attributes.name,
                                image,
                                attributes.validFile,
                                attributes.viewportW,
                                attributes.viewportH,
                                attributes.fileSize
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Log error or handle appropriately
                println("Error processing file ${validFile.file.name}: ${e.message}")
                // emitter.onError(e) // Optionally propagate error
            } finally {
                emitter.onComplete()
            }
        }
    }

    fun getPresenterEvents(): Observable<PresenterEvent> {
        return presenterEvents
    }

    fun onVectorClicked(project: Project, item: VectorItem) {
        uiEvents.onNext(VectorClickedUiEvent(project, item))
    }

    fun filter(text: String?) {
        // The current implementation of VectorItemNameFilter handles lowercasing,
        // so direct toLowerCase() here might be redundant but harmless.
        // Let's keep it for now as it doesn't break the logic.
        this.filterText = text?.toLowerCase()
    }

    fun itemsFiltered(): List<VectorItem> {
        val itemsToFilter = ArrayList(items) // Work on a copy
        val filteredItems = itemFilter.filter(itemsToFilter, filterText)
        return itemSorter.sort(filteredItems, sort, sortDirection)
    }

    fun sortBy2(sort: String) {
        this.sort = sort
    }

    fun sortByDirection(direction: String) {
        this.sortDirection = direction
    }

    init {
        uiEvents
            .subscribeOn(ioScheduler) // Subscribing part of uiEvents chain
            .observeOn(ioScheduler)   // Observing part for RefreshUiEvent, can be processingScheduler if refresh logic is heavy
            .ofType(RefreshUiEvent::class.java)
            .compose(RxUtils.avoidFastClicks())
            .retry() // Consider if retry is always appropriate
            .doOnError { x: Throwable? -> println(x) } // Better error handling might be needed
            .subscribe { ui: RefreshUiEvent -> refresh(ui.project, ui.delay) }

        uiEvents
            .subscribeOn(ioScheduler) // Subscribing part of uiEvents chain
            .observeOn(ioScheduler)   // Observing part for VectorClickedUiEvent, typically for UI interactions or quick tasks
            .ofType(VectorClickedUiEvent::class.java)
            .compose(RxUtils.avoidFastClicks())
            .retry() // Consider if retry is always appropriate
            .doOnError { x: Throwable? -> println(x) } // Better error handling
            .subscribe { ui: VectorClickedUiEvent -> Utils.openValidFile(ui.project, ui.item.validFile) }
    }
}