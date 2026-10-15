## 📖 About

ChickensShed makes chickens drop feathers occasionally, so you can get feathers without killing them. Any mob can be set to drop any item.

Each mob drops on its own timer, like chickens laying eggs: by default a feather every 15–25 minutes. Turning off mob drops with the game rule also stops shedding.

Works with [**Supplementaries**](https://github.com/MehVahdJukaar/Supplementaries) cages and jars: captured mobs keep shedding, and hoppers can collect the drops.

## ⚙️ Configuration

Open the mods list (Mod Menu on Fabric), pick ChickensShed and press **Config** to add mobs and change their settings. On a server, edit `config/chickensshed.json` and restart.

Each mob can be configured with:
- Which item it drops and how many;
- Shortest and longest wait between drops (15 and 25 minutes by default);
- Whether baby mobs drop the item too.

## 🔄 2.0 Rewrite

Version 2.0 is a full rewrite of the mod. The old `chickensshed-common.toml` config is no longer used, so set up your mobs again after updating. Fabric API and Forge Config API Port are no longer required.
