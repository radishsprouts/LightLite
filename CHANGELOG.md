# Changelog

## 0.1.0 (unreleased)

- First release for Minecraft 26.1.2, 26.2 and 26.3 (Fabric).
- Spawn overlay with red (always) / yellow (night only) markers, tile or cross style.
- Section-cached scanning driven by vanilla block/light change notifications, time-boxed per tick.
- GPU-retained region meshes (one draw call per 64x64 region), with an immediate fallback for shader mods.
- Mod Menu config screen, `config/lightlite.json`, `/lightlite stats|resetstats|reload`.
