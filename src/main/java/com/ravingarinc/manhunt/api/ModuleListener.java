package com.ravingarinc.manhunt.api;

import com.ravingarinc.manhunt.RavinPlugin;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class ModuleListener extends Module implements Listener {

    @SafeVarargs
    public ModuleListener(final Class<? extends Module> identifier, final RavinPlugin plugin, final Class<? extends Module>... dependsOn) {
        super(identifier, plugin, dependsOn);
    }

    @Override
    public void cancel() {
        HandlerList.unregisterAll(this);
    }

    @Override
    protected void load() throws ModuleLoadException {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
}
