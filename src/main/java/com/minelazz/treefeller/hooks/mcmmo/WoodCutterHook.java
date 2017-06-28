package com.minelazz.treefeller.hooks.mcmmo;

import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.skills.woodcutting.Woodcutting;
import com.gmail.nossr50.skills.woodcutting.WoodcuttingManager;
import com.gmail.nossr50.util.Misc;
import com.gmail.nossr50.util.skills.CombatUtils;
import com.minelazz.treefeller.TreeCutter;
import com.minelazz.treefeller.TreeFeller;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class WoodCutterHook extends WoodcuttingManager {

    public WoodCutterHook(McMMOPlayer mcMMOPlayer) {
        super(mcMMOPlayer);
    }

    @Override
    public void processTreeFeller(final BlockState blockState) {
        new BukkitRunnable() {

            @Override
            public void run() {
                Block block = blockState.getBlock();
                for (BlockFace face : BlockFace.values()) {
                    Material type = blockState.getBlock().getRelative(face).getType();
                    if (type == Material.LOG || type == Material.LOG_2) {
                        block = blockState.getBlock().getRelative(face);
                        break;
                    }
                }
                //Check if tree is vanilla
                if (block.getData() < 11 || (block.getType() != Material.LOG && block.getData() != 2)) {
                    new BukkitRunnable() {

                        @Override
                        public void run() {
                            WoodCutterHook.super.processTreeFeller(blockState);
                        }

                    }.runTask(TreeFeller.instance);
                    return;
                }

                TreeCutter cutter = new TreeCutter(mcMMOPlayer.getPlayer(), block);
                Set<BlockState> blockStates = new HashSet<>();
                cutter.runLoop(block, block.getX(), block.getZ());
                blockStates.addAll(cutter.getBlocks().stream().map(Block::getState).collect(Collectors.toList()));
                Player player = mcMMOPlayer.getPlayer();

                try {
                    Method method = Woodcutting.class.getDeclaredMethod("handleDurabilityLoss", Set.class, ItemStack.class);
                    method.setAccessible(true);
                    if (!(Boolean) method.invoke(null, blockStates, player.getInventory().getItemInHand())) {
                        player.sendMessage(LocaleLoader.getString("Woodcutting.Skills.TreeFeller.Splinter"));
                        double health = player.getHealth();
                        if (health > 1) {
                            //Damage player sync
                            new BukkitRunnable() {

                                @Override
                                public void run() {
                                    CombatUtils.dealDamage(mcMMOPlayer.getPlayer(), Misc.getRandom().nextInt((int) (health - 1)));
                                }

                            }.runTask(TreeFeller.instance);
                        }
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                final Set<BlockState> blockStatesFinal = blockStates;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            Method method = WoodcuttingManager.class.getDeclaredMethod("dropBlocks", Set.class);
                            method.setAccessible(true);
                            method.invoke(WoodCutterHook.this, blockStatesFinal);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.runTask(TreeFeller.instance);
            }
        }.runTaskAsynchronously(TreeFeller.instance);


    }
}
