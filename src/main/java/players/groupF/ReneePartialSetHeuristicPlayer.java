package groupF;

import players.PlayerConstants;
import players.basicMCTS.BasicMCTSParams;
import players.basicMCTS.BasicMCTSPlayer;

public class ReneePartialSetHeuristicPlayer extends BasicMCTSPlayer {

    public ReneePartialSetHeuristicPlayer() {
        this(new BasicMCTSParams());
    }

    public ReneePartialSetHeuristicPlayer(BasicMCTSParams params) {
        super(params);
        params.budgetType = PlayerConstants.BUDGET_TIME;
        params.budget = 40;
        params.rolloutLength = 10;
        params.maxTreeDepth = 30;
        params.heuristic = new ReneePartialSetHeuristic();
        setName("Renee Partial Set Heuristic");
    }

    @Override
    public ReneePartialSetHeuristicPlayer copy() {
        return new ReneePartialSetHeuristicPlayer((BasicMCTSParams) getParameters().copy());
    }
}
