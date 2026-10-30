package groupF;

import players.basicMCTS.BasicMCTSParams;

/**
 * Backwards-compatible name for the groupF-only MuditBot_v2 submission agent.
 */
public class MuditRenneBot extends MuditBot_v2 {

    public MuditRenneBot() {
        super();
        setName("MuditRenneBot");
    }

    public MuditRenneBot(long seed) {
        super(seed);
        setName("MuditRenneBot");
    }

    public MuditRenneBot(BasicMCTSParams params) {
        super(params);
        setName("MuditRenneBot");
    }

    @Override
    public MuditRenneBot copy() {
        return new MuditRenneBot((BasicMCTSParams) getParameters().copy());
    }
}
