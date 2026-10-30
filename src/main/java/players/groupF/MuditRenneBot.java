package groupF;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import games.sushigo.SGGameState;
import players.basicMCTS.BasicMCTSParams;
import players.muditbot.MuditBot;

/**
 * Combines MuditBot's projected score evaluation with Renee's partial-set heuristic
 * while retaining MuditBot's Basic MCTS search configuration.
 */
public class MuditRenneBot extends MuditBot {

    private final ReneePartialSetHeuristic reneeHeuristic = new ReneePartialSetHeuristic();

    public MuditRenneBot() {
        super();
        configureCombinedHeuristic();
    }

    public MuditRenneBot(long seed) {
        super(seed);
        configureCombinedHeuristic();
    }

    public MuditRenneBot(BasicMCTSParams params) {
        super(params);
        configureCombinedHeuristic();
    }

    private void configureCombinedHeuristic() {
        getParameters().setParameterValue("heuristic",
                (IStateHeuristic) this::evaluateCombinedState);
        getParameters().heuristic = this::evaluateCombinedState;
        setName("MuditRenneBot");
    }

    private double evaluateCombinedState(AbstractGameState state, int playerId) {
        double muditValue = MuditBot.evaluateState(state, playerId);
        if (!(state instanceof SGGameState) || !state.isNotTerminal()) {
            return muditValue;
        }

        double reneeValue = reneeHeuristic.evaluateState(state, playerId);
        double reneeWinEstimate = logistic(4.0 * reneeValue);
        return (muditValue + reneeWinEstimate) / 2.0;
    }

    private static double logistic(double value) {
        if (value >= 0.0) {
            double exp = Math.exp(-value);
            return 1.0 / (1.0 + exp);
        }
        double exp = Math.exp(value);
        return exp / (1.0 + exp);
    }

    @Override
    public MuditRenneBot copy() {
        return new MuditRenneBot((BasicMCTSParams) getParameters().copy());
    }
}
