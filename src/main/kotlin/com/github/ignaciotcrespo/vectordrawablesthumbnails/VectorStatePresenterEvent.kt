package com.github.ignaciotcrespo.vectordrawablesthumbnails

class VectorStatePresenterEvent(val state: State) : PresenterEvent {
    enum class State {
        IDLE, SEARCHING
    }
}