
package groupF;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.sushigo.SGGameState;
import games.sushigo.cards.SGCard;
import players.basicMCTS.BasicMCTSPlayer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class SushiGoMCTSPlayerV6 extends BasicMCTSPlayer {

    // Shared with the heuristic used by this player.
    private final Map<SGCard.SGCardType, Integer> observedCardTypes =
            new EnumMap<>(SGCard.SGCardType.class);

    private List<SGCard.SGCardType> previousHand = new ArrayList<>();
    private int previousRound = -1;

    public SushiGoMCTSPlayerV6(SushiGoMCTSParamsV6 params) {
        super(params);
        setStateHeuristic(new SushiGoHeuristicV6(observedCardTypes));
        setName("SushiGoMCTS");
    }

    @Override
    public AbstractAction _getAction(
            AbstractGameState gameState,
            List<AbstractAction> actions) {

        // This method is called for the real player's decision.
        // Update memory before MCTS searches from the current state.
        if (gameState instanceof SGGameState) {
            rememberOwnHand((SGGameState) gameState);
        }

        return super._getAction(gameState, actions);
    }

    private void rememberOwnHand(SGGameState state) {
        int playerId = getPlayerID();

        if (playerId < 0 ||
                playerId >= state.getPlayerHands().size()) {
            return;
        }

        int currentRound = state.getRoundCounter();

        if (currentRound != previousRound) {
            observedCardTypes.clear();
            previousHand.clear();
            previousRound = currentRound;
        }

        List<SGCard.SGCardType> currentHand = new ArrayList<>();

        for (SGCard card :
                state.getPlayerHands().get(playerId).getComponents()) {
            currentHand.add(card.type);
        }

        // Count only increases in the number of cards of each type
        // relative to the previous observed hand.
        // This reduces repeated counting of unchanged cards.
        Map<SGCard.SGCardType, Integer> oldCounts =
                countTypes(previousHand);
        Map<SGCard.SGCardType, Integer> newCounts =
                countTypes(currentHand);

        for (Map.Entry<SGCard.SGCardType, Integer> entry
                : newCounts.entrySet()) {
            SGCard.SGCardType type = entry.getKey();
            int increase = entry.getValue()
                    - oldCounts.getOrDefault(type, 0);

            if (increase > 0) {
                observedCardTypes.merge(type, increase, Integer::sum);
            }
        }

        previousHand = currentHand;
    }

    private Map<SGCard.SGCardType, Integer> countTypes(
            List<SGCard.SGCardType> cards) {

        Map<SGCard.SGCardType, Integer> counts =
                new EnumMap<>(SGCard.SGCardType.class);

        for (SGCard.SGCardType type : cards) {
            counts.merge(type, 1, Integer::sum);
        }

        return counts;
    }

    @Override
    public void initializePlayer(AbstractGameState gameState) {
        observedCardTypes.clear();
        previousHand.clear();
        previousRound = -1;
        super.initializePlayer(gameState);
    }

    @Override
    public SushiGoMCTSPlayerV6 copy() {
        SushiGoMCTSPlayerV6 copy = new SushiGoMCTSPlayerV6(
                (SushiGoMCTSParamsV6) getParameters().copy());

        copy.setName(toString());
        return copy;
    }
}

