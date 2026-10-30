package groupF;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IStateHeuristic;
import games.sushigo.SGGameState;
import games.sushigo.SGParameters;
import games.sushigo.actions.ChooseCard;
import games.sushigo.cards.SGCard;
import players.PlayerConstants;
import players.basicMCTS.BasicMCTSParams;
import utilities.Pair;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static games.sushigo.cards.SGCard.SGCardType.*;

/**
 * Submission-ready Mudit bot: groupF-owned MCTS with Mudit's score projection,
 * blended with Renee's partial-set estimate.
 */
public class MuditBot_v2 extends MuditMCTSPlayer {
    private final ReneePartialSetHeuristic reneeHeuristic = new ReneePartialSetHeuristic();

    public MuditBot_v2() {
        this(System.currentTimeMillis());
    }

    public MuditBot_v2(long seed) {
        super(seed);
        configure();
    }

    public MuditBot_v2(BasicMCTSParams params) {
        super(params);
        configure();
    }

    private void configure() {
        BasicMCTSParams params = getParameters();
        params.setParameterValue("budgetType", PlayerConstants.BUDGET_TIME);
        params.setParameterValue("budget", 50);
        params.setParameterValue("rolloutLength", 100);
        params.setParameterValue("maxTreeDepth", 5);
        params.setParameterValue("heuristic", (IStateHeuristic) this::evaluateCombinedState);
        params.budgetType = PlayerConstants.BUDGET_TIME;
        params.budget = 50;
        params.rolloutLength = 100;
        params.maxTreeDepth = 5;
        params.heuristic = this::evaluateCombinedState;
        setName("MuditBot_v2");
    }

    private double evaluateCombinedState(AbstractGameState gameState, int playerId) {
        if (!(gameState instanceof SGGameState state) || !state.isNotTerminal()) {
            return gameState.getHeuristicScore(playerId);
        }
        double muditValue = evaluateMuditState(state, playerId);
        double reneeValue = logistic(4.0 * reneeHeuristic.evaluateState(state, playerId));
        return (muditValue + reneeValue) / 2.0;
    }

    private static double evaluateMuditState(SGGameState state, int playerId) {
        SGParameters params = (SGParameters) state.getGameParameters();
        int nPlayers = state.getNPlayers();
        int[] remainingPicks = new int[nPlayers];
        double[] projectedScores = new double[nPlayers];
        double[] probabilities = cardProbabilities(params);

        for (int player = 0; player < nPlayers; player++) {
            remainingPicks[player] = Math.max(0,
                    state.getPlayerHands().get(player).getSize() - state.getCardChoices().get(player).size());
            Map<SGCard.SGCardType, Integer> counts = effectiveCounts(state, player);
            projectedScores[player] = state.getGameScore(player)
                    + pendingPoints(state, params, player)
                    + expectedFuturePoints(params, counts, remainingPicks[player], probabilities);
        }

        double uncertainty = 5.0 + Math.max(0, params.nRounds - state.getRoundCounter() - 1) * 3.0;
        for (int picks : remainingPicks) {
            uncertainty += picks * 0.15;
        }
        double total = 0.0;
        for (int opponent = 0; opponent < nPlayers; opponent++) {
            if (opponent != playerId) {
                total += logistic((projectedScores[playerId] - projectedScores[opponent]) / uncertainty);
            }
        }
        return total / (nPlayers - 1);
    }

    private static Map<SGCard.SGCardType, Integer> effectiveCounts(SGGameState state, int player) {
        Map<SGCard.SGCardType, Integer> counts = new EnumMap<>(SGCard.SGCardType.class);
        for (SGCard.SGCardType type : SGCard.SGCardType.values()) {
            counts.put(type, state.getPlayedCardTypes()[player].get(type).getValue());
        }
        for (ChooseCard choice : state.getCardChoices().get(player)) {
            SGCard card = (SGCard) choice.getCard(state);
            counts.merge(card.type, card.count, Integer::sum);
            if (isNigiri(card.type) && counts.get(Wasabi) > 0) {
                counts.merge(Wasabi, -1, Integer::sum);
            }
        }
        return counts;
    }

