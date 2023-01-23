package com.ravingarinc.manhunt.gameplay;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class Trackable {
    private final Player player;
    private final Map<Class<? extends Event>, Boolean> eventHandlers;

    private long startTime;

    public Trackable(final Player player) {
        this.player = player;
        this.startTime = -1;
        this.eventHandlers = new HashMap<>();
    }

    public final void addEventHandler(final Class<? extends Event> clazz, final boolean shouldCancel) {
        eventHandlers.put(clazz, shouldCancel);
    }

    public Player player() {
        return player;
    }


    public void start() {
        startTime = System.currentTimeMillis();
    }

    public void end() {
        startTime = -1;
    }

    public String getTimeAlive() {
        if (startTime == -1) {
            return "0 seconds";
        } else {
            final long diff = System.currentTimeMillis() - startTime;
            final long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            final long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);

            final StringBuilder builder = new StringBuilder();
            if (minutes > 0) {
                builder.append(" ");
                builder.append(minutes);
                builder.append(minutes == 1 ? " minute" : " minutes");
            }
            if (seconds > 0) {
                builder.append(minutes > 0 ? ", " : " ");
                builder.append(seconds);
                builder.append(seconds == 1 ? " second" : " seconds");
            }
            return builder.toString();
        }
    }

    /**
     * Considers event based on event handlers
     *
     * @param event The event
     * @return true if event should be cancelled, false if not.
     */
    public boolean handleEvent(final Event event) {
        final Boolean result = eventHandlers.get(event.getClass());
        return result != null && result;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Trackable trackable = (Trackable) o;
        return player.getUniqueId().equals(trackable.player.getUniqueId());
    }

    @Override
    public int hashCode() {
        return player.getUniqueId().hashCode();
    }
}
