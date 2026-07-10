package dev.shraeder.bucketdimension.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class StorageManager {

    private static final int WATER_PRIMING_SOURCES = 2;

    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    public StorageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "storage.yml");
    }

    public void load() {
        if (!plugin.getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            plugin.getDataFolder().mkdirs();
        }

        if (!file.exists()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                file.createNewFile();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create storage.yml", e);
            }
        }

        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save storage.yml: " + e.getMessage());
        }
    }

    public int getStored(UUID uuid, FluidType type) {
        return Math.max(0, config.getInt(uuid.toString() + "." + type.getConfigKey(), 0));
    }

    public void setStored(UUID uuid, FluidType type, int value) {
        config.set(uuid.toString() + "." + type.getConfigKey(), Math.max(0, value));
    }

    public boolean tryAdd(UUID uuid, FluidType type, int amount) {
        if (amount <= 0) {
            return true;
        }

        int current = getStored(uuid, type);
        if (type == FluidType.WATER && isWaterPrimingEnabled()) {
            setStored(uuid, type, Math.min(WATER_PRIMING_SOURCES, current + amount));
            return true;
        }

        if (isLimitEnabled()) {
            int max = getMaxSources();
            if (max >= 0 && current + amount > max) {
                return false;
            }
        }

        setStored(uuid, type, current + amount);
        return true;
    }

    public boolean tryRemove(UUID uuid, FluidType type, int amount) {
        if (amount <= 0) {
            return true;
        }

        if (hasUnlimitedDispense(uuid, type)) {
            return true;
        }

        int current = getStored(uuid, type);
        if (current < amount) {
            return false;
        }

        setStored(uuid, type, current - amount);
        return true;
    }

    public boolean isLimitEnabled() {
        return plugin.getConfig().getBoolean("storage.limit-enabled", true);
    }

    public int getMaxSources() {
        return plugin.getConfig().getInt("storage.max-sources", 64);
    }

    public boolean isWaterPrimingEnabled() {
        return plugin.getConfig().getBoolean("storage.water-priming.enabled", true);
    }

    public boolean hasUnlimitedDispense(UUID uuid, FluidType type) {
        return type == FluidType.WATER && isWaterPrimingEnabled() && getStored(uuid, type) >= WATER_PRIMING_SOURCES;
    }

    public String getDisplayAmount(UUID uuid, FluidType type) {
        if (hasUnlimitedDispense(uuid, type)) {
            return "Infinite (primed)";
        }

        return String.valueOf(getStored(uuid, type));
    }
}
