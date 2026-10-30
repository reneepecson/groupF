package groupF;

import core.Game;
import games.GameType;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MuditBotV2Test {

    @Test
    public void heuristicIsFiniteAndCopyKeepsSubmissionConfiguration() {
        MuditBot_v2 bot = new MuditBot_v2(740234);
        Game game = GameType.SushiGo.createGameInstance(2, 1234);
        game.reset(List.of(bot, new RandomPlayer()));
        double value = bot.getParameters().heuristic.evaluateState(game.getGameState(), 0);

        MuditBot_v2 copy = bot.copy();
        double copiedValue = copy.getParameters().heuristic.evaluateState(game.getGameState(), 0);

        assertTrue(Double.isFinite(value));
        assertEquals(value, copiedValue, 1e-9);
        assertNotNull(copy.getParameters().heuristic);
        assertEquals("MuditBot_v2", copy.toString());
    }

    @Test
    public void choosesLegalActionWithGroupOwnedSearch() {
        MuditBot_v2 bot = new MuditBot_v2(740235);
        Game game = GameType.SushiGo.createGameInstance(2, 1235);
        game.reset(List.of(bot, new RandomPlayer()));
        List<core.actions.AbstractAction> actions = bot.getForwardModel()
                .computeAvailableActions(game.getGameState(), bot.getParameters().actionSpace);

        assertNotNull(bot.getAction(game.getGameState(), actions));
    }
}
