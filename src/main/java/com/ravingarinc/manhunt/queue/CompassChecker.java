package com.ravingarinc.manhunt.queue;

import com.ravingarinc.manhunt.gameplay.CompassUtil;
import com.ravingarinc.manhunt.gameplay.Hunter;
import com.ravingarinc.manhunt.gameplay.Prey;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Set;

import static com.ravingarinc.manhunt.gameplay.CompassUtil.distance;

public class CompassChecker extends BukkitRunnable {
    private final Set<Hunter> hunters;
    private final Set<Prey> prey;

    private final GameplayManager manager;

    public CompassChecker(final GameplayManager manager, final Set<Hunter> hunters, final Set<Prey> prey) {
        this.hunters = hunters;
        this.prey = prey;
        this.manager = manager;
    }

    @Override
    public void run() {
        final Prey foundPrey = manager.getAnyPrey();
        hunters.forEach(hunter -> CompassUtil.findCompass(manager, hunter.player(), CompassUtil.HUNTER_COMPASS).ifPresent((compass) -> {
            final CompassMeta meta = (CompassMeta) compass.getItemMeta();
            meta.setLodestone(foundPrey == null ? null : foundPrey.player().getLocation());
            meta.setLodestoneTracked(foundPrey == null);
            compass.setItemMeta(meta);
        }));

        final Hunter hunter = manager.getAnyHunter();
        prey.forEach(prey -> {
            CompassUtil.findCompass(manager, prey.player(), CompassUtil.PREY_COMPASS).ifPresent(compass -> {
                final CompassMeta meta = (CompassMeta) compass.getItemMeta();
                final Location location;
                final List<String> lore = meta.getLore();
                if (hunter == null) {
                    location = null;
                    if (lore != null && lore.size() >= 5) {
                        lore.set(4, ChatColor.WHITE + "There is are no hunters nearby!");
                    }
                } else {
                    location = hunter.player().getLocation();
                    if (lore != null && lore.size() >= 5) {
                        final Location hunterLoc = hunter.player().getLocation();
                        meta.setLodestone(hunterLoc);
                        final int distance = distance(hunterLoc, prey.player().getLocation());
                        lore.set(4, ChatColor.WHITE + "There is a hunter " + distance + (distance == 1 ? " block" : " blocks") + " away!");
                    }
                }
                meta.setLodestoneTracked(foundPrey == null);
                meta.setLore(lore);
                meta.setLodestone(location);
                compass.setItemMeta(meta);
            });
        });
    }


}
