package org.luxons.sevenwonders.game.wonders;

import java.util.List;

import org.luxons.sevenwonders.game.boards.Board;
import org.luxons.sevenwonders.game.cards.Requirements;
import org.luxons.sevenwonders.game.effects.Effect;

public class WonderLevel {

    private Requirements requirements;

    private List<Effect> effects;

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

    public void activate(Board board, Board leftNeighbourBoard, Board rightNeighbourBoard) {
        effects.forEach(e -> e.apply(board, leftNeighbourBoard, rightNeighbourBoard));
    }
}
