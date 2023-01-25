package com.ravingarinc.manhunt.queue;

import com.ravingarinc.manhunt.RavinPlugin;
import com.ravingarinc.manhunt.api.Module;
import com.ravingarinc.manhunt.api.ModuleLoadException;
import com.ravingarinc.manhunt.api.util.I;
import com.ravingarinc.manhunt.gameplay.Hunter;
import com.ravingarinc.manhunt.gameplay.PlayerManager;
import com.ravingarinc.manhunt.gameplay.Prey;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

public class QueueManager extends Module {

    private final Map<UUID, QueueCallback> callbacks;
    private final Map<UUID, FutureTask<?>> futureTasks;
    private final Random random;
    private final BukkitScheduler scheduler;
    private Set<Hunter> ignoringHunters = null;
    private LinkedList<Hunter> queue;
    private PlayerManager manager;
    private long confirmTimeout = 15;
    private int maxSpawnRange;
    private int minSpawnRange;
    private int spawnX = 0;
    private int spawnZ = 0;

    public QueueManager(final RavinPlugin plugin) {
        super(QueueManager.class, plugin, PlayerManager.class);
        callbacks = new ConcurrentHashMap<>();
        queue = new LinkedList<>();
        random = new Random(System.currentTimeMillis());
        futureTasks = new ConcurrentHashMap<>();
        scheduler = plugin.getServer().getScheduler();
    }

    public void setMinSpawnRange(final int range) {
        this.minSpawnRange = range;
    }

    public void setMaxSpawnRange(final int range) {
        this.maxSpawnRange = range;
    }

    public void setConfirmTimeout(final long timeout) {
        this.confirmTimeout = timeout;
    }

    public void setPreySpawn(final int x, final int z) {
        this.spawnX = x;
        this.spawnZ = z;
    }

    @Override
    protected void load() throws ModuleLoadException {
        manager = plugin.getModule(PlayerManager.class);

        final Set<Hunter> newIgnores = new HashSet<>();
        if (ignoringHunters != null) {
            ignoringHunters.forEach(hunter -> manager.getPlayer(hunter.player()).ifPresent(h -> {
                if (h instanceof Hunter newHunter) {
                    newIgnores.add(newHunter);
                }
            }));
        }
        ignoringHunters = newIgnores;

        final LinkedList<Hunter> newQueue = new LinkedList<>();
        if (queue != null) {
            queue.forEach(trackable -> manager.getPlayer(trackable.player()).ifPresent(h -> {
                if (h instanceof Hunter hunter && !ignoringHunters.contains(hunter)) {
                    newQueue.addLast(hunter);
                }
            }));
            queue.clear();
        }
        queue = newQueue;
    }

    @Override
    public void cancel() {
        callbacks.values().forEach(BukkitRunnable::cancel);
        futureTasks.values().forEach(task -> task.cancel(true));
    }

    public void addIgnore(final Hunter hunter) {
        ignoringHunters.add(hunter);
    }

    public void remove(final Hunter hunter) {
        queue.remove(hunter);
    }

    public void removeIgnore(final Hunter hunter) {
        ignoringHunters.remove(hunter);
    }

    public boolean isInQueue(final Hunter hunter) {
        return queue.contains(hunter);
    }

    public void enqueue(final Hunter trackable) {
        if (ignoringHunters.contains(trackable)) {
            return;
        }
        if (trackable.hasPriority()) {
            for (int i = queue.size() - 1; i >= 0; i--) {
                final Hunter next = queue.get(i);
                if (next.hasPriority()) {
                    queue.add(i - 1, trackable);
                }
            }
        } else {
            queue.addLast(trackable);
        }
        for (int i = 0; i < queue.size(); i++) {
            queue.get(i).player().sendMessage(ChatColor.GRAY + "You are now position " + (i + 1) + " out of " + queue.size());
        }
    }

    @Nullable
    public Hunter poll() {
        return queue.poll();
    }

