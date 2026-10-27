package groupF;

import core.AbstractPlayer;
import core.Game;
import games.GameType;
import players.PlayerConstants;
import players.basicMCTS.BasicMCTSParams;
import players.basicMCTS.BasicMCTSPlayer;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SweepRunner {

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

    // print to the console and save to the results file
    static void out(FileWriter file, String line) throws IOException {
        System.out.println(line);
        file.write(line + "\n");
        file.flush();
    }

    public static void main(String[] args) throws IOException {
        System.out.println("wasabiFrac = " + new ReneePartialSetHeuristic().wasabiFrac);
        int nGames = 9;            // multiple of 3; about 12 minutes per setting
        int budgetMs = 100;
        int seedOffset = 3000;      // fresh tuning seeds
        // each setting changes one weight (or one group of weights) of the current heuristic
        Map<String, Consumer<ReneePartialSetHeuristic>> settings = new LinkedHashMap<>();
        settings.put("current weights (reference)", h -> {});
     //   settings.put("wasabi 0", h -> h.wasabiFrac = 0);

        try (FileWriter file = new FileWriter("sweep_smoke.txt", true)) {
            for (Map.Entry<String, Consumer<ReneePartialSetHeuristic>> s : settings.entrySet()) {
                double[] wins = new double[3], scoreSum = new double[3], ordinalSum = new double[3];
                String[] names = {"variant", "plain A", "plain B"};

                for (int g = 0; g < nGames; g++) {
                    BasicMCTSPlayer modified = newMcts(budgetMs);
                    ReneePartialSetHeuristic h = new ReneePartialSetHeuristic();
                    s.getValue().accept(h);
                    modified.getParameters().heuristic = h;
                    AbstractPlayer[] agents = {modified, newMcts(budgetMs), newMcts(budgetMs)};

                    // rotate seats so each agent plays each seat equally often
                    List<AbstractPlayer> seats = new ArrayList<>();
                    int[] agentInSeat = new int[3];
                    for (int seat = 0; seat < 3; seat++) {
                        agentInSeat[seat] = (seat + g) % 3;
                        seats.add(agents[agentInSeat[seat]]);
                    }

                    Game game = Game.runOne(GameType.SushiGo, null, seats, seedOffset + g, false, null, null, 0);

                    double[] sc = new double[3];
                    double best = -Double.MAX_VALUE;
                    for (int seat = 0; seat < 3; seat++) {
                        sc[seat] = game.getGameState().getGameScore(seat);
                        best = Math.max(best, sc[seat]);
                    }
                    int nWinners = 0;
                    for (int seat = 0; seat < 3; seat++) if (sc[seat] == best) nWinners++;
                    for (int seat = 0; seat < 3; seat++) {
                        int a = agentInSeat[seat];
                        scoreSum[a] += sc[seat];
                        ordinalSum[a] += game.getGameState().getOrdinalPosition(seat);
                        if (sc[seat] == best) wins[a] += 1.0 / nWinners; // ties split the win
                    }
                }

                out(file, "==== " + s.getKey() + " (" + nGames + " games, seeds " + seedOffset + "+, chance = 0.333) ====");
                for (int a = 0; a < 3; a++) {
                    double p = wins[a] / nGames;
                    double[] ci = wilson(p, nGames);
                    out(file, String.format("%s: win rate %.3f [%.3f, %.3f], mean score %.2f, mean ordinal %.2f",
                            names[a], p, ci[0], ci[1], scoreSum[a] / nGames, ordinalSum[a] / nGames));
                }
            }
        }
    }
}