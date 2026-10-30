package groupF;

import players.basicMCTS.BasicMCTSParams;
import players.basicMCTS.BasicMCTSPlayer;

public class ReneeMCTSPlayer extends BasicMCTSPlayer {

    public ReneeMCTSPlayer() {
        super();
        installHeuristic();
    }

    public ReneeMCTSPlayer(BasicMCTSParams params) {
        super(params);
        installHeuristic();
    }

    // registered as a parameter AND set on the field, so it survives copy() and reset
    private void installHeuristic() {
        BasicMCTSParams p = getParameters();
        ReneePartialSetHeuristic h = new ReneePartialSetHeuristic();
        p.setParameterValue("heuristic", h);
        p.heuristic = h;
    }

    @Override
    public ReneeMCTSPlayer copy() {
        ReneeMCTSPlayer copy = new ReneeMCTSPlayer((BasicMCTSParams) parameters.copy());
        copy.setName(toString());
        return copy;
    }
}