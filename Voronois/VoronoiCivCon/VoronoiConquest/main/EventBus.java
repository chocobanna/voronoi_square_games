// File: main/EventBus.java
package main;

import java.util.ArrayList;
import java.util.List;

public class EventBus {
    private static EventBus instance;
    private final List<EventListener> listeners;

    private EventBus() {
        listeners = new ArrayList<>();
    }

    public static synchronized EventBus getInstance() {
        if (instance == null) {
            instance = new EventBus();
        }
        return instance;
    }

    public void register(EventListener listener) {
        listeners.add(listener);
    }

    public void fireEvent(GameEvent event) {
        for (EventListener listener : listeners) {
            listener.onEvent(event);
        }
    }
}
