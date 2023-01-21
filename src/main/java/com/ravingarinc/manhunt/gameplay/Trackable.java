package com.ravingarinc.manhunt.gameplay;

import com.ravingarinc.manhunt.queue.QueueManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.HashMap;
import java.util.Map;

public abstract class Trackable {
    private final Player player;
    private final Map<Class<? extends Event>, Boolean> eventHandlers;
    private String role;
    private long lastAttempt;

    public Trackable(final Player player, final String role) {
        this.player = player;
        this.role = role;
        this.lastAttempt = 0;
        this.eventHandlers = new HashMap<>();
    }

    public final void addEventHandler(final Class<? extends Event> clazz, final boolean shouldCancel) {
        eventHandlers.put(clazz, shouldCancel);
    }

    public void updateRole(final String newRole) {
        this.role = newRole;
    }

    public Player player() {
        return player;
    }

    public String role() {
        return role;
    }

    /**
     * Gets the last hunter attempt of this trackable. A value of 0 means the hunter
     * has never had an attempt.
     *
     * @return The system time
     */
    public long lastAttempt() {
        return lastAttempt;
    }

    public void setLastAttempt(final long attempt) {
        this.lastAttempt = attempt;
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

    public abstract void handleDeath(QueueManager manager);

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
