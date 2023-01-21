package com.ravingarinc.manhunt.gameplay;

import com.ravingarinc.manhunt.queue.QueueManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public class Prey extends Trackable {
    public Prey(final Player player, final String role) {
        super(player, role);
    }

    @Override
    public boolean handleEvent(final Event event) {
        return false;
    }

    @Override
    public void handleDeath(final QueueManager manager) {

    }
}
