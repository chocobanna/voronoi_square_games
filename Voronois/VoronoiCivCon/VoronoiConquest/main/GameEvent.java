// File: main/GameEvent.java
package main;

public abstract class GameEvent {
    private final String eventType;
    
    public GameEvent(String eventType) {
        this.eventType = eventType;
    }
    
    public String getEventType() {
        return eventType;
    }
}

public class TurnEndedEvent extends GameEvent {
    private final int newCurrentTeam;
    
    public TurnEndedEvent(int newCurrentTeam) {
        super("TurnEnded");
        this.newCurrentTeam = newCurrentTeam;
    }
    
    public int getNewCurrentTeam() {
        return newCurrentTeam;
    }
}

public class RegionChangedEvent extends GameEvent {
    private final int regionIndex;
    
    public RegionChangedEvent(int regionIndex) {
        super("RegionChanged");
        this.regionIndex = regionIndex;
    }
    
    public int getRegionIndex() {
        return regionIndex;
    }
}
