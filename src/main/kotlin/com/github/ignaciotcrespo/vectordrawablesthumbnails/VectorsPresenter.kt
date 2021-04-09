package com.github.ignaciotcrespo.vectordrawablesthumbnails

import com.android.ide.common.vectordrawable.VdPreview
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import io.reactivex.Emitter
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.apache.commons.io.IOUtils
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

internal class VectorsPresenter {
    private val uiEvents = PublishSubject.create<UiEvent>()
    val presenterEvents = PublishSubject.create<PresenterEvent>()
    private var state = VectorStatePresenterEvent.State.IDLE
    fun refreshPropertiesData(project: Project?) {
        uiEvents.onNext(RefreshUiEvent(project!!))
    }

    private fun refresh(project: Project) {
        if (setState(VectorStatePresenterEvent.State.SEARCHING)) {
            getValidFilesObservable(project)
                .delay(2, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.newThread()) //                .doOnNext(tableModel::addItem)
                .flatMap { f: ValidFile -> getItemsObservable(f) }
                .doOnNext { vectorItem: VectorItem? -> presenterEvents.onNext(VectorFoundPresenterEvent(vectorItem)) } //                .doOnSubscribe(disposable -> tableModel.clear())
                .doOnComplete { setState(VectorStatePresenterEvent.State.IDLE) }
                .doOnError { setState(VectorStatePresenterEvent.State.IDLE) }
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

    private fun getValidFilesObservable(project: Project): Observable<ValidFile> {
        return Observable.create { emitter: ObservableEmitter<ValidFile> ->
            try {
                val modules = ModuleManager.getInstance(project).modules
                if (modules.size > 0) {
                    val file = modules[0].project.baseDir
                    val allExcludedRoots: MutableList<VirtualFile> = ArrayList()
                    for (module in modules) {
                        val excludedRoots = ModuleRootManager.getInstance(
                            module!!
                        ).excludeRoots
                        allExcludedRoots.addAll(Arrays.asList(*excludedRoots))
                    }
                    val projectRootFolder = file.canonicalPath
                    if (projectRootFolder != null) {
                        val file1 = File(projectRootFolder)
                        searchFiles(emitter, file1, projectRootFolder, allExcludedRoots)
                    }
                }
            } finally {
                emitter.onComplete()
            }
        }
    }

    private fun searchFiles(
        emitter: ObservableEmitter<ValidFile>,
        folder: File,
        projectRootFolder: String,
        excludedRoots: List<VirtualFile>
    ) {
        val files = folder.listFiles()
        if (files != null) {
            for (f in files) {
                if (f.isDirectory) {
                    if (".gradle" == f.name) {
                        continue
                    }
                    if (".idea" == f.name) {
                        continue
                    }
                    if (f.absolutePath.contains("build") && f.absolutePath.contains("generated")) {
                        continue
                    }
                    if (f.absolutePath.contains("build") && f.absolutePath.contains("intermediates")) {
                        continue
                    }
                    val fVirtual = LocalFileSystem.getInstance().findFileByIoFile(f)
                    var isExcluded = false
                    for (excluded in excludedRoots) {
                        if (excluded == fVirtual) {
                            isExcluded = true
                            break
                        }
                    }
                    if (!isExcluded) {
                        searchFiles(emitter, f, projectRootFolder, excludedRoots)
                    }
                } else if (f.toString().endsWith(".xml")) {
                    emitter.onNext(ValidFile(f, projectRootFolder))
                }
            }
        }
    }

    private fun getItemsObservable(f: ValidFile): Observable<VectorItem?> {
        return Observable.create { emitter: ObservableEmitter<VectorItem?> -> searchVectors(emitter, f) }
    }

    private fun searchVectors(emitter: Emitter<VectorItem?>, file: ValidFile) {
        var bmp: BufferedImage? = null
        var inputStream: FileInputStream? = null
        try {
            inputStream = FileInputStream(file.file)
            var xml = IOUtils.toString(inputStream)
            if (xml.contains("</vector>")) {
                if (xml.contains("@color/")) {
                    xml = xml.replace("@color/\\w+".toRegex(), "#000000")
                }
                bmp = VdPreview.getPreviewFromVectorXml(
                    VdPreview.TargetSize.createSizeFromWidth(50),
                    xml,
                    StringBuilder()
                )
                if (bmp != null) {
                    emitter.onNext(VectorItem(file.file.name, bmp, file))
                }
            }
        } catch (t: Throwable) {
            println(t)
        } finally {
            try {
                inputStream!!.close()
            } catch (e1: IOException) {
                e1.printStackTrace()
            }
            emitter.onComplete()
        }
    }

    fun getPresenterEvents(): Observable<PresenterEvent> {
        return presenterEvents
    }

    fun onVectorClicked(project: Project, item: VectorItem) {
        uiEvents.onNext(VectorClickedUiEvent(project, item))
    }

    init {
        uiEvents
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .ofType(RefreshUiEvent::class.java)
            .compose(RxUtils.avoidFastClicks())
            .retry()
            .doOnError { x: Throwable? -> println(x) }
            .subscribe { ui: RefreshUiEvent -> refresh(ui.project) }
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