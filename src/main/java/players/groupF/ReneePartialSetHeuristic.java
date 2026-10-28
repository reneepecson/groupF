package groupF;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import games.sushigo.SGGameState;
import games.sushigo.SGParameters;
import games.sushigo.cards.SGCard.SGCardType;

public class ReneePartialSetHeuristic implements IStateHeuristic {

    // starting guesses, to be tuned and ablated
    public double tempuraFrac = 0.4;     // share of a pair's value for one unpaired tempura
    public double sashimiOneFrac = 0.2;  // share of a triple's value for 1 of 3 sashimi
    public double sashimiTwoFrac = 0.5;  // share for 2 of 3
    public double wasabiFrac = 0.0;     // share of the expected wasabi bonus
    public double makiWeight = 0.5;      // share of the projected round-end maki points
    public double puddingWeight = 0.3;   // share of the projected game-end pudding points
    public boolean relative = true;      // my value minus the opponents' average
    public double scale = 50.0;          // same scale as the default heuristic

    public static long calls = 0;        // to check the heuristic is really being used

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        calls++;
        if (!gs.isNotTerminal())
            return gs.getHeuristicScore(playerId); // keep the default result at the end of the game
        SGGameState s = (SGGameState) gs;
        double mine = playerValue(s, playerId);
        double value = mine;
        if (relative) {
            int n = s.getNPlayers();
            double others = 0;
            for (int p = 0; p < n; p++) if (p != playerId) others += playerValue(s, p);
            value = mine - others / (n - 1);
        }
        value /= scale;
        return value;
    }

    private double playerValue(SGGameState s, int p) {
        SGParameters par = (SGParameters) s.getGameParameters();
        double v = s.getPlayerScore()[p].getValue();

        // one unpaired tempura
        if (s.getPlayedCardTypes(SGCardType.Tempura, p).getValue() % 2 == 1)
            v += tempuraFrac * par.valueTempuraPair;

        // sashimi part-way to a triple
        int sashimi = s.getPlayedCardTypes(SGCardType.Sashimi, p).getValue() % 3;
        if (sashimi == 1) v += sashimiOneFrac * par.valueSashimiTriple;
        else if (sashimi == 2) v += sashimiTwoFrac * par.valueSashimiTriple;

        // unused wasabi: extra points if a nigiri comes. 2.0 is roughly the average nigiri value in the deck
        int wasabi = s.getPlayedCardTypes(SGCardType.Wasabi, p).getValue();
        v += wasabi * wasabiFrac * (par.multiplierWasabi - 1) * 2.0;

        v += makiWeight * projectedMaki(s, par, p);
        v += puddingWeight * projectedPudding(s, par, p);
        return v;
    }

    // maki points player p would get if the round ended now (mirrors the rules in SGCard)
    private double projectedMaki(SGGameState s, SGParameters par, int p) {
        int n = s.getNPlayers();
        int[] c = new int[n];
        int most = 0;
        for (int q = 0; q < n; q++) {
            c[q] = s.getPlayedCardTypes(SGCardType.Maki, q).getValue();
            most = Math.max(most, c[q]);
        }
        if (most == 0) return 0;
        int nMost = 0;
        for (int q = 0; q < n; q++) if (c[q] == most) nMost++;
        if (c[p] == most) return (double) par.valueMakiMost / nMost;
        if (nMost > 1) return 0; // second place is only paid when there is no tie for most
        int second = 0;
        for (int q = 0; q < n; q++) if (c[q] < most) second = Math.max(second, c[q]);
        if (second == 0 || c[p] != second) return 0;
        int nSecond = 0;
        for (int q = 0; q < n; q++) if (c[q] == second) nSecond++;
        return (double) par.valueMakiSecond / nSecond;
    }

    // pudding points player p would get if the game ended now
    private double projectedPudding(SGGameState s, SGParameters par, int p) {
        int n = s.getNPlayers();
        int[] c = new int[n];
        int best = Integer.MIN_VALUE, worst = Integer.MAX_VALUE;
        for (int q = 0; q < n; q++) {
            c[q] = s.getPlayedCardTypes(SGCardType.Pudding, q).getValue();
            best = Math.max(best, c[q]);
            worst = Math.min(worst, c[q]);
        }
        if (best == worst) return 0;
        int nBest = 0, nWorst = 0;
        for (int q = 0; q < n; q++) {
            if (c[q] == best) nBest++;
            if (c[q] == worst) nWorst++;
        }
        if (c[p] == best && best > 0) return (double) par.valuePuddingMost / nBest;
        if (c[p] == worst && n > 2) return (double) par.valuePuddingLeast / nWorst;
        return 0;
    }
}