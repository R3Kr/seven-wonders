package com.palantir.blueprintjs.org.luxons.sevenwonders.ui.components.gameBrowser

import org.luxons.sevenwonders.model.api.PlayerDTO
import org.luxons.sevenwonders.ui.redux.connectState
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.*

interface PlayerInfoProps : RProps {
    var currentPlayer: PlayerDTO?
}

class PlayerInfoPresenter(props: PlayerInfoProps) : RComponent<PlayerInfoProps, RState>(props) {

    override fun RBuilder.render() {
        span {
            b {
                +"Username:"
            }
            props.currentPlayer?.let {
                + " ${it.displayName}"
            }
        }
    }
}

fun RBuilder.playerInfo() = playerInfo {}

private val playerInfo = connectState(
    clazz = PlayerInfoPresenter::class,
    mapStateToProps = { state, _ ->
        currentPlayer = state.currentPlayer
    }
)