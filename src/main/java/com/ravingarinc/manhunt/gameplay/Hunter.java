package com.ravingarinc.manhunt.gameplay;

import com.ravingarinc.manhunt.queue.QueueManager;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class Hunter extends Trackable {
    public Hunter(final Player player, final String role) {
        super(player, role);

        addEventHandler(BlockPlaceEvent.class, true);
        addEventHandler(PlayerInteractEvent.class, true);
        addEventHandler(BlockBreakEvent.class, true);
        addEventHandler(BlockIgniteEvent.class, true);
    }

    @Override
    public void handleDeath(final QueueManager manager) {

    }
}
