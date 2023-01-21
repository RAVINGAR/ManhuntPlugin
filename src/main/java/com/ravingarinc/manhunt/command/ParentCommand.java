package com.ravingarinc.manhunt.command;

import com.ravingarinc.manhunt.RavinPlugin;

public class ParentCommand extends BaseCommand {

    public ParentCommand(final RavinPlugin plugin) {
        super("template");

        addOption("reload", 1, (player, args) -> {
            plugin.reload();
            return true;
        });
    }
}
