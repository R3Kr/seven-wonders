package org.luxons.sevenwonders.ui.router

import kotlin.browser.window

object Router {

    fun games() {
        push("/games")
    }

    fun game(id: Long) {
        push("/game/$id")
    }

    fun lobby() {
        push("/lobby")
    }

    private fun push(path: String) {
        window.location.hash = path
    }
}