package com.ravingarinc.manhunt.gameplay;

import com.ravingarinc.manhunt.RavinPlugin;
import com.ravingarinc.manhunt.api.ModuleListener;
import com.ravingarinc.manhunt.api.ModuleLoadException;
import com.ravingarinc.manhunt.api.async.AsyncHandler;
import com.ravingarinc.manhunt.queue.GameplayManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class PlayerListener extends ModuleListener {
    private final Set<Material> blockBlacklist;
    private final Set<Material> itemBlacklist;
    private PlayerManager manager;

    private GameplayManager gameplayManager;

    public PlayerListener(final RavinPlugin plugin) {
        super(PlayerListener.class, plugin, PlayerManager.class, GameplayManager.class);
        this.blockBlacklist = new HashSet<>();
        this.itemBlacklist = new HashSet<>();
    }

    public void blacklistBlock(final Material material) {
        this.blockBlacklist.add(material);
    }

    public void blacklistItem(final Material material) {
        this.itemBlacklist.add(material);
    }

    @Override
    protected void load() throws ModuleLoadException {
        manager = plugin.getModule(PlayerManager.class);
        gameplayManager = plugin.getModule(GameplayManager.class);
        super.load();
    }

    @Override
    public void cancel() {
        super.cancel();
        blockBlacklist.clear();
        itemBlacklist.clear();
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        AsyncHandler.runSyncTaskLater(() -> {
            if (manager.loadPlayer(event.getPlayer()) instanceof Hunter hunter) {
                gameplayManager.resetLocation(hunter);
            }
        }, 5L);
    }

    @EventHandler
    public void onPlayerLeave(final PlayerQuitEvent event) {
        manager.unloadPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlaceBlock(final BlockPlaceEvent event) {
        if (blockBlacklist.contains(event.getBlockPlaced().getType())) {
            manager.getPlayer(event.getPlayer()).ifPresent(p -> event.setCancelled(p.handleEvent(event)));
        }
    }

    @EventHandler
    public void onBreakBlock(final BlockBreakEvent event) {
        if (blockBlacklist.contains(event.getBlock().getType())) {
            manager.getPlayer(event.getPlayer()).ifPresent(p -> event.setCancelled(p.handleEvent(event)));
        }
    }

    @EventHandler
    public void onInteract(final PlayerInteractEvent event) {
        final ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        if (itemBlacklist.contains(item.getType())) {
            manager.getPlayer(event.getPlayer()).ifPresent(p -> event.setCancelled(p.handleEvent(event)));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onIgnite(final BlockIgniteEvent event) {
        if (event.getPlayer() == null) {
            return;
        }

        if (itemBlacklist.contains(Material.FLINT_AND_STEEL) && event.getBlock().getType() != Material.OBSIDIAN) {
            manager.getPlayer(event.getPlayer()).ifPresent(p -> event.setCancelled(p.handleEvent(event)));
        }
    }

    @EventHandler
    public void onDeath(final PlayerDeathEvent event) {
        manager.getPlayer(event.getEntity()).ifPresent(player -> {
            gameplayManager.onDeath(player);

        });
    }

    @EventHandler
    public void onSpawn(final PlayerRespawnEvent event) {
        manager.getPlayer(event.getPlayer()).ifPresent(player -> gameplayManager.onSpawn(player));
    }
}
