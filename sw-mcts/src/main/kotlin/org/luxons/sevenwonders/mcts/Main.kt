package org.luxons.sevenwonders.mcts

import org.luxons.sevenwonders.engine.Game
import org.luxons.sevenwonders.engine.data.GameDefinition
import org.luxons.sevenwonders.model.*
import org.luxons.sevenwonders.model.cards.Color
import org.luxons.sevenwonders.model.cards.HandCard
import org.luxons.sevenwonders.model.wonders.deal
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random



data class Data(
    val win: Boolean,
    val points: Int,
    val playouts: Int
)


fun OutputStream.writeCsv(data: List<Data>){
    val writer = bufferedWriter()
    //writer.write(""""Win","Points","playouts""")
    //writer.newLine()
    data.forEach {
        writer.write("${it.win}, ${it.points}, ${it.playouts}")
        writer.newLine()
    }
    writer.flush()
}

fun OutputStream.writeCsvPlays(plays: List<PlayerMove>){
    val writer = bufferedWriter()
    plays.forEach{
        writer.write("${it.cardName},${it.type.name}")
        writer.newLine()
    }
    writer.flush()
}

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
@Suppress("UNCHECKED_CAST")
class RuleBasedRobot(private val random: Random = Random(0)) : Agent {

    override fun getMoveToPerform(turnInfo: PlayerTurnInfo<*>): PlayerMove? {
        return when (val a = turnInfo.action){
            is TurnAction.PlayFromHand -> {
                if(turnInfo.wonderBuildability.isBuildable){
                    if (turnInfo.wonderBuildability.isFree) PlayerMove(
                        MoveType.UPGRADE_WONDER,
                        a.hand.first().name
                    )
                    else {
                        if((0..1).random(random) > 0) PlayerMove(
                            MoveType.UPGRADE_WONDER,
                            a.hand.first().name,
                            turnInfo.wonderBuildability.transactionsOptions.first()) else pickCard(turnInfo as PlayerTurnInfo<TurnAction.PlayFromHand>)


                    }

                }else pickCard(turnInfo as PlayerTurnInfo<TurnAction.PlayFromHand>)
            }
            else -> null
        }


    }
}


private fun pickCard(turnInfo: PlayerTurnInfo<TurnAction.PlayFromHand>, random: Random = Random(0)): PlayerMove{

    val availablePlays = turnInfo.action.hand.filter {
        it.playability.isPlayable
    }.toMutableList()

    class QCard(val handCard: HandCard, val prio: Int)

    val prioQ = PriorityQueue<QCard>{a, b -> a.prio - b.prio}
    //val playerBoard = turnInfo.table.boards[turnInfo.playerIndex]

    for (i in availablePlays){
        when (i.color){
            Color.BROWN -> {

                if((0..1).random(random) == 0){
                    prioQ.offer(QCard(i,2))
                }
                else{
                    prioQ.offer(QCard(i,5))
                }
            }
            Color.GREY -> {
                prioQ.offer(QCard(i,5))
            }
            Color.YELLOW -> {
                prioQ.offer(QCard(i,5))
            }
            Color.RED -> {
                var myReds= 0
                var ennemyReds= 0
                for(board in turnInfo.table.boards){
                    if(board.playerIndex == turnInfo.playerIndex){
                        myReds = board.military.nbShields
                    }else{
                        ennemyReds = if(ennemyReds > board.military.nbShields) ennemyReds else board.military.nbShields
                    }
                    if(ennemyReds > myReds) prioQ.offer(QCard(i,1))
                }
            }
            Color.BLUE -> {
                prioQ.offer(QCard(i,3))
            }
            Color.GREEN -> {
                prioQ.offer(QCard(i,4))
            }
            Color.PURPLE -> {
                prioQ.offer(QCard(i,4))
            }
        }
    }

    if(availablePlays.isEmpty() || prioQ.peek() == null){
        return PlayerMove(MoveType.DISCARD, turnInfo.action.hand.first().name)
    }

    return PlayerMove(MoveType.PLAY, prioQ.peek().handCard.name, prioQ.peek().handCard.playability.transactionOptions.first())

}


