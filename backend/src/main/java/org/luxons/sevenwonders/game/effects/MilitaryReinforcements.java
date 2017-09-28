package org.luxons.sevenwonders.game.effects;

import java.util.Objects;

import org.luxons.sevenwonders.game.boards.Board;

public class MilitaryReinforcements extends InstantOwnBoardEffect {

    private final int count;

    public MilitaryReinforcements(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    @Override
    public void apply(Board board) {
        board.getMilitary().addShields(count);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MilitaryReinforcements that = (MilitaryReinforcements) o;
        return count == that.count;
    }

    @Override
    public int hashCode() {
        return Objects.hash(count);
    }
}
