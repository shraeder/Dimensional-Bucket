package dev.shraeder.bucketdimension.storage;

public enum FluidType {
    WATER("water"),
    LAVA("lava");

    private final String configKey;

    FluidType(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }
}