class RandomAgent : Agent {
    private val random = Random(0)
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
    override fun getMoveToPerform(turnInfo: PlayerTurnInfo<*>): PlayerMove? {

        return when (val a = turnInfo.action) {
            is TurnAction.PlayFromHand -> {
                mcts = MCTS(
                    simulationCount,
                    State(playerIndex, listOf(turnInfo), PlayerMove(MoveType.DISCARD, ""), a.hand),
                    deterministicGameFactory
                )
                val bestChild = mcts?.runSimulation { }
                bestChild?.state?.correspondingPlayerMove

            }

            else -> null
        }

    }

}


data class State(
    val playerIndex: Int,
    val turnInfos: List<PlayerTurnInfo<*>>,
    val correspondingPlayerMove: PlayerMove,
    val mctsHand: List<HandCard>,
    val player1Hand: List<HandCard>? = null,
    val player2Hand: List<HandCard>? = null
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
    return root.visitedChildren.maxBy { it.accumulatedReward }
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
    }


    val testReward = if (Random.nextInt(0, 10) > 0) 0 else 1
    val visitedChildren = mutableListOf<MCTS_Node>()
    val unvisitedChildrenFactories: MutableList<MCTS_NodeFactory>
    //(0..Random.nextInt(3)).map { { MCTS_Node(this, state, deterministicGameFactory) } }.toMutableList()

    var visited = 0
    var accumulatedReward = 0

    init {
        if (state.player1Hand == null || state.player2Hand == null) {
            determinizeOtherPlayerHands()
        }
        unvisitedChildrenFactories = createUnvisitedChildrenFactories()
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

                else -> throw IllegalStateException("You arent handling if turn action is not playfromhand $a")
            }
        }
        assert(player1hand != null && player2hand != null)
        state = State(
            state.playerIndex,
            state.turnInfos,
            state.correspondingPlayerMove,
            state.mctsHand,
            player1hand,
            player2hand
        )

    }

    private fun playerMoveToGameState(move: PlayerMove, otherPlayerAgent: Agent): State {
        val game = createAndPlayGameToCurrentState()
        val mctsPlayerIndex = state.playerIndex

        game.getCurrentTurnInfo().forEach {
            if (it.playerIndex == mctsPlayerIndex) {
                game.prepareMove(
                    it.playerIndex,
                    move
                )
            } else {
                otherPlayerAgent.getMoveToPerform(it)?.let { move -> game.prepareMove(it.playerIndex, move) }
            }
        }
        game.playTurn()
        return game.getCurrentTurnInfo().find { it.playerIndex == state.playerIndex }!!.let { turninfo ->
            State(
                state.playerIndex,
                state.turnInfos + turninfo,
                move,
                when (val a = turninfo.action) {
                    is TurnAction.PlayFromHand -> a.hand
                    is TurnAction.WatchScore -> emptyList()
                    else -> throw IllegalStateException("You arent handling this case $a")
                },
                emptyList(),
                emptyList()

            )
        }
    }

    private fun createUnvisitedChildrenFactories(): MutableList<MCTS_NodeFactory> {

        val discardAgent = DiscardAgent()
        //val allCards = state.hand//.filter { it.playability.isPlayable }
        //val playableCards = state.hand.filter { it.playability.isPlayable }

        val allDiscardPlays = state.mctsHand.map { PlayerMove(MoveType.DISCARD, it.name) }
        val allCardPlays = state.mctsHand.filter { it.playability.isPlayable }
            .map { PlayerMove(MoveType.PLAY, it.name, it.playability.transactionOptions.first()) }
        val allWonderPlays = if (state.turnInfos.last().wonderBuildability.isBuildable) state.mctsHand.map {
            PlayerMove(
                MoveType.UPGRADE_WONDER,
                it.name,
                state.turnInfos.last().wonderBuildability.transactionsOptions.first()
            )
        } else emptyList()

        val allPlays = allCardPlays + allWonderPlays + allDiscardPlays

        val gameStatesFromPlayableCards = allPlays.map { playerMoveToGameState(it, discardAgent) }

//        val old = allCards.map { card ->
//            val game = createAndPlayGameToCurrentState()
//            val mctsPlayerIndex = state.playerIndex
//            val move = if (card.playability.isPlayable) PlayerMove(
//                MoveType.PLAY,
//                card.name,
//                card.playability.transactionOptions.first()
//            ) else PlayerMove(MoveType.DISCARD, card.name)
//            game.getCurrentTurnInfo().forEach {
//                if (it.playerIndex == mctsPlayerIndex) {
//                    game.prepareMove(
//                        it.playerIndex,
//                        move
//                    )
//                } else {
//                    discardAgent.getMoveToPerform(it)?.let { move -> game.prepareMove(it.playerIndex, move) }
//                }
//            }
//            game.playTurn()
//            game.getCurrentTurnInfo().find { it.playerIndex == state.playerIndex }!!.let { turninfo ->
//                State(
//                    state.playerIndex,
//                    state.turnInfos + turninfo,
//                    when (val a = turninfo.action) {
//                        is TurnAction.PlayFromHand -> a.hand
//                        is TurnAction.WatchScore -> emptyList()
//                        else -> throw IllegalStateException("You arent handling this case $a")
//                    },
//                    move
//                )
//            }
//        }

        return gameStatesFromPlayableCards.map { { MCTS_Node(this, it, deterministicGameFactory) } }.toMutableList()
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
        val randomAgent = RandomAgent()

        while (!game.endOfGameReached()) {
            game.getCurrentTurnInfo().forEach {
                randomAgent.getMoveToPerform(it)?.let { move -> game.prepareMove(it.playerIndex, move) }
            }
            game.playTurn()
        }

        val scores = game.computeScore().scores.sortedByDescending { it.totalPoints }

        return if (scores[0].playerIndex == state.playerIndex) 1 else 0
    }


}

