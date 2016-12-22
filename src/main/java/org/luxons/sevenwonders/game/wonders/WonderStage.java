package org.luxons.sevenwonders.game.wonders;

import java.util.List;

import org.luxons.sevenwonders.game.api.BoughtResources;
import org.luxons.sevenwonders.game.api.Table;
import org.luxons.sevenwonders.game.cards.CardBack;
import org.luxons.sevenwonders.game.cards.Requirements;
import org.luxons.sevenwonders.game.effects.Effect;

public class WonderStage {

    private Requirements requirements;

    private List<Effect> effects;

    private CardBack cardBack;

    public Requirements getRequirements() {
        return requirements;
    }

    public void setRequirements(Requirements requirements) {
        this.requirements = requirements;
    }

    public List<Effect> getEffects() {
        return effects;
    }

    public void setEffects(List<Effect> effects) {
        this.effects = effects;
    }

    public boolean isBuilt() {
        return cardBack != null;
    }

    public boolean isBuildable(Table table, int playerIndex, List<BoughtResources> boughtResources) {
        return requirements.isAffordedBy(table, playerIndex, boughtResources);
    }

    void build(CardBack cardBack) {
        this.cardBack = cardBack;
    }

    void activate(Table table, int playerIndex, List<BoughtResources> boughtResources) {
        effects.forEach(e -> e.apply(table, playerIndex));
    }
}