package com.ravingarinc.manhunt.command;

import com.ravingarinc.manhunt.RavinPlugin;
import com.ravingarinc.manhunt.gameplay.Hunter;
import com.ravingarinc.manhunt.gameplay.PlayerManager;
import com.ravingarinc.manhunt.gameplay.Prey;
import com.ravingarinc.manhunt.queue.GameplayManager;
import com.ravingarinc.manhunt.queue.QueueCallback;
import com.ravingarinc.manhunt.queue.QueueManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ManhuntCommand extends BaseCommand {

    public ManhuntCommand(final RavinPlugin plugin) {
        super("manhunt");

        final PlayerManager playerManager = plugin.getModule(PlayerManager.class);
        final GameplayManager manager = plugin.getModule(GameplayManager.class);
        final QueueManager queue = plugin.getModule(QueueManager.class);

        addOption("admin", "manhunt.admin", "", 2, (sender, args) -> false)
                .addOption("reload", "- Reloads the configuration and all modules", 2, (player, args) -> {
                    plugin.reload();
                    player.sendMessage(ChatColor.GRAY + "Plugin has been reloaded..");
                    return true;
                }).getParent()
                .addOption("reset-attempt", ChatColor.RED + "<player> " + ChatColor.GRAY + "- Reset a player's last attempt so they can enjoy the queue again", 3, (sender, args) -> {
                    final Player player = plugin.getServer().getPlayer(args[2]);
                    if (player == null) {
                        sender.sendMessage(ChatColor.RED + "Could not find a valid player called " + args[2]);
                    } else {
                        playerManager.getPlayer(player).ifPresentOrElse(trackable -> {
                            if (trackable instanceof Hunter hunter) {
                                hunter.setLastAttempt(0L);
                                if (!queue.isInQueue(hunter) && !queue.hasCallback(hunter)) {
                                    queue.removeIgnore(hunter);
                                    manager.tryJoin(hunter);
                                }
                            } else {
                                sender.sendMessage(ChatColor.RED + "That player is not a hunter!");
                            }
                        }, () -> sender.sendMessage(ChatColor.RED + "Could not find that player!"));
                    }
                    return true;
                })
                .addOption("spawn-all-prey",
                        "- Teleports all prey to the configured spawn location. " +
                                "This teleports them regardless of where they are on the server.", 2, (sender, args) -> {
                            manager.spawnAllPrey();
                            return true;
                        }).getParent()
                .addOption("spawn-prey", ChatColor.RED + "<prey> " + ChatColor.GRAY + "- Teleport the specified prey to the spawn location and add them to the 'current prey list'", 3, (sender, args) -> {
                    final Player player = plugin.getServer().getPlayer(args[2]);
                    if (player == null) {
                        sender.sendMessage(ChatColor.RED + "Could not find a valid player called " + args[2]);
                    } else {
                        playerManager.getPlayer(player).ifPresentOrElse(trackable -> {
                            if (trackable instanceof Prey prey) {
                                manager.spawnNewPrey(prey);
                            } else {
                                sender.sendMessage(ChatColor.RED + "You cannot teleport a player that is not a prey!");
                            }
                        }, () -> sender.sendMessage(ChatColor.RED + "Could not find a valid player called " + args[2]));
                    }
                    return true;
                }).getParent()
                .addOption("clear-hunters",
                        "- Any current hunters are teleported to the spawn location of the specified spawn world. " +
                                "The inventories of these hunters are also cleared.", 2, (sender, args) -> {
                            manager.clearHunters();
                            return true;
                        }).getParent()
                .addOption("set-lives", ChatColor.RED + "<prey> <lives>" + ChatColor.GRAY + " - Sets the lives of a prey, must be greater than 0.", 4, (sender, args) -> {
                    final Player player = plugin.getServer().getPlayer(args[2]);
                    if (player == null) {
                        sender.sendMessage(ChatColor.RED + "Could not find a valid player called " + args[2]);
                    } else {
                        playerManager.getPlayer(player).ifPresentOrElse(trackable -> {
                            final int i;
                            try {
                                i = Integer.parseInt(args[3]);
                                if (i < 1) {
                                    throw new NumberFormatException("Cannot be less than 1");
                                }
                            } catch (final NumberFormatException e) {
                                sender.sendMessage(ChatColor.RED + args[3] + " is not a valid number!");
                                return;
                            }
                            if (trackable instanceof Prey prey) {
                                prey.setLives(i);
                            } else {
                                sender.sendMessage(ChatColor.RED + "You cannot set the lives of a player that is not a prey!");
                            }
                        }, () -> sender.sendMessage(ChatColor.RED + "Could not find a valid player called " + args[2]));
                    }
                    return true;
                }).buildTabCompletions((sender, args) -> {
                    final List<String> list = new ArrayList<>();
                    if (args.length == 2) {
                        playerManager.getPlayers().stream().filter(trackable -> trackable instanceof Prey).forEach(t -> list.add(t.player().getName()));
                    } else if (args.length == 3) {
                        for (int i = 1; i < 4; i++) {
                            list.add("" + i);
                        }
                    }
                    return list;
                })
                .getParent()
                .addHelpOption(ChatColor.DARK_RED, ChatColor.RED);

        addOption("list", "manhunt.list", "- List a specific group of players", 2, (sender, args) -> false)
                .addOption("queue", "Lists all players in the queue.", 2, (sender, args) -> {
                    sender.sendMessage(ChatColor.GRAY + "------- " + ChatColor.DARK_RED + "All in Queue" + ChatColor.GRAY + " -------");
                    final StringBuilder builder = new StringBuilder();
                    final Iterator<String> iterator = queue.getNamesInQueue().iterator();
                    if (iterator.hasNext()) {
                        builder.append(ChatColor.RED).append("Current Players | ").append(ChatColor.GRAY);
                        while (iterator.hasNext()) {
                            builder.append(iterator.next());
                            if (iterator.hasNext()) {
                                builder.append(", ");
                            }
                        }
                    } else {
                        builder.append(ChatColor.RED).append("There are currently no players in the queue");
                    }
                    sender.sendMessage(builder.toString());
                    return true;
                }).getParent()
                .addOption("hunters", "- Lists all currently active hunters.", 2, (sender, args) -> {
                    sender.sendMessage(ChatColor.GRAY + "------- " + ChatColor.DARK_RED + "All Hunters" + ChatColor.GRAY + " -------");
                    final StringBuilder builder = new StringBuilder();
                    final Iterator<String> iterator = manager.getCurrentHunters().iterator();
                    if (iterator.hasNext()) {
                        builder.append(ChatColor.RED).append("Active Hunters | ").append(ChatColor.GRAY);
                        while (iterator.hasNext()) {
                            builder.append(iterator.next());
                            if (iterator.hasNext()) {
                                builder.append(", ");
                            }
                        }
                    } else {
                        builder.append(ChatColor.RED).append("There are currently no current hunters!");
                    }
                    sender.sendMessage(builder.toString());
                    return true;
                }).getParent()
                .addOption("prey", "- Lists all current prey", 2, (sender, args) -> {
                    sender.sendMessage(ChatColor.GRAY + "------- " + ChatColor.DARK_RED + "All Prey" + ChatColor.GRAY + " -------");
                    final StringBuilder builder = new StringBuilder();
                    final Iterator<String> iterator = manager.getCurrentPrey().iterator();
                    if (iterator.hasNext()) {
                        builder.append(ChatColor.RED).append("Active Prey | ").append(ChatColor.GRAY);
                        while (iterator.hasNext()) {
                            builder.append(iterator.next());
                            if (iterator.hasNext()) {
                                builder.append(", ");
                            }
                        }
                    } else {
                        builder.append(ChatColor.RED).append("There are currently no current prey!");
                    }
                    sender.sendMessage(builder.toString());
                    return true;
                }).getParent()
                .addHelpOption(ChatColor.DARK_RED, ChatColor.RED);

        addOption("queue", "- Commands relating to the queue.", 2, (sender, args) -> false)
                .addOption("start", "manhunt.queue.start",
                        "Starts/resumes the queue, if there are any hunter slots available then they will be filled",
                        2, (sender, args) -> {
                            manager.startQueue();
                            return true;
                        }).getParent()
                .addOption("stop", "manhunt.queue.stop",
                        "- Stops the queue, meaning if any hunters die, they will not be replaced",
                        2, (sender, args) -> {
                            manager.stopQueue();
                            return true;
                        }).getParent()
                .addOption("join", "- Join the queue if you are not already in the queue.", 1, (sender, args) -> {
                    if (sender instanceof Player player) {
                        playerManager.getPlayer(player).ifPresent(t -> {
                            if (t instanceof Hunter hunter) {
                                if (queue.isInQueue(hunter)) {
                                    sender.sendMessage(ChatColor.GRAY + "You are already in the queue!");
                                } else {
                                    if (!queue.hasCallback(hunter)) {

                                        queue.removeIgnore(hunter);
                                        manager.tryJoin(hunter);
                                    } else {
                                        sender.sendMessage(ChatColor.GRAY + "You cannot join the queue as you have a pending hunter invitation!");
                                    }
                                }
                            } else {
                                player.sendMessage(ChatColor.GRAY + "Only hunters can use this command!");
                            }
                        });
                    } else {
                        sender.sendMessage(ChatColor.RED + "This command can only be used by a player!");
                    }
                    return true;
                }).getParent()
                .addOption("leave", null, "- Leave the queue and therefore not be considered if a new position for a hunter arises.", 1, (sender, args) -> {
                    if (sender instanceof Player player) {
                        plugin.getModule(PlayerManager.class).getPlayer(player).ifPresent(t -> {
                            if (t instanceof Hunter hunter) {
                                if (queue.isInQueue(hunter)) {
                                    queue.addIgnore(hunter); //task has to be null if hunter is in queue.
                                    queue.remove(hunter);
                                    sender.sendMessage(ChatColor.GRAY + "You have left the queue!");
                                } else {
                                    final QueueCallback task = queue.removeCallback(hunter);
                                    if (task == null) {
                                        sender.sendMessage(ChatColor.GRAY + "You are already not in the queue!");
                                    } else {
                                        queue.addIgnore(hunter);
                                        task.decline();
                                        sender.sendMessage(ChatColor.GRAY + "You have left the queue!");
                                    }
                                }
                            } else {
                                player.sendMessage(ChatColor.GRAY + "Only hunters can use this command!");
                            }
                        });
                    } else {
                        sender.sendMessage(ChatColor.RED + "This command can only be used by a player!");
                    }
                    return true;
                }).getParent()
                .addHelpOption(ChatColor.DARK_RED, ChatColor.RED);

        addOption("accept", "Accept the position as a hunter if one is available.", 1, (sender, args) -> {
            if (sender instanceof Player player) {
                playerManager.getPlayer(player).ifPresent(trackable -> {
                    if (trackable instanceof Hunter hunter) {
                        manager.acceptCallback(hunter);
                    }
                });
            }
            return true;
        });

        addOption("decline", "Decline the position as a hunter if one is available.", 1, (sender, args) -> {
            if (sender instanceof Player player) {
                playerManager.getPlayer(player).ifPresent(trackable -> {
                    if (trackable instanceof Hunter hunter) {
                        manager.declineCallback(hunter);
                    }
                });
            }
            return true;
        });

        addHelpOption(ChatColor.DARK_RED, ChatColor.RED);
    }
}
