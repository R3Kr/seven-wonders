package org.luxons.sevenwonders.game.effects;

import org.luxons.sevenwonders.game.api.Table;

public class SpecialAbilityActivation implements Effect {

    private final SpecialAbility specialAbility;

    public SpecialAbilityActivation(SpecialAbility specialAbility) {
        this.specialAbility = specialAbility;
    }

    public SpecialAbility getSpecialAbility() {
        return specialAbility;
    }

    @Override
    public void apply(Table table, int playerIndex) {
        specialAbility.apply(table.getBoard(playerIndex));
    }

    @Override
    public int computePoints(Table table, int playerIndex) {
        return 0;
    }
}