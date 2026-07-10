package dev.shraeder.bucketdimension;

import dev.shraeder.bucketdimension.bucket.BucketItems;
import dev.shraeder.bucketdimension.command.BucketCommand;
import dev.shraeder.bucketdimension.listener.BucketListener;
import dev.shraeder.bucketdimension.listener.GuiListener;
import dev.shraeder.bucketdimension.storage.StorageManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class BucketDimensionPlugin extends JavaPlugin {

    private StorageManager storageManager;
    private BucketItems bucketItems;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.storageManager = new StorageManager(this);
        this.storageManager.load();

        this.bucketItems = new BucketItems(this);

        getServer().getPluginManager().registerEvents(new BucketListener(this, bucketItems, storageManager), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this, bucketItems, storageManager), this);

        if (getCommand("bucket") != null) {
            getCommand("bucket").setExecutor(new BucketCommand(bucketItems));
        }

        getLogger().info("BucketDimension enabled.");
    }

    @Override
    public void onDisable() {
        if (storageManager != null) {
            storageManager.save();
        }
        getLogger().info("BucketDimension disabled.");
    }
}
