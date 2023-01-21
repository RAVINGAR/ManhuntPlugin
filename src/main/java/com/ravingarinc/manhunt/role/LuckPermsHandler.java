package com.ravingarinc.manhunt.role;

import com.ravingarinc.manhunt.RavinPlugin;
import com.ravingarinc.manhunt.api.Module;
import com.ravingarinc.manhunt.api.ModuleLoadException;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class LuckPermsHandler extends Module {
    private LuckPerms luckPerms;

    public LuckPermsHandler(final RavinPlugin plugin) {
        super(LuckPermsHandler.class, plugin);
    }

    @Override
    protected void load() throws ModuleLoadException {
        final RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider == null) {
            throw new ModuleLoadException(this, "Could not load " + this.getName() + " as LuckPerms was not loaded!");
        } else {
            luckPerms = provider.getProvider();
        }
    }

    public String getRoleForPlayer(final Player player) {
        //todo
        return null;
    }

    @Override
    public void cancel() {

    }
}
