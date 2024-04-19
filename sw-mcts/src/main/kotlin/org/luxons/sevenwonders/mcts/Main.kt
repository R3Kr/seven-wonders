package org.luxons.sevenwonders.mcts

import org.luxons.sevenwonders.engine.Game
import org.luxons.sevenwonders.engine.data.GameDefinition
import org.luxons.sevenwonders.model.*
import org.luxons.sevenwonders.model.cards.HandCard
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

class RandomAgent(seed: Int) : Agent {
    private val random = Random(seed)
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

                return availablePlays.randomOrNull(random) ?: PlayerMove(MoveType.DISCARD, a.hand.first().name)
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

class MCTSAgent(val playerIndex: Int, val simulationCount: Int, val deterministicGameFactory: GameFactory) : Agent {
    var mcts: MCTS? = null
    val playerOneIndex = if (playerIndex == 0) 1 else 0
    val playerTwoIndex = if (playerIndex == 2) 1 else 2
    override fun getMoveToPerform(turnInfo: PlayerTurnInfo<*>): PlayerMove? {

        return when (val a = turnInfo.action) {
            is TurnAction.PlayFromHand -> {
                mcts = MCTS(
                    simulationCount,
                    State(playerIndex, playerOneIndex, playerTwoIndex, listOf(turnInfo), PlayerMove(MoveType.DISCARD, ""), a.hand),
                    deterministicGameFactory
                )
                val bestChild = mcts?.runSimulation { }
                bestChild?.state?.correspondingPlayerMove

            }

            else -> null
        }

    }

}

data class OtherTurnInfos(
    val player1TurnInfo: PlayerTurnInfo<TurnAction.PlayFromHand>,
    val player2TurnInfo: PlayerTurnInfo<TurnAction.PlayFromHand>
)

data class State(
    val mctsIndex: Int,
    val player1Index: Int,
    val player2Index: Int,
    val turnInfos: List<PlayerTurnInfo<*>>,
    val correspondingPlayerMove: PlayerMove,
    val mctsHand: List<HandCard>,
    val otherHands: OtherTurnInfos? = null
)

class MCTS(val simulationCount: Int, state: State, deterministicGameFactory: GameFactory) {
    val root = MCTS_Node(null, state, deterministicGameFactory)
}

fun MCTS.runSimulation(
    sideEffect: (MCTS_Node) -> Unit = {
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
    val playerMoveToReward = root.visitedChildren.groupBy { it.state.correspondingPlayerMove }.map {
        Pair(it.key, it.value.sumOf { it.accumulatedReward })
    }
    println(playerMoveToReward)
    val bestMove = playerMoveToReward.maxBy { it.second }
    return root.visitedChildren.first { it.state.correspondingPlayerMove == bestMove.first }
}

typealias MCTS_NodeFactory = () -> MCTS_Node
typealias GameFactory = () -> Game
typealias GameFactoryFactory = () -> GameFactory

class MCTS_Node(
    var parent: MCTS_Node?,
    var state: State,
    val deterministicGameFactory: GameFactory,
) {
    companion object {
        const val EXPLORE_CONSTANT = 0.3

        fun allAvailableMoves(hand: List<HandCard>, turnInfo: PlayerTurnInfo<*>): List<PlayerMove> {
            val allDiscardPlays = hand.map { PlayerMove(MoveType.DISCARD, it.name) }
            val allCardPlays = hand.filter { it.playability.isPlayable }
                .map { PlayerMove(MoveType.PLAY, it.name, it.playability.transactionOptions.first()) }
            val allWonderPlays = if (turnInfo.wonderBuildability.isBuildable) hand.map {
                PlayerMove(
                    MoveType.UPGRADE_WONDER,
                    it.name,
                    turnInfo.wonderBuildability.transactionsOptions.first()
                )
            } else emptyList()

            return allCardPlays + allWonderPlays + allDiscardPlays
        }

    }


    val visitedChildren = mutableListOf<MCTS_Node>()

    //unvisted children factories are lazyloaded for perf reasons
    private var unvisitedChildrenFactories: MutableList<MCTS_NodeFactory>? = null
    //(0..Random.nextInt(3)).map { { MCTS_Node(this, state, deterministicGameFactory) } }.toMutableList()

    var visited = 0
    var accumulatedReward = 0

    init {
        if (state.otherHands == null) {
            state = State(
                state.mctsIndex,
                state.player1Index,
                state.player2Index,
                state.turnInfos,
                state.correspondingPlayerMove,
                state.mctsHand,
                otherHands = getOtherHands(createAndPlayGameToCurrentState())
            )
        }
    }

    fun selectChild(): MCTS_Node {

        // if unvisitedCHildrenFactories is null, null != false
        if (unvisitedChildrenFactories == null) {
            unvisitedChildrenFactories = createUnvisitedChildrenFactories()
        }
        if (this.unvisitedChildrenFactories!!.isNotEmpty()) {
            return expand()
        }
        val bestUCTchild = visitedChildren.maxByOrNull { it.UCTscore() }
        return bestUCTchild?.selectChild() ?: this
    }

    private fun determinizeOtherPlayerHands() {


        val game = createAndPlayGameToCurrentState()
        var player1hand: List<HandCard>? = null
        var player2hand: List<HandCard>? = null
        game.getCurrentTurnInfo().forEach {
            when (val a = it.action) {
                is TurnAction.PlayFromHand -> {
                    if (it.playerIndex == 1) {
                        player1hand = a.hand
                    }
                    if (it.playerIndex == 2) {
                        player2hand = a.hand
                    }
                }

                is TurnAction.WatchScore -> {
                    if (it.playerIndex == 1) {
                        player1hand = emptyList()
                    }
                    if (it.playerIndex == 2) {
                        player2hand = emptyList()
                    }
                }

                else -> throw IllegalStateException("You arent handling if turn action is not playfromhand $a")
            }
        }
        assert(player1hand != null && player2hand != null)
        state = State(
            state.mctsIndex,
            state.player1Index,
            state.player2Index,
            state.turnInfos,
            state.correspondingPlayerMove,
            state.mctsHand,
            //otherHands = OtherTurnInfos(player1hand!!, player2hand!!)
        )

    }

    private fun expand(): MCTS_Node {
        //this is not needed as the creating of the children factories happen in selectchild
        val nodeFactory = unvisitedChildrenFactories.let {
            if (it == null) {
                //"lazily loaded"
                unvisitedChildrenFactories = createUnvisitedChildrenFactories()
            }
            unvisitedChildrenFactories!!.removeFirst()
        }
        val newChild = nodeFactory();
        visitedChildren.add(newChild)
        return newChild
    }

    private fun getOtherHands(game: Game): OtherTurnInfos? {
        @Suppress("UNCHECKED_CAST")
        val turnInfos = game.getCurrentTurnInfo().filter {
            it.playerIndex != state.mctsIndex && it.action is TurnAction.PlayFromHand
        } as List<PlayerTurnInfo<TurnAction.PlayFromHand>>
        if (turnInfos.size != 2) {
            return null
        }
        return OtherTurnInfos(player1TurnInfo = turnInfos[0], player2TurnInfo = turnInfos[1])
    }

    private fun movesToGameState(mctsMove: PlayerMove, player1Move: PlayerMove, player2Move: PlayerMove): State {
        val game = createAndPlayGameToCurrentState()

        game.prepareMove(state.mctsIndex, mctsMove)
        game.prepareMove(state.player1Index, player1Move)
        game.prepareMove(state.player2Index, player2Move)
        game.playTurn()
        return game.getCurrentTurnInfo().find { it.playerIndex == state.mctsIndex }!!.let { turninfo ->
            State(
                state.mctsIndex,
                state.player1Index,
                state.player2Index,
                state.turnInfos + turninfo,
                mctsMove,
                when (val a = turninfo.action) {
                    is TurnAction.PlayFromHand -> a.hand
                    is TurnAction.WatchScore -> emptyList()
                    else -> throw IllegalStateException("You arent handling this case $a")
                },
                otherHands = getOtherHands(game)

            )
        }

    }



    private fun createUnvisitedChildrenFactories(): MutableList<MCTS_NodeFactory> {
        //val discardAgent = DiscardAgent()


        val allPlaysForMCTS =
            allAvailableMoves(state.mctsHand, state.turnInfos.last())//allCardPlays + allWonderPlays + allDiscardPlays

        //TA INTE BORT
        val allPlayer1Plays = state.otherHands?.let {
            allAvailableMoves(it.player1TurnInfo.action.hand, it.player1TurnInfo)
        }
        val allPlayer2Plays = state.otherHands?.let {
            allAvailableMoves(it.player2TurnInfo.action.hand, it.player2TurnInfo)
        }

        val allPossibleMoveCombinations = mutableListOf<Triple<PlayerMove, PlayerMove, PlayerMove>>()


        if (allPlayer1Plays != null) {
            for (player1Play in allPlayer1Plays) {
                if (allPlayer2Plays != null) {
                    for (player2Play in allPlayer2Plays) {
                        //mcts in inner to get more "even" trial ov moves
                        for (mctsPlay in allPlaysForMCTS) {
                            allPossibleMoveCombinations.add(Triple(mctsPlay, player1Play, player2Play))
                        }
                    }
                }
            }
        }


        val gameStateFunctionsFromAllPlayableMoves = allPossibleMoveCombinations.map {
            {
                val (mctsPlay, player1play, player2play) = it
                movesToGameState(mctsPlay, player1play, player2play)
            }
        }


//        val asd = allPlaysForMCTS.map {
//            {
//                movesToGameState(
//                    it,
//                    discardAgent.getMoveToPerform(state.otherHands?.player1TurnInfo!!)!!,
//                    discardAgent.getMoveToPerform(state.otherHands?.player2TurnInfo!!)!!
//                )
//            }
//        }

        return gameStateFunctionsFromAllPlayableMoves.map { { MCTS_Node(this, it(), deterministicGameFactory) } }
            .toMutableList()
    }

    private fun createAndPlayGameToCurrentState(): Game {
        val game = deterministicGameFactory()
        val movesToPlay = state.turnInfos.flatMap { it.table.lastPlayedMoves }
        for (move in movesToPlay) {
            game.prepareMove(move.playerIndex, PlayerMove(move.type, move.card.name, move.transactions))
            if (game.allPlayersPreparedTheirMove()) {
                game.playTurn()
            }
        }
        return game
    }

    fun simulate(): Int {
        val game = createAndPlayGameToCurrentState()
        val randomAgent = RandomAgent(visited)

        while (!game.endOfGameReached()) {
            game.getCurrentTurnInfo().forEach {
                randomAgent.getMoveToPerform(it)?.let { move -> game.prepareMove(it.playerIndex, move) }
            }
            game.playTurn();
        }

        val scores = game.computeScore().scores.sortedByDescending { it.totalPoints }

        return if (scores[0].playerIndex == state.mctsIndex) 1 else 0
    }


}

fun MCTS_Node.backpropagate(reward: Int) {
    visited++
    accumulatedReward += reward
    parent?.backpropagate(reward)
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


//fun mctsTest() {
//    val gameDefinition = GameDefinition.load()
//    val wonders = gameDefinition.allWonders.deal(3, Random(0))
//
//    val deterministicGameFactory = {
//        gameDefinition.createGame(0, wonders, Settings(randomSeedForTests = 0))
//    }
//    val mcsts = MCTS(1000, deterministicGameFactory = deterministicGameFactory)
//    mcsts.runSimulation {
//        //printTree(it, 0)
//        //readln()
//        //println("-----------------------")
//    }
//    printTree(mcsts.root, 0)
//}


fun mctsTest() {
    val agentCount = 3
    val gameDefinition = GameDefinition.load()
    val wonders = gameDefinition.allWonders.deal(agentCount, Random(0))

    val lastPlayedMoves: MutableList<PlayedMove> = emptyList<PlayedMove>().toMutableList()

    val deterministicGameFactory = {
        val game = gameDefinition.createGame(0, wonders, Settings(randomSeedForTests = 0))
        //until size - 3 as the last three is handled by the mcts agent
        for (i in 0 until if (lastPlayedMoves.size - 3 <= 0) 0 else lastPlayedMoves.size - 3) {
            val move = lastPlayedMoves[i]
            game.prepareMove(move.playerIndex, PlayerMove(move.type, move.card.name, move.transactions))
            if (game.allPlayersPreparedTheirMove()) {
                game.playTurn()
            }
        }
        game
    }


    val mctsagent = MCTSAgent(0, 50, deterministicGameFactory)
//    val mctsagent2 = MCTSAgent(1, 2000, deterministicGameFactory)
//    val mctsagent3 = MCTSAgent(2, 50, deterministicGameFactory)
    val agents = listOf(mctsagent, RandomAgent(0), RandomAgent(0))

    val game = deterministicGameFactory()

    while (!game.endOfGameReached()) {
        //readln()
        game.getCurrentTurnInfo().forEach {
            agents[it.playerIndex].getMoveToPerform(it)?.let { it1 -> game.prepareMove(it.playerIndex, it1) }
            println("Player: " + it.playerIndex + " TurnAction: " + it.action)
        }
        game.playTurn()
        lastPlayedMoves += game.getCurrentTurnInfo()[0].table.lastPlayedMoves

        mctsagent.mcts?.let { printTree(it.root, 0) }
    }
    println(game.computeScore())

}

fun printTree(root: MCTS_Node, tabcount: Int) {
    println("\t".repeat(tabcount) + root.accumulatedReward)
    root.visitedChildren.forEach { printTree(it, tabcount + 1) }
}

fun envTest() {

    val agentCount = 3


    val gameDefinition = GameDefinition.load()
    val wonders = gameDefinition.allWonders.deal(agentCount, Random(0))

    val deterministicGameFactory = {
        gameDefinition.createGame(0, wonders, Settings(randomSeedForTests = 0))
    }

    val agents = listOf(DiscardAgent(), RandomAgent(0), RandomAgent(0))

    val game = deterministicGameFactory()

    while (!game.endOfGameReached()) {
        //readln()
        game.getCurrentTurnInfo().forEach {
            agents[it.playerIndex].getMoveToPerform(it)?.let { it1 -> game.prepareMove(it.playerIndex, it1) }
            println("Player: " + it.playerIndex + " TurnAction: " + it.action)
        }
        game.playTurn()
    }
    println(game.computeScore())


}

fun main() {
    //while (true) {
    val startTime = System.nanoTime()
    val tests = listOf({ mctsTest() },{ mctsTest() },{ mctsTest() },{ mctsTest() },{ mctsTest() },{ mctsTest() },{ mctsTest() })
    tests.parallelStream().forEach { it() }
    val endTime = System.nanoTime()
    println("Execution time: ${(endTime - startTime) / 1_000_000} ms")
    //mctsTest(
    // }
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

