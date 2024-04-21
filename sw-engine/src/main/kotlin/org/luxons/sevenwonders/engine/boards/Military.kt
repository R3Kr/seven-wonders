package org.luxons.sevenwonders.engine.boards

import org.luxons.sevenwonders.model.Age

class Military(
    private val lostPointsPerDefeat: Int,
    private val wonPointsPerVictoryPerAge: Map<Age, Int>,
) {
    var nbShields = 0
        private set

    val totalPoints
        get() = victoryPoints - lostPointsPerDefeat * nbDefeatTokens

    var victoryPoints = 0
        private set

    var nbDefeatTokens = 0
        private set

    internal fun addShields(nbShields: Int) {
        this.nbShields += nbShields
    }

    internal fun victory(age: Age) {
        val wonPoints = wonPointsPerVictoryPerAge[age] ?: throw UnknownAgeException(age)
        victoryPoints += wonPoints
    }

    internal fun defeat() {
        nbDefeatTokens++
    }

    internal class UnknownAgeException(unknownAge: Age) : IllegalArgumentException(unknownAge.toString())
}
