package com.ravingarinc.manhunt.role;

import com.ravingarinc.manhunt.RavinPlugin;
import com.ravingarinc.manhunt.api.Module;
import com.ravingarinc.manhunt.api.ModuleLoadException;
import com.ravingarinc.manhunt.api.util.I;
import com.ravingarinc.manhunt.gameplay.Hunter;
import com.ravingarinc.manhunt.gameplay.Prey;
import com.ravingarinc.manhunt.gameplay.Trackable;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.MetaNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class LuckPermsHandler extends Module {
    private static final String META_KEY = "last_attempt";
    private final List<String> priorityRoles;
    private String preyRole = "";

    private int maxLives = 5;

    private LuckPerms luckPerms;

    public LuckPermsHandler(final RavinPlugin plugin) {
        super(LuckPermsHandler.class, plugin);
        priorityRoles = new ArrayList<>();
    }

    public void setPreyRole(final String preyRole) {
        this.preyRole = preyRole;
    }

    public void addPriorityRole(final String role) {
        this.priorityRoles.add(role);
    }

    public void setMaxLives(final int lives) {
        this.maxLives = lives;
    }

    public void clearPriorityRoles() {
        this.priorityRoles.clear();
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

    public void savePlayer(final Trackable player) {
        if (!(player instanceof Hunter hunter)) {
            return;
        }
        final User user = luckPerms.getUserManager().getUser(hunter.player().getUniqueId());
        final MetaNode node = MetaNode.builder(META_KEY, String.valueOf(hunter.lastAttempt())).build();
        user.data().clear(NodeType.META.predicate(m -> m.getMetaKey().equals(META_KEY)));
        user.data().add(node);
        luckPerms.getUserManager().saveUser(user);
    }

    public Trackable loadPlayer(final Player player) {
        final CachedMetaData metaData = luckPerms.getPlayerAdapter(Player.class).getMetaData(player);
        Long lastAttempt = metaData.getMetaValue(META_KEY, Long::parseLong).orElse(null);
        if (lastAttempt == null) {
            lastAttempt = 0L;
        }
        final Trackable trackable;
        final String role = queryRole(player);
        if (role == null || !priorityRoles.contains(role)) {
            if (preyRole.equalsIgnoreCase(role)) {
                trackable = new Prey(player, maxLives);
            } else {
                trackable = new Hunter(player, false, lastAttempt);
            }
        } else {
            // priority
            trackable = new Hunter(player, true, lastAttempt);
        }
        return trackable;
    }

    @Nullable
    public String queryRole(final Player player) {
        final ImmutableContextSet contextSet = luckPerms.getContextManager().getContext(player);

        for (final String value : contextSet.getValues("discordsrv")) {
            if (value.startsWith("role=")) {
                final String format = value.substring(5);
                I.log(Level.WARNING, "Found role called " + format);
                return format;
            }
        }
        return null;
    }


    @Override
    public void cancel() {

    }
}
