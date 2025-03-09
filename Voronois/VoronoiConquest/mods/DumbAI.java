package mods;

import main.GameEngine;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.Random;

public class DumbAI {
    public static void doMove(GameEngine engine) {
        int numRegions = engine.getNumRegions();
        int currentTeam = engine.getCurrentTeam();
        int[] regionTeam = engine.getRegionTeam();
        int[] troops = engine.getTroops();
        boolean[][] adjacent = engine.getAdjacent();
        String[] teamNames = engine.getTeamNames();
        Random rand = engine.getRand();
        
        java.util.List<int[]> moves = new ArrayList<>();
        for (int i = 0; i < numRegions; i++) {
            if (regionTeam[i] == currentTeam && troops[i] > 0) {
                for (int j = 0; j < numRegions; j++) {
                    if (adjacent[i][j]) {
                        moves.add(new int[]{i, j});
                    }
                }
            }
        }
        if (moves.isEmpty()) {
            System.out.println("Dumb AI (" + teamNames[currentTeam] + ") has no valid moves. Skipping turn.");
            SwingUtilities.invokeLater(engine::endTurn);
            return;
        }
        int[] move = moves.get(rand.nextInt(moves.size()));
        SwingUtilities.invokeLater(() -> {
            if (regionTeam[move[0]] == regionTeam[move[1]]) {
                System.out.println("Dumb AI (" + teamNames[currentTeam] + ") reinforces region " + move[1]);
                engine.executeReinforce(move[0], move[1]);
            } else {
                System.out.println("Dumb AI (" + teamNames[currentTeam] + ") attacks from region " + move[0] + " to region " + move[1]);
                engine.executeMove(move[0], move[1]);
            }
            engine.endTurn();
        });
    }
}
