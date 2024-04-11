package org.luxons.sevenwonders.mcts

import org.luxons.sevenwonders.engine.data.GameDefinition
import org.luxons.sevenwonders.model.*
import org.luxons.sevenwonders.model.wonders.deal
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random


interface Agent {
    fun getMoveToPerform(turnInfo: PlayerTurnInfo<*>): PlayerMove?
}

class DiscardAgent : Agent {
    override fun getMoveToPerform(turnInfo: PlayerTurnInfo<*>): PlayerMove? {
        return when (val a = turnInfo.action) {
            is TurnAction.PlayFromHand -> PlayerMove(MoveType.DISCARD, a.hand.first().name)
            is TurnAction.Wait -> null
            else -> throw IllegalStateException(a.toString())
        }
    }

}

class RandomAgent : Agent {
    override fun getMoveToPerform(turnInfo: PlayerTurnInfo<*>): PlayerMove? {
        when (val a = turnInfo.action) {
            is TurnAction.PlayFromHand -> {
                val playWonderMove = if (turnInfo.wonderBuildability.isBuildable) PlayerMove(
                    MoveType.UPGRADE_WONDER,
                    a.hand.first().name,
                    turnInfo.wonderBuildability.transactionsOptions.first()
                ) else null

                val availablePlays = a.hand.filter {
                    it.playability.isPlayable
                }.map { PlayerMove(MoveType.PLAY, it.name, it.playability.transactionOptions.first()) }.toMutableList()

                playWonderMove?.let { availablePlays.add(it) }

                return availablePlays.randomOrNull() ?: PlayerMove(MoveType.DISCARD, a.hand.first().name)
            }
            is TurnAction.PlayFromDiscarded -> {
                val card = a.discardedCards.first()
                return PlayerMove(MoveType.PLAY_FREE_DISCARDED, card.name)
            }
            is TurnAction.Wait -> return null
            else -> throw IllegalStateException(a.toString())
        }
    }

}

class MCTS(val playerIndex: Int) {
    val root = MCTS_Node(null)
}

fun MCTS.runSimulation(
    simulationCount: Int, sideEffect: (MCTS_Node) -> Unit = {
    }
): MCTS_Node {
    for (i in 1..simulationCount) {
        val leaf = root.selectChild()
        val simulationResult = leaf.simulate()
        leaf.backpropagate(simulationResult)
        sideEffect(root)
    }
    return bestChild()
}

private fun MCTS.bestChild(): MCTS_Node {
    return root.visitedChildren.maxBy { it.accumulatedReward }
}

typealias MCTS_NodeFactory = () -> MCTS_Node

class MCTS_Node(var parent: MCTS_Node?, val terminal: Boolean = false) {
    companion object {
        const val EXPLORE_CONSTANT = 0.3
    }

    val testReward = if (Random.nextInt(0, 10) > 0) 0 else 1
    val visitedChildren = mutableListOf<MCTS_Node>()
    val unvisitedChildrenFactories: MutableList<MCTS_NodeFactory> =
        if (!terminal) (0..Random.nextInt(3)).map { { MCTS_Node(this) } }.toMutableList() else mutableListOf()

    var visited = 0
    var accumulatedReward = 0
}

fun MCTS_Node.backpropagate(reward: Int) {
    visited++
    accumulatedReward += reward
    parent?.backpropagate(reward)
}

fun MCTS_Node.simulate(): Int {
    return testReward;
}

fun MCTS_Node.selectChild(): MCTS_Node {

    if (!terminal) {
        if (this.unvisitedChildrenFactories.isNotEmpty()) {
            return expand()
        }
        val bestUCTchild = visitedChildren.maxBy { it.UCTscore() }
        return bestUCTchild.selectChild()
    }
    return this;
}

fun MCTS_Node.expand(): MCTS_Node {
    val nodeFactory = unvisitedChildrenFactories.removeFirst()
    val newChild = nodeFactory();
    visitedChildren.add(newChild)
    return newChild
}


fun MCTS_Node.UCTscore(): Double {
    return averageReward() + (MCTS_Node.EXPLORE_CONSTANT * sqrt(
        2 * ln(
            parent?.visited?.toDouble() ?: throw IllegalStateException("Examining UCT of root")
        ) / visited.toDouble()
    ))
}

fun MCTS_Node.averageReward(): Double {
    return accumulatedReward / (visited.toDouble())
}


fun mctsTest() {
    val mcsts = MCTS(0)
    mcsts.runSimulation(100) {
        printTree(it, 0)
        readln()
        println("-----------------------")
    }
}

fun printTree(root: MCTS_Node, tabcount: Int) {
    println("\t".repeat(tabcount) + root.accumulatedReward)
    root.visitedChildren.forEach { printTree(it, tabcount + 1) }

}

fun envTest() {

    val agentCount = 3

    val agents = listOf(DiscardAgent(), RandomAgent(), RandomAgent())

    val game = GameDefinition.load().let {
        val wonders = it.allWonders.deal(agentCount)
        it.createGame(0, wonders, Settings())
    }

    while (!game.endOfGameReached()) {
        readln()
        game.getCurrentTurnInfo().forEach {
            agents[it.playerIndex].getMoveToPerform(it)?.let { it1 -> game.prepareMove(it.playerIndex, it1) }
            println("Player: " + it.playerIndex + " TurnAction: " + it.action)
        }
        game.playTurn()
    }
    println(game.computeScore())


}

fun main() {
    while (true) {
        //envTest()
        mctsTest()
    }
}

//fun main() {
//    println("hello werold")
//
//    val settings = Settings()
//
//    //val board = Board()
//    val game = GameDefinition.load().let {
//        val wonders = it.allWonders.deal(3)
//        it.createGame(0, wonders, settings)
//    }
//
//    while (!game.endOfGameReached()) {
//        readln()
//        game.getCurrentTurnInfo().forEach { it1 ->
//            when (val a = it1.action) {
//                is TurnAction.PlayFromHand -> {
//                    val card = a.hand.firstOrNull {
//                        it.playability.isPlayable
//                    }
//                    if (card != null) {
//                        game.prepareMove(
//                            it1.playerIndex,
//                            PlayerMove(MoveType.PLAY, card.name, card.playability.transactionOptions.first())
//                        )
//                    } else {
//                        game.prepareMove(it1.playerIndex, PlayerMove(MoveType.DISCARD, a.hand.first().name))
//                    }
//                }
//
//                else -> println()
//            }
//            println("Player: " + it1.playerIndex + " TurnAction: " + it1.action)
//        }
//        game.playTurn()
//    }
//    println(game.computeScore())
//
//    val asd = MCTS(32)
//
//
//}

