package com.ravingarinc.manhunt;

import com.ravingarinc.manhunt.command.ParentCommand;
import com.ravingarinc.manhunt.gameplay.PlayerListener;
import com.ravingarinc.manhunt.gameplay.PlayerManager;
import com.ravingarinc.manhunt.queue.QueueManager;
import com.ravingarinc.manhunt.role.LuckPermsHandler;
import com.ravingarinc.manhunt.storage.ConfigManager;
import com.ravingarinc.manhunt.storage.sql.PlayerDatabase;

public final class ManhuntPlugin extends RavinPlugin {
    @Override
    public void loadModules() {

        // add managers
        addModule(ConfigManager.class);
        addModule(LuckPermsHandler.class);
        addModule(PlayerDatabase.class);
        addModule(PlayerManager.class);
        addModule(QueueManager.class);
        addModule(PlayerListener.class);
        //addModule(SQLHandler.class); // comment out if not needed
        // add listeners

    }

    @Override
    public void loadCommands() {
        new ParentCommand(this).register(this);
    }
}
