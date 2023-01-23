package com.ravingarinc.manhunt.queue;

import com.ravingarinc.manhunt.RavinPlugin;
import com.ravingarinc.manhunt.api.Module;
import com.ravingarinc.manhunt.api.ModuleLoadException;
import com.ravingarinc.manhunt.gameplay.Hunter;
import com.ravingarinc.manhunt.gameplay.PlayerManager;
import com.ravingarinc.manhunt.gameplay.Prey;
import com.ravingarinc.manhunt.gameplay.Trackable;
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

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameplayManager extends Module {
    private final Set<UUID> respawningPlayers;
    private final Random random;
    private final AtomicBoolean isQueueStopped;
    private Set<Hunter> currentHunters;
    private Set<Prey> currentPrey;
    private PlayerManager manager;
    private QueueManager queue;
    private int maxHunters;

    private long attemptCooldown;

    private World world = null;

    public GameplayManager(final RavinPlugin plugin) {
        super(GameplayManager.class, plugin, PlayerManager.class, QueueManager.class);
        maxHunters = 5;
        respawningPlayers = new HashSet<>();
        currentHunters = new HashSet<>();
        random = new Random(System.currentTimeMillis());
        isQueueStopped = new AtomicBoolean(true);
    }

    public void setWorld(final World world) {
        this.world = world;
    }

    public void setMaxHunters(final int hunters) {
        this.maxHunters = hunters;
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

    public void tryJoin(final Trackable trackable) {
        if (trackable instanceof Hunter hunter) {
            final long attempt = hunter.lastAttempt();
            if (attempt != 0) {
                final long difference = TimeUnit.SECONDS.convert(System.currentTimeMillis() - attempt, TimeUnit.MILLISECONDS);
                // Lets say the time that has passed since the last attempt is 50 seconds.
                // Lets say the cooldown is 60 seconds.
                // If diff is LESS than cooldown. Then invlaid
                //
                if (difference < attemptCooldown) {
                    String message = "";
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
                    return;
                }
            }
            queue.enqueue(hunter);

            hunter.player().teleport(world.getSpawnLocation());
        }
    }

    public void onSpawn(final Trackable trackable) {
        final Player player = trackable.player();
        if (respawningPlayers.contains(player.getUniqueId())) {
            if (trackable instanceof Hunter) {
                player.teleport(world.getSpawnLocation());
            } else if (trackable instanceof Prey prey) {
                if (prey.getLives() == 1) {
                    currentPrey.remove(prey);
                    player.setGameMode(GameMode.SPECTATOR);
                    if (currentPrey.isEmpty()) {
                        finishGame();
                    } else {
                        player.sendTitle(ChatColor.RED + "You have run out of lives!", ChatColor.RED + "You have been alive for" + prey.getTimeAlive() + "!", 10, 70, 20);
                    }
                } else {
                    prey.removeLife();
                    player.sendTitle(ChatColor.RED + "You have lost a life!", ChatColor.RED + "You have " + prey.getLives() + " lives left..", 10, 70, 20);
                    player.sendMessage();
                }
            }
        }
    }

    public void onRemoval(final Trackable trackable, final boolean disconnected) {
        if (!disconnected) {
            respawningPlayers.add(trackable.player().getUniqueId());
        }
        if (trackable instanceof Hunter hunter) {
            if (currentHunters.remove(hunter)) {
                Bukkit.broadcastMessage(ChatColor.DARK_RED + "The hunter '" + hunter.player().getName() + "' has been defeated! They were alive for " + hunter.getTimeAlive());
                hunter.end();
                if (!isQueueStopped.getAcquire() && currentHunters.size() + queue.callbackSize() < maxHunters) {
                    spawnNewHunter();
                }
            }

        } else if (trackable instanceof Prey prey) {
            if (disconnected) {
                currentPrey.remove(prey);
            }
        }
    }

    public void spawnNewHunter() {
        final Hunter hunter = queue.poll();
        if (hunter == null) {
            throw new IllegalStateException("There are no hunters!");
        }
        currentPrey.stream().findAny().ifPresent(prey -> queue.addCallback(hunter, prey.player().getLocation()));
    }


    public void teleportPrey() {
        currentPrey.clear();
        manager.getPlayers().forEach(trackable -> {
            if (trackable instanceof Prey prey) {
                currentPrey.add(prey);
            }
        });
        currentPrey.forEach(prey -> queue.addPreySpawn(prey));
    }

    public void startQueue() {
        isQueueStopped.setRelease(false);
        while (currentHunters.size() + queue.callbackSize() < maxHunters) {
            spawnNewHunter();
        }
    }

    public void stopQueue() {
        isQueueStopped.setRelease(true);
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
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "All hunters have been cleared!");
        if (!isQueueStopped.getAcquire()) {
            while (currentHunters.size() + queue.callbackSize() < maxHunters) {
                spawnNewHunter();
            }
        }
    }

    public void finishGame() {
        // todo a lot of this is just kinda lazy
        final Location spawn = world.getSpawnLocation();
        final StringBuilder subTitles = new StringBuilder();
        subTitles.append(ChatColor.DARK_GREEN);
        subTitles.append("Congratulations to ");

        currentHunters.forEach(hunter -> {
            subTitles.append(" ");
            subTitles.append(hunter.player().getName());

            visualEffects(hunter.player(), random.nextLong(20));

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                hunter.player().teleport(spawn);
            }, 100L);
        });
        currentHunters.clear();

        Bukkit.getOnlinePlayers().forEach(player -> {
            player.sendTitle(ChatColor.GREEN + "The Hunters have Won!", subTitles.toString(), 10, 80, 20);
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
                    .with(FireworkEffect.Type.STAR).withColor(Color.PURPLE).withColor(Color.ORANGE).build());
            firework.setFireworkMeta(meta);
        }, delay + 50L);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            final Firework firework = player.getLocation().getWorld().spawn(player.getLocation(), Firework.class);
            final FireworkMeta meta = firework.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BALL_LARGE).flicker(true).withColor(Color.PURPLE).withColor(Color.YELLOW).build());
            firework.setFireworkMeta(meta);
        }, delay + 95L);
    }

    @Override
    public void cancel() {

    }
}
