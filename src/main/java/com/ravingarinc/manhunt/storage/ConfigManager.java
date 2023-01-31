package com.ravingarinc.manhunt.storage;

import com.ravingarinc.manhunt.RavinPlugin;
import com.ravingarinc.manhunt.api.Module;
import com.ravingarinc.manhunt.api.util.I;
import com.ravingarinc.manhunt.gameplay.PlayerListener;
import com.ravingarinc.manhunt.queue.GameplayManager;
import com.ravingarinc.manhunt.queue.QueueManager;
import com.ravingarinc.manhunt.role.LuckPermsHandler;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigManager extends Module {
    private final ConfigFile configFile;

    private final Pattern numberMatcher = Pattern.compile("^[^\\d]*(\\d+)");

    public ConfigManager(final RavinPlugin plugin) {
        super(ConfigManager.class, plugin);
        this.configFile = new ConfigFile(plugin, "config.yml");
    }

    @Override
    protected void load() {
        final PlayerListener listener = plugin.getModule(PlayerListener.class);
        final QueueManager queueManager = plugin.getModule(QueueManager.class);
        final GameplayManager gameplayManager = plugin.getModule(GameplayManager.class);

        final FileConfiguration config = configFile.getConfig();
        consumeSection(config, "gameplay", (child) -> {
            wrap("spawn-world-name", child::getString).ifPresent(w -> {
                World world = plugin.getServer().getWorld(w);
                if (world == null) {
                    I.log(Level.WARNING, "Could not find a world named '" + w + "'! Using default instead...");
                    world = plugin.getServer().getWorlds().get(0);
                }
                gameplayManager.setWorld(world);
            });
            wrap("prey-spawn-world", child::getString).ifPresent(w -> {
                World world = plugin.getServer().getWorld(w);
                if (world == null) {
                    I.log(Level.WARNING, "Could not find a world named '" + w + "'! Using default instead...");
                    world = plugin.getServer().getWorlds().get(0);
                }
                queueManager.setGameWorld(world);
            });
            wrap("max-hunters", child::getInt).ifPresent(gameplayManager::setMaxHunters);
            wrap("prey-lives", child::getInt).ifPresent(gameplayManager::setMaxLives);
            wrap("hunter-min-spawn-range", child::getInt).ifPresent(queueManager::setMinSpawnRange);
            wrap("hunter-max-spawn-range", child::getInt).ifPresent(queueManager::setMaxSpawnRange);
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

            wrap("prey-spawn-location", child::getString).ifPresent(v -> {
                final String[] split = v.split(",");
                if (split.length > 1) {
                    try {
                        final int x = Integer.parseInt(split[0]);
                        final int z = Integer.parseInt(split[1]);
                        queueManager.setPreySpawn(x, z);
                    } catch (final NumberFormatException e) {
                        I.log(Level.WARNING, "Invalid prey-spawn-location in config.yml!");
                    }
                } else {
                    I.log(Level.WARNING, "Invalid prey-spawn-location in config.yml!");
                }
            });
        });
        consumeSection(config, "queue", (child) -> {
            final LuckPermsHandler manager = plugin.getModule(LuckPermsHandler.class);
            manager.clearPriorityRoles();

            wrap("twitch-quick-link", child::getString).ifPresent(gameplayManager::setTwitchLink);
            wrap("youtube-quick-link", child::getString).ifPresent(gameplayManager::setYoutubeLink);

            wrap("prey-role", child::getString).ifPresent(manager::setPreyRole);
            wrap("confirm-timeout", child::getInt).ifPresent(queueManager::setConfirmTimeout);
            wrap("priority-roles", child::getStringList).ifPresent(v -> v.forEach(manager::addPriorityRole));
            wrap("auto-teleport-new-prey", child::getBoolean).ifPresent(gameplayManager::setTeleportNewPrey);
            wrap("cooldown", child::getString).ifPresent(v -> {
                final Matcher matcher = numberMatcher.matcher(v);
                if (!matcher.find()) {
                    I.log(Level.WARNING, "The configuration option in config.yml / queue.cooldown could not be parsed as '" + v + "' is not a valid value!");
                    return;
                }
                final int time;
                try {
                    time = Integer.parseInt(matcher.group());
                } catch (final NumberFormatException e) {
                    I.log(Level.WARNING, "The configuration option in config.yml / queue.cooldown could not be parsed as '" + v + "' is not a valid value!");
                    return;
                }
                if (time < 0) {
                    I.log(Level.WARNING, "The configuration option in config.yml / queue.cooldown cannot be less than 1!");
                    return;
                }
                if (v.endsWith("h") || v.endsWith("hour") || v.endsWith("hours")) {
                    gameplayManager.setAttemptCooldown(time, TimeUnit.HOURS);
                } else if (v.endsWith("m") || v.endsWith("min") || v.endsWith("mins") || v.endsWith("minutes") || v.endsWith("minute")) {
                    gameplayManager.setAttemptCooldown(time, TimeUnit.MINUTES);
                } else if (v.endsWith("s") || v.endsWith("sec") || v.endsWith("secs") || v.endsWith("seconds") || v.endsWith("second")) {
                    gameplayManager.setAttemptCooldown(time, TimeUnit.SECONDS);
                }
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
