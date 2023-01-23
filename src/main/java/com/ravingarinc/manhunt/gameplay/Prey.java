package com.ravingarinc.manhunt.gameplay;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public class Prey extends Trackable {
    private int lives;

    public Prey(final Player player, final int lives) {
        super(player);
        this.lives = lives;
    }

    public int getLives() {
        return lives;
    }

    public void removeLife() {
        lives--;
    }

    @Override
    public boolean handleEvent(final Event event) {
        return false;
    }
}
