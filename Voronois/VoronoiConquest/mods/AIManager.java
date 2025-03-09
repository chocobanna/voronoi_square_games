// File: mods/AIManager.java
package mods;

import main.AIContext;
import main.TeamControl;

public class AIManager {
    public static void doMove(AIContext context) {
        TeamControl control = context.getTeamControl();
        switch (control) {
            case DUMB:
                DumbAI.doMove(context);
                break;
            case SMART:
                SmartAI.doMove(context);
                break;
            default:
                break;
        }
    }
}
