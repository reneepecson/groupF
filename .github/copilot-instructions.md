# Copilot instructions for Tabletop Games

## Project overview

This repository is TAG (Tabletop Games Framework), a Java 21 Maven project for game AI research. It provides a shared API for implementing board/card/dice games, AI agents, tournaments, and analysis pipelines.

The codebase is organized around a few central abstractions:

- `core` contains the framework primitives: `AbstractGameState`, `AbstractForwardModel`, `AbstractPlayer`, `AbstractParameters`, `Game`, and the action/event abstractions.
- `games` contains concrete game implementations and the canonical registry in `games.GameType`; new games are added here and must be wired into the enum.
- `players` contains agents and player decorators; `players.PlayerFactory.createPlayer(...)` is how JSON descriptors are turned into runtime agents.
- `evaluation` contains experiment runners, tournaments, optimisation, metrics, and listeners.
- `json`, `metrics`, and `data` hold runtime configs, listener definitions, and game assets.
- `llm` integrates language-model-based heuristics and code generation; it depends on provider API keys in the environment.

## Build, test, and validation commands

This project uses Maven and Java 21.

```bash
mvn package
mvn test -Dmaven.test.skip=false
mvn test -Dmaven.test.skip=false -Dtest=CustomDiceTest
mvn test -Dmaven.test.skip=false -Dtest=CustomDiceTest#methodName
mvn test-compile -Dmaven.test.skip=false
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt
```

Important notes:

- Tests are skipped by default because `maven.test.skip=true` is set in `pom.xml`.
- Use targeted test execution instead of the full suite when validating a change; the project’s full test run is intentionally broad and may include slow integration coverage.
- There is no dedicated lint target or formatter configuration in this repo; compile, targeted tests, and Maven packaging are the expected validation path.
- To run a single JUnit 4 test directly from the command line:

```bash
java -cp "target/classes:target/test-classes:$(cat cp.txt)" \
  org.junit.runner.JUnitCore games.cantstop.TestCantStop
```

For the packaged game runner:

```bash
java -jar target/TAG.jar <EntryPoint> [args...]
```

Common entry points include `RunGames`, `ParameterSearch`, `FrontEnd`, `FrontEndSimple`, `ExpertIteration`, `OneStepDeviations`, and `SkillLadder`.

## Architecture and working model

The core design is game-state driven:

- `AbstractGameState` owns all mutable game data and defines the state copy/serialization/score contract.
- `AbstractForwardModel` applies rules, computes legal actions, and advances the game state.
- `Game` orchestrates resets, turn flow, listeners, statistics, and player interaction.
- `AbstractAction` represents a move, and `IExtendedSequence` is used for multi-step decisions that extend across turns.

When working on a new game or rule change, the usual pattern is to update the game implementation and its registration in `games.GameType`, rather than patching ad hoc code elsewhere.

The player/agent side is also framework-shaped:

- `core.AbstractPlayer` defines the decision API (`_getAction` + `copy()`).
- Game-specific and generic players live under `players/`.
- JSON-based player definitions in `json/players` are instantiated through `PlayerFactory`, which is how tournaments and UI tooling configure agents.

Evaluation and experiment code are separate from game logic:

- `evaluation` contains the tournament and search infrastructure.
- `metrics` and `json` contain listener and experiment configs.
- `outputdir` and `metrics/out` are common result locations.

## Codebase conventions and repo-specific patterns

- Use JUnit 4 conventions. The project is not set up for JUnit 5; tests use `@Before`, `@Test`, and standard JUnit 4 assertions.
- Keep tests under `src/test/java` mirroring the main source tree, e.g. `games/<name>/...` or `core/...`.
- Prefer test names that describe behaviour (`availableActionsExcludeBlockedMoves`) over generic `testX` naming.
- For integration tests, prefer constructing a `Game` and calling `reset(...)` with real players when the scenario requires full framework flow.
- For smaller unit tests, prefer direct instantiation of the state and forward model, rather than simulation-heavy setup.
- When a saved game state is available in JSON form, prefer loading it and editing the JSON scenario rather than mutating private fields through reflection.
- If a GUI/rendering harness is used for inspection, remove any throwaway PNG or debugging artifact before committing.
- If a fix adds behavior or prevents a regression, commit the corresponding test with it.
- Add new games to the central registry in `games.GameType`; do not treat that enum as optional metadata.
- When working with generated heuristic or LLM code, treat `llm` output as code that must compile and fit the project’s Java patterns.

## Usage notes

- Java source generally targets a single, shared game-engine abstraction instead of per-game ad hoc patterns; follow established abstractions when extending the framework.
- JSON-based configuration is a first-class part of the project; many runtime choices (players, listeners, experiments, learners) are driven from files under `json/`.
- Project docs and architecture summary are tracked in `AGENTS.md`; use that as the repo-specific source of authoritative context when the code is ambiguous.
