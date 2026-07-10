package dev.shraeder.bucketdimension.bucket;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class BucketItems {

    private final NamespacedKey keyIsBucket;
    private final NamespacedKey keyMode;

    public BucketItems(JavaPlugin plugin) {
        this.keyIsBucket = new NamespacedKey(plugin, "bucket_dimension_bucket");
        this.keyMode = new NamespacedKey(plugin, "bucket_dimension_mode");
    }

    public boolean isBucket(ItemStack item) {
        if (item == null) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Byte flag = pdc.get(keyIsBucket, PersistentDataType.BYTE);
        return flag != null && flag == (byte) 1;
    }

    public BucketMode getMode(ItemStack item) {
        if (!isBucket(item)) {
            return BucketMode.COLLECT;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return BucketMode.COLLECT;
        }
        String raw = meta.getPersistentDataContainer().get(keyMode, PersistentDataType.STRING);
        if (raw == null) {
            return BucketMode.COLLECT;
        }
        try {
            return BucketMode.valueOf(raw);
        } catch (IllegalArgumentException ignored) {
            return BucketMode.COLLECT;
        }
    }

    public ItemStack createBucket(BucketMode mode) {
        Material material = switch (mode) {
            case WATER -> Material.WATER_BUCKET;
            case LAVA -> Material.LAVA_BUCKET;
            case COLLECT -> Material.BUCKET;
        };

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(ChatColor.AQUA + "Bucket Dimension");
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyIsBucket, PersistentDataType.BYTE, (byte) 1);
        pdc.set(keyMode, PersistentDataType.STRING, mode.name());

        item.setItemMeta(meta);
        return item;
    }

    public ItemStack setMode(ItemStack existing, BucketMode mode) {
        if (existing == null || existing.getType() == Material.AIR) {
            return createBucket(mode);
        }

        ItemStack updated = existing.clone();
        updated.setType(switch (mode) {
            case WATER -> Material.WATER_BUCKET;
            case LAVA -> Material.LAVA_BUCKET;
            case COLLECT -> Material.BUCKET;
        });

        ItemMeta meta = updated.getItemMeta();
        if (meta == null) {
            return updated;
        }

        meta.setDisplayName(ChatColor.AQUA + "Bucket Dimension");
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyIsBucket, PersistentDataType.BYTE, (byte) 1);
        pdc.set(keyMode, PersistentDataType.STRING, mode.name());

        updated.setItemMeta(meta);
        return updated;
    }
}
