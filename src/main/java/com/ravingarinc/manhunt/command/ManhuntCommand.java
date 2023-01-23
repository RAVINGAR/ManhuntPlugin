package com.ravingarinc.manhunt.command;

import com.ravingarinc.manhunt.RavinPlugin;
import com.ravingarinc.manhunt.gameplay.Hunter;
import com.ravingarinc.manhunt.gameplay.PlayerManager;
import com.ravingarinc.manhunt.queue.GameplayManager;
import com.ravingarinc.manhunt.queue.QueueCallback;
import com.ravingarinc.manhunt.queue.QueueManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ManhuntCommand extends BaseCommand {

    public ManhuntCommand(final RavinPlugin plugin) {
        super("manhunt");

        final PlayerManager playerManager = plugin.getModule(PlayerManager.class);
        final GameplayManager manager = plugin.getModule(GameplayManager.class);
        final QueueManager queue = plugin.getModule(QueueManager.class);

        addOption("admin", "manhunt.admin", "", 2, (sender, args) -> false)
                .addOption("reload", "Reloads the configuration and all modules", 2, (player, args) -> {
                    plugin.reload();
                    player.sendMessage(ChatColor.GRAY + "Plugin has been reloaded..");
                    return true;
                }).getParent()
                .addOption("start-queue",
                        "Starts/resumes the queue, if there are any hunter slots " +
                                "available then they will be filled", 2, (sender, args) -> {
                            manager.startQueue();
                            return true;
                        }).getParent()
                .addOption("stop-queue", "Stops the queue, meaning if any hunters die, they will not be replaced", 2, (sender, args) -> {
                    manager.stopQueue();
                    return true;
                }).getParent()
                .addOption("tp-prey",
                        "Teleports all prey to the configured spawn location. " +
                                "This teleports them regardless of where they are on the server.", 2, (sender, args) -> {
                            manager.teleportPrey();
                            return true;
                        }).getParent()
                .addOption("clear-hunters",
                        "Any current hunters are teleported to the spawn location of the specified spawn world. " +
                                "The inventories of these hunters are also cleared.", 2, (sender, args) -> {
                            manager.clearHunters();
                            return true;
                        });

        addOption("join-queue", null,
                "Join the queue if you are not already in the queue.", 1, (sender, args) -> {
                    if (sender instanceof Player player) {
                        playerManager.getPlayer(player).ifPresent(t -> {
                            if (t instanceof Hunter hunter) {
                                if (queue.isInQueue(hunter)) {
                                    sender.sendMessage(ChatColor.GRAY + "You are already in the queue!");
                                } else {
                                    final QueueCallback task = queue.removeCallback(hunter);
                                    if (task == null) {
                                        sender.sendMessage(ChatColor.GRAY + "You have re-joined the queue!");
                                        queue.enqueue(hunter);
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
                });

        addOption("leave-queue", null,
                "Leave the queue and therefore not be considered if a new position for a hunter arises.", 1, (sender, args) -> {
                    if (sender instanceof Player player) {
                        plugin.getModule(PlayerManager.class).getPlayer(player).ifPresent(t -> {
                            if (t instanceof Hunter hunter) {
                                if (queue.isInQueue(hunter)) {
                                    queue.remove(hunter); //task has to be null if hunter is in queue.
                                    sender.sendMessage(ChatColor.GRAY + "You have left the queue!");
                                } else {
                                    final QueueCallback task = queue.removeCallback(hunter);
                                    if (task == null) {
                                        sender.sendMessage(ChatColor.GRAY + "You are already not in the queue!");
                                    } else {
                                        task.forceDecline();
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
                });

        addOption("accept", 1, (sender, args) -> {
            if (sender instanceof Player player) {
                playerManager.getPlayer(player).ifPresent(trackable -> {
                    if (trackable instanceof Hunter hunter) {
                        queue.acceptCallback(hunter);
                    }
                });
            }
            return true;
        });

        addOption("decline", 1, (sender, args) -> {
            if (sender instanceof Player player) {
                playerManager.getPlayer(player).ifPresent(trackable -> {
                    if (trackable instanceof Hunter hunter) {
                        queue.declineCallback(hunter);
                    }
                });
            }
            return true;
        });
    }

    private void sendHelp(final CommandSender sender) {
        sender.sendMessage(ChatColor.GRAY + "------" + ChatColor.DARK_RED + " Manhunt Command Help " + ChatColor.GRAY + "------");
        sender.sendMessage(ChatColor.RED + "/mh reload" + ChatColor.GRAY + " - Reloads the plugin with a new configuration. This does not clear the current queue nor restart any ongoing 'game'.");
        sender.sendMessage(ChatColor.RED + "/mh tp-prey" + ChatColor.GRAY + " - Teleports all prey to the spawn location, even if they are already in the world. This is generally used initially before a world 'starts'");
        sender.sendMessage(ChatColor.RED + "/mh force-tp-hunter" + ChatColor.GRAY + " - Teleports a hunter to a valid spawn location. This command will only work is the hunter is currently queued and if the max hunters is not reached.");
        sender.sendMessage(ChatColor.RED + "/mh start-queue" + ChatColor.GRAY + " - Starts/resumes the queue, if there are any hunter slots available then they will be filled.");
        sender.sendMessage(ChatColor.RED + "/mh stop-queue" + ChatColor.GRAY + " - Stops the queue, meaning if any hunters die, they will not be replaced");
        sender.sendMessage(ChatColor.RED + "/mh clear-hunters" + ChatColor.GRAY + " - All current hunters will be teleported to spawn and have their items cleared.");
        sender.sendMessage(ChatColor.RED + "/mh auto-decline" + ChatColor.GRAY + " - Use this command to toggle auto-decline if you reach the start of the queue. This is useful for spectators.");
    }
}
