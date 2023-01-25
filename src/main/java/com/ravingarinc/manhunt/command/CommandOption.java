package com.ravingarinc.manhunt.command;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class CommandOption {
    protected final String identifier;
    private final Map<String, CommandOption> options;
    private final int requiredArgs;
    private final CommandOption parent;
    private final @Nullable String permission;
    private final String description;
    private BiFunction<CommandSender, String[], Boolean> function;
    private BiFunction<CommandSender, String[], List<String>> tabCompletions;

    /**
     * Adds an option for this command. An option can have sub-options since this method returns the command option
     * created.
     *
     * @param identifier   The key of the option
     * @param parent       The parent of this. Can be null.
     * @param permission   The required permission to use and show this command
     * @param description  The description/usage of this command.
     * @param requiredArgs When {@link #execute(CommandSender, String[], int)} is called, if the number of string arguments
     *                     is greater than or equal to the {@link #requiredArgs} of this option, only then will it search
     *                     for sub-options OR execute its function.
     * @param function     The function to execute. This should in most cases return true. If it returns false, this is
     *                     interpreted that the arguments provided was incorrect and so this option's description will be sent.
     * @return The new command option that was just created.
     */
    public CommandOption(final String identifier, final CommandOption parent, final @Nullable String permission, final @NotNull String description, final int requiredArgs, final BiFunction<CommandSender, String[], Boolean> function) {
        this.identifier = identifier;
        this.parent = parent;
        this.function = function;
        this.options = new LinkedHashMap<>();
        this.tabCompletions = null;
        this.requiredArgs = requiredArgs;
        this.permission = permission;
        this.description = description;
    }

    public void register() {
        parent.addOption(identifier, this);
    }

    public void setFunction(final BiFunction<CommandSender, String[], Boolean> function) {
        this.function = function;
    }

    public CommandOption addOption(final String key, final int requiredArgs, final BiFunction<CommandSender, String[], Boolean> function) {
        return addOption(key, null, "", requiredArgs, function);
    }

    public CommandOption addOption(final String key, final String description, final int requiredArgs, final BiFunction<CommandSender, String[], Boolean> function) {
        return addOption(key, null, description, requiredArgs, function);
    }

    public CommandOption addOption(final String key, final @Nullable String permission, final String description, final int requiredArgs, final BiFunction<CommandSender, String[], Boolean> function) {
        final CommandOption option = new CommandOption(key, this, permission, description, requiredArgs, function);
        this.options.put(key, option);
        return option;
    }

    public CommandOption addOption(final String key, final CommandOption option) {
        this.options.put(key, option);
        return option;
    }

    public void addHelpOption(final ChatColor primary, final ChatColor secondary) {
        final CommandOption option = new CommandOption("?", this, null,
                "Shows a list of sub-commands for this command.", requiredArgs,
                (sender, args) -> {
                    getSubOptionHelp(sender, primary, secondary).forEach(sender::sendMessage);
                    return true;
                });
        this.options.put("?", option);
    }

    public CommandOption getParent() {
        return parent;
    }

    public String getHelp(final ChatColor prefix) {
        return prefix + "/" + getIdentifiers() + ChatColor.GRAY + " " + description;
    }

    /**
     * Formats the command by getting parent identifiers forming the full command in a string with spaces. The string
     * will always end with a trailing space.
     *
     * @return The command.
     */
    public String getIdentifiers() {
        final LinkedList<String> paths = new LinkedList<>();
        paths.add(identifier);
        CommandOption parent = this.parent;
        while (parent != null) {
            paths.addFirst(parent.identifier);
            parent = parent.getParent();
        }
        final StringBuilder builder = new StringBuilder();
        paths.forEach(p -> builder.append(p).append(" "));
        return builder.toString();
    }

    public String getUsage() {
        final StringBuilder command = new StringBuilder(getIdentifiers());
        if (!options.isEmpty()) {
            command.append("<");
            final Iterator<String> iterator = options.keySet().iterator();
            while (iterator.hasNext()) {
                command.append(iterator.next());
                command.append(iterator.hasNext() ? "|" : ">");
            }
        }
        return command.toString();
    }

    public List<String> getSubOptionHelp(final CommandSender sender, final ChatColor primary, final ChatColor secondary) {
        final List<String> list = new LinkedList<>();
        options.values().forEach(option -> {
            if (!option.identifier.equals("?") && option.hasPermission(sender)) {
                list.add(option.getHelp(secondary));
            }
        });
        if (!options.isEmpty()) {
            final String formatted = Character.toUpperCase(identifier.charAt(0)) + identifier.substring(1);
            list.add(0, ChatColor.GRAY + "------- " + primary + formatted + ChatColor.GRAY + "-------");
        }
        return list;
    }

    /**
     * If options is empty then provide these tab completions
     *
     * @param tabCompletions The function to use for tab completions
     */
    public CommandOption buildTabCompletions(final BiFunction<CommandSender, String[], List<String>> tabCompletions) {
        this.tabCompletions = tabCompletions;
        return this;
    }

    public boolean hasPermission(final CommandSender sender) {
        if (permission == null) {
            return true;
        }
        return sender.hasPermission(permission);
    }


    /**
     * Executes a command option. If this command option has children it will search through for a specified key
     * and if one is found it will search through that option. The function will be accepted if no option is available
     *
     * @param sender The sender using the command
     * @param args   The args
     * @return true if command was run successfully, or false if not
     */
    public boolean execute(@NotNull final CommandSender sender, final String[] args, final int index) {
        if (args.length >= requiredArgs) {
            final CommandOption option = args.length == index ? null : options.get(args[index].toLowerCase());
            if (option == null) {
                if (!function.apply(sender, args)) {
                    sender.sendMessage(ChatColor.GRAY + "Unknown sub-command | Use /" + getIdentifiers() + "? to show all available commands.");
                }
                return true;
            } else {
                if (option.hasPermission(sender)) {
                    return option.execute(sender, args, index + 1);
                }
                sender.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
                return true;
            }
        }
        sender.sendMessage(ChatColor.GRAY + "Invalid arguments! Usage | /" + getUsage());
        return true;
    }

    @Nullable
    public List<String> getTabCompletion(@NotNull final CommandSender sender, @NotNull final String[] args, final int index) {
        if (tabCompletions == null) {
            if (args.length == index + 1) {
                return options.isEmpty()
                        ? null
                        : options.entrySet().stream()
                        .filter((entry) -> entry.getValue().hasPermission(sender))
                        .map(Map.Entry::getKey).collect(Collectors.toList());
            } else {
                final CommandOption option = options.get(args[index]);
                if (option != null) {
                    return option.getTabCompletion(sender, args, index + 1);
                }
            }
        } else {
            return tabCompletions.apply(sender, args);
        }

        return null;
    }
}