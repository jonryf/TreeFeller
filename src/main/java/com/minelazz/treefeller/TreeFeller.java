package com.minelazz.treefeller;

import com.minelazz.treefeller.hooks.AACHook;
import com.minelazz.treefeller.hooks.NoCheatPlusHook;
import com.minelazz.treefeller.hooks.SpartanHook;
import com.minelazz.treefeller.hooks.mcmmo.McMMOHook;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class TreeFeller extends JavaPlugin implements Listener {

    public static TreeFeller instance;
    public static PluginSettings settings;
    public static List<UUID> currentFellers = new ArrayList<>();

    private ArrayList<UUID> delayLimitedUsers = new ArrayList<>();
    private HashMap<UUID, Integer> delayOnUsers = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        PluginSettings.load();
        settings.save();

        Bukkit.getPluginManager().registerEvents(this, this);
        if (Bukkit.getPluginManager().isPluginEnabled(("NoCheatPlus")))
            NoCheatPlusHook.addNCPSupport();
        if (Bukkit.getPluginManager().isPluginEnabled("AAC"))
            Bukkit.getPluginManager().registerEvents(new AACHook(), this);
        if (Bukkit.getPluginManager().isPluginEnabled("mcMMO"))
            Bukkit.getPluginManager().registerEvents(new McMMOHook(), this);
        if (Bukkit.getPluginManager().isPluginEnabled("Spartan"))
            Bukkit.getPluginManager().registerEvents(new SpartanHook(), this);


    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreakEvent(BlockBreakEvent event) {
        if (event.isCancelled() || settings.mcMMOTreeFeller || event.getPlayer().getGameMode() != GameMode.SURVIVAL)
            return;
        if (event.getBlock().getType() != Material.LOG && event.getBlock().getType() != Material.LOG_2)
            return;
        if (currentFellers.contains(event.getPlayer().getUniqueId()))
            return;
        if (event.getPlayer().getItemInHand().getType() == null || TreeFeller.settings.cuttingSpeed.get(event.getPlayer().getItemInHand().getType()) == null)
            return;
        if (!event.getPlayer().hasPermission("treefeller.use." + event.getPlayer().getItemInHand().getType().toString().toLowerCase()))
            return;

        if (settings.delay > 0) {
            if (delayLimitedUsers.contains(event.getPlayer().getUniqueId())) {
                event.getPlayer().sendMessage("§cYou can not fell the tree yet, please wait " + (settings.delay + delayOnUsers.get(event.getPlayer().getUniqueId()) - (int) System.currentTimeMillis() / 1000) + " seconds");
                return;
            }
            final UUID id = event.getPlayer().getUniqueId();
            new BukkitRunnable() {

                @Override
                public void run() {
                    delayOnUsers.remove(id);
                    delayLimitedUsers.remove(id);
                }

            }.runTaskLater(this, settings.delay * 20);
            delayLimitedUsers.add(id);
            delayOnUsers.put(id, (int) System.currentTimeMillis() / 1000);
        }
        event.setCancelled(true);
        new TreeCutter(event.getPlayer(), event.getBlock()).runTaskAsynchronously(this);
    }

}
