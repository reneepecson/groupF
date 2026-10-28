package groupF;

import core.AbstractPlayer;
import core.Game;
import games.GameType;
import players.PlayerConstants;
import players.basicMCTS.BasicMCTSParams;
import players.basicMCTS.BasicMCTSPlayer;

import java.util.ArrayList;
import java.util.List;

public class TuningRunner {

    static double[] wilson(double p, double n) {
        double z = 1.96;
        double centre = (p + z * z / (2 * n)) / (1 + z * z / n);
        double half = z * Math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / (1 + z * z / n);
        return new double[]{centre - half, centre + half};
    }

    static BasicMCTSPlayer newMcts(int budgetMs) {
        BasicMCTSPlayer m = new BasicMCTSPlayer();
        BasicMCTSParams params = m.getParameters();
        params.budgetType = PlayerConstants.BUDGET_TIME;
        params.budget = budgetMs;
        return m;
    }

    public static void main(String[] args) {
        int nGames = 99;            // multiple of 3; about 12 minutes per setting
        int budgetMs = 100;
        int seedOffset = 1000;      // tuning seeds, so seeds 0-500 stay unused for the final runs
        double[] scales = {50.0, 20.0, 10.0};   // settings to try, one block of games each

        for (double scale : scales) {
            String[] names = {"MCTS+heuristic", "MCTS plain A", "MCTS plain B"};
            double[] wins = new double[3], scoreSum = new double[3], ordinalSum = new double[3];

            for (int g = 0; g < nGames; g++) {
                BasicMCTSPlayer modified = newMcts(budgetMs);
                ReneePartialSetHeuristic h = new ReneePartialSetHeuristic();
                h.scale = scale;                 // the setting being tested
                modified.getParameters().heuristic = h;
                AbstractPlayer[] agents = {modified, newMcts(budgetMs), newMcts(budgetMs)};

                // rotate seats so each agent plays each seat equally often
                List<AbstractPlayer> seats = new ArrayList<>();
                int[] agentInSeat = new int[3];
                for (int s = 0; s < 3; s++) {
                    agentInSeat[s] = (s + g) % 3;
                    seats.add(agents[agentInSeat[s]]);
                }

                Game game = Game.runOne(GameType.SushiGo, null, seats, seedOffset + g, false, null, null, 0);

                double[] sc = new double[3];
                double best = -Double.MAX_VALUE;
                for (int s = 0; s < 3; s++) {
                    sc[s] = game.getGameState().getGameScore(s);
                    best = Math.max(best, sc[s]);
                }
                int nWinners = 0;
                for (int s = 0; s < 3; s++) if (sc[s] == best) nWinners++;
                for (int s = 0; s < 3; s++) {
                    int a = agentInSeat[s];
                    scoreSum[a] += sc[s];
                    ordinalSum[a] += game.getGameState().getOrdinalPosition(s);
                    if (sc[s] == best) wins[a] += 1.0 / nWinners; // ties split the win
                }
            }

            System.out.println("==== scale = " + scale + " (" + nGames + " games, chance = 0.333) ====");
            for (int a = 0; a < 3; a++) {
                double p = wins[a] / nGames;
                double[] ci = wilson(p, nGames);
                System.out.printf("%s: win rate %.3f [%.3f, %.3f], mean score %.2f, mean ordinal %.2f%n",
                        names[a], p, ci[0], ci[1], scoreSum[a] / nGames, ordinalSum[a] / nGames);
            }
        }
    }
}