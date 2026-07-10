package dev.shraeder.bucketdimension;

import dev.shraeder.bucketdimension.bucket.BucketItems;
import dev.shraeder.bucketdimension.bucket.BucketMode;
import dev.shraeder.bucketdimension.command.BucketCommand;
import dev.shraeder.bucketdimension.listener.BucketListener;
import dev.shraeder.bucketdimension.listener.GuiListener;
import dev.shraeder.bucketdimension.storage.StorageManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapelessRecipe;
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

        registerRecipes();

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

    private void registerRecipes() {
        NamespacedKey recipeKey = new NamespacedKey(this, "bucket_dimension_bucket");
        ShapelessRecipe recipe = new ShapelessRecipe(recipeKey, bucketItems.createBucket(BucketMode.COLLECT));
        recipe.addIngredient(Material.BUCKET);
        recipe.addIngredient(Material.ENDER_EYE);
        getServer().addRecipe(recipe);
    }
}
