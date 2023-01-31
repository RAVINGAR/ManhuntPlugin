package com.ravingarinc.manhunt.gameplay;

import com.ravingarinc.manhunt.RavinPlugin;
import com.ravingarinc.manhunt.api.Module;
import com.ravingarinc.manhunt.api.ModuleLoadException;
import com.ravingarinc.manhunt.queue.GameplayManager;
import com.ravingarinc.manhunt.role.LuckPermsHandler;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager extends Module {
    private final Map<UUID, Trackable> trackables;

    private LuckPermsHandler handler;

    private GameplayManager gameplayManager;

    public PlayerManager(final RavinPlugin plugin) {
        super(PlayerManager.class, plugin, LuckPermsHandler.class);
        this.trackables = new ConcurrentHashMap<>();
    }

    @Override
    protected void load() throws ModuleLoadException {
        handler = plugin.getModule(LuckPermsHandler.class);
        gameplayManager = plugin.getModule(GameplayManager.class);
        for (final Player player : plugin.getServer().getOnlinePlayers()) {
            this.handler.loadPlayer(player).ifPresent(trackable -> trackables.put(player.getUniqueId(), trackable));
        }
    }

    @Nullable
    public Trackable loadPlayer(final Player player) {
        final Optional<Trackable> opt = this.handler.loadPlayer(player);
        if (opt.isPresent()) {
            final Trackable trackable = opt.get();
            trackables.put(player.getUniqueId(), trackable);
            gameplayManager.tryJoin(trackable);
            return trackable;
        }
        return null;
    }

    public Optional<Trackable> getPlayer(final Player player) {
        return Optional.ofNullable(trackables.get(player.getUniqueId()));
    }

    @Override
    public void cancel() {
        trackables.values().forEach(trackable -> this.handler.savePlayer(trackable));
        trackables.clear();
    }

    public Collection<Trackable> getPlayers() {
        return Collections.unmodifiableCollection(trackables.values());
    }

    public void savePlayer(final Player player) {
        getPlayer(player).ifPresent(trackable -> this.handler.savePlayer(trackable));
    }

    public void unloadPlayer(final Player player) {
        getPlayer(player).ifPresent(trackable -> {
            this.gameplayManager.remove(trackable);
            this.handler.savePlayer(trackable);
            this.trackables.remove(trackable.player().getUniqueId());
        });
    }
}
