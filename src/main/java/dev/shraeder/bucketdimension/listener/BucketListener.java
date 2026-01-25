package dev.shraeder.bucketdimension.listener;

import dev.shraeder.bucketdimension.bucket.BucketItems;
import dev.shraeder.bucketdimension.bucket.BucketMode;
import dev.shraeder.bucketdimension.storage.FluidType;
import dev.shraeder.bucketdimension.storage.StorageManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class BucketListener implements Listener {

    private final JavaPlugin plugin;
    private final BucketItems bucketItems;
    private final StorageManager storage;

    public BucketListener(JavaPlugin plugin, BucketItems bucketItems, StorageManager storage) {
        this.plugin = plugin;
        this.bucketItems = bucketItems;
        this.storage = storage;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == null || event.getClickedBlock() == null) {
            return;
        }

        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        ItemStack item = getItemInHand(player, hand);
        if (!bucketItems.isBucket(item)) {
            return;
        }

        Block block = event.getClickedBlock();
        Material type = block.getType();
        BucketMode mode = bucketItems.getMode(item);

        // Waterlogging is handled during interaction for some blocks/versions (it may not fire
        // PlayerBucketEmptyEvent), so we implement it here to match vanilla.
        if (mode == BucketMode.WATER) {
            BlockData data = block.getBlockData();
            if (data instanceof Waterlogged waterlogged && !waterlogged.isWaterlogged()) {
                if (storage.getStored(player.getUniqueId(), FluidType.WATER) <= 0) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "You have no water stored.");
                    return;
                }

                event.setCancelled(true);

                waterlogged.setWaterlogged(true);
                block.setBlockData(waterlogged, true);
                triggerNeighborUpdates(block);

                storage.tryRemove(player.getUniqueId(), FluidType.WATER, 1);
                storage.save();

                player.playSound(player.getLocation(), Sound.ITEM_BUCKET_EMPTY, 1f, 1f);
                player.sendMessage(ChatColor.GREEN + "Used 1 water source. Remaining: "
                        + ChatColor.WHITE + storage.getStored(player.getUniqueId(), FluidType.WATER));

                setItemInHand(player, hand, bucketItems.setMode(item, BucketMode.WATER));
                return;
            }
        }

        if (type != Material.CAULDRON && type != Material.WATER_CAULDRON && type != Material.LAVA_CAULDRON) {
            return;
        }

        // We handle cauldrons ourselves so the special bucket is never consumed/replaced.
        event.setCancelled(true);

        // COLLECT mode: drain cauldrons into storage.
        if (mode == BucketMode.COLLECT) {
            if (type == Material.WATER_CAULDRON) {
                int level = getCauldronLevel(block);
                if (level <= 0) {
                    return;
                }

                if (!storage.tryAdd(player.getUniqueId(), FluidType.WATER, 1)) {
                    player.sendMessage(ChatColor.RED + "Storage is full.");
                    return;
                }

                setCauldronLevel(block, level - 1);
                storage.save();
                player.playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL, 1f, 1f);
                player.sendMessage(ChatColor.GREEN + "Stored 1 water source. Now: "
                        + ChatColor.WHITE + storage.getStored(player.getUniqueId(), FluidType.WATER));

                setItemInHand(player, hand, bucketItems.setMode(item, BucketMode.COLLECT));
                return;
            }

            if (type == Material.LAVA_CAULDRON) {
                if (!storage.tryAdd(player.getUniqueId(), FluidType.LAVA, 1)) {
                    player.sendMessage(ChatColor.RED + "Storage is full.");
                    return;
                }

                block.setType(Material.CAULDRON, true);
                storage.save();
                player.playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL_LAVA, 1f, 1f);
                player.sendMessage(ChatColor.GREEN + "Stored 1 lava source. Now: "
                        + ChatColor.WHITE + storage.getStored(player.getUniqueId(), FluidType.LAVA));

                setItemInHand(player, hand, bucketItems.setMode(item, BucketMode.COLLECT));
                return;
            }

            player.sendMessage(ChatColor.YELLOW + "Nothing to collect.");
            return;
        }

        // WATER mode: fill/increase water cauldron.
        if (mode == BucketMode.WATER) {
            if (storage.getStored(player.getUniqueId(), FluidType.WATER) <= 0) {
                player.sendMessage(ChatColor.RED + "You have no water stored.");
                return;
            }

            if (type == Material.LAVA_CAULDRON) {
                player.sendMessage(ChatColor.RED + "Can't put water into a lava cauldron.");
                return;
            }

            if (type == Material.CAULDRON) {
                block.setType(Material.WATER_CAULDRON, true);
                setCauldronLevel(block, 1);
            } else {
                int level = getCauldronLevel(block);
                if (level >= 3) {
                    player.sendMessage(ChatColor.YELLOW + "That cauldron is already full.");
                    return;
                }
                setCauldronLevel(block, level + 1);
            }

            storage.tryRemove(player.getUniqueId(), FluidType.WATER, 1);
            storage.save();
            player.playSound(player.getLocation(), Sound.ITEM_BUCKET_EMPTY, 1f, 1f);
            player.sendMessage(ChatColor.GREEN + "Used 1 water source. Remaining: "
                    + ChatColor.WHITE + storage.getStored(player.getUniqueId(), FluidType.WATER));

            setItemInHand(player, hand, bucketItems.setMode(item, BucketMode.WATER));
            return;
        }

        // LAVA mode: fill empty cauldron with lava.
        if (mode == BucketMode.LAVA) {
            if (storage.getStored(player.getUniqueId(), FluidType.LAVA) <= 0) {
                player.sendMessage(ChatColor.RED + "You have no lava stored.");
                return;
            }

            if (type != Material.CAULDRON) {
                player.sendMessage(ChatColor.YELLOW + "Lava can only fill an empty cauldron.");
                return;
            }

            block.setType(Material.LAVA_CAULDRON, true);
            storage.tryRemove(player.getUniqueId(), FluidType.LAVA, 1);
            storage.save();
            player.playSound(player.getLocation(), Sound.ITEM_BUCKET_EMPTY_LAVA, 1f, 1f);
            player.sendMessage(ChatColor.GREEN + "Used 1 lava source. Remaining: "
                    + ChatColor.WHITE + storage.getStored(player.getUniqueId(), FluidType.LAVA));

            setItemInHand(player, hand, bucketItems.setMode(item, BucketMode.LAVA));
        }
    }

    @EventHandler
    public void onBucketEntity(PlayerBucketEntityEvent event) {
        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        ItemStack item = getItemInHand(player, hand);
        if (!bucketItems.isBucket(item)) {
            return;
        }

        // Prevent capturing fish/axolotl/etc with the special bucket.
        event.setCancelled(true);
        event.getPlayer().sendMessage(ChatColor.RED + "This bucket can't capture creatures.");
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        ItemStack inHand = getItemInHand(player, hand);

        if (!bucketItems.isBucket(inHand)) {
            return;
        }

        BucketMode mode = bucketItems.getMode(inHand);
        if (mode != BucketMode.COLLECT) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.YELLOW + "Switch to Empty (Collect Mode) to store liquids.");
            return;
        }

        Block clicked = event.getBlockClicked();
        Material type = clicked.getType();
        FluidType fluidType = switch (type) {
            case WATER -> FluidType.WATER;
            case LAVA -> FluidType.LAVA;
            default -> null;
        };

        // Vanilla buckets can collect from waterlogged blocks; treat that as collecting a water source.
        if (fluidType == null) {
            BlockData data = clicked.getBlockData();
            if (data instanceof Waterlogged waterlogged && waterlogged.isWaterlogged()) {
                fluidType = FluidType.WATER;
            }
        }

        if (fluidType == null) {
            return;
        }

        if (!isSourceBlock(clicked)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Only source blocks can be stored.");
            return;
        }

        boolean added = storage.tryAdd(player.getUniqueId(), fluidType, 1);
        if (!added) {
            event.setCancelled(true);
            int max = storage.getMaxSources();
            player.sendMessage(ChatColor.RED + "Storage is full" + (storage.isLimitEnabled() ? " (max " + max + ")" : "") + ".");
            return;
        }

        event.setCancelled(true);

        // If the clicked block was waterlogged, remove the waterlogged state instead of breaking the block.
        BlockData data = clicked.getBlockData();
        if (fluidType == FluidType.WATER && data instanceof Waterlogged waterlogged && waterlogged.isWaterlogged()) {
            waterlogged.setWaterlogged(false);
            clicked.setBlockData(waterlogged, true);
            triggerNeighborUpdates(clicked);
        } else {
            clicked.setType(Material.AIR, true);
        }
        storage.save();

        if (fluidType == FluidType.WATER) {
            player.playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL, 1f, 1f);
        } else {
            player.playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL_LAVA, 1f, 1f);
        }

        int stored = storage.getStored(player.getUniqueId(), fluidType);
        player.sendMessage(ChatColor.GREEN + "Stored 1 " + fluidType.name().toLowerCase() + " source. Now: " + ChatColor.WHITE + stored);

        // Ensure the bucket stays as the special bucket.
        ItemStack updated = bucketItems.setMode(inHand, BucketMode.COLLECT);
        setItemInHand(player, hand, updated);
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        ItemStack inHand = getItemInHand(player, hand);

        if (!bucketItems.isBucket(inHand)) {
            return;
        }

        BucketMode mode = bucketItems.getMode(inHand);
        FluidType fluidType = switch (mode) {
            case WATER -> FluidType.WATER;
            case LAVA -> FluidType.LAVA;
            default -> null;
        };

        if (fluidType == null) {
            // In collect mode, don't allow placing anything.
            if (mode == BucketMode.COLLECT) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.YELLOW + "Select Water or Lava mode in the GUI to place stored liquid.");
            }
            return;
        }

        int stored = storage.getStored(player.getUniqueId(), fluidType);
        if (stored <= 0) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You have no " + fluidType.name().toLowerCase() + " stored.");
            return;
        }

        Block clickedBlock = event.getBlockClicked();
        Block target = event.getBlock();
        Material placeMaterial = (fluidType == FluidType.WATER) ? Material.WATER : Material.LAVA;

        // Match vanilla bucket behavior for waterlogging (stairs/slabs/etc).
        if (fluidType == FluidType.WATER) {
            // Prefer waterlogging the clicked block (e.g., leaves) if it supports the state.
            if (clickedBlock != null) {
                BlockData clickedData = clickedBlock.getBlockData();
                if (clickedData instanceof Waterlogged waterlogged && !waterlogged.isWaterlogged()) {
                    event.setCancelled(true);

                    waterlogged.setWaterlogged(true);
                    clickedBlock.setBlockData(waterlogged, true);
                    triggerNeighborUpdates(clickedBlock);

                    boolean removed = storage.tryRemove(player.getUniqueId(), fluidType, 1);
                    if (!removed) {
                        plugin.getLogger().warning("Failed to decrement storage for " + player.getUniqueId());
                    }
                    storage.save();

                    player.playSound(player.getLocation(), Sound.ITEM_BUCKET_EMPTY, 1f, 1f);
                    int now = storage.getStored(player.getUniqueId(), fluidType);
                    player.sendMessage(ChatColor.GREEN + "Used 1 water source. Remaining: " + ChatColor.WHITE + now);

                    // Keep the bucket as the special bucket with the same mode.
                    ItemStack updated = bucketItems.setMode(inHand, mode);
                    setItemInHand(player, hand, updated);
                    return;
                }
            }

            // Otherwise, waterlog the event target if it supports waterlogging.
            BlockData targetData = target.getBlockData();
            if (targetData instanceof Waterlogged waterlogged) {
                event.setCancelled(true);

                if (waterlogged.isWaterlogged()) {
                    player.sendMessage(ChatColor.YELLOW + "That block is already waterlogged.");
                    return;
                }

                waterlogged.setWaterlogged(true);
                target.setBlockData(waterlogged, true);
                triggerNeighborUpdates(target);

                boolean removed = storage.tryRemove(player.getUniqueId(), fluidType, 1);
                if (!removed) {
                    plugin.getLogger().warning("Failed to decrement storage for " + player.getUniqueId());
                }
                storage.save();

                player.playSound(player.getLocation(), Sound.ITEM_BUCKET_EMPTY, 1f, 1f);
                int now = storage.getStored(player.getUniqueId(), fluidType);
                player.sendMessage(ChatColor.GREEN + "Used 1 water source. Remaining: " + ChatColor.WHITE + now);

                // Keep the bucket as the special bucket with the same mode.
                ItemStack updated = bucketItems.setMode(inHand, mode);
                setItemInHand(player, hand, updated);
                return;
            }
        }

        // Some interactions (notably with certain blocks like leaves) report the clicked block as the
        // placement target even though vanilla would place into the adjacent block.
        Block placeTarget = target;
        if (clickedBlock != null && placeTarget.equals(clickedBlock)) {
            Block faceTarget = clickedBlock.getRelative(event.getBlockFace());
            if (!placeTarget.equals(faceTarget)) {
                placeTarget = faceTarget;
            }
        }

        if (placeTarget.getType() == placeMaterial) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.YELLOW + "There's already " + fluidType.name().toLowerCase() + " there.");
            return;
        }

        if (!canPlaceInto(placeTarget)) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        placeTarget.setType(placeMaterial, true);

        boolean removed = storage.tryRemove(player.getUniqueId(), fluidType, 1);
        if (!removed) {
            // Shouldn't happen if counts were accurate, but keep it safe.
            plugin.getLogger().warning("Failed to decrement storage for " + player.getUniqueId());
        }
        storage.save();

        if (fluidType == FluidType.WATER) {
            player.playSound(player.getLocation(), Sound.ITEM_BUCKET_EMPTY, 1f, 1f);
        } else {
            player.playSound(player.getLocation(), Sound.ITEM_BUCKET_EMPTY_LAVA, 1f, 1f);
        }

        int now = storage.getStored(player.getUniqueId(), fluidType);
        player.sendMessage(ChatColor.GREEN + "Placed 1 " + fluidType.name().toLowerCase() + " source. Remaining: " + ChatColor.WHITE + now);

        // Keep the bucket as the special bucket with the same mode.
        ItemStack updated = bucketItems.setMode(inHand, mode);
        setItemInHand(player, hand, updated);
    }

    private static boolean isSourceBlock(Block block) {
        BlockData data = block.getBlockData();
        if (data instanceof Levelled levelled) {
            return levelled.getLevel() == 0;
        }
        return true;
    }

    private static int getCauldronLevel(Block block) {
        BlockData data = block.getBlockData();
        if (data instanceof Levelled levelled) {
            return levelled.getLevel();
        }
        return 0;
    }

    private static void setCauldronLevel(Block block, int newLevel) {
        int clamped = Math.max(0, Math.min(3, newLevel));
        if (block.getType() != Material.WATER_CAULDRON) {
            // Only water cauldrons have levels we care about.
            return;
        }

        if (clamped <= 0) {
            block.setType(Material.CAULDRON, true);
            return;
        }

        BlockData data = block.getBlockData();
        if (data instanceof Levelled levelled) {
            levelled.setLevel(clamped);
            block.setBlockData(levelled, true);
        }
    }

    private static boolean canPlaceInto(Block target) {
        if (target.isEmpty()) {
            return true;
        }
        // Buckets can generally place into passable blocks (grass, etc.).
        return target.isPassable();
    }

    private static void triggerNeighborUpdates(Block block) {
        // Apply physics on the changed block and its neighbors so water starts flowing immediately.
        block.getState().update(true, true);

        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN}) {
            Block relative = block.getRelative(face);
            relative.getState().update(true, true);
        }
    }

    private static ItemStack getItemInHand(Player player, EquipmentSlot hand) {
        if (hand == EquipmentSlot.OFF_HAND) {
            return player.getInventory().getItemInOffHand();
        }
        return player.getInventory().getItemInMainHand();
    }

    private static void setItemInHand(Player player, EquipmentSlot hand, ItemStack item) {
        if (hand == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(item);
        } else {
            player.getInventory().setItemInMainHand(item);
        }
    }
}
