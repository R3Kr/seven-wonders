package org.luxons.sevenwonders.game.wonders

import org.luxons.sevenwonders.game.Player
import org.luxons.sevenwonders.game.boards.Board
import org.luxons.sevenwonders.game.cards.CardBack
import org.luxons.sevenwonders.game.resources.ResourceTransactions
import org.luxons.sevenwonders.game.resources.ResourceType

class Wonder internal constructor(
    val name: String,
    val initialResource: ResourceType,
    val stages: List<WonderStage>,
    val image: String
) {
    val nbBuiltStages: Int
        get() = stages.filter { it.isBuilt }.count()

    private val nextStage: WonderStage
        get() {
            val nextLevel = nbBuiltStages
            if (nextLevel == stages.size) {
                throw IllegalStateException("This wonder has already reached its maximum level")
            }
            return stages[nextLevel]
        }

    private val lastBuiltStage: WonderStage
        get() {
            val lastLevel = nbBuiltStages - 1
            return stages[lastLevel]
        }

    internal fun isNextStageBuildable(board: Board, boughtResources: ResourceTransactions): Boolean =
        nbBuiltStages < stages.size && nextStage.isBuildable(board, boughtResources)

    internal fun placeCard(cardBack: CardBack) = nextStage.placeCard(cardBack)

    internal fun activateLastBuiltStage(player: Player, boughtResources: ResourceTransactions) =
        lastBuiltStage.activate(player, boughtResources)

    internal fun computePoints(player: Player): Int =
        stages.filter { it.isBuilt }
            .flatMap { it.effects }
            .map { it.computePoints(player) }
            .sum()
}
