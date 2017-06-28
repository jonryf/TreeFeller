package com.minelazz.treefeller.hooks;

import com.minelazz.treefeller.TreeFeller;
import me.vagdedes.spartan.api.PlayerViolationEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SpartanHook implements Listener {

    @EventHandler
    public void onPlayerViolation(PlayerViolationEvent e) {
        if (TreeFeller.currentFellers.contains(e.getPlayer().getUniqueId()))
            e.setCancelled(true);
    }
}
