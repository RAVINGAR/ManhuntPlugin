package com.ravingarinc.manhunt.gameplay;

import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class Hunter extends Trackable {
    private boolean hasPriority;

    private long lastAttempt;

    public Hunter(final Player player, final boolean hasPriority, final long lastAttempt) {
        super(player);
        this.hasPriority = hasPriority;
        this.lastAttempt = lastAttempt;

        addEventHandler(BlockPlaceEvent.class, true);
        addEventHandler(PlayerInteractEvent.class, true);
        addEventHandler(BlockBreakEvent.class, true);
        addEventHandler(BlockIgniteEvent.class, true);
    }

    /**
     * Gets the last hunter attempt of this trackable. A value of 0 means the hunter
     * has never had an attempt.
     *
     * @return The system time
     */
    public synchronized long lastAttempt() {
        return lastAttempt;
    }

    @Override
    public void end() {
        super.end();
        this.lastAttempt = System.currentTimeMillis();
    }

    public void setPriority(final boolean priority) {
        this.hasPriority = priority;
    }

    public boolean hasPriority() {
        return hasPriority;
    }
}
