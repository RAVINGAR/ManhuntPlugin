package com.ravingarinc.manhunt.gameplay;

import com.ravingarinc.manhunt.RavinPlugin;
import com.ravingarinc.manhunt.api.Module;
import com.ravingarinc.manhunt.api.ModuleLoadException;
import com.ravingarinc.manhunt.role.LuckPermsHandler;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager extends Module {
    private final Map<UUID, Trackable> trackables;

    private LuckPermsHandler handler;

    public PlayerManager(final RavinPlugin plugin) {
        super(PlayerManager.class, plugin, LuckPermsHandler.class);
        this.trackables = new ConcurrentHashMap<>();
    }

    @Override
    protected void load() throws ModuleLoadException {
        handler = plugin.getModule(LuckPermsHandler.class);
        for (final Player player : plugin.getServer().getOnlinePlayers()) {
            loadPlayer(player);
        }
    }

    public void loadPlayer(final Player player) {
        trackables.put(player.getUniqueId(), this.handler.loadPlayer(player));
    }

    public Optional<Trackable> getPlayer(final Player player) {
        return Optional.ofNullable(trackables.get(player.getUniqueId()));
    }

    @Override
    public void cancel() {
        trackables.values().forEach(trackable -> handler.savePlayer(trackable));
        trackables.clear();
    }

    public Collection<Trackable> getPlayers() {
        return Collections.unmodifiableCollection(trackables.values());
    }

    public void unloadPlayer(final Player player) {
        final Trackable trackable = trackables.get(player.getUniqueId());
        this.handler.savePlayer(trackable);
        this.trackables.remove(player.getUniqueId());
    }
}
