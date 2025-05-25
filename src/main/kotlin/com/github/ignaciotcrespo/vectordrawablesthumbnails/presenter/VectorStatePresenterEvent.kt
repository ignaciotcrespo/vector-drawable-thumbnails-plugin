package com.github.ignaciotcrespo.vectordrawablesthumbnails.presenter

class VectorStatePresenterEvent(val state: State) : PresenterEvent {
    enum class State {
        IDLE, SEARCHING
    }
}