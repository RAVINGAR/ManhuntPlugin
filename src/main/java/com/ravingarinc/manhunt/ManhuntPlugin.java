package com.ravingarinc.manhunt;

import com.ravingarinc.manhunt.command.ManhuntCommand;
import com.ravingarinc.manhunt.gameplay.PlayerListener;
import com.ravingarinc.manhunt.gameplay.PlayerManager;
import com.ravingarinc.manhunt.queue.GameplayManager;
import com.ravingarinc.manhunt.queue.QueueManager;
import com.ravingarinc.manhunt.role.LuckPermsHandler;
import com.ravingarinc.manhunt.storage.ConfigManager;

public final class ManhuntPlugin extends RavinPlugin {
    @Override
    public void loadModules() {

        // add managers
        addModule(ConfigManager.class);
        addModule(LuckPermsHandler.class);
        addModule(PlayerManager.class);
        addModule(QueueManager.class);
        addModule(GameplayManager.class);
        addModule(PlayerListener.class);
        //addModule(SQLHandler.class); // comment out if not needed
        // add listeners

    }

    @Override
    public void loadCommands() {
        new ManhuntCommand(this).register(this);
    }

    @Override
    public void onDisable() {
        getModule(GameplayManager.class).clearHunters();
        super.onDisable();
    }
}
