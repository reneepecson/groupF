package groupF;

import core.Game;
import games.GameType;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MuditRenneBotTest {

    @Test
    public void combinesBothHeuristicsAndCopiesItsSettings() {
        MuditRenneBot bot = new MuditRenneBot(740234);
        Game game = GameType.SushiGo.createGameInstance(2, 1234);
        game.reset(List.of(bot, new RandomPlayer()));
        double value = bot.getParameters().heuristic.evaluateState(game.getGameState(), 0);

        MuditRenneBot copy = bot.copy();
        double copiedValue = copy.getParameters().heuristic.evaluateState(game.getGameState(), 0);

        assertTrue(Double.isFinite(value));
        assertEquals(value, copiedValue, 1e-9);
        assertNotNull(copy.getParameters().heuristic);
        assertEquals("MuditRenneBot", copy.toString());
    }
}
