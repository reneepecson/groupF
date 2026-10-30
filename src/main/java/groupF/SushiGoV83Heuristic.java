package groupF;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import games.sushigo.SGGameState;

public class SushiGoV83Heuristic implements IStateHeuristic {

    private final SushiGoV83Params params;

    public SushiGoV83Heuristic(SushiGoV83Params params) {
        this.params = params;
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {

        SGGameState state = (SGGameState) gs;

        if (!state.isNotTerminal()) {
            return state.getPlayerResults()[playerId].value;
        }

        double score = state.getPlayerScore()[playerId].getValue();

        double potential =
                SushiGoV83CardEvaluator.evaluatePotential(
                        state, playerId, params);

        double threat =
                SushiGoV83CardEvaluator.opponentThreat(
                        state, playerId);

        double estimate = score + potential
                - params.blockingWeight * threat;

        return Math.max(-1.0, Math.min(1.0, estimate / 50.0));
    }
}