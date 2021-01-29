package org.luxons.sevenwonders.engine.data.definitions

import kotlinx.serialization.Serializable
import org.luxons.sevenwonders.engine.cards.Card
import org.luxons.sevenwonders.engine.cards.Decks
import org.luxons.sevenwonders.model.cards.CardBack
import kotlin.random.Random

@Serializable
internal class DeckDefinition(
    val cards: List<CardDefinition>,
    val backImage: String,
) {
    fun prepareFor(nbPlayers: Int): List<Card> = cards.flatMap { it.create(CardBack(backImage), nbPlayers) }
}

@Serializable
internal class DecksDefinition(
    private val age1: DeckDefinition,
    private val age2: DeckDefinition,
    private val age3: DeckDefinition,
    private val guildCards: List<CardDefinition>,
) {
    fun prepareDecks(nbPlayers: Int, random: Random) = Decks(
        mapOf(
            1 to age1.prepareFor(nbPlayers).shuffled(random),
            2 to age2.prepareFor(nbPlayers).shuffled(random),
            3 to (age3.prepareFor(nbPlayers) + pickGuildCards(nbPlayers, random)).shuffled(random),
        ),
    )

    private fun pickGuildCards(nbPlayers: Int, random: Random): List<Card> {
        val back = CardBack(age3.backImage)
        val guild = guildCards.map { it.create(back) }.shuffled(random)
        return guild.subList(0, nbPlayers + 2)
    }
}
