package dev.shraeder.bucketdimension.gui;

import dev.shraeder.bucketdimension.storage.FluidType;
import dev.shraeder.bucketdimension.storage.StorageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class BucketGui {

    public static final String TITLE = ChatColor.DARK_AQUA + "Bucket Dimension";

    private BucketGui() {
    }

    public static Inventory create(Player player, StorageManager storage) {
        Inventory inv = Bukkit.createInventory(new BucketGuiHolder(player.getUniqueId()), 27, TITLE);

        inv.setItem(11, makeFluidItem(Material.WATER_BUCKET, ChatColor.AQUA + "Water", storage, player, FluidType.WATER));
        inv.setItem(13, makeEmptyModeItem());
        inv.setItem(15, makeFluidItem(Material.LAVA_BUCKET, ChatColor.GOLD + "Lava", storage, player, FluidType.LAVA));

        return inv;
    }

    private static ItemStack makeEmptyModeItem() {
        ItemStack item = new ItemStack(Material.BUCKET);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(ChatColor.WHITE + "Empty (Collect Mode)");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Click to switch to collect mode.");
        lore.add(ChatColor.GRAY + "Then right-click water/lava sources");
        lore.add(ChatColor.GRAY + "to store them.");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeFluidItem(Material material, String name, StorageManager storage, Player player, FluidType type) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(name);
        int stored = storage.getStored(player.getUniqueId(), type);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Stored: " + ChatColor.WHITE + stored);

        if (storage.isLimitEnabled()) {
            lore.add(ChatColor.GRAY + "Max: " + ChatColor.WHITE + storage.getMaxSources());
        } else {
            lore.add(ChatColor.GRAY + "Max: " + ChatColor.WHITE + "Unlimited");
        }

        lore.add(ChatColor.DARK_GRAY + "Click to set bucket mode.");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
