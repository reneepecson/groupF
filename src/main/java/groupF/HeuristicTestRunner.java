package groupF;

import core.AbstractPlayer;
import core.Game;
import games.GameType;
import players.IAnyTimePlayer;
import players.PlayerConstants;
import players.basicMCTS.BasicMCTSParams;
import players.basicMCTS.BasicMCTSPlayer;
import players.mcts.MCTSPlayer;

import java.util.ArrayList;
import java.util.List;

public class HeuristicTestRunner {

    static double[] wilson(double p, double n) {
        double z = 1.96;
        double centre = (p + z * z / (2 * n)) / (1 + z * z / n);
        double half = z * Math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / (1 + z * z / n);
        return new double[]{centre - half, centre + half};
    }

    public static void main(String[] args) {
        int nGames = 99;        // start with 9, then 99; multiples of 3 keep seating balanced
        int budgetMs = 100;
        String[] names = {"BasicMCTS+heuristic", "BasicMCTS plain", "MCTSPlayer"};
        double[] wins = new double[3], scoreSum = new double[3], ordinalSum = new double[3];

        for (int g = 0; g < nGames; g++) {
            BasicMCTSPlayer modified = new BasicMCTSPlayer();
            BasicMCTSPlayer plain = new BasicMCTSPlayer();
            for (BasicMCTSPlayer m : new BasicMCTSPlayer[]{modified, plain}) {
                BasicMCTSParams params = m.getParameters();
                params.budgetType = PlayerConstants.BUDGET_TIME;
                params.budget = budgetMs;
            }
            modified.getParameters().heuristic = new ReneePartialSetHeuristic();

            MCTSPlayer advanced = new MCTSPlayer();
            if (advanced instanceof IAnyTimePlayer p) p.setBudget(budgetMs);
            else if (g == 0) System.out.println("WARNING: MCTSPlayer budget was not set");

            AbstractPlayer[] agents = {modified, plain, advanced};

            List<AbstractPlayer> seats = new ArrayList<>();
            int[] agentInSeat = new int[3];
            for (int s = 0; s < 3; s++) {
                agentInSeat[s] = (s + g) % 3;
                seats.add(agents[agentInSeat[s]]);
            }

            long start = System.currentTimeMillis();
            Game game = Game.runOne(GameType.SushiGo, null, seats, g, false, null, null, 0);

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
            System.out.println("Game " + (g + 1) + "/" + nGames + " took " + (System.currentTimeMillis() - start) + " ms");
        }

        System.out.println("Heuristic calls: " + ReneePartialSetHeuristic.calls);
        for (int a = 0; a < 3; a++) {
            double p = wins[a] / nGames;
            double[] ci = wilson(p, nGames);
            System.out.printf("%s: win rate %.3f [%.3f, %.3f], mean score %.2f, mean ordinal %.2f%n",
                    names[a], p, ci[0], ci[1], scoreSum[a] / nGames, ordinalSum[a] / nGames);
        }
    }
}