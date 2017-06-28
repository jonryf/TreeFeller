package com.minelazz.treefeller;

import com.boydti.fawe.util.EditSessionBuilder;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.blocks.BaseBlock;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class TreeCutter extends BukkitRunnable {

    private Player player;
    private Block startBlock;
    private List<String> comparisonBlockArray = new ArrayList<>();
    private List<String> comparisonBlockArrayLeaves = new ArrayList<>();
    private List<Block> blocks = new ArrayList<>();
    private int indexed = 0;
    private boolean loop = false;


    public TreeCutter(Player cutter, Block startBlock) {
        this.player = cutter;
        this.startBlock = startBlock;
    }

    public void runLoop(Block b1, final int x1, final int z1) {
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0)
                        continue;
                    Block b2 = b1.getRelative(x, y, z);
                    String s = b2.getX() + ":" + b2.getY() + ":" + b2.getZ();

                    if ((b2.getType() == Material.LEAVES || b2.getType() == Material.LEAVES_2) && !comparisonBlockArrayLeaves.contains(s))
                        comparisonBlockArrayLeaves.add(s);
                    if (b2.getType() != Material.LOG && b2.getType() != Material.LOG_2)
                        continue;
                    int searchSquareSize = 25;
                    if (b2.getX() > x1 + searchSquareSize || b2.getX() < x1 - searchSquareSize || b2.getZ() > z1 + searchSquareSize || b2.getZ() < z1 - searchSquareSize)
                        break;
                    if (!comparisonBlockArray.contains(s)) {
                        comparisonBlockArray.add(s);
                        blocks.add(b2);
                        this.runLoop(b2, x1, z1);
                    }
                }
            }
        }
    }

    @Override
    public void run() {
        blocks.add(startBlock);
        runLoop(startBlock, startBlock.getX(), startBlock.getZ());
        if (isTree()) {
            cutDownTree();
            stop();
        } else {
            new BukkitRunnable() {

                @Override
                public void run() {
                    Location center = startBlock.getLocation().add(0.5, 0.5, 0.5);
                    for (ItemStack stack : startBlock.getDrops())
                        startBlock.getWorld().dropItem(center, stack);
                    startBlock.getWorld().playEffect(center, Effect.STEP_SOUND, startBlock.getType());
                    startBlock.setType(Material.AIR);
                }
            }.runTask(TreeFeller.instance);
        }
    }

    private boolean isTree() {
        return (comparisonBlockArrayLeaves.size() * 1D) / (blocks.size() * 1D) > 0.3;
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    private void stop() {
        if (TreeFeller.currentFellers.contains(player.getUniqueId()))
            TreeFeller.currentFellers.remove(player.getUniqueId());
    }

    public void cutDownTree() {
        if ((player.getItemInHand() == null || player.getItemInHand().getType() == Material.AIR) && !updateItemInHand()) {
            stop();
            return;
        }

        if (!TreeFeller.currentFellers.contains(player.getUniqueId()))
            TreeFeller.currentFellers.add(player.getUniqueId());

        blocks = blocks.stream().sorted((b, b2) -> b2.getY() - b.getY()).collect(Collectors.toList());
        long speed = TreeFeller.settings.cuttingSpeed.get(player.getItemInHand().getType());

        new BukkitRunnable() {
            int blocksCut = 0;
            EditSession editSession = new EditSessionBuilder(blocks.get(0).getWorld().getName()).fastmode(true).build();

            @Override
            public void run() {
                //Instant cut down tree
                if (TreeFeller.settings.instantTreeCut && !loop) {
                    for (int i = 0; i < blocks.size(); i++) {
                        loop = true;
                        run();
                    }
                    this.cancel();
                    return;
                }

                //In case player disconnect
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }

                ItemStack item = player.getItemInHand();
                Integer speed = TreeFeller.settings.cuttingSpeed.get(item.getType());
                if (speed == null) {
                    if (updateItemInHand())
                        cutDownTree();
                    this.cancel();
                    return;
                }

                if (blocks.size() < indexed - 2) {
                    this.cancel();
                    return;
                }

                Block block = blocks.get(indexed++);

                //Fire events
                PlayerAnimationEvent animationEvent = new PlayerAnimationEvent(player);
                Bukkit.getPluginManager().callEvent(animationEvent);
                BlockBreakEvent event = new BlockBreakEvent(block, player);
                Bukkit.getPluginManager().callEvent(event);

                if (!event.isCancelled()) {
                    Location center = block.getLocation().add(0.5, 0.5, 0.5);
                    if (TreeFeller.settings.particles)
                        startBlock.getWorld().playEffect(center, Effect.STEP_SOUND, block.getType());

                    for (ItemStack drop : block.getDrops()) {
                        if (TreeFeller.settings.instantToInventory)
                            player.getInventory().addItem(drop);
                        else
                            startBlock.getWorld().dropItem(center, drop);
                    }

                    item.setDurability((short) (item.getDurability() + 1));
                    if (item.getType().getMaxDurability() == item.getDurability())
                        player.setItemInHand(null);

                    blocksCut++;
                    if (TreeFeller.settings.plantSapling &&
                            block.getX() == startBlock.getX() && block.getZ() == startBlock.getZ() && block.getY() <= startBlock.getY()) {
                        Block b = block.getRelative(BlockFace.DOWN);
                        if ((b.getType() == Material.DIRT || b.getType() == Material.GRASS) && blocks.size() > 5) {
                            block.setType(Material.SAPLING);
                            block.setData((byte) getSaplingType(block));
                        } else
                            editSession.setBlock(block.getX(), block.getY(), block.getZ(), new BaseBlock(0, 0));
                    } else
                        editSession.setBlock(block.getX(), block.getY(), block.getZ(), new BaseBlock(0, 0));

                }

                if (blocks.size() <= indexed || blocksCut >= TreeFeller.settings.maxLogBlocksPerCut)
                    this.cancel();
            }

            @Override
            public void cancel() {
                editSession.flushQueue();
                stop();
                super.cancel();
            }


        }.runTaskTimer(TreeFeller.instance, 0L, speed);
    }

    private int getSaplingType(Block block) {
        if (block.getType() == Material.LOG)
            return block.getData() % 4;
        return 4 + block.getData() % 2;
    }

    private boolean updateItemInHand() {
        ItemStack item = player.getItemInHand();
        if (item != null && item.getType() != Material.AIR)
            return false;

        for (int index = 0; index < 36; index++) {
            ItemStack stack = player.getInventory().getItem(index);
            if (stack != null && TreeFeller.settings.cuttingSpeed.containsKey(stack.getType())) {
                player.setItemInHand(stack);
                player.getInventory().setItem(index, null);
                return true;
            }
        }
        return false;
    }
}
