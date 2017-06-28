package com.minelazz.treefeller.hooks;

import com.minelazz.treefeller.TreeFeller;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;


public class AACHook implements Listener {

    @EventHandler
    public void onPlayerViolation(me.konsolas.aac.api.PlayerViolationEvent e) {
        if (TreeFeller.currentFellers.contains(e.getPlayer().getUniqueId()))
            e.setCancelled(true);
    }
}
