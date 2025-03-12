// File: main/AIContext.java
package main;

import java.util.Random;

public interface AIContext {
    int getNumRegions();
    int[] getRegionTeam();
    int[] getTroops();
    boolean[][] getAdjacent();
    double[] getCombatPower();
    String[] getTeamNames();
    int getCurrentTeam();
    Random getRand();
    double getSmartRisk();
    
    // New generalized method for current team's control type.
    TeamControl getTeamControl();
    
    void executeReinforce(int source, int dest);
    void executeMove(int source, int dest);
    void endTurn();
}
