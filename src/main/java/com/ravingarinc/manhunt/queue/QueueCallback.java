package com.ravingarinc.manhunt.queue;

import com.ravingarinc.manhunt.api.util.I;
import com.ravingarinc.manhunt.gameplay.Hunter;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

public class QueueCallback extends BukkitRunnable {
    private final Hunter hunter;
    private final long timeout;
    private final FutureTask<Location> spawnLocation;
    private final QueueManager manager;
    private final Location origin;
    private boolean accepted;
    private boolean declined;

    public QueueCallback(final QueueManager manager, final Hunter hunter, final long timeout, final Location origin, final FutureTask<Location> spawnLocation) {
        this.hunter = hunter;
        this.manager = manager;
        this.timeout = timeout;
        this.spawnLocation = spawnLocation;
        this.accepted = false;
        this.declined = false;
        this.origin = origin;
    }

    public void ask() {
        final Player player = hunter.player();
        player.sendTitle(ChatColor.DARK_GREEN + "A new hunter position is available!", "", 10, 60, 20);
        final ComponentBuilder builder = new ComponentBuilder(ChatColor.GRAY + "A new hunter position is available! Click ");
        builder.append(ChatColor.GREEN + "[Accept]")
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mh accept"))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Accept your position and join the hunt!")))
                .append(ChatColor.GRAY + " or ")
                .event((ClickEvent) null)
                .event((HoverEvent) null)
                .append(ChatColor.RED + "[Decline]")
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mh decline"))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Decline the position and give the chance to the next player in queue!")))
                .append(ChatColor.GRAY + " within " + timeout + "seconds to make your choice!")
                .event((ClickEvent) null)
                .event((HoverEvent) null);
        player.spigot().sendMessage(builder.create());
    }

    public synchronized void accept() {
        if (declined) {
            return;
        }
        accepted = true;
        final Player player = hunter.player();
        player.sendMessage(ChatColor.GREEN + "You have accepted the position!");
        player.getInventory().clear();
        player.updateInventory();
        Location location = null;
        try {
            location = spawnLocation.get(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            I.log(Level.SEVERE, "Encountered issue finding a suitable spawn location!", e);
        }
        if (location == null) {
            return;
        }
        player.teleport(location);
        player.sendTitle("", ChatColor.DARK_GREEN + "Good luck out there!", 20, 60, 20);

        hunter.start();
        manager.removeCallback(hunter);
        cancel();
    }

    public synchronized void decline() {
        if (accepted) {
            return;
        }
        declined = true;
        hunter.player().sendMessage(ChatColor.GRAY + "You have declined the position and have been placed at the back of the queue!");
        manager.removeCallback(hunter);
        manager.enqueue(hunter);
        cancel();
    }

    public synchronized void forceDecline() {
        if (accepted) {
            return;
        }
        declined = true;
        cancel();
    }

    @Override
    public void run() {
        if (!accepted && !declined) {
            manager.removeCallback(hunter);
            final Hunter newPoll = manager.poll();
            if (newPoll != null) {
                manager.addCallback(newPoll, origin, spawnLocation);
            }
        }
    }
}
