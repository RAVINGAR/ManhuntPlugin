package com.ravingarinc.manhunt.storage;

import com.ravingarinc.manhunt.RavinPlugin;
import com.ravingarinc.manhunt.api.Module;
import com.ravingarinc.manhunt.api.util.I;
import com.ravingarinc.manhunt.gameplay.PlayerListener;
import com.ravingarinc.manhunt.gameplay.PlayerManager;
import com.ravingarinc.manhunt.queue.QueueManager;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

public class ConfigManager extends Module {
    private final ConfigFile configFile;

    public ConfigManager(final RavinPlugin plugin) {
        super(ConfigManager.class, plugin);
        this.configFile = new ConfigFile(plugin, "config.yml");
    }

    @Override
    protected void load() {
        // fill with config fillers
        final Settings settings = plugin.getModule(PlayerManager.class).getSettings();

        final PlayerListener listener = plugin.getModule(PlayerListener.class);

        final FileConfiguration config = configFile.getConfig();
        consumeSection(config, "gameplay", (child) -> {
            wrap("max-hunters", child::getInt).ifPresent(v -> plugin.getModule(QueueManager.class).setMaxHunters(v));
            wrap("prey-lives", child::getInt).ifPresent(v -> settings.preyLives = v);
            wrap("hunter-spawn-range", child::getInt).ifPresent(v -> settings.hunterSpawnRange = v);
            wrap("block-blacklist", child::getStringList).ifPresent(v -> {
                v.forEach(s -> {
                    final Material match = Material.matchMaterial(s);
                    if (match != null) {
                        listener.blacklistBlock(match);
                    }
                });
            });
            wrap("item-blacklist", child::getStringList).ifPresent(v -> {
                v.forEach(s -> {
                    final Material match = Material.matchMaterial(s);
                    if (match != null) {
                        listener.blacklistItem(match);
                    }
                });
            });
        });
        consumeSection(config, "queue", (child) -> {
            wrap("confirm-timeout", child::getInt).ifPresent(v -> settings.confirmTimeout = v);
            wrap("priority-roles", child::getStringList).ifPresent(v -> {
                final QueueManager manager = plugin.getModule(QueueManager.class);
                v.forEach(s -> manager.addPriorityRole(s.toLowerCase()));
            });
        });
    }

    /**
     * Validates if a configuration section exists at the path from parent. If it does exist then it is consumed
     *
     * @param parent   The parent section
     * @param path     The path to child section
     * @param consumer The consumer
     */
    private void consumeSection(final ConfigurationSection parent, final String path, final Consumer<ConfigurationSection> consumer) {
        final ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) {
            I.log(Level.WARNING, parent.getCurrentPath() + " is missing a '%s' section!", path);
        }
        consumer.accept(section);
    }

    private <V> Optional<V> wrap(final String option, final Function<String, V> wrapper) {
        final V value = wrapper.apply(option);
        if (value == null) {
            I.log(Level.WARNING,
                    "Could not find configuration option '%s', please check your config! " +
                            "Using default value for now...", option);
        }
        return Optional.ofNullable(value);
    }

    @Override
    public void cancel() {
        this.configFile.reloadConfig();
    }
}