fun MCTS_Node.backpropagate(reward: Int) {
    visited++
    accumulatedReward += reward
    parent?.backpropagate(reward)
}


fun MCTS_Node.selectChild(): MCTS_Node {

    if (this.unvisitedChildrenFactories.isNotEmpty()) {
        return expand()
    }
    val bestUCTchild = visitedChildren.maxByOrNull { it.UCTscore() }
    return bestUCTchild?.selectChild() ?: this
}

fun MCTS_Node.expand(): MCTS_Node {
    val nodeFactory = unvisitedChildrenFactories.removeFirst()
    val newChild = nodeFactory()
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


    val mctsagent = MCTSAgent(0, 13, deterministicGameFactory)
    val agents = listOf(mctsagent, RandomAgent(), RandomAgent())

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

    val data: MutableList<Data> = mutableListOf()

    val gameDefinition = GameDefinition.load()
    val wonders = gameDefinition.allWonders.deal(agentCount, Random(0))

    val deterministicGameFactory = {
        gameDefinition.createGame(0, wonders, Settings(randomSeedForTests = 0))
    }

        val agents = listOf(DiscardAgent(), RandomAgent(), RuleBasedRobot())

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
    //2 is the bot that we ar looking after
    val botIndex = 2
    data.add(Data(game.getWinner() == botIndex,game.getPoints(botIndex),1000))
    FileOutputStream("data/test.csv",true).apply {writeCsv(data)}
    //FileOutputStream("data/testDiscard.csv",true).apply { writeCsvPlays() }

}

fun main() {
    //while (true) {
    val startTime = System.nanoTime()
    //val tests = listOf({ mctsTest() },{ mctsTest() },{ mctsTest() },{ mctsTest() },{ mctsTest() },{ mctsTest() },{ mctsTest() })
    //tests.parallelStream().forEach { it() }
    envTest()
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

