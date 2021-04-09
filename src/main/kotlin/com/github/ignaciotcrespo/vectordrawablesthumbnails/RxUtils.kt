package com.github.ignaciotcrespo.vectordrawablesthumbnails

import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import java.util.concurrent.TimeUnit

object RxUtils {
    fun <T> doubleClick(): ObservableTransformer<T, T> {
        return ObservableTransformer { upstream: Observable<T> ->
            val shared = upstream.share()
            shared
                .buffer(upstream.debounce(350, TimeUnit.MILLISECONDS))
                .filter { list: List<T> -> list.size > 1 }
                .map { list: List<T> -> list[0] }
        }
    }

    fun <R> avoidFastClicks(): ObservableTransformer<R, R> {
        return ObservableTransformer { upstream: Observable<R> -> upstream.throttleFirst(1, TimeUnit.SECONDS) }
    }
}