package dev.shraeder.bucketdimension.listener;

import dev.shraeder.bucketdimension.bucket.BucketItems;
import dev.shraeder.bucketdimension.bucket.BucketMode;
import dev.shraeder.bucketdimension.gui.BucketGui;
import dev.shraeder.bucketdimension.gui.BucketGuiHolder;
import dev.shraeder.bucketdimension.storage.StorageManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GuiListener implements Listener {

    private final JavaPlugin plugin;
    private final BucketItems bucketItems;
    private final StorageManager storage;
    private final Map<UUID, EquipmentSlot> openHandByPlayer = new ConcurrentHashMap<>();

    public GuiListener(JavaPlugin plugin, BucketItems bucketItems, StorageManager storage) {
        this.plugin = plugin;
        this.bucketItems = bucketItems;
        this.storage = storage;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!player.isSneaking()) {
            return;
        }

        // Only open on shift + LEFT click.
        switch (event.getAction()) {
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> {
                // continue
            }
            default -> {
                return;
            }
        }

        if (!bucketItems.isBucket(item)) {
            return;
        }

        event.setCancelled(true);

        openHandByPlayer.put(player.getUniqueId(), event.getHand() == null ? EquipmentSlot.HAND : event.getHand());
        player.openInventory(BucketGui.create(player, storage));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!(event.getInventory().getHolder() instanceof BucketGuiHolder holder)) {
            return;
        }

        if (!holder.getPlayerId().equals(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        BucketMode mode = switch (clicked.getType()) {
            case WATER_BUCKET -> BucketMode.WATER;
            case LAVA_BUCKET -> BucketMode.LAVA;
            case BUCKET -> BucketMode.COLLECT;
            default -> null;
        };

        if (mode == null) {
            return;
        }

        EquipmentSlot slot = openHandByPlayer.getOrDefault(player.getUniqueId(), EquipmentSlot.HAND);
        ItemStack inHand = (slot == EquipmentSlot.OFF_HAND)
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();

        if (!bucketItems.isBucket(inHand)) {
            player.sendMessage(ChatColor.RED + "Hold the Bucket Dimension bucket.");
            player.closeInventory();
            return;
        }

        ItemStack updated = bucketItems.setMode(inHand, mode);
        if (slot == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(updated);
        } else {
            player.getInventory().setItemInMainHand(updated);
        }

        player.sendMessage(ChatColor.GREEN + "Bucket mode set to " + ChatColor.WHITE + mode.name() + ChatColor.GREEN + ".");
        player.closeInventory();
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        if (event.getInventory().getHolder() instanceof BucketGuiHolder) {
            openHandByPlayer.remove(player.getUniqueId());
        }
    }
}
