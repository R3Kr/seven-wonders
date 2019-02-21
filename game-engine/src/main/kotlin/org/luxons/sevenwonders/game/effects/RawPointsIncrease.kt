package org.luxons.sevenwonders.game.effects

import org.luxons.sevenwonders.game.Player

internal data class RawPointsIncrease(val points: Int) : EndGameEffect() {

    override fun computePoints(player: Player): Int = points
}