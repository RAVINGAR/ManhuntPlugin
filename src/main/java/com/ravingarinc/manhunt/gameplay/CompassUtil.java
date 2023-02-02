package com.ravingarinc.manhunt.gameplay;

import com.ravingarinc.manhunt.queue.GameplayManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CompassUtil {
    public static final String HUNTER_COMPASS = "hunter_compass";
    public static final String PREY_COMPASS = "prey_compass";

    public static ItemStack createHunterCompass(final GameplayManager manager) {
        final ItemStack item = new ItemStack(Material.COMPASS, 1);
        final CompassMeta meta = (CompassMeta) item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Hunter's Compass");
        final List<String> lore = new ArrayList<>();
        final long interval = manager.getCompassIntervalSeconds();
        lore.add(ChatColor.GRAY + "Points to your target's previous location.");
        lore.add(ChatColor.GRAY + "The location is updated every ");
        lore.add(ChatColor.GRAY + "" + manager.getCompassIntervalSeconds() + (interval == 1 ? " second" : " seconds"));
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(manager.getKey(), PersistentDataType.STRING, HUNTER_COMPASS);
        meta.setLodestoneTracked(false);
        final Prey prey = manager.getAnyPrey();
        if (prey != null) {
            meta.setLodestone(prey.player().getLocation());
        }
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createPreyCompass(final GameplayManager manager, final Prey prey) {
        final ItemStack item = new ItemStack(Material.COMPASS, 1);
        final CompassMeta meta = (CompassMeta) item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Prey's Compass");
        final List<String> lore = new ArrayList<>();
        final long interval = manager.getCompassIntervalSeconds();
        lore.add(ChatColor.GRAY + "Points a random hunter's previous location.");
        lore.add(ChatColor.GRAY + "The location is updated every ");
        lore.add(ChatColor.GRAY + "" + manager.getCompassIntervalSeconds() + (interval == 1 ? " second" : " seconds"));
        lore.add("");
        meta.getPersistentDataContainer().set(manager.getKey(), PersistentDataType.STRING, PREY_COMPASS);
        meta.setLodestoneTracked(false);
        final Hunter hunter = manager.getAnyHunter();
        if (hunter == null) {
            lore.add(ChatColor.WHITE + "There is are no hunters nearby!");
        } else {
            final Location hunterLoc = hunter.player().getLocation();
            meta.setLodestone(hunterLoc);
            final int distance = distance(hunterLoc, prey.player().getLocation());
            lore.add(ChatColor.WHITE + "There is a hunter " + distance + (distance == 1 ? " block" : " blocks") + " away!");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public static int distance(final Location from, final Location to) {
        final double a = Math.abs(from.getBlockX() - to.getBlockX());
        final double b = Math.abs(from.getBlockZ() - to.getBlockZ());
        return (int) Math.sqrt((a * a) + (b * b));
    }

    public static Optional<ItemStack> findCompass(final GameplayManager manager, final Player player, final String expectedKey) {
        for (final ItemStack item : player.getInventory().getContents()) {
            if (item == null) {
                continue;
            }
            if (item.getType() == Material.COMPASS && item.getItemMeta() != null) {
                final PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
                if (expectedKey.equals(container.get(manager.getKey(), PersistentDataType.STRING))) {
                    return Optional.of(item);
                }
            }
        }
        return Optional.empty();
    }

    public static void removeCompass(final GameplayManager manager, final Player player) {
        final ItemStack[] contents = player.getInventory().getContents();
        int foundIndex = -1;
        for (int i = 0; i < contents.length; i++) {
            final ItemStack item = contents[i];
            if (item == null) {
                continue;
            }
            if (item.getType() == Material.COMPASS && item.getItemMeta() != null) {
                final PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
                if (container.has(manager.getKey(), PersistentDataType.STRING)) {
                    foundIndex = i;
                    break;
                }
            }
        }
        if (foundIndex == -1) {
            return;
        }
        player.getInventory().setItem(foundIndex, null);
    }

}
