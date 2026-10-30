package groupF;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import players.basicMCTS.BasicMCTSParams;

import java.util.List;
import java.util.Random;

/**
 * Basic MCTS player implementation owned by groupF. This deliberately uses TAG's
 * public BasicMCTSParams, but does not depend on modifications to TAG's MCTS classes.
 */
public class MuditMCTSPlayer extends AbstractPlayer {

    public MuditMCTSPlayer() {
        this(new BasicMCTSParams());
    }

    public MuditMCTSPlayer(long seed) {
        this(parametersWithSeed(seed));
    }

    public MuditMCTSPlayer(BasicMCTSParams params) {
        super(params, "Mudit MCTS");
        rnd = new Random(params.getRandomSeed());
    }

    private static BasicMCTSParams parametersWithSeed(long seed) {
        BasicMCTSParams params = new BasicMCTSParams();
        params.setRandomSeed((int) seed);
        return params;
    }

    @Override
    public BasicMCTSParams getParameters() {
        return (BasicMCTSParams) parameters;
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> actions) {
        MuditTreeNode root = new MuditTreeNode(this, null, gameState, rnd);
        root.mctsSearch();
        return root.bestAction();
    }

    @Override
    public MuditMCTSPlayer copy() {
        return new MuditMCTSPlayer((BasicMCTSParams) getParameters().copy());
    }
}
