package com.ravingarinc.manhunt.role;

import com.ravingarinc.manhunt.RavinPlugin;
import com.ravingarinc.manhunt.api.Module;
import com.ravingarinc.manhunt.api.ModuleLoadException;
import com.ravingarinc.manhunt.api.async.AsyncHandler;
import com.ravingarinc.manhunt.api.util.I;
import com.ravingarinc.manhunt.gameplay.Hunter;
import com.ravingarinc.manhunt.gameplay.PlayerManager;
import com.ravingarinc.manhunt.gameplay.Prey;
import com.ravingarinc.manhunt.gameplay.Trackable;
import com.ravingarinc.manhunt.queue.GameplayManager;
import com.ravingarinc.manhunt.queue.QueueManager;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.user.track.UserTrackEvent;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.MetaNode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

public class LuckPermsHandler extends Module {
    private static final String ATTEMPT_META = "last_attempt";
    private static final String NO_PRIORITY_META = "no_priority";
    private static final String LIVES_META = "lives_left";
    private final List<String> priorityRoles;
    private final LuckPerms luckPerms;
    private String preyRole = "";
    private PlayerManager manager = null;

    private QueueManager queue = null;

    private GameplayManager gameplayManager = null;

    public LuckPermsHandler(final RavinPlugin plugin) {
        super(LuckPermsHandler.class, plugin);
        priorityRoles = new ArrayList<>();

        final RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider == null) {
            luckPerms = null;
        } else {
            luckPerms = provider.getProvider();

            final EventBus eventBus = luckPerms.getEventBus();

            eventBus.subscribe(plugin, UserTrackEvent.class, this::onUserRoleChange);
        }
    }

    private void onUserRoleChange(final UserTrackEvent event) {
        final String groupFrom = event.getGroupFrom().orElse(null);
        final String groupTo = event.getGroupTo().orElse(null);

        if (Objects.equals(groupFrom, groupTo)) {
            return;
        }
        final Player player = Bukkit.getPlayer(event.getUser().getUniqueId());
        if (player == null) {
            return;
        }
        final Optional<Trackable> opt = manager.getPlayer(player);
        if (opt.isEmpty()) {
            return;
        }
        if (gameplayManager.isInGame(opt.get())) {
            return;
        }

        if (groupTo == null) {
            if (preyRole.equals(groupFrom)) {
                // only one prey role so must be to no group basically
                manager.unloadPlayer(player);
                AsyncHandler.runSyncTaskLater(() -> manager.loadPlayer(player), 5L);
            } else if (priorityRoles.contains(groupFrom)) {
                manager.getPlayer(player).ifPresent(trackable -> {
                    if (trackable instanceof Hunter hunter) {
                        queue.remove(hunter);
                        hunter.setPriority(false);
                        hunter.player().sendMessage(ChatColor.RED + "You no longer have priority as a hunter!");
                        if (!queue.hasCallback(hunter)) {
                            queue.enqueue(hunter);
                        }
                    }
                });
            }
        } else {
            if (preyRole.equals(groupTo)) {
                manager.unloadPlayer(player);
                AsyncHandler.runSyncTaskLater(() -> manager.loadPlayer(player), 5L);
            } else if (priorityRoles.contains(groupTo)) {
                if (preyRole.equals(groupFrom)) {
                    manager.unloadPlayer(player);
                    AsyncHandler.runSyncTaskLater(() -> manager.loadPlayer(player), 5L);
                } else {
                    manager.getPlayer(player).ifPresent(trackable -> {
                        if (trackable instanceof Hunter hunter) {
                            if (!hunter.hasPriority()) {
                                hunter.setPriority(true);
                                if (!queue.isHunterAhead(hunter)) {
                                    queue.remove(hunter);
                                    queue.enqueue(hunter);
                                }
                                hunter.player().sendMessage(ChatColor.GREEN + "You now have priority as a hunter!");
                            }
                        }
                    });
                }
            }
        }
    }

    public void setPreyRole(final String preyRole) {
        this.preyRole = preyRole;
    }

    public void addPriorityRole(final String role) {
        this.priorityRoles.add(role);
    }


    public void clearPriorityRoles() {
        this.priorityRoles.clear();
    }

    @Override
    protected void load() throws ModuleLoadException {
        if (luckPerms == null) {
            throw new ModuleLoadException(this, "Could not load " + this.getName() + " as LuckPerms was not loaded!");
        }
        manager = plugin.getModule(PlayerManager.class);
        queue = plugin.getModule(QueueManager.class);
        gameplayManager = plugin.getModule(GameplayManager.class);
    }

    public void savePlayer(final Trackable player) {
        final User user = luckPerms.getUserManager().getUser(player.player().getUniqueId());
        if (user == null) {
            I.log(Level.WARNING, "Could not save player before they were unloaded by LuckPerms!");
            return;
        }
        if (player instanceof Hunter hunter) {
            final MetaNode node = MetaNode.builder(ATTEMPT_META, String.valueOf(hunter.lastAttempt())).build();
            user.data().clear(NodeType.META.predicate(m -> m.getMetaKey().equals(ATTEMPT_META)));
            user.data().add(node);

            user.data().clear(NodeType.META.predicate(m -> m.getMetaKey().equals(NO_PRIORITY_META)));
            if (!hunter.hasPriority()) {
                final MetaNode priorityNode = MetaNode.builder(NO_PRIORITY_META, "true").build();
                user.data().add(priorityNode);
            }
        } else if (player instanceof Prey prey) {
            final MetaNode node = MetaNode.builder(LIVES_META, String.valueOf(prey.getLives())).build();
            user.data().clear(NodeType.META.predicate(m -> m.getMetaKey().equals(LIVES_META)));
            user.data().add(node);
        }
        luckPerms.getUserManager().saveUser(user);
    }


    /**
     * Loads a player's metadata from LuckPerms. Will return an empty optional if an error occurred or if the player is offline.
     */
    public Optional<Trackable> loadPlayer(final Player player) {
        if (!player.isOnline()) {
            return Optional.empty();
        }
        final User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            I.log(Level.WARNING, "LuckPerms could not load user " + player.getName() + " due to an unknown reason! Using default hunter role...");
            return Optional.of(new Hunter(player, false, 0));
        }

        final Trackable trackable;
        final CachedMetaData metaData = user.getCachedData().getMetaData();

        if (isPreyUser(user)) {
            trackable = new Prey(player, metaData.getMetaValue(LIVES_META, Integer::parseInt).orElse(-1));
        } else {
            long lastAttempt = metaData.getMetaValue(ATTEMPT_META, Long::parseLong).orElse(0L);
            final boolean isPriorityNow = isPriorityUser(user);
            if (metaData.getMetaValue(NO_PRIORITY_META, Boolean::parseBoolean).orElse(false) && isPriorityNow) {
                // if user previously had no priority, but is priority now -> give them a second chance by setting their last attempt to 0
                lastAttempt = 0L;
            }
            trackable = new Hunter(player, isPriorityNow, lastAttempt);
        }
        return Optional.of(trackable);
    }

    public boolean isPreyUser(final User user) {
        return user.getInheritedGroups(user.getQueryOptions()).stream().anyMatch(g -> preyRole.equals(g.getName()));
    }

    public boolean isPriorityUser(final User user) {
        return user.getInheritedGroups(user.getQueryOptions()).stream().anyMatch(g -> priorityRoles.contains(g.getName()));
    }

    @Override
    public void cancel() {

    }
}
