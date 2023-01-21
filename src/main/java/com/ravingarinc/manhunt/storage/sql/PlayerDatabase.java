package com.ravingarinc.manhunt.storage.sql;

import com.ravingarinc.manhunt.RavinPlugin;
import com.ravingarinc.manhunt.api.ModuleLoadException;
import com.ravingarinc.manhunt.api.util.I;
import com.ravingarinc.manhunt.gameplay.Hunter;
import com.ravingarinc.manhunt.gameplay.PlayerManager;
import com.ravingarinc.manhunt.gameplay.Prey;
import com.ravingarinc.manhunt.gameplay.Trackable;
import com.ravingarinc.manhunt.role.LuckPermsHandler;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;

public class PlayerDatabase extends Database {
    private PlayerManager manager;

    private LuckPermsHandler handler;

    public PlayerDatabase(final RavinPlugin plugin) {
        super(Schema.PLAYERS, Schema.createTable, PlayerDatabase.class, plugin, LuckPermsHandler.class);
    }

    @Override
    public void load() throws ModuleLoadException {
        super.load();
        this.manager = plugin.getModule(PlayerManager.class);
        this.handler = plugin.getModule(LuckPermsHandler.class);
    }

    @Override
    public void cancel() {
        super.cancel();
    }

    public void savePlayer(final Trackable trackable) {
        prepareStatement(Schema.select, (statement) -> {
            try {
                statement.setLong(1, trackable.lastAttempt());
                statement.setString(2, trackable.player().getUniqueId().toString());
            } catch (final SQLException e) {
                I.log(Level.SEVERE, "Encountered issue preparing statement!", e);
            }
        });
    }

    public Optional<Trackable> loadPlayer(final Player player) {
        return Optional.ofNullable(query(Schema.select, (statement) -> {
            try {
                statement.setString(1, player.getUniqueId().toString());
            } catch (final SQLException exception) {
                I.log(Level.SEVERE, "Encountered issue preparing statement!", exception);
            }
        }, (result) -> {
            try {
                final String role = handler.getRoleForPlayer(player);
                final Trackable trackable = manager.getSettings().preyRole.equals(role)
                        ? new Prey(player, role)
                        : new Hunter(player, role);
                if (result.next()) {
                    // Found player
                    trackable.setLastAttempt(result.getLong(1));
                } else {
                    // No player
                    prepareStatement(Schema.insert, (statement) -> {
                        try {
                            statement.setString(1, player.getUniqueId().toString());
                            statement.setLong(2, 0L);
                        } catch (final SQLException exception) {
                            I.log(Level.SEVERE, "Encountered issue preparing statement!", exception);
                        }
                    });
                }
                return trackable;
            } catch (final SQLException exception) {
                I.log(Level.SEVERE, "Encountered issue loading player!", exception);
            }
            return null;
        }));
    }
}
