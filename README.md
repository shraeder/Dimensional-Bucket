# BucketDimension

BucketDimension is a lightweight Minecraft plugin that gives players one special bucket with persistent per-player storage for water and lava.

## Features

- One special Bucket Dimension bucket
- Shift + Left Click opens the mode selection GUI
- 3 modes: Collect, Water, Lava
- Collect mode stores water and lava source blocks
- Water and Lava modes place stored fluids back into the world
- Cauldron support for draining and filling
- Waterlogging support for compatible blocks
- Persistent per-player storage in `plugins/BucketDimension/storage.yml`
- Optional storage cap via `config.yml`
- Optional water priming: storing 2 water sources grants infinite water dispensing
- Prevents creature capture with the special bucket

## Commands

- `/bucket`: Gives you the Bucket Dimension bucket

## Usage

1. Use `/bucket` to get your Bucket Dimension bucket.
2. Hold it and Shift + Left Click to open the GUI.
3. Choose one of the modes: Collect, Water, or Lava.
4. In Collect mode, right-click water or lava source blocks to store them.
5. In Water or Lava mode, right-click to place stored fluid back into the world.

## Config

Edit `plugins/BucketDimension/config.yml`:

- `storage.limit-enabled`: enable or disable the normal storage cap
- `storage.max-sources`: max stored sources per fluid type while capped
- `storage.water-priming.enabled`: when true, storing 2 water sources primes infinite water dispensing

When water priming is enabled, primed water is not affected by `storage.max-sources`.

## Build

Requirements:

- Java 21+
- Maven 3.9+

Build with:

```bash
mvn -q package
```

Jar output:

- `target/bucket-dimension-1.0.0.jar`

## Notes

- Paper API version is currently set in `pom.xml`.
- If you target a different Paper version, update the Paper dependency version accordingly.
