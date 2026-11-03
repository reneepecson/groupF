package groupF;

import players.basicMCTS.BasicMCTSParams;
import players.basicMCTS.BasicMCTSPlayer;

public class SushiGoMCTSParamsV6 extends BasicMCTSParams {

    @Override
    protected SushiGoMCTSParamsV6 _copy() {
        return new SushiGoMCTSParamsV6();
    }

    @Override
    public BasicMCTSPlayer instantiate() {
        return new SushiGoMCTSPlayerV6(
                (SushiGoMCTSParamsV6) this.copy());
    }
}
