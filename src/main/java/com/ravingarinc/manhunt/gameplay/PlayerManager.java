package com.ravingarinc.manhunt.gameplay;

import com.ravingarinc.manhunt.RavinPlugin;
import com.ravingarinc.manhunt.api.Module;
import com.ravingarinc.manhunt.api.ModuleLoadException;
import com.ravingarinc.manhunt.storage.Settings;
import com.ravingarinc.manhunt.storage.sql.PlayerDatabase;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager extends Module {
    private final Settings settings;
    private final Map<UUID, Trackable> trackables;

    private PlayerDatabase database;

    public PlayerManager(final RavinPlugin plugin) {
        super(PlayerManager.class, plugin, PlayerDatabase.class);
        this.settings = new Settings();
        this.trackables = new ConcurrentHashMap<>();
    }

    public Settings getSettings() {
        return settings;
    }

    @Override
    protected void load() throws ModuleLoadException {
        database = plugin.getModule(PlayerDatabase.class);
        for (final Player player : plugin.getServer().getOnlinePlayers()) {
            loadPlayer(player);
        }
    }

    public void loadPlayer(final Player player) {
        this.database.queue(() -> this.database.loadPlayer(player).ifPresent(trackable -> trackables.put(player.getUniqueId(), trackable)));
    }

    public Optional<Trackable> getPlayer(final Player player) {
        return Optional.ofNullable(trackables.get(player.getUniqueId()));
    }

    @Override
    public void cancel() {
        trackables.values().forEach(trackable -> database.savePlayer(trackable));
        trackables.clear();
    }

    public Collection<Trackable> getPlayers() {
        return Collections.unmodifiableCollection(trackables.values());
    }

    public void unloadPlayer(final Player player) {
        final Trackable trackable = trackables.get(player.getUniqueId());
        this.database.queue(() -> this.database.savePlayer(trackable));
        this.trackables.remove(player.getUniqueId());
    }
}
