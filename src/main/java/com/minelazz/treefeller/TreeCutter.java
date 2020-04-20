package com.minelazz.treefeller;

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
    private List<String> scanned = new ArrayList<>();
    private List<String> comparisonBlockArrayLeaves = new ArrayList<>();
    private List<Block> blocks = new ArrayList<>();
    private int indexed = 0;
    private boolean loop = false;
    private boolean initialized;


    public TreeCutter(Player cutter, Block startBlock) {
        this.player = cutter;
        this.startBlock = startBlock;
    }

    /**
     * Scan an object find all connecting leaves and wood blocks
     *
     * @param block start block
     * @param centerX center of object, x
     * @param centerZ center of object, z
     */
    public void runLoop(Block block, final int centerX, final int centerZ) {
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0)
                        continue;
                    Block blockRelative = block.getRelative(x, y, z);
                    String s = blockRelative.getX() + ":" + blockRelative.getY() + ":" + blockRelative.getZ();
                    if (scanned.contains(s)){
                        continue;
                    }
                    scanned.add(s);
                    if (Utils.isLeaves(blockRelative.getType()) && !comparisonBlockArrayLeaves.contains(s)) {
                        comparisonBlockArrayLeaves.add(s);
                        continue;
                    }

                    if (Utils.isWood(blockRelative.getType())) {
                        int searchSquareSize = 25;
                        if (blockRelative.getX() > centerX + searchSquareSize || blockRelative.getX() < centerX - searchSquareSize
                                || blockRelative.getZ() > centerZ + searchSquareSize || blockRelative.getZ() < centerZ - searchSquareSize)
                            break;
                        if (!comparisonBlockArray.contains(s)) {
                            comparisonBlockArray.add(s);
                            blocks.add(blockRelative);
                            this.runLoop(blockRelative, centerX, centerZ);
                        }
                    }
                }
            }
        }
    }

    /**
     * Chop down a tree if the object is a tree
     */
    @Override
    public void run() {
        if (initialized){
            return;
        }
        initialized = true;
        blocks.add(startBlock);
        runLoop(startBlock, startBlock.getX(), startBlock.getZ());

        if (isTree()) {
            cutDownTree();
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
        stop();
    }

    /**
     * Compare the amount of leaves to wood blocks to determine whether the object is tree or not
     *
     * @return true if object is a tree
     */
    private boolean isTree() {
        return (comparisonBlockArrayLeaves.size() * 1D) / (blocks.size() * 1D) > 0.3;
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    private void stop() {
        TreeFeller.currentFellers.remove(player.getUniqueId());
    }

    private void cutDownTree() {
        if ((player.getItemInHand().getType() == Material.AIR) && !updateItemInHand()) {
            stop();
            return;
        }

        if (!TreeFeller.currentFellers.contains(player.getUniqueId()))
            TreeFeller.currentFellers.add(player.getUniqueId());

        blocks = blocks.stream().sorted((b, b2) -> b2.getY() - b.getY()).collect(Collectors.toList());
        long speed = TreeFeller.settings.cuttingSpeed.get(player.getItemInHand().getType());

        new BukkitRunnable() {
            int blocksCut = 0;

            @Override
            public void run() {
                //Instant cut down tree
                if (TreeFeller.settings.instantTreeCut && !loop) {
                    for (int i = 0; i < blocks.size(); i++) {
                        loop = true;
                        this.run();
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
                            block.setType(getSaplingType(block));
                        } else {
                            block.setType(Material.AIR);
                        }
                    } else {
                        block.setType(Material.AIR);
                    }
                }

                if (blocks.size() <= indexed || blocksCut >= TreeFeller.settings.maxLogBlocksPerCut)
                    this.cancel();
            }

            @Override
            public void cancel() {
                stop();
                super.cancel();
            }


        }.runTaskTimer(TreeFeller.instance, 0L, speed);
    }


    /**
     * Based on tree chopped down, find the sapling type
     *
     * @param block block in tree
     * @return sapling for this tree
     */
    private Material getSaplingType(Block block) {
        switch (block.getType()){
            case ACACIA_LOG:
            case STRIPPED_ACACIA_LOG:
            case STRIPPED_ACACIA_WOOD:
            case ACACIA_WOOD:
                return Material.ACACIA_SAPLING;
            case BIRCH_LOG:
            case STRIPPED_BIRCH_LOG:
            case STRIPPED_BIRCH_WOOD:
            case BIRCH_WOOD:
                return Material.BIRCH_SAPLING;
            case DARK_OAK_LOG:
            case STRIPPED_DARK_OAK_LOG:
            case STRIPPED_DARK_OAK_WOOD:
            case DARK_OAK_WOOD:
                return Material.DARK_OAK_SAPLING;
            case JUNGLE_LOG:
            case STRIPPED_JUNGLE_LOG:
            case STRIPPED_JUNGLE_WOOD:
            case JUNGLE_WOOD:
                return Material.JUNGLE_SAPLING;
            case OAK_LOG:
            case STRIPPED_OAK_LOG:
            case STRIPPED_OAK_WOOD:
            case OAK_WOOD:
                return Material.OAK_SAPLING;
            case SPRUCE_LOG:
            case STRIPPED_SPRUCE_LOG:
            case STRIPPED_SPRUCE_WOOD:
            case SPRUCE_WOOD:
                return Material.SPRUCE_SAPLING;
        }
        return Material.OAK_SAPLING;
    }

    /**
     * An axe can be broken, this method finds a new axe in the inventory if broken
     *
     * @return true if player still has an axe
     */
    private boolean updateItemInHand() {
        ItemStack item = player.getItemInHand();
        if (item.getType() != Material.AIR) {
            return false;
        }

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
