package org.luxons.sevenwonders.game.data

import org.junit.Test
import org.luxons.sevenwonders.game.api.CustomizableSettings
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GameDefinitionTest {

    @Test
    fun successfulGameInit() {
        val gameDefinition = GameDefinition.load()
        assertNotNull(gameDefinition)
        assertEquals(3, gameDefinition.minPlayers)
        assertEquals(7, gameDefinition.maxPlayers)

        val game = gameDefinition.initGame(0, CustomizableSettings(), 7)
        assertNotNull(game)
    }
}