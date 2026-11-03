
package groupF;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import games.sushigo.SGGameState;
import games.sushigo.SGParameters;
import games.sushigo.cards.SGCard;
import core.components.Counter;

import java.util.EnumMap;
import java.util.Map;

public class SushiGoHeuristicV6 implements IStateHeuristic {

    private final Map<SGCard.SGCardType, Integer> observedCardTypes;

    public SushiGoHeuristicV6() {
        this(new EnumMap<>(SGCard.SGCardType.class));
    }

    public SushiGoHeuristicV6(
            Map<SGCard.SGCardType, Integer> observedCardTypes) {
        this.observedCardTypes = observedCardTypes;
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        SGGameState state = (SGGameState) gs;

        if (!state.isNotTerminal()) {
            return state.getPlayerResults()[playerId].value;
        }

        double estimatedScore =
                state.getPlayerScore()[playerId].getValue();

        Map<SGCard.SGCardType, Counter> counts =
                state.getPlayedCardTypes()[playerId];

        SGParameters params =
                (SGParameters) state.getGameParameters();

        int remainingPicks =
                state.getPlayerHands().get(playerId).getSize();

        // Estimate future value from incomplete Tempura pairs.
        int tempuraCount = count(counts, SGCard.SGCardType.Tempura);

        if (tempuraCount % 2 == 1 && remainingPicks > 0) {
            double completionProbability =
                    completionProbability(
                            remainingPicks,
                            observationStrength(SGCard.SGCardType.Tempura));

            estimatedScore += params.valueTempuraPair
                    * completionProbability;
        }

        // Estimate future value from incomplete Sashimi triples.
        int sashimiCount = count(counts, SGCard.SGCardType.Sashimi);
        int sashimiRemainder = sashimiCount % 3;

        if (sashimiRemainder != 0 && remainingPicks > 0) {
            int cardsNeeded = 3 - sashimiRemainder;

            double completionProbability =
                    completionProbability(
                            remainingPicks,
                            observationStrength(SGCard.SGCardType.Sashimi));

            // A triple needs one or two additional cards.
            if (cardsNeeded > remainingPicks) {
                completionProbability = 0.0;
            } else if (cardsNeeded == 2) {
                // Two additional cards are harder to collect.
                completionProbability *= 0.55;
            }

            estimatedScore += params.valueSashimiTriple
                    * completionProbability;
        }

        // Conservative estimate of future Dumpling value.
        int dumplingCount = count(counts, SGCard.SGCardType.Dumpling);
        int[] dumplingValues = params.valueDumpling;

        if (dumplingCount > 0 && dumplingCount < dumplingValues.length) {
            int nextValue = dumplingValues[dumplingCount];
            estimatedScore += nextValue * 0.20;
        }

        // Small estimate for unused Wasabi.
        int unusedWasabi = count(counts, SGCard.SGCardType.Wasabi);

        estimatedScore += unusedWasabi
                * params.valueSquidNigiri
                * (params.multiplierWasabi - 1)
                * 0.10;

        // Keep the heuristic on a stable scale for MCTS.
        return Math.max(-1.0, Math.min(1.0, estimatedScore / 50.0));
    }

    private double completionProbability(
            int remainingPicks,
            double observationStrength) {

        // Conservative starting estimate; observations can adjust it
        // slightly but cannot make completion certain.
        double probability;

        if (remainingPicks >= 3) {
            probability = 0.45;
        } else if (remainingPicks == 2) {
            probability = 0.30;
        } else {
            probability = 0.15;
        }

        probability += 0.10 * observationStrength;

        return Math.min(0.55, probability);
    }

    private double observationStrength(SGCard.SGCardType type) {
        int sightings = observedCardTypes.getOrDefault(type, 0);

        // Cap the memory signal to avoid allowing observations
        // to dominate the current score.
        return Math.min(sightings, 5) / 5.0;
    }

    private int count(
            Map<SGCard.SGCardType, Counter> counts,
            SGCard.SGCardType type) {

        Counter counter = counts.get(type);
        return counter == null ? 0 : counter.getValue();
    }
}

