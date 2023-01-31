package com.ravingarinc.manhunt.queue;

import com.ravingarinc.manhunt.RavinPlugin;
import com.ravingarinc.manhunt.api.Module;
import com.ravingarinc.manhunt.api.ModuleLoadException;
import com.ravingarinc.manhunt.gameplay.Hunter;
import com.ravingarinc.manhunt.gameplay.PlayerManager;
import com.ravingarinc.manhunt.gameplay.Prey;
import com.ravingarinc.manhunt.gameplay.Trackable;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class GameplayManager extends Module {
    private final Set<UUID> respawningPlayers;
    private final Random random;
    private Set<Hunter> currentHunters;
    private Set<Prey> currentPrey;
    private PlayerManager manager;
    private QueueManager queue;
    private int maxHunters;

    private long attemptCooldown;

    private boolean teleportNewPrey = true;

    private int maxLives = 5;

    private World world = null;

    private QueueRunner queueChecker;

    private String youtubeLink = "";
    private String twitchLink = "";

    public GameplayManager(final RavinPlugin plugin) {
        super(GameplayManager.class, plugin, PlayerManager.class, QueueManager.class);
        maxHunters = 5;
        respawningPlayers = new HashSet<>();
        currentHunters = new HashSet<>();
        random = new Random(System.currentTimeMillis());
    }

    public void setYoutubeLink(final String youtube) {
        this.youtubeLink = youtube;
    }

    public void setTwitchLink(final String twitch) {
        this.twitchLink = twitch;
    }

    public void setTeleportNewPrey(final boolean newPrey) {
        this.teleportNewPrey = newPrey;
    }

    public void setWorld(final World world) {
        this.world = world;
    }

    public void setMaxHunters(final int hunters) {
        this.maxHunters = hunters;
    }

    public void setMaxLives(final int lives) {
        this.maxLives = lives;
    }

    public void setAttemptCooldown(final long attemptCooldown, final TimeUnit timeUnit) {
        this.attemptCooldown = TimeUnit.SECONDS.convert(attemptCooldown, timeUnit);
    }

    @Override
    protected void load() throws ModuleLoadException {
        manager = plugin.getModule(PlayerManager.class);
        queue = plugin.getModule(QueueManager.class);

        final Set<Prey> newPrey = new HashSet<>();
        if (currentPrey != null) {
            currentPrey.forEach(trackable -> manager.getPlayer(trackable.player()).ifPresent(p -> {
                if (p instanceof Prey prey) {
                    newPrey.add(prey);
                }
            }));
            currentPrey.clear();
        }
        currentPrey = newPrey;

        final Set<Hunter> newSet = new HashSet<>(maxHunters);
        if (currentHunters != null) {
            currentHunters.forEach(trackable -> manager.getPlayer(trackable.player()).ifPresent(h -> {
                if (h instanceof Hunter hunter) {
                    newSet.add(hunter);
                }
            }));
            currentHunters.clear();
        }
        currentHunters = newSet;
    }

    public void sendSupportMessage(final Player player) {
        final ComponentBuilder builder = new ComponentBuilder();
        builder.append(ChatColor.GRAY + "You did not join the queue as you have already played as a hunter! If you wish to try your luck again, become a paid subscriber for UriahShorts on ")
                .append(ChatColor.LIGHT_PURPLE + "[Twitch]")
                .event(new ClickEvent(ClickEvent.Action.OPEN_URL, twitchLink))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.DARK_GRAY + "Click here to open the URL")))
                .append(ChatColor.GRAY + " or ")
                .event((ClickEvent) null)
                .event((HoverEvent) null)
                .append(ChatColor.RED + "[YouTube]")
                .event(new ClickEvent(ClickEvent.Action.OPEN_URL, youtubeLink))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.DARK_GRAY + "Click here to open the URL")))
                .append(ChatColor.GRAY + "!")
                .event((ClickEvent) null)
                .event((HoverEvent) null);
        player.spigot().sendMessage(builder.create());
    }

    public void resetLocation(final Hunter hunter) {
        hunter.player().teleport(world.getSpawnLocation());
    }

    public void tryJoin(final Trackable trackable) {
        if (trackable instanceof Hunter hunter) {
            final long attempt = hunter.lastAttempt();
            if (attempt != 0) {
                if (!hunter.hasPriority()) {
                    sendSupportMessage(hunter.player());
                    return;
                }
                if (checkCooldown(hunter)) {
                    return;
                }
            }
            hunter.player().sendMessage(ChatColor.GRAY + "You have joined the queue!");
            queue.enqueue(hunter);
        } else if (trackable instanceof Prey prey) {
            if (prey.getLives() == -1) {
                prey.setLives(maxLives);
                if (teleportNewPrey) {
                    spawnNewPrey(prey);
                }
            } else {
                currentPrey.add(prey);
            }
        }
    }

    public boolean checkCooldown(final Hunter hunter) {
        final long difference = TimeUnit.SECONDS.convert(System.currentTimeMillis() - hunter.lastAttempt(), TimeUnit.MILLISECONDS);
        if (difference < attemptCooldown) {
            final String message;
            final long seconds = attemptCooldown - difference;
            if (seconds >= 60) {
                final long minutes = TimeUnit.MINUTES.convert(seconds, TimeUnit.SECONDS);
                if (minutes >= 60) {
                    final long hours = TimeUnit.HOURS.convert(minutes, TimeUnit.MINUTES);
                    message = hours + (hours == 1 ? "hour" : "hours");
                } else {
                    message = minutes + (minutes == 1 ? "minute" : "minutes");
                }
            } else {
                message = seconds + (seconds == 1 ? "second" : "seconds");
            }
            hunter.player().sendMessage(ChatColor.RED + "You did not join the queue as you have played as a hunter too recently! You must wait an additional " + message);
            return true;
        }
        return false;
    }

    public List<String> getCurrentHunters() {
        final List<String> list = new ArrayList<>();
        currentHunters.forEach(hunter -> list.add(hunter.player().getName()));
        return list;
    }

    public void onSpawn(final Trackable trackable) {
        final Player player = trackable.player();
        if (respawningPlayers.remove(player.getUniqueId())) {
            if (trackable instanceof Hunter) {
                player.teleport(world.getSpawnLocation());
            } else if (trackable instanceof Prey prey) {
                if (prey.getLives() == 1) {
                    currentPrey.remove(prey);
                    player.setGameMode(GameMode.SPECTATOR);
                    if (currentPrey.isEmpty()) {
                        finishGame();
                    } else {
                        player.sendTitle(ChatColor.RED + "You have run out of lives!", ChatColor.RED + "You have been alive for" + prey.getTimeAlive() + "!", 10, 100, 20);
                    }
                } else {
                    prey.removeLife();
                    player.sendTitle(ChatColor.RED + "You have lost a life!", ChatColor.RED + "You have " + prey.getLives() + " lives left..", 10, 100, 20);
                    player.sendMessage();
                }
            }
        }
    }

    public void onDeath(final Trackable trackable) {
        respawningPlayers.add(trackable.player().getUniqueId());
        if (trackable instanceof Hunter hunter) {
            if (currentHunters.remove(hunter)) {
                Bukkit.broadcastMessage(ChatColor.DARK_RED + "The hunter '" + hunter.player().getName() + "' has been defeated! They were alive for " + hunter.getTimeAlive());
                hunter.end();
                manager.savePlayer(hunter.player());
            }
        }
    }

    public boolean isInGame(final Trackable trackable) {
        if (trackable instanceof Hunter hunter) {
            return currentHunters.contains(hunter);
        } else if (trackable instanceof Prey prey) {
            return currentPrey.contains(prey);
        }
        return false;
    }

    public void remove(final Trackable trackable) {
        if (trackable instanceof Hunter hunter) {
            if (currentHunters.remove(hunter)) {
                Bukkit.broadcastMessage(ChatColor.DARK_RED + "The hunter '" + hunter.player().getName() + "' has disconnected! They were alive for " + hunter.getTimeAlive());
                hunter.end();
                manager.savePlayer(hunter.player());
            }
            queue.remove(hunter);
            queue.removeIgnore(hunter);
            queue.removeCallback(hunter);
        } else if (trackable instanceof Prey prey) {
            currentPrey.remove(prey);
            queue.cancelFuture(prey.player().getUniqueId());
        }
    }

    public List<String> getCurrentPrey() {
        final List<String> list = new ArrayList<>();
        currentPrey.forEach(prey -> list.add(prey.player().getName()));
        return list;
    }

    public void acceptCallback(final Hunter hunter) {
        if (queue.tryAcceptCallback(hunter)) {
            hunter.start();
            currentHunters.add(hunter);
        }
    }

    public void declineCallback(final Hunter hunter) {
        if (queue.tryDeclineCallback(hunter)) {
            hunter.player().sendMessage(ChatColor.GRAY + "You have declined the position and have been placed at the back of the queue!");
            queue.enqueue(hunter);
        }
    }

    public void spawnNewPrey(final Prey prey) {
        currentPrey.add(prey);
        queue.addPreySpawn(prey);
    }

    public void spawnAllPrey() {
        currentPrey.clear();
        manager.getPlayers().forEach(trackable -> {
            if (trackable instanceof Prey prey) {
                currentPrey.add(prey);
            }
        });
        currentPrey.forEach(prey -> queue.addPreySpawn(prey));
    }

    public void startQueue() {
        if (queueChecker == null) {
            queueChecker = new QueueRunner();
            queueChecker.runTaskTimer(plugin, 0L, 40L);
        }
    }

    public void stopQueue() {
        if (queueChecker != null) {
            queueChecker.cancel();
            queueChecker = null;
        }
    }

    public void clearHunters() {
        currentHunters.forEach(hunter -> {
            hunter.end();
            final Player player = hunter.player();
            player.getInventory().clear();
            player.teleport(world.getSpawnLocation());
            player.updateInventory();
        });
        currentHunters.clear();
    }

    public void finishGame() {
        stopQueue();
        final Location spawn = world.getSpawnLocation();
        final StringBuilder subTitles = new StringBuilder();
        subTitles.append(ChatColor.DARK_GREEN);
        subTitles.append("Congratulations to");

        currentHunters.forEach(hunter -> {
            subTitles.append(" ");
            subTitles.append(hunter.player().getName());

            visualEffects(hunter.player(), random.nextLong(20));

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                hunter.player().teleport(spawn);
                hunter.player().getInventory().clear();
                hunter.player().updateInventory();
            }, 200L);
        });
        currentHunters.clear();

        Bukkit.getOnlinePlayers().forEach(player -> {
            player.sendTitle(ChatColor.GREEN + "The Hunters have Won!", subTitles.toString(), 10, 200, 20);
        });
    }

    private void visualEffects(final Player player, final long delay) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            final Firework firework = player.getLocation().getWorld().spawn(player.getLocation(), Firework.class);
            final FireworkMeta meta = firework.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder()
                    .flicker(true).withColor(Color.RED).withColor(Color.GREEN).build());
            firework.setFireworkMeta(meta);
        }, delay + 20L);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {

            final Firework firework = player.getLocation().getWorld().spawn(player.getLocation(), Firework.class);
            final FireworkMeta meta = firework.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder()
                    .with(FireworkEffect.Type.BURST).withColor(Color.BLUE).withColor(Color.ORANGE).build());
            meta.addEffect(FireworkEffect.builder()
                    .with(FireworkEffect.Type.STAR).withColor(Color.BLUE).withColor(Color.ORANGE).build());
            firework.setFireworkMeta(meta);
        }, delay + 30L);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            final Firework firework = player.getLocation().getWorld().spawn(player.getLocation(), Firework.class);
            final FireworkMeta meta = firework.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BALL_LARGE).flicker(true).withColor(Color.GREEN).withColor(Color.YELLOW).build());
            firework.setFireworkMeta(meta);
        }, delay + 40L);
    }

    @Override
    public void cancel() {
        if (queueChecker != null) {
            queueChecker.cancel();
            queueChecker = null;
        }
    }


    private class QueueRunner extends BukkitRunnable {
        @Override
        public void run() {
            while (!isCancelled() && currentHunters.size() + queue.callbackSize() < maxHunters) {
                final Hunter hunter = queue.poll();
                if (hunter == null) {
                    break;
                }
                currentPrey.stream().findAny().ifPresent(prey -> queue.addCallback(hunter, prey.player().getLocation()));
            }
        }
    }
}
