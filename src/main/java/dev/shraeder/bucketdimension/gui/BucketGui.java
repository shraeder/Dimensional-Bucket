package dev.shraeder.bucketdimension.gui;

import dev.shraeder.bucketdimension.bucket.BucketMode;
import dev.shraeder.bucketdimension.storage.FluidType;
import dev.shraeder.bucketdimension.storage.StorageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class BucketGui {

    public static final String TITLE = ChatColor.DARK_AQUA + "Bucket Dimension";

    private BucketGui() {
    }

    public static Inventory create(Player player, StorageManager storage, BucketMode currentMode) {
        Inventory inv = Bukkit.createInventory(new BucketGuiHolder(player.getUniqueId()), InventoryType.HOPPER, TITLE);

        inv.setItem(0, makeFluidItem(Material.WATER_BUCKET, ChatColor.AQUA + "Water Mode", storage, player, FluidType.WATER, currentMode));
        inv.setItem(1, makeWaterStatusItem(storage, player));
        inv.setItem(2, makeCollectModeItem(currentMode));
        inv.setItem(3, makeStorageRulesItem(storage));
        inv.setItem(4, makeFluidItem(Material.LAVA_BUCKET, ChatColor.GOLD + "Lava Mode", storage, player, FluidType.LAVA, currentMode));

        return inv;
    }

    private static ItemStack makeCollectModeItem(BucketMode currentMode) {
        ItemStack item = new ItemStack(Material.BUCKET);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(ChatColor.WHITE + "Collect Mode");
        List<String> lore = new ArrayList<>();
        addModeStateLine(lore, currentMode == BucketMode.COLLECT);
        lore.add(ChatColor.GRAY + "Right-click source blocks to store fluid.");
        lore.add(ChatColor.DARK_GRAY + "Use this before collecting water or lava.");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeFluidItem(Material material, String name, StorageManager storage, Player player,
                                          FluidType type, BucketMode currentMode) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        addModeStateLine(lore, currentMode == toMode(type));

        if (type == FluidType.WATER && storage.isWaterPrimingEnabled()) {
            addWaterLore(lore, storage, player);
        } else {
            lore.add(ChatColor.GRAY + "Stored: " + ChatColor.WHITE + storage.getDisplayAmount(player.getUniqueId(), type));
            if (storage.isLimitEnabled()) {
                lore.add(ChatColor.GRAY + "Capacity: " + ChatColor.WHITE + storage.getMaxSources() + " sources");
            } else {
                lore.add(ChatColor.GRAY + "Capacity: " + ChatColor.WHITE + "Unlimited");
            }
        }

        if (type == FluidType.LAVA && storage.isLimitEnabled()) {
            lore.add(ChatColor.DARK_GRAY + "Lava follows the configured storage cap.");
        } else if (type == FluidType.LAVA) {
            lore.add(ChatColor.DARK_GRAY + "Lava storage is currently uncapped.");
        }

        lore.add(ChatColor.DARK_GRAY + "Click to switch this bucket mode.");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeWaterStatusItem(StorageManager storage, Player player) {
        boolean primingEnabled = storage.isWaterPrimingEnabled();
        boolean primed = storage.hasUnlimitedDispense(player.getUniqueId(), FluidType.WATER);

        ItemStack item = new ItemStack(primingEnabled ? Material.HEART_OF_THE_SEA : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(primingEnabled
                ? (primed ? ChatColor.AQUA + "Infinite Water Ready" : ChatColor.YELLOW + "Water Priming")
                : ChatColor.GRAY + "Water Priming Disabled");

        List<String> lore = new ArrayList<>();
        if (!primingEnabled) {
            lore.add(ChatColor.GRAY + "Water uses normal storage rules.");
            lore.add(ChatColor.DARK_GRAY + "The config has infinite priming turned off.");
        } else if (primed) {
            lore.add(ChatColor.GRAY + "Status: " + ChatColor.AQUA + "Primed");
            lore.add(ChatColor.GRAY + "Stored water now dispenses without being consumed.");
        } else {
            int progress = storage.getWaterPrimingProgress(player.getUniqueId());
            int required = storage.getWaterPrimingSourcesRequired();
            lore.add(ChatColor.GRAY + "Progress: " + ChatColor.WHITE + progress + ChatColor.GRAY + "/" + ChatColor.WHITE + required + ChatColor.GRAY + " water sources");
            lore.add(ChatColor.DARK_GRAY + "Store " + required + " water sources to unlock infinite water.");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeStorageRulesItem(StorageManager storage) {
        ItemStack item = new ItemStack(Material.HOPPER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(ChatColor.BLUE + "Storage Rules");
        List<String> lore = new ArrayList<>();
        if (storage.isLimitEnabled()) {
            lore.add(ChatColor.GRAY + "Lava capacity: " + ChatColor.WHITE + storage.getMaxSources() + " sources");
        } else {
            lore.add(ChatColor.GRAY + "Lava capacity: " + ChatColor.WHITE + "Unlimited");
        }

        if (storage.isWaterPrimingEnabled()) {
            lore.add(ChatColor.GRAY + "Water capacity: " + ChatColor.AQUA + "Priming only until ready");
            lore.add(ChatColor.DARK_GRAY + "After priming, water placement becomes infinite.");
        } else if (storage.isLimitEnabled()) {
            lore.add(ChatColor.GRAY + "Water capacity: " + ChatColor.WHITE + storage.getMaxSources() + " sources");
        } else {
            lore.add(ChatColor.GRAY + "Water capacity: " + ChatColor.WHITE + "Unlimited");
        }

        lore.add(ChatColor.DARK_GRAY + "Sneak + left-click the bucket to reopen this menu.");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static void addWaterLore(List<String> lore, StorageManager storage, Player player) {
        if (storage.hasUnlimitedDispense(player.getUniqueId(), FluidType.WATER)) {
            lore.add(ChatColor.GRAY + "Stored: " + ChatColor.AQUA + "Infinite water ready");
            lore.add(ChatColor.GRAY + "State: " + ChatColor.WHITE + "Primed");
            lore.add(ChatColor.DARK_GRAY + "Water sources are no longer consumed on placement.");
            return;
        }

        int progress = storage.getWaterPrimingProgress(player.getUniqueId());
        int required = storage.getWaterPrimingSourcesRequired();
        lore.add(ChatColor.GRAY + "Stored: " + ChatColor.WHITE + progress + ChatColor.GRAY + "/" + ChatColor.WHITE + required + ChatColor.GRAY + " priming sources");
        lore.add(ChatColor.GRAY + "State: " + ChatColor.YELLOW + "Charging");
        lore.add(ChatColor.DARK_GRAY + "Store " + required + " water sources to unlock infinite water.");
    }

    private static void addModeStateLine(List<String> lore, boolean active) {
        if (active) {
            lore.add(ChatColor.GREEN + "Selected now");
        } else {
            lore.add(ChatColor.GRAY + "Click to select this mode.");
        }
    }

    private static BucketMode toMode(FluidType type) {
        return switch (type) {
            case WATER -> BucketMode.WATER;
            case LAVA -> BucketMode.LAVA;
        };
    }
}
