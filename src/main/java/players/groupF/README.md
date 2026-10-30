# MuditBot_v2 submission

`MuditBot_v2` is intended to compile and run when the `groupF` Java package is
copied into a clean TAG checkout. Its MCTS search and tree node are included as
`MuditMCTSPlayer` and `MuditTreeNode`; the agent does not extend TAG's
`BasicMCTSPlayer` or rely on a custom rollout hook.

The only framework classes it uses for search configuration and game execution
are part of TAG's standard API, including `BasicMCTSParams`. The heuristic
combines Mudit's projected score and card-synergy estimate with
`ReneePartialSetHeuristic`.

The `MuditBot_v2.json` descriptor names the class for TAG's JSON player
configuration. If the clean checkout expects descriptors under `json/players`,
copy this JSON file there unchanged.
