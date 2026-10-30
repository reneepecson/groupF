package players.muditbot;

import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.components.Deck;
import games.GameType;
import games.sushigo.SGGameState;
import games.sushigo.actions.ChooseCard;
import games.sushigo.cards.SGCard;
import org.junit.Test;
import players.PlayerFactory;
import players.PlayerConstants;
import players.simple.RandomPlayer;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class MuditBotTest {

    @Test
    public void prioritisesHighValueNigiriOverEgg() {
        MuditBot bot = new MuditBot(7);
        Game game = newGame(bot);
        SGGameState state = (SGGameState) game.getGameState();
        state.getPlayerHands().get(0).clear();
        state.getPlayerHands().get(0).add(new SGCard(SGCard.SGCardType.EggNigiri));
        state.getPlayerHands().get(0).add(new SGCard(SGCard.SGCardType.SquidNigiri));

        SGGameState observation = (SGGameState) state.copy(0);
        SGGameState eggPicked = (SGGameState) observation.copy(0);
        eggPicked.addCardChoice(new ChooseCard(0, cardIndex(eggPicked, SGCard.SGCardType.EggNigiri), false), 0);
        SGGameState squidPicked = (SGGameState) observation.copy(0);
        squidPicked.addCardChoice(new ChooseCard(0, cardIndex(squidPicked, SGCard.SGCardType.SquidNigiri), false), 0);
        assertTrue(MuditBot.evaluateState(squidPicked, 0) > MuditBot.evaluateState(eggPicked, 0));
    }

    @Test
    public void prioritisesWasabiAheadOfTempuraOnAnEarlyPick() {
        MuditBot bot = new MuditBot(11);
        Game game = newGame(bot);
        SGGameState state = (SGGameState) game.getGameState();
        state.getPlayerHands().get(0).clear();
        state.getPlayerHands().get(0).add(new SGCard(SGCard.SGCardType.Tempura));
        state.getPlayerHands().get(0).add(new SGCard(SGCard.SGCardType.Wasabi));

        SGGameState observation = (SGGameState) state.copy(0);
        SGGameState tempuraPicked = (SGGameState) observation.copy(0);
        tempuraPicked.addCardChoice(new ChooseCard(0, cardIndex(tempuraPicked, SGCard.SGCardType.Tempura), false), 0);
        SGGameState wasabiPicked = (SGGameState) observation.copy(0);
        wasabiPicked.addCardChoice(new ChooseCard(0, cardIndex(wasabiPicked, SGCard.SGCardType.Wasabi), false), 0);
        assertTrue(MuditBot.evaluateState(wasabiPicked, 0) > MuditBot.evaluateState(tempuraPicked, 0));
    }

    @Test
    public void mctsChoosesALegalDraftAction() {
        MuditBot bot = new MuditBot(17);
        Game game = newGame(bot);
        SGGameState observation = (SGGameState) game.getGameState().copy(0);
        List<AbstractAction> actions = game.getForwardModel().computeAvailableActions(observation);

        AbstractAction chosen = bot.getAction(observation, actions);

        assertTrue(chosen instanceof ChooseCard);
        assertTrue(actions.contains(chosen));
    }

    @Test
    public void rolloutPolicyChoosesHigherValueNigiri() {
        MuditBotCardPicking bot = new MuditBotCardPicking(19);
        Game game = newGame(bot);
        SGGameState state = (SGGameState) game.getGameState();
        state.getPlayerHands().get(0).clear();
        state.getPlayerHands().get(0).add(new SGCard(SGCard.SGCardType.EggNigiri));
        state.getPlayerHands().get(0).add(new SGCard(SGCard.SGCardType.SquidNigiri));
        List<AbstractAction> actions = game.getForwardModel().computeAvailableActions(state);

        AbstractAction chosen = bot.getRolloutAction(state, actions, new RandomPlayer(new Random(5)));

        assertEquals(SGCard.SGCardType.SquidNigiri,
                ((SGCard) ((ChooseCard) chosen).getCard(state)).type);
    }

    @Test
    public void knownPlayedCardsReduceRemainingCardProbability() {
        MuditBot bot = new MuditBot(21);
        Game game = newGame(bot);
        SGGameState state = (SGGameState) game.getGameState().copy(0);
        double before = MuditBot.estimateAvailability(state, 0)
                .probability(SGCard.SGCardType.Tempura);
        state.getPlayedCardTypesAllGame()[1].get(SGCard.SGCardType.Tempura).increment(1);

        double after = MuditBot.estimateAvailability(state, 0)
                .probability(SGCard.SGCardType.Tempura);

        assertTrue(after < before);
    }

    @Test
    public void knownOpponentHandAffectsRemainingCardProbability() {
        MuditBot bot = new MuditBot(22);
        Game game = newGame(bot);
        SGGameState state = (SGGameState) game.getGameState();
        for (int turn = 0; turn < 2 && !state.isHandKnown(0, 1); turn++) {
            List<AbstractAction> actions = game.getForwardModel().computeAvailableActions(state);
            game.getForwardModel().next(state, actions.getFirst());
        }
        assertTrue(state.isHandKnown(0, 1));
        double before = MuditBot.estimateAvailability(state, 0)
                .probability(SGCard.SGCardType.Tempura);

        Deck<SGCard> knownHand = state.getPlayerHands().get(1);
        knownHand.add(new SGCard(SGCard.SGCardType.Tempura));
        double after = MuditBot.estimateAvailability(state, 0)
                .probability(SGCard.SGCardType.Tempura);

        assertTrue(after < before);
    }

    @Test
    public void projectedStateValueReflectsScoreLead() {
        MuditBot bot = new MuditBot(23);
        Game game = newGame(bot);
        SGGameState state = (SGGameState) game.getGameState().copy(0);
        double tiedValue = MuditBot.evaluateState(state, 0);
        state.getPlayerScore()[0].increment(20);

        double leadingValue = MuditBot.evaluateState(state, 0);

        assertTrue(leadingValue > tiedValue);
        assertTrue(leadingValue <= 1.0);
    }

    @Test
    public void searchDefaultsAndHeuristicSurviveCopy() {
        MuditBot bot = new MuditBot(29);
        MuditBot copy = bot.copy();

        assertEquals(PlayerConstants.BUDGET_TIME, bot.getParameters().budgetType);
        assertEquals(50, bot.getParameters().budget);
        assertEquals(100, bot.getParameters().rolloutLength);
        assertEquals(bot.getParameters().budgetType, copy.getParameters().budgetType);
        assertEquals(bot.getParameters().budget, copy.getParameters().budget);
        assertNotNull(copy.getParameters().heuristic);
    }

    @Test
    public void playerFactoryRecognisesMuditBot() {
        AbstractPlayer player = PlayerFactory.createPlayer("muditbot");

        assertTrue(player instanceof MuditBot);
        assertFalse(player instanceof MuditBotCardPicking);
    }

    @Test
    public void playerFactoryRecognisesCardPickingVariant() {
        AbstractPlayer player = PlayerFactory.createPlayer("muditbot_cardpicking");

        assertTrue(player instanceof MuditBotCardPicking);
    }

    private Game newGame(MuditBot bot) {
        Game game = GameType.SushiGo.createGameInstance(2, 1234);
        game.reset(List.of(bot, new RandomPlayer(new Random(42))));
        return game;
    }

    private int cardIndex(SGGameState state, SGCard.SGCardType type) {
        for (int i = 0; i < state.getPlayerHands().get(0).getSize(); i++) {
            if (state.getPlayerHands().get(0).get(i).type == type) {
                return i;
            }
        }
        fail("Card not found: " + type);
        return -1;
    }
}