    private static double pendingPoints(SGGameState state, SGParameters params, int player) {
        Map<SGCard.SGCardType, Integer> counts = new EnumMap<>(SGCard.SGCardType.class);
        for (SGCard.SGCardType type : SGCard.SGCardType.values()) {
            counts.put(type, state.getPlayedCardTypes()[player].get(type).getValue());
        }
        double points = 0.0;
        for (ChooseCard choice : state.getCardChoices().get(player)) {
            SGCard card = (SGCard) choice.getCard(state);
            switch (card.type) {
                case EggNigiri -> points += params.valueEggNigiri * wasabiMultiplier(params, counts);
                case SalmonNigiri -> points += params.valueSalmonNigiri * wasabiMultiplier(params, counts);
                case SquidNigiri -> points += params.valueSquidNigiri * wasabiMultiplier(params, counts);
                case Tempura -> {
                    if ((counts.get(Tempura) + card.count) / 2 > counts.get(Tempura) / 2) {
                        points += params.valueTempuraPair;
                    }
                }
                case Sashimi -> {
                    if ((counts.get(Sashimi) + card.count) / 3 > counts.get(Sashimi) / 3) {
                        points += params.valueSashimiTriple;
                    }
                }
                case Dumpling -> {
                    for (int i = 0; i < card.count; i++) {
                        int index = Math.min(counts.get(Dumpling) + i, params.valueDumpling.length - 1);
                        points += params.valueDumpling[index];
                    }
                }
                default -> {
                }
            }
            counts.merge(card.type, card.count, Integer::sum);
        }
        return points;
    }

    private static double wasabiMultiplier(SGParameters params, Map<SGCard.SGCardType, Integer> counts) {
        if (counts.get(Wasabi) > 0) {
            counts.merge(Wasabi, -1, Integer::sum);
            return params.multiplierWasabi;
        }
        return 1.0;
    }

    private static double expectedFuturePoints(SGParameters params,
                                               Map<SGCard.SGCardType, Integer> counts,
                                               int picks,
                                               double[] probabilities) {
        if (picks == 0) {
            return 0.0;
        }
        double expected = expectedGroupedPoints(picks, probabilities[0], counts.get(Tempura), 2,
                params.valueTempuraPair);
        expected += expectedGroupedPoints(picks, probabilities[1], counts.get(Sashimi), 3,
                params.valueSashimiTriple);

        double dumplingProbability = probabilities[2] * 0.75;
        for (int future = 0; future <= picks; future++) {
            double probability = binomialProbability(picks, future, dumplingProbability);
            for (int i = 0; i < future; i++) {
                int index = counts.get(Dumpling) + i;
                if (index < params.valueDumpling.length) {
                    expected += probability * params.valueDumpling[index];
                }
            }
        }
        double nigiriPerPick = probabilities[3] * params.valueEggNigiri
                + probabilities[4] * params.valueSalmonNigiri
                + probabilities[5] * params.valueSquidNigiri;
        expected += picks * nigiriPerPick;
        expected += Math.min(counts.get(Wasabi), picks * (probabilities[3] + probabilities[4] + probabilities[5]))
                * (params.multiplierWasabi - 1.0) * 2.0;
        expected += Math.min(counts.get(Chopsticks), picks) * 2.2;
        return expected;
    }

    private static double[] cardProbabilities(SGParameters params) {
        Map<SGCard.SGCardType, Integer> cards = new EnumMap<>(SGCard.SGCardType.class);
        int total = 0;
        for (Map.Entry<Pair<SGCard.SGCardType, Integer>, Integer> entry : params.nCardsPerType.entrySet()) {
            cards.merge(entry.getKey().a, entry.getValue(), Integer::sum);
            total += entry.getValue();
        }
        if (total == 0) {
            return new double[6];
        }
        return new double[]{
                probability(cards, Tempura, total),
                probability(cards, Sashimi, total),
                probability(cards, Dumpling, total),
                probability(cards, EggNigiri, total),
                probability(cards, SalmonNigiri, total),
                probability(cards, SquidNigiri, total)
        };
    }

    private static double probability(Map<SGCard.SGCardType, Integer> cards,
                                      SGCard.SGCardType type, int total) {
        return cards.getOrDefault(type, 0) / (double) total;
    }

    private static double expectedGroupedPoints(int picks, double probability,
                                                int currentCount, int groupSize, int pointsPerGroup) {
        double expected = 0.0;
        int existingGroups = currentCount / groupSize;
        for (int future = 0; future <= picks; future++) {
            int groups = (currentCount + future) / groupSize;
            expected += binomialProbability(picks, future, probability)
                    * (groups - existingGroups) * pointsPerGroup;
        }
        return expected;
    }

    private static double binomialProbability(int trials, int successes, double probability) {
        if (successes < 0 || successes > trials) {
            return 0.0;
        }
        double combinations = 1.0;
        for (int i = 1; i <= successes; i++) {
            combinations *= (trials - (double) successes + i) / i;
        }
        return combinations * Math.pow(probability, successes)
                * Math.pow(1.0 - probability, trials - successes);
    }

    private static boolean isNigiri(SGCard.SGCardType type) {
        return type == EggNigiri || type == SalmonNigiri || type == SquidNigiri;
    }

    private static double logistic(double value) {
        if (value >= 0.0) {
            double exp = Math.exp(-value);
            return 1.0 / (1.0 + exp);
        }
        double exp = Math.exp(value);
        return exp / (1.0 + exp);
    }

    @Override
    public MuditBot_v2 copy() {
        return new MuditBot_v2((BasicMCTSParams) getParameters().copy());
    }
}
