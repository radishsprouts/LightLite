# LightLite

Lightweight mob-spawn overlay for Fabric (Minecraft 26.1.2, 26.2, 26.3).  
軽量な湧き潰し可視化 MOD（Fabric / Minecraft 26.1.2・26.2・26.3）。[日本語は下にあります](#日本語)

## English

### Overview

LightLite shows colored markers on the blocks where hostile mobs can spawn.

| Color | Meaning |
|---|---|
| Red | Mobs can spawn by day and by night (the sky light is not enough either) |
| Yellow | Mobs can spawn only at night (or during thunderstorms) |

Markers can be translucent tiles (the default) or crosses.

### How it stays light

- **Only what changed is recalculated.** Results are stored per 16×16×16 section, and only sections whose blocks or light changed are recalculated. Changes are picked up from the notifications vanilla uses to update chunk rendering.
- **Each tick has a time limit.** By default, scanning uses at most 1.5 ms per tick, starting with the sections closest to the player.
- **No vertices are built every frame.** Markers are sent to a GPU buffer once for each 64×64-block area. Every frame, the only work is issuing draw calls for what is visible.
- **Less is drawn out of view and far away.** Chunk columns outside the screen are not drawn. Beyond `gridDistance` (16 blocks by default), neighbouring tiles of the same color are merged into one, which greatly reduces the vertex count.
- **The spawn check is vanilla's own.** Floor checks (`isValidSpawn`) and space checks (`NaturalSpawner.isValidEmptySpawnBlock`) call vanilla functions, and like vanilla, a spot counts only if the mob's body does not collide with blocks (so soul sand, mud, carpets and fences are not spawn spots). The light conditions are read from the dimension settings, so the result is also correct in the Nether and other dimensions.

### Controls

| Key | Action |
|---|---|
| F9 | Toggle the overlay on/off |
| F10 | Switch between tiles and crosses |

Keys can be changed in Options → Controls.

Commands:

- `/lightlite stats`: show LightLite's own cost (scan ms/tick, render µs/frame, GPU memory, and so on).
- `/lightlite resetstats`: reset the measurements.
- `/lightlite reload`: re-read `config/lightlite.json`.

### Settings

If Mod Menu is installed, the settings screen opens from the mod list. Every setting can also be changed in `config/lightlite.json`.

| Setting | Default | Description |
|---|---|---|
| `enabled` | `true` | Overlay on/off |
| `mode` | `TILE` | `TILE` (tiles) / `CROSS` (crosses) |
| `horizontalRange` | `32` | Horizontal range (8–128 blocks; rounded up to a multiple of 16) |
| `verticalRange` | `16` | Vertical range up and down (4–64 blocks) |
| `alwaysColor` / `nightColor` | `70FF2A2A` / `70FFD21E` | Colors as ARGB hex |
| `tickBudgetMs` | `1.5` | Maximum scan time per tick |
| `backend` | `AUTO` | `RETAINED` (kept on the GPU, fastest) / `IMMEDIATE` (most compatible) / `AUTO` (`RETAINED`; falls back to `IMMEDIATE` if it fails) |
| `gridDistance` | `16` | Within this distance (blocks), each block gets its own tile or cross; beyond it, neighbouring blocks are merged into one tile (0 always merges) |
| `onlyWhenHoldingLight` | `false` | Show the overlay, and scan, only while holding a light-emitting block |
| `excludedBiomes` | Mushroom Fields, Deep Dark | Biomes where hostile mobs do not spawn naturally |

### Assumptions and limits

- The check is for ground mobs that need 2 blocks of height (zombies, skeletons, and so on). Mobs with other body shapes such as spiders, slime chunks, and structure-specific spawns are not considered.
- Magma blocks are treated as floors where mobs cannot spawn, following the zombie rules.
- The range works in sections (16 blocks).
- Light level numbers are not shown.
- With an Iris shader pack, the markers are drawn by the pack's basic program, the same way as MiniHUD and Light Overlay. Depending on the pack, their colors and brightness can look slightly different from those without shaders.

### Requirements

- Minecraft 26.1.2, 26.2 or 26.3
- Fabric Loader 0.19.5 or later
- Fabric API
- Java 25
- Mod Menu (optional)

### Build

```
./gradlew buildAndCollect
```

The jars for all three versions are collected in `build/libs/<mod version>/`. Building several versions from one source uses [Stonecutter](https://stonecutter.kikugie.dev/).

### Performance comparison

See [docs/BENCHMARK.md](docs/BENCHMARK.md) for how to compare the cost of overlay mods and for the automated benchmark against other spawn overlay mods (Light Overlay, MiniHUD, Lighty). In the automated benchmark, LightLite added the least cost in every scenario and every version. This is a relative comparison with software rendering, so values on real hardware will differ.

### Publishing

The steps and listing text for publishing on CurseForge are in [docs/CURSEFORGE.md](docs/CURSEFORGE.md).

### License

MIT

---

## 日本語

### 概要

敵モブが湧けるブロックの上に色付きのマーカーを表示します。

| 色 | 意味 |
|---|---|
| 赤 | 昼でも夜でも湧く（空の光も足りていない） |
| 黄 | 夜（や雷雨）の間だけ湧く |

表示は半透明のタイル（既定）と X 印を切り替えられます。

### 軽さのための設計

- **変化があった所だけを計算する。** 16×16×16 のセクション単位で結果を保存し、ブロックや明るさが変わったセクションだけを計算し直します。変化はバニラがチャンク描画を更新するときの通知から拾います。
- **1ティックの処理時間に上限がある。** スキャンは既定で 1ティックあたり 1.5ms までで、プレイヤーに近い所から処理します。
- **毎フレームの頂点生成がない。** マーカーは 64×64 ブロックの領域ごとにGPUのバッファへ一度だけ送ります。毎フレームの処理は、見えている範囲の描画命令だけです。
- **見えない所・遠い所は描く量を減らす。** 画面外のチャンク列は描きません。`gridDistance`（既定16ブロック）より遠くは、隣り合う同じ色のタイルを1枚にまとめ、頂点数を大きく減らします。
- **判定はバニラそのもの。** 足場の判定（`isValidSpawn`）と空間の判定（`NaturalSpawner.isValidEmptySpawnBlock`）はバニラの関数を使います。バニラと同じく、モブの体がブロックにぶつかる場所は除きます（ソウルサンド・泥・カーペット・フェンスなどは湧く場所になりません）。明るさの条件はディメンションの設定から読むので、ネザーなどでも正しく判定します。

### 操作

| キー | 動作 |
|---|---|
| F9 | 表示の ON/OFF |
| F10 | タイル / X 印の切り替え |

キーは「設定 → 操作設定」で変更できます。

コマンド:

- `/lightlite stats`: LightLite 自身の処理負荷（スキャン ms/tick、描画 µs/frame、GPU使用量など）を表示します。
- `/lightlite resetstats`: 計測値をリセットします。
- `/lightlite reload`: `config/lightlite.json` を読み直します。

### 設定

Mod Menu が入っていれば、MOD一覧から設定画面を開けます。すべての項目は `config/lightlite.json` でも変更できます。

| 項目 | 既定値 | 説明 |
|---|---|---|
| `enabled` | `true` | 表示の ON/OFF |
| `mode` | `TILE` | `TILE`（タイル）/ `CROSS`（X印） |
| `horizontalRange` | `32` | 水平方向の表示範囲（8〜128ブロック。実際は16ブロック単位に切り上げ） |
| `verticalRange` | `16` | 上下方向の表示範囲（4〜64ブロック） |
| `alwaysColor` / `nightColor` | `70FF2A2A` / `70FFD21E` | ARGB の16進数 |
| `tickBudgetMs` | `1.5` | 1ティックあたりのスキャン時間の上限 |
| `backend` | `AUTO` | `RETAINED`（GPU保持、最速）/ `IMMEDIATE`（互換性重視）/ `AUTO`（`RETAINED`。失敗したら `IMMEDIATE` に切り替え） |
| `gridDistance` | `16` | この距離（ブロック）以内は1マスずつ（タイルまたはX印）、それより遠くは隣り合うマスを1枚のタイルにまとめて描く（0で常にまとめる） |
| `onlyWhenHoldingLight` | `false` | 光源ブロックを手に持っている時だけ表示し、スキャンもその間だけ行う |
| `excludedBiomes` | キノコ島、ディープダーク | 敵が自然に湧かないバイオーム |

### 判定の前提と制限

- 基準は「2マスの高さが必要な地上モブ」（ゾンビ、スケルトンなど）です。クモなど体の形が違うモブ、スライムチャンク、構造物固有の湧きは考慮しません。
- マグマブロックはゾンビ基準では湧かない足場として扱います。
- 表示範囲はセクション（16ブロック）単位です。
- 明るさの数値は表示しません。
- Iris のシェーダーパックを使っているときは、MiniHUD や Light Overlay と同じく、パックの basic プログラムでマーカーを描きます。パックによっては、シェーダーなしのときと色や明るさが少し違って見えます。

### 必要なもの

- Minecraft 26.1.2 / 26.2 / 26.3 のいずれか
- Fabric Loader 0.19.5 以上
- Fabric API
- Java 25
- Mod Menu（任意）

### ビルド

```
./gradlew buildAndCollect
```

3バージョン分の jar が `build/libs/<MODバージョン>/` にまとめて出力されます。複数バージョンのビルドには [Stonecutter](https://stonecutter.kikugie.dev/) を使っています。

### 軽さの比較

軽さの比較方法と、他の湧き潰し表示MOD（Light Overlay / MiniHUD / Lighty）との自動ベンチマークの結果は [docs/BENCHMARK.md](docs/BENCHMARK.md) を参照してください。自動ベンチマークでは、どの場面・どのバージョンでも LightLite の追加負荷が最も小さい結果でした。ただしソフトウェア描画での相対比較なので、実機の値とは異なります。

### 公開

CurseForge への公開の手順と掲載文は [docs/CURSEFORGE.md](docs/CURSEFORGE.md) にあります。

### ライセンス

MIT
