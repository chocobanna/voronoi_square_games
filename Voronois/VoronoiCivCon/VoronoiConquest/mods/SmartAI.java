// File: mods/SmartAI.java
package mods;

import main.AIContext;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.Random;

public class SmartAI {
    public static void doMove(AIContext engine) {
        int numRegions = engine.getNumRegions();
        int currentTeam = engine.getCurrentTeam();
        int[] regionTeam = engine.getRegionTeam();
        int[] troops = engine.getTroops();
        boolean[][] adjacent = engine.getAdjacent();
        double[] combatPower = engine.getCombatPower();
        String[] teamNames = engine.getTeamNames();
        double smartRisk = engine.getSmartRisk();
        Random rand = engine.getRand();

        double bestScore = Double.NEGATIVE_INFINITY;
        int[] bestMove = null;
        boolean isFriendly = false;
        for (int i = 0; i < numRegions; i++) {
            if (regionTeam[i] == currentTeam && troops[i] > 0) {
                for (int j = 0; j < numRegions; j++) {
                    if (adjacent[i][j]) {
                        double score;
                        if (regionTeam[i] == regionTeam[j]) {
                            score = 2 * troops[i];
                        } else {
                            double sourcePower = combatPower[i];
                            double destPower = combatPower[j];
                            score = (sourcePower > destPower) ? (sourcePower - destPower) * (1 - smartRisk) : -1000;
                        }
                        if (score > bestScore) {
                            bestScore = score;
                            bestMove = new int[]{i, j};
                            isFriendly = (regionTeam[i] == regionTeam[j]);
                        }
                    }
                }
            }
        }
        final int[] move = bestMove;
        final double score = bestScore;
        final boolean friendly = isFriendly;
        SwingUtilities.invokeLater(() -> {
            if (move == null || score <= 0) {
                System.out.println("Smart AI (" + teamNames[currentTeam] + ") found no advantageous moves. Skipping turn.");
                engine.endTurn();
                return;
            }
            if (friendly) {
                System.out.println("Smart AI (" + teamNames[currentTeam] + ") reinforces from region " + move[0] + " to region " + move[1] + " with score " + score);
                engine.executeReinforce(move[0], move[1]);
            } else {
                System.out.println("Smart AI (" + teamNames[currentTeam] + ") attacks from region " + move[0] + " to region " + move[1] + " with score " + score);
                engine.executeMove(move[0], move[1]);
            }
            engine.endTurn();
        });
    }
}
