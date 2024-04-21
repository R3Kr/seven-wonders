import org.luxons.sevenwonders.engine.Game
import org.luxons.sevenwonders.engine.data.GameDefinition
import org.luxons.sevenwonders.mcts.RandomAgent
import org.luxons.sevenwonders.mcts.getScrambledDeckForPlayer
import org.luxons.sevenwonders.model.Settings
import org.luxons.sevenwonders.model.TurnAction
import org.luxons.sevenwonders.model.cards.HandCard
import org.luxons.sevenwonders.model.wonders.deal
import kotlin.random.Random
import kotlin.test.*


class MCTS_Tests {

    @Test
    fun test1() {
        assert(true);
    }


    @Test
    fun testScrambleDeckForPlayerRetainsSameCardsAge1() {

        val gd = GameDefinition.load()
        val wonders = gd.allWonders.deal(3, random = Random(0))
        val game = gd.createGame(0, wonders, Settings(randomSeedForTests = 0))

        val newDecks = game.decks.getScrambledDeckForPlayer(0, 1, true, Random(0))

        val game2 = Game(
            1,
            Settings(),
            wonders.mapIndexed { index, wonder -> gd.createBoard(index, wonder, Settings(0)) },
            newDecks
        )


        assert(game.getCurrentTurnInfo()[0] == game2.getCurrentTurnInfo()[0])
        assert(game.getCurrentTurnInfo()[1] != game2.getCurrentTurnInfo()[1])

        val randomAgent = RandomAgent(0)
        (0 until 7).forEach {
            game.getCurrentTurnInfo().forEach { ti ->
                randomAgent.getMoveToPerform(ti)!!.let {
                    game.prepareMove(ti.playerIndex, it)
                }
            }
            game2.getCurrentTurnInfo().forEach { ti ->
                randomAgent.getMoveToPerform(ti)!!.let {
                    game2.prepareMove(ti.playerIndex, it)
                }
            }
            game.playTurn()
            game2.playTurn()
        }

        assert(game.getCurrentTurnInfo()[0] != game2.getCurrentTurnInfo()[0])
    }

    @Test
    fun testScrambleDeckForPlayerRetainsSameCardsAge2() {

        val gd = GameDefinition.load()
        val wonders = gd.allWonders.deal(3, random = Random(0))
        val game = gd.createGame(0, wonders, Settings(randomSeedForTests = 0))

        val newDecks = game.decks.getScrambledDeckForPlayer(0, 2, true, Random(0))

        val game2 = Game(
            1,
            Settings(),
            wonders.mapIndexed { index, wonder -> gd.createBoard(index, wonder, Settings(0)) },
            newDecks
        )

        assert(game.getCurrentTurnInfo()[0] == game2.getCurrentTurnInfo()[0])
        assert(game.getCurrentTurnInfo()[1] == game2.getCurrentTurnInfo()[1])

        val randomAgent = RandomAgent(0)
        (0 until 6).forEach {
            game.getCurrentTurnInfo().forEach { ti ->
                randomAgent.getMoveToPerform(ti)!!.let {
                    game.prepareMove(ti.playerIndex, it)
                }
            }
            game2.getCurrentTurnInfo().forEach { ti ->
                randomAgent.getMoveToPerform(ti)!!.let {
                    game2.prepareMove(ti.playerIndex, it)
                }
            }
            game.playTurn()
            game2.playTurn()
        }

        assert((game.getCurrentTurnInfo()[0].action as TurnAction.PlayFromHand).hand.toNameList() == (game2.getCurrentTurnInfo()[0].action as TurnAction.PlayFromHand).hand.toNameList())
        assert((game.getCurrentTurnInfo()[1].action as TurnAction.PlayFromHand).hand.toNameList() != (game2.getCurrentTurnInfo()[1].action as TurnAction.PlayFromHand).hand.toNameList())


    }

    @Test
    fun testScrambleDeckForPlayerRetainsSameCardsAge3() {

        val gd = GameDefinition.load()
        val wonders = gd.allWonders.deal(3, random = Random(0))
        val game = gd.createGame(0, wonders, Settings(randomSeedForTests = 0))

        val newDecks = game.decks.getScrambledDeckForPlayer(0, 3, true, Random(0))

        val game2 = Game(
            1,
            Settings(),
            wonders.mapIndexed { index, wonder -> gd.createBoard(index, wonder, Settings(0)) },
            newDecks
        )

        assert(game.getCurrentTurnInfo()[0] == game2.getCurrentTurnInfo()[0])
        assert(game.getCurrentTurnInfo()[1] == game2.getCurrentTurnInfo()[1])

        val randomAgent = RandomAgent(0)
        (0 until 12).forEach {
            game.getCurrentTurnInfo().forEach { ti ->
                randomAgent.getMoveToPerform(ti)!!.let {
                    game.prepareMove(ti.playerIndex, it)
                }
            }
            game2.getCurrentTurnInfo().forEach { ti ->
                randomAgent.getMoveToPerform(ti)!!.let {
                    game2.prepareMove(ti.playerIndex, it)
                }
            }
            game.playTurn()
            game2.playTurn()
        }

        assert((game.getCurrentTurnInfo()[0].action as TurnAction.PlayFromHand).hand.toNameList() == (game2.getCurrentTurnInfo()[0].action as TurnAction.PlayFromHand).hand.toNameList())
        assert((game.getCurrentTurnInfo()[1].action as TurnAction.PlayFromHand).hand.toNameList() != (game2.getCurrentTurnInfo()[1].action as TurnAction.PlayFromHand).hand.toNameList())


    }

}



fun List<HandCard>.toNameList(): List<String> {
    return map { it.name }
}
