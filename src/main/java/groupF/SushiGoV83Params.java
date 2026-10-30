
package groupF;

import players.basicMCTS.BasicMCTSParams;
import players.basicMCTS.BasicMCTSPlayer;

public class SushiGoV83Params extends BasicMCTSParams {

    // Memory-aware estimates
    public double memoryWeight = 0.30;

    // Opponent blocking
    public double blockingWeight = 0.20;

    // Chopsticks timing
    public double chopsticksWeight = 0.25;

    @Override
    protected SushiGoV83Params _copy() {
        SushiGoV83Params copy = new SushiGoV83Params();

        copy.K = K;
        copy.rolloutLength = rolloutLength;
        copy.maxTreeDepth = maxTreeDepth;
        copy.epsilon = epsilon;
        copy.heuristic = heuristic;

        copy.memoryWeight = memoryWeight;
        copy.blockingWeight = blockingWeight;
        copy.chopsticksWeight = chopsticksWeight;

        return copy;
    }

    @Override
    public BasicMCTSPlayer instantiate() {
        return new SushiGoV83Player(
                (SushiGoV83Params) this.copy());
    }
}
