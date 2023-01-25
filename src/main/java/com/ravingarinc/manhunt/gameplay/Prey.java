package com.ravingarinc.manhunt.gameplay;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public class Prey extends Trackable {
    private int lives;

    public Prey(final Player player, final int lives) {
        super(player);
        this.lives = lives;
    }

    public synchronized int getLives() {
        return lives;
    }

    public synchronized void setLives(final int lives) {
        this.lives = lives;
    }

    public synchronized void removeLife() {
        lives--;
    }

    @Override
    public boolean handleEvent(final Event event) {
        return false;
    }
}
