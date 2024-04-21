package org.luxons.sevenwonders.engine.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.luxons.sevenwonders.engine.Game
import org.luxons.sevenwonders.engine.boards.Board
import org.luxons.sevenwonders.engine.data.definitions.DecksDefinition
import org.luxons.sevenwonders.engine.data.definitions.WonderDefinition
import org.luxons.sevenwonders.model.Age
import org.luxons.sevenwonders.model.Settings
import org.luxons.sevenwonders.model.wonders.AssignedWonder
import org.luxons.sevenwonders.model.wonders.PreGameWonder

internal const val LAST_AGE: Age = 3

@Serializable
internal data class GlobalRules(
    val minPlayers: Int,
    val maxPlayers: Int,
)

class GameDefinition internal constructor(
    rules: GlobalRules,
    wonderDefinitions: List<WonderDefinition>,
    private val decksDefinition: DecksDefinition,
) {
    val minPlayers: Int = rules.minPlayers
    val maxPlayers: Int = rules.maxPlayers

    val allWonders: List<PreGameWonder> = wonderDefinitions.map { w ->
        PreGameWonder(w.name, w.sides.mapValues { (_, def) -> def.image })
    }

    private val wondersByName = wonderDefinitions.associateBy { it.name }

    fun createGame(id: Long, wonders: Collection<AssignedWonder>, settings: Settings): Game {
        val nbPlayers = wonders.size
        val boards = wonders.mapIndexed { index, wonder -> createBoard(index, wonder, settings) }
        val decks = decksDefinition.prepareDecks(nbPlayers, settings.random)
        return Game(id, settings, boards, decks)
    }

    //not private anymore, needed in noncheating gamefactory
    fun createBoard(playerIndex: Int, asswonder: AssignedWonder, settings: Settings): Board {
        val wonder = wondersByName[asswonder.name] ?: error("Unknown wonder '${asswonder.name}'")
        return Board(wonder.create(asswonder.side), playerIndex, settings)
    }

    companion object {

        fun load(): GameDefinition {
            val rules = loadJson<GlobalRules>("global_rules.json")
            val wonders = loadJson<Array<WonderDefinition>>("wonders.json")
            val decksDefinition = loadJson<DecksDefinition>("cards.json")
            return GameDefinition(rules, wonders.toList(), decksDefinition)
        }

        fun loadWithRemovedWonders(wondersToFilter: List<String>): GameDefinition {
            val rules = loadJson<GlobalRules>("global_rules.json")
            val wonders = loadJson<Array<WonderDefinition>>("wonders.json").let {
                it.filterNot { wd -> wondersToFilter.any { it == wd.name } }
            }
            val decksDefinition = loadJson<DecksDefinition>("cards.json")
            return GameDefinition(rules, wonders, decksDefinition)
        }
    }
}

private inline fun <reified T> loadJson(filename: String): T {
    val packageAsPath = GameDefinition::class.java.`package`.name.replace('.', '/')
    val resourcePath = "/$packageAsPath/$filename"
    val resource = GameDefinition::class.java.getResource(resourcePath)
    val json = resource.readText()
    return Json.decodeFromString(json)
}
