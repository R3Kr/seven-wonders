package org.luxons.sevenwonders.engine.cards

import org.luxons.sevenwonders.engine.Player
import org.luxons.sevenwonders.engine.converters.toHandCards
import org.luxons.sevenwonders.model.cards.HandCard
import org.luxons.sevenwonders.model.cards.HandRotationDirection

class Hands(private val hands: List<List<Card>>) {

    val areEmpty: Boolean = this.hands.all(List<Card>::isEmpty)

    operator fun get(playerIndex: Int): List<Card> {
        return hands[playerIndex]
    }

    fun clearHand(playerIndex: Int): Hands {
        val mutatedHands = hands.toMutableList()
        mutatedHands[playerIndex] = emptyList()
        return Hands(mutatedHands)
    }

    fun remove(playerIndex: Int, card: Card): Hands {
        val mutatedHands = hands.toMutableList()
        mutatedHands[playerIndex] = hands[playerIndex] - card
        return Hands(mutatedHands)
    }

    fun createHand(player: Player): List<HandCard> = hands[player.index].toHandCards(player, false)

    fun rotate(direction: HandRotationDirection): Hands {
        val newHands = when (direction) {
            HandRotationDirection.RIGHT -> hands.takeLast(1) + hands.dropLast(1)
            HandRotationDirection.LEFT -> hands.drop(1) + hands.take(1)
        }
        return Hands(newHands)
    }

    fun maxOneCardRemains(): Boolean = hands.map { it.size }.maxOrNull() ?: 0 <= 1
}