    public void addCallback(final Hunter hunter, final Location origin) {
        final FutureTask<Location> future = new FutureTask<>(() -> findSuitableLocation(origin.getWorld(), origin.getBlockX(), origin.getBlockZ(), minSpawnRange, maxSpawnRange));
        scheduler.runTask(plugin, future);
        this.addCallback(hunter, origin, future);
    }

    public List<String> getNamesInQueue() {
        final List<String> names = new ArrayList<>();
        queue.forEach(hunter -> names.add(hunter.player().getName()));
        return names;
    }

    public int callbackSize() {
        return callbacks.size();
    }

    public void acceptCallback(final Hunter hunter) {
        final QueueCallback callback = callbacks.get(hunter.player().getUniqueId());
        if (callback != null) {
            callback.accept();
        }
    }

    public void declineCallback(final Hunter hunter) {
        final QueueCallback callback = callbacks.get(hunter.player().getUniqueId());
        if (callback != null) {
            callback.decline();
        }
    }

    public void addCallback(final Hunter hunter, final Location origin, final FutureTask<Location> future) {
        final UUID uuid = hunter.player().getUniqueId();
        futureTasks.put(uuid, future);

        final QueueCallback task = new QueueCallback(this, hunter, confirmTimeout, origin, future);
        callbacks.put(uuid, task);
        task.ask();
        task.runTaskLater(plugin, confirmTimeout * 20L);
    }

    public boolean hasCallback(final Hunter hunter) {
        return callbacks.containsKey(hunter.player().getUniqueId());
    }

    @Nullable
    public QueueCallback removeCallback(final Hunter hunter) {
        final UUID uuid = hunter.player().getUniqueId();
        futureTasks.remove(uuid);
        return callbacks.remove(uuid);
    }

    public void addPreySpawn(final Prey prey) {
        final Player player = prey.player();
        final World world = player.getWorld();
        final FutureTask<Location> future = new FutureTask<>(() -> findSuitableLocation(world, spawnX, spawnZ, 0, 20));
        futureTasks.put(player.getUniqueId(), future);
        scheduler.runTask(plugin, future);
        scheduler.runTaskLater(plugin, () -> {
            try {
                if (player.isOnline()) {
                    final Location location = future.get(1000, TimeUnit.MILLISECONDS);
                    player.setGameMode(GameMode.SURVIVAL);
                    player.teleport(location);
                }
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                I.log(Level.SEVERE, "Could not find suitable spawn location for prey!", e);
            } finally {
                cancelFuture(player.getUniqueId());
            }
        }, 1L);
    }

    public void cancelFuture(final UUID uuid) {
        final FutureTask<?> future = futureTasks.remove(uuid);
        if (future != null) {
            future.cancel(false);
        }
    }

    private Location findSuitableLocation(final World world, final int x, final int z, final int minRange, final int range) {
        for (int i = 0; i < 5; i++) {
            int randomX = minRange + random.nextInt(range - minRange);
            int randomZ = minRange + random.nextInt(range - minRange);

            if (random.nextBoolean()) {
                randomX *= -1;
            }
            if (random.nextBoolean()) {
                randomZ *= -1;
            }
            randomX += x;
            randomZ += z;

            final Location location = world.getHighestBlockAt(randomX, randomZ).getLocation();
            location.add(0, 1, 0);
            final int newX = location.getBlockX();
            final int newY = location.getBlockY();
            final int newZ = location.getBlockZ();
            boolean isValid = true;
            for (int dX = -2; dX < 2; dX++) {
                for (int dZ = -2; dZ < 2; dZ++) {
                    if (!world.getBlockAt(new Location(world, newX, newY, newZ)).getType().isAir()) {
                        isValid = false;
                        break;
                    }
                }
            }
            if (isValid) {
                return location;
            }
        }
        final int i = random.nextBoolean() ? 1 : -1;
        return new Location(world, x + minRange * i, world.getHighestBlockYAt(x, z) + 1, z - minRange * i);
    }
}
