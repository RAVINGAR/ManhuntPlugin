package com.ravingarinc.manhunt.queue;

import com.ravingarinc.manhunt.RavinPlugin;
import com.ravingarinc.manhunt.api.Module;
import com.ravingarinc.manhunt.api.ModuleLoadException;
import com.ravingarinc.manhunt.gameplay.Hunter;
import com.ravingarinc.manhunt.gameplay.PlayerManager;
import com.ravingarinc.manhunt.gameplay.Trackable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class QueueManager extends Module {
    private final List<String> priorityRoles;
    private LinkedList<Hunter> queue;
    private Set<Hunter> currentHunters;
    private PlayerManager manager;
    private int maxHunters;

    public QueueManager(final RavinPlugin plugin) {
        super(QueueManager.class, plugin, PlayerManager.class);
        maxHunters = 5;
        priorityRoles = new ArrayList<>();
    }

    public void addPriorityRole(final String role) {
        this.priorityRoles.add(role);
    }

    public void setMaxHunters(final int hunters) {
        this.maxHunters = hunters;
    }

    @Override
    protected void load() throws ModuleLoadException {
        manager = plugin.getModule(PlayerManager.class);
        final Set<Hunter> newSet = new HashSet<>(maxHunters);
        if (currentHunters != null) {
            currentHunters.forEach(trackable -> manager.getPlayer(trackable.player()).ifPresent(h -> {
                if (h instanceof Hunter hunter) {
                    newSet.add(hunter);
                }
            }));
            currentHunters.clear();
        }
        currentHunters = newSet;

        final LinkedList<Hunter> newQueue = new LinkedList<>();
        if (queue != null) {
            queue.forEach(trackable -> manager.getPlayer(trackable.player()).ifPresent(h -> {
                if (h instanceof Hunter hunter) {
                    newQueue.addLast(hunter);
                }
            }));
            queue.clear();
        }
        queue = newQueue;
    }

    public void enqueue(final Hunter trackable) {
        final String role = trackable.role();
        if (priorityRoles.contains(role)) {
            for (int i = queue.size() - 1; i >= 0; i--) {
                final Trackable next = queue.get(i);
                if (priorityRoles.contains(next.role())) {
                    queue.add(i - 1, trackable);
                }
            }
        } else {
            queue.addLast(trackable);
        }
    }

    public void addHunter(final Hunter hunter) {

    }

    public void removeHunter(final Hunter hunter) {
        currentHunters.remove(hunter);
    }

    @Override
    public void cancel() {
        priorityRoles.clear();
    }
}
