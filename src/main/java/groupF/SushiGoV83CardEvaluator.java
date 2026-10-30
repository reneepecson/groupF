
package groupF;

import core.components.Counter;
import core.components.Deck;
import games.sushigo.SGGameState;
import games.sushigo.SGParameters;
import games.sushigo.actions.ChooseCard;
import games.sushigo.cards.SGCard;
import games.sushigo.cards.SGCard.SGCardType;

import java.util.Map;

public final class SushiGoV83CardEvaluator {

    private SushiGoV83CardEvaluator() {}

    public static double evaluateAction(
            SGGameState state,
            int playerId,
            ChooseCard action,
            SushiGoV83Params v83) {

        SGParameters rules =
                (SGParameters) state.getGameParameters();

        SGCard card = (SGCard) action.getCard(state);

        double value = cardValue(
                state, playerId, card.type, rules, v83);

        // Penalise giving opponents a useful card.
        value -= v83.blockingWeight
                * opponentBenefit(state, playerId, card.type, rules);

        // On the first pick, consider whether a second pick is worthwhile.
        if (action.useChopsticks) {
            Deck<SGCard> hand =
                    state.getPlayerHands().get(playerId);

            double bestSecond = 0.0;

            for (int i = 0; i < hand.getSize(); i++) {
                if (i == action.cardIdx) {
                    continue;
                }

                SGCard second = hand.get(i);

                double secondValue = cardValue(
                        state, playerId, second.type, rules, v83);

                secondValue -= v83.blockingWeight
                        * opponentBenefit(
                                state, playerId, second.type, rules);

                bestSecond = Math.max(bestSecond, secondValue);
            }

            value += v83.chopsticksWeight
                    * Math.max(0.0, bestSecond);
        }

        return value;
    }

    public static double evaluatePotential(
            SGGameState state,
            int playerId,
            SushiGoV83Params v83) {

        SGParameters rules =
                (SGParameters) state.getGameParameters();

        Map<SGCardType, Counter> cards =
                state.getPlayedCardTypes()[playerId];

        int handSize =
                state.getPlayerHands().get(playerId).getSize();

        double potential = 0.0;

        int tempura = count(cards, SGCardType.Tempura);
        if (tempura % 2 == 1) {
            potential += rules.valueTempuraPair
                    * completionProbability(
                            state, playerId, SGCardType.Tempura,
                            handSize, v83);
        }

        int sashimi = count(cards, SGCardType.Sashimi);
        int needed = (3 - sashimi % 3) % 3;

        if (needed > 0) {
            double probability = completionProbability(
                    state, playerId, SGCardType.Sashimi,
                    handSize, v83);

            if (needed == 2) probability *= 0.55;
            if (needed == 3) probability *= 0.25;

            potential += rules.valueSashimiTriple * probability;
        }

        int dumplings = count(cards, SGCardType.Dumpling);
        if (dumplings > 0 && dumplings < rules.valueDumpling.length) {
            potential += rules.valueDumpling[dumplings] * 0.20;
        }

        return potential;
    }

    private static double cardValue(
            SGGameState state,
            int playerId,
            SGCardType type,
            SGParameters rules,
            SushiGoV83Params v83) {

        Map<SGCardType, Counter> cards =
                state.getPlayedCardTypes()[playerId];

        int current = count(cards, type);
        int handSize =
                state.getPlayerHands().get(playerId).getSize();

        switch (type) {
            case Tempura:
                return current % 2 == 0
                        ? rules.valueTempuraPair
                            * completionProbability(
                                state, playerId, type, handSize, v83)
                        : rules.valueTempuraPair;

            case Sashimi:
                // Control version: no experimental multipliers.
                return rules.valueSashimiTriple
                        * completionProbability(
                            state, playerId, type, handSize, v83);

            case Dumpling:
                int next = Math.min(
                        current, rules.valueDumpling.length - 1);
                return rules.valueDumpling[next];

            case SquidNigiri:
                return hasWasabi(state, playerId)
                        ? rules.valueSquidNigiri * rules.multiplierWasabi
                        : rules.valueSquidNigiri;

            case SalmonNigiri:
                return hasWasabi(state, playerId)
                        ? rules.valueSalmonNigiri * rules.multiplierWasabi
                        : rules.valueSalmonNigiri;

            case EggNigiri:
                return hasWasabi(state, playerId)
                        ? rules.valueEggNigiri * rules.multiplierWasabi
                        : rules.valueEggNigiri;

            case Wasabi:
                return 1.5;

            case Chopsticks:
                return handSize >= 3 ? 1.0 : 0.15;

            case Maki:
                return 1.5;

            case Pudding:
                return 1.2;

            default:
                return 0.0;
        }
    }

