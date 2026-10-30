
package groupF;

import core.AbstractGameState;
import core.actions.AbstractAction;
import players.basicMCTS.BasicMCTSPlayer;

import java.util.List;

public class SushiGoV83Player extends BasicMCTSPlayer {

    public SushiGoV83Player(SushiGoV83Params params) {
        super(params);
        setStateHeuristic(new SushiGoV83Heuristic(params));
        setName("SushiGoV83");
    }

    @Override
    public AbstractAction _getAction(
            AbstractGameState gameState,
            List<AbstractAction> actions) {

        // Let MCTS select the final action.
        return super._getAction(gameState, actions);
    }

    @Override
    public SushiGoV83Player copy() {
        SushiGoV83Player copy = new SushiGoV83Player(
                (SushiGoV83Params) getParameters().copy());

        copy.setName("SushiGoV83");
        return copy;
    }
}
