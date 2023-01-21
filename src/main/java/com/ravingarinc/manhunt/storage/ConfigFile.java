package com.ravingarinc.manhunt.storage;

import com.ravingarinc.manhunt.RavinPlugin;
import com.ravingarinc.manhunt.api.util.I;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

public class ConfigFile {
    private final RavinPlugin plugin;
    private final String name;
    private final File file;
    private final FileConfiguration config;

    public ConfigFile(final RavinPlugin plugin, final String name) {
        this.plugin = plugin;
        this.name = name;
        this.file = new File(plugin.getDataFolder(), name);
        this.config = YamlConfiguration.loadConfiguration(file);
        saveDefaultConfig();
    }

    private void saveDefaultConfig() {
        try (final InputStream stream = plugin.getResource(name)) {
            if (stream != null) {
                final YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(stream));
                this.config.setDefaults(defaultConfig);
            }
        } catch (final IOException e) {
            I.log(Level.SEVERE, "Encountered issue loading default configuration for file '%s'!", name, e);
        }
        config.options().copyDefaults(true);
        saveConfig();
    }

    public final void reloadConfig() {
        try {
            this.config.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            I.log(Level.SEVERE, "Encountered issue reloading configuration!", e);
        }
    }

    public final void saveConfig() {
        try {
            config.save(file);
        } catch (final IOException e) {
            I.log(Level.SEVERE, "Encountered error saving config; " + name + "\n" + e.getMessage());
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

}
