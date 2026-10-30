package groupF;

import core.AbstractPlayer;
import core.Game;
import games.GameType;
import players.PlayerConstants;
import players.basicMCTS.BasicMCTSParams;
import players.basicMCTS.BasicMCTSPlayer;
import players.simple.RandomPlayer;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BaselineRunner {

    // 95% Wilson score interval for a proportion p measured over n games
    static double[] wilson(double p, double n) {
        double z = 1.96;
        double centre = (p + z * z / (2 * n)) / (1 + z * z / n);
        double half = z * Math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / (1 + z * z / n);
        return new double[]{centre - half, centre + half};
    }

    // standard error of a mean, from the sum and sum of squares of n values
    static double stdErr(double sum, double sumSq, double n) {
        double mean = sum / n;
        double var = (sumSq - n * mean * mean) / (n - 1);
        return Math.sqrt(Math.max(var, 0) / n);
    }

    public static void main(String[] args) throws IOException {
        int nGames = 501;      // multiple of 3 so every agent sits in every seat equally often
        int budgetMs = 100;    // thinking time per decision for BasicMCTS
        String[] names = {"BasicMCTS", "Random1", "Random2"};
        double[] wins = new double[3];
        double[] scoreSum = new double[3], scoreSumSq = new double[3];
        double[] ordinalSum = new double[3], ordinalSumSq = new double[3];

        try (FileWriter csv = new FileWriter("baseline_B1.csv")) {
            csv.write("game,agent,seat,score,ordinal,win\n");

            for (int g = 0; g < nGames; g++) {
                BasicMCTSPlayer mcts = new BasicMCTSPlayer();
                BasicMCTSParams params = mcts.getParameters();
                params.budgetType = PlayerConstants.BUDGET_TIME;
                params.budget = budgetMs;
                if (g == 0) System.out.println("Budget settings: " + params.budgetType + " " + params.budget);

                AbstractPlayer[] agents = {mcts, new RandomPlayer(), new RandomPlayer()};

                // rotate who sits where each game
                List<AbstractPlayer> seats = new ArrayList<>();
                int[] agentInSeat = new int[3];
                for (int s = 0; s < 3; s++) {
                    agentInSeat[s] = (s + g) % 3;
                    seats.add(agents[agentInSeat[s]]);
                }

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
                    double win = (sc[s] == best) ? 1.0 / nWinners : 0.0; // ties split the win
                    double ord = game.getGameState().getOrdinalPosition(s);
                    wins[a] += win;
                    scoreSum[a] += sc[s];
                    scoreSumSq[a] += sc[s] * sc[s];
                    ordinalSum[a] += ord;
                    ordinalSumSq[a] += ord * ord;
                    csv.write(g + "," + names[a] + "," + s + "," + sc[s] + "," + ord + "," + win + "\n");
                }
                csv.flush();
                if ((g + 1) % 25 == 0) System.out.println("Finished " + (g + 1) + "/" + nGames);
            }
        }

        System.out.println("---- Results over " + nGames + " games (SE = standard error) ----");
        for (int a = 0; a < 3; a++) {
            double p = wins[a] / nGames;
            double[] ci = wilson(p, nGames);
            double winSE = Math.sqrt(p * (1 - p) / nGames);
            System.out.printf("%s: win rate %.3f (SE %.3f) Wilson 95%% CI [%.3f, %.3f] | mean score %.2f (SE %.2f) | mean ordinal %.2f (SE %.3f)%n",
                    names[a], p, winSE, ci[0], ci[1],
                    scoreSum[a] / nGames, stdErr(scoreSum[a], scoreSumSq[a], nGames),
                    ordinalSum[a] / nGames, stdErr(ordinalSum[a], ordinalSumSq[a], nGames));
        }
    }
}