    private static double completionProbability(
            SGGameState state,
            int playerId,
            SGCardType type,
            int remainingPicks,
            SushiGoV83Params v83) {

        double base;

        if (remainingPicks >= 5) base = 0.50;
        else if (remainingPicks == 4) base = 0.42;
        else if (remainingPicks == 3) base = 0.32;
        else if (remainingPicks == 2) base = 0.22;
        else if (remainingPicks == 1) base = 0.12;
        else return 0.0;

        int visibleCopies = 0;

        for (int opponent = 0;
             opponent < state.getNPlayers();
             opponent++) {

            if (opponent == playerId
                    || !state.isHandKnown(playerId, opponent)) {
                continue;
            }

            Deck<SGCard> hand =
                    state.getPlayerHands().get(opponent);

            for (SGCard card : hand) {
                if (card.type == type) {
                    visibleCopies++;
                }
            }
        }

        // Visible copies may help later, but opponents may select them first.
        double adjustment = 1.0
                - Math.min(0.55,
                    visibleCopies * v83.memoryWeight * 0.10);

        return Math.max(0.04, Math.min(0.60, base * adjustment));
    }

    public static double opponentThreat(
            SGGameState state,
            int playerId) {

        double threat = 0.0;

        for (int opponent = 0;
             opponent < state.getNPlayers();
             opponent++) {

            if (opponent == playerId) continue;

            Map<SGCardType, Counter> cards =
                    state.getPlayedCardTypes()[opponent];

            if (count(cards, SGCardType.Tempura) % 2 == 1) {
                threat += 5.0 * 0.15;
            }

            if (count(cards, SGCardType.Sashimi) % 3 != 0) {
                threat += 10.0 * 0.12;
            }
        }

        return threat;
    }

    private static double opponentBenefit(
            SGGameState state,
            int playerId,
            SGCardType type,
            SGParameters rules) {

        double best = 0.0;

        for (int opponent = 0;
             opponent < state.getNPlayers();
             opponent++) {

            if (opponent == playerId) continue;

            Map<SGCardType, Counter> cards =
                    state.getPlayedCardTypes()[opponent];

            int current = count(cards, type);
            double benefit = 0.0;

            if (type == SGCardType.Tempura) {
                benefit = current % 2 == 1
                        ? rules.valueTempuraPair
                        : rules.valueTempuraPair * 0.15;
            } else if (type == SGCardType.Sashimi) {
                benefit = current % 3 == 2
                        ? rules.valueSashimiTriple
                        : rules.valueSashimiTriple * 0.15;
            } else if (type == SGCardType.Dumpling) {
                int index = Math.min(
                        current, rules.valueDumpling.length - 1);
                benefit = rules.valueDumpling[index];
            } else if (type == SGCardType.SquidNigiri) {
                benefit = rules.valueSquidNigiri;
            } else if (type == SGCardType.SalmonNigiri) {
                benefit = rules.valueSalmonNigiri;
            } else if (type == SGCardType.EggNigiri) {
                benefit = rules.valueEggNigiri;
            }

            best = Math.max(best, benefit);
        }

        return best;
    }

    private static boolean hasWasabi(
            SGGameState state, int playerId) {

        return count(
                state.getPlayedCardTypes()[playerId],
                SGCardType.Wasabi) > 0;
    }

    private static int count(
            Map<SGCardType, Counter> cards,
            SGCardType type) {

        Counter counter = cards.get(type);
        return counter == null ? 0 : counter.getValue();
    }
}
