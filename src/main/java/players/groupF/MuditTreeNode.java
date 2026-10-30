package groupF;

import core.AbstractGameState;
import core.actions.AbstractAction;
import players.basicMCTS.BasicMCTSParams;
import players.simple.RandomPlayer;
import utilities.ElapsedCpuTimer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static java.util.stream.Collectors.toList;
import static players.PlayerConstants.*;
import static utilities.Utils.noise;

/**
 * Group-owned MCTS tree node. Kept in groupF so the submitted agent does not
 * require modifications to TAG's BasicTreeNode.
 */
final class MuditTreeNode {
    private final MuditTreeNode root;
    private final MuditTreeNode parent;
    private final Map<AbstractAction, MuditTreeNode> children = new HashMap<>();
    private final int depth;
    private double totalValue;
    private int visits;
    private int forwardModelCalls;
    private final MuditMCTSPlayer player;
    private final Random random;
    private final RandomPlayer rolloutPlayer = new RandomPlayer();
    private final AbstractGameState state;

    MuditTreeNode(MuditMCTSPlayer player, MuditTreeNode parent,
                  AbstractGameState state, Random random) {
        this.player = player;
        this.parent = parent;
        this.root = parent == null ? this : parent.root;
        this.depth = parent == null ? 0 : parent.depth + 1;
        this.random = random;
        this.state = state;
        if (state.isNotTerminal()) {
            for (AbstractAction action : player.getForwardModel()
                    .computeAvailableActions(state, player.getParameters().actionSpace)) {
                children.put(action, null);
            }
        }
        rolloutPlayer.setForwardModel(player.getForwardModel());
    }

    void mctsSearch() {
        BasicMCTSParams params = player.getParameters();
        ElapsedCpuTimer timer = new ElapsedCpuTimer();
        if (params.budgetType == BUDGET_TIME) {
            timer.setMaxTimeMillis(params.budget);
        }

        double accumulatedMillis = 0.0;
        int iterations = 0;
        boolean stop = false;
        while (!stop) {
            ElapsedCpuTimer iterationTimer = new ElapsedCpuTimer();
            MuditTreeNode selected = treePolicy();
            selected.backUp(selected.rollOut());
            iterations++;

            if (params.budgetType == BUDGET_TIME) {
                accumulatedMillis += iterationTimer.elapsedMillis();
                double averageMillis = accumulatedMillis / iterations;
                long remainingMillis = timer.remainingTimeMillis();
                stop = remainingMillis <= 2 * averageMillis || remainingMillis <= params.breakMS;
            } else if (params.budgetType == BUDGET_ITERATIONS) {
                stop = iterations >= params.budget;
            } else if (params.budgetType == BUDGET_FM_CALLS) {
                stop = forwardModelCalls > params.budget;
            } else if (params.budgetType == BUDGET_FMANDCOPY_CALLS
                    || params.budgetType == BUDGET_COPY_CALLS) {
                stop = forwardModelCalls > params.budget;
            } else {
                throw new IllegalStateException("Unsupported MCTS budget type: " + params.budgetType);
            }
        }
    }

    private MuditTreeNode treePolicy() {
        MuditTreeNode current = this;
        while (current.state.isNotTerminal()
                && current.depth < player.getParameters().maxTreeDepth) {
            if (current.unexpandedActions().size() > 0) {
                return current.expand();
            }
            current = current.children.get(current.selectUcbAction());
        }
        return current;
    }

    private List<AbstractAction> unexpandedActions() {
        return children.keySet().stream()
                .filter(action -> children.get(action) == null)
                .collect(toList());
    }

    private MuditTreeNode expand() {
        List<AbstractAction> available = unexpandedActions();
        AbstractAction selected = available.get(random.nextInt(available.size()));
        AbstractGameState nextState = state.copy();
        advance(nextState, selected.copy());
        MuditTreeNode child = new MuditTreeNode(player, this, nextState, random);
        children.put(selected, child);
        return child;
    }

    private void advance(AbstractGameState gameState, AbstractAction action) {
        player.getForwardModel().next(gameState, action);
        root.forwardModelCalls++;
    }

    private AbstractAction selectUcbAction() {
        AbstractAction bestAction = null;
        double bestValue = -Double.MAX_VALUE;
        BasicMCTSParams params = player.getParameters();
        for (Map.Entry<AbstractAction, MuditTreeNode> entry : children.entrySet()) {
            MuditTreeNode child = entry.getValue();
            if (child == null) {
                throw new AssertionError("Unexpanded action encountered during UCB selection");
            }
            double meanValue = child.totalValue / (child.visits + params.epsilon);
            double exploration = params.K * Math.sqrt(
                    Math.log(visits + 1.0) / (child.visits + params.epsilon));
            boolean controlledPlayerMoves = state.getCurrentPlayer() == player.getPlayerID();
            double value = (controlledPlayerMoves ? meanValue : -meanValue) + exploration;
            value = noise(value, params.epsilon, player.getRnd().nextDouble());
            if (bestAction == null || value > bestValue) {
                bestAction = entry.getKey();
                bestValue = value;
            }
        }
        if (bestAction == null) {
            throw new AssertionError("No action available during UCB selection");
        }
        root.forwardModelCalls++;
        return bestAction;
    }

    private double rollOut() {
        AbstractGameState rolloutState = state.copy();
        for (int depth = 0; depth < player.getParameters().rolloutLength
                && rolloutState.isNotTerminal(); depth++) {
            List<AbstractAction> actions = rolloutPlayer.getForwardModel()
                    .computeAvailableActions(rolloutState, rolloutPlayer.parameters.actionSpace);
            advance(rolloutState, rolloutPlayer.getAction(rolloutState, actions));
        }
        double value = player.getParameters().getStateHeuristic()
                .evaluateState(rolloutState, player.getPlayerID());
        if (Double.isNaN(value)) {
            throw new AssertionError("Illegal heuristic value - should be a number");
        }
        return value;
    }

    private void backUp(double value) {
        for (MuditTreeNode node = this; node != null; node = node.parent) {
            node.visits++;
            node.totalValue += value;
        }
    }

    AbstractAction bestAction() {
        AbstractAction bestAction = null;
        double bestVisits = -Double.MAX_VALUE;
        for (Map.Entry<AbstractAction, MuditTreeNode> entry : children.entrySet()) {
            MuditTreeNode child = entry.getValue();
            if (child != null) {
                double value = noise(child.visits, player.getParameters().epsilon,
                        player.getRnd().nextDouble());
                if (bestAction == null || value > bestVisits) {
                    bestAction = entry.getKey();
                    bestVisits = value;
                }
            }
        }
        if (bestAction == null) {
            throw new AssertionError("MCTS search did not expand a root action");
        }
        return bestAction;
    }
}
