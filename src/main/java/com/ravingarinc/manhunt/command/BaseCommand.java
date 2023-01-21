package com.ravingarinc.manhunt.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class BaseCommand extends CommandOption implements CommandExecutor {
    private final String identifier;

    public BaseCommand(final String identifier) {
        super(null, 1, (p, s) -> false);
        this.identifier = identifier;
    }

    public void register(final JavaPlugin plugin) {
        final PluginCommand command = plugin.getCommand(identifier);
        Objects.requireNonNull(command, "Command /" + identifier + " was not registered correctly!");
        command.setExecutor(this);
        command.setTabCompleter(new CommandCompleter(this));
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
        return execute(sender, args, 0);
    }

    public static class CommandCompleter implements TabCompleter {
        private final BaseCommand command;

        public CommandCompleter(final BaseCommand command) {
            this.command = command;
        }

        @Override
        public List<String> onTabComplete(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
            return this.command.getTabCompletion(sender, args, 0);
        }
    }
}
