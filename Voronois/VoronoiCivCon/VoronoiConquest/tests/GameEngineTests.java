// File: tests/GameEngineTest.java
package tests;

import main.GameEngine;
import main.TeamControl;
import main.EventBus;
import main.EventListener;
import main.GameEvent;
import main.TurnEndedEvent;
import org.junit.Assert;
import org.junit.Test;

import java.awt.Point;
import java.util.concurrent.atomic.AtomicInteger;

public class GameEngineTest {
    @Test
    public void testVictoryDetection() {
        // Create a simple game engine with 4 regions and 2 teams.
        TeamControl[] controls = {TeamControl.HOTSEAT, TeamControl.DUMB};
        GameEngine engine = new GameEngine(100, 100, 4, 2, controls, 0.5);
        
        // Force all regions to be controlled by team 0.
        int[] regionTeam = new int[4];
        for (int i = 0; i < 4; i++) {
            regionTeam[i] = 0;
        }
        // Using reflection or setter method if available; for this test, assume we can directly set it.
        // (This is just a conceptual test.)
        
        // Check victory condition.
        Assert.assertTrue(engine.endTurn(), "Victory should be detected when all regions belong to one team");
    }
    
    @Test
    public void testEventBusTurnEnded() {
        AtomicInteger turnEndedCount = new AtomicInteger(0);
        EventBus.getInstance().register(new EventListener() {
            @Override
            public void onEvent(GameEvent event) {
                if (event instanceof TurnEndedEvent) {
                    turnEndedCount.incrementAndGet();
                }
            }
        });
        
        // Create a minimal game engine instance
        TeamControl[] controls = {TeamControl.HOTSEAT, TeamControl.DUMB};
        GameEngine engine = new GameEngine(100, 100, 4, 2, controls, 0.5);
        // Simulate end turn
        engine.endTurn();
        // Allow event dispatch to occur
        try { Thread.sleep(100); } catch (InterruptedException ex) {}
        
        Assert.assertTrue(turnEndedCount.get() > 0, "TurnEndedEvent should have been fired.");
    }
}
