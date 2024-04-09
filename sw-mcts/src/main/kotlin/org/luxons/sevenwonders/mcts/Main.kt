package org.luxons.sevenwonders.mcts

import org.luxons.sevenwonders.engine.Game
import org.luxons.sevenwonders.model.Settings
import org.luxons.sevenwonders.engine.data.GameDefinition
import org.luxons.sevenwonders.model.MoveType
import org.luxons.sevenwonders.model.PlayerMove
import org.luxons.sevenwonders.model.TurnAction
import org.luxons.sevenwonders.model.wonders.deal
import kotlin.random.Random


fun main() {
    println("hello werold")

    val settings = Settings()

    //val board = Board()
    val game = GameDefinition.load().let {
        val wonders = it.allWonders.deal(3)
        it.createGame(0, wonders, settings)
    }

    while (!game.endOfGameReached()) {
        readln()
        game.getCurrentTurnInfo().forEach {
            when (val a = it.action) {
                is TurnAction.PlayFromHand -> {

                    game.prepareMove(it.playerIndex, PlayerMove(MoveType.DISCARD, a.hand.first().name))
                }
                else -> println()
            }
            println("Player: " + it.playerIndex + " TurnAction: " + it.action)
        }
        game.playTurn()
    }
    println(game.computeScore())
}

