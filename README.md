# 🪣 BucketDimension

**BucketDimension** is a lightweight Minecraft plugin that gives players a single **special bucket** that can store **water and lava source blocks** in persistent, per-player storage.

Stop hauling stacks of buckets around. Carry one bucket. Hoard like a professional.

---

## Features

- ✅ One special “Bucket Dimension” bucket (glowing)
- ✅ **Shift + Left Click** while holding it to open a mode selection GUI
- ✅ 3 modes: **Collect**, **Water**, **Lava**
- ✅ Collect mode stores **water/lava source blocks** (and removes them from the world)
- ✅ Water/Lava modes let you **place stored sources back into the world**
- ✅ Cauldron support:
  - Drain **water/lava cauldrons** into storage (Collect mode)
  - Fill **water cauldrons** from storage (Water mode)
  - Fill an **empty cauldron** with lava from storage (Lava mode)
- ✅ Persistent per-player storage in `plugins/BucketDimension/storage.yml`
- ✅ Optional storage cap via `config.yml`
- ✅ Prevents creature capture (fish/axolotl/etc) with the special bucket

---

## Commands

| Command   | Description                              | Permission              |
|----------|------------------------------------------|-------------------------|
| `/bucket` | Gives you the Bucket Dimension bucket     | `bucketdimension.bucket` |

---

## Usage

1. Use `/bucket` to get your Bucket Dimension bucket.
2. Hold it and **Shift + Left Click** to open the GUI.
3. Click one of the modes:
	- **Empty (Collect Mode)**
	- **Water**
	- **Lava**
4. **Collect Mode**
	- Right-click a **water/lava source block** to store it (only source blocks; flowing won’t work).
	- Right-click a **water/lava cauldron** to drain it into storage.
5. **Water / Lava Mode**
	- Right-click to place a stored source block (uses 1 from storage).
	- Right-click cauldrons to fill them (see Features for details).

---

## Config

Edit `plugins/BucketDimension/config.yml`:

- `storage.limit-enabled`: enable/disable a storage cap
- `storage.max-sources`: max stored sources per fluid type (water and lava)

Stored amounts are saved per-player in `plugins/BucketDimension/storage.yml`.

---

## Requirements

- Paper/Spigot 1.21+
- Java 21

---

