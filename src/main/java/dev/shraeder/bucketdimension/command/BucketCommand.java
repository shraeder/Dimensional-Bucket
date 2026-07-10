package dev.shraeder.bucketdimension.command;

import dev.shraeder.bucketdimension.bucket.BucketItems;
import dev.shraeder.bucketdimension.bucket.BucketMode;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class BucketCommand implements CommandExecutor {

    private final BucketItems bucketItems;

    public BucketCommand(BucketItems bucketItems) {
        this.bucketItems = bucketItems;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }

        player.getInventory().addItem(bucketItems.createBucket(BucketMode.COLLECT));
        player.sendMessage(ChatColor.GREEN + "You received the Bucket Dimension bucket.");
        return true;
    }
}
