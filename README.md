# LightLite

軽量な湧き潰し可視化 MOD（Fabric / Minecraft 26.1.2・26.2・26.3）  
Lightweight mob-spawn overlay for Fabric (Minecraft 26.1.2, 26.2, 26.3). [English below](#english)

## 概要

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
- **判定はバニラそのもの。** 足場の判定（`isValidSpawn`）と空間の判定（`NaturalSpawner.isValidEmptySpawnBlock`）はバニラの関数を使います。明るさの条件はディメンションの設定から読むので、ネザーなどでも正しく判定します。

## 操作

| キー | 動作 |
|---|---|
| F9 | 表示の ON/OFF |
| F10 | タイル / X 印の切り替え |

キーは「設定 → 操作設定」で変更できます。

コマンド:

- `/lightlite stats`: LightLite 自身の処理負荷（スキャン ms/tick、描画 µs/frame、GPU使用量など）を表示します。
- `/lightlite resetstats`: 計測値をリセットします。
- `/lightlite reload`: `config/lightlite.json` を読み直します。

## 設定

Mod Menu が入っていれば、MOD一覧から設定画面を開けます。すべての項目は `config/lightlite.json` でも変更できます。

| 項目 | 既定値 | 説明 |
|---|---|---|
| `enabled` | `true` | 表示の ON/OFF |
| `mode` | `TILE` | `TILE`（タイル）/ `CROSS`（X印） |
| `horizontalRange` | `32` | 水平方向の表示範囲（8〜128ブロック。実際は16ブロック単位に切り上げ） |
| `verticalRange` | `16` | 上下方向の表示範囲（4〜64ブロック） |
| `alwaysColor` / `nightColor` | `70FF2A2A` / `70FFD21E` | ARGB の16進数 |
| `tickBudgetMs` | `1.5` | 1ティックあたりのスキャン時間の上限 |
| `backend` | `AUTO` | `RETAINED`（GPU保持、最速）/ `IMMEDIATE`（互換性重視）/ `AUTO`（Iris があれば IMMEDIATE） |
| `gridDistance` | `16` | この距離（ブロック）以内は1マスずつ（タイルまたはX印）、それより遠くは隣り合うマスを1枚のタイルにまとめて描く（0で常にまとめる） |
| `onlyWhenHoldingLight` | `false` | 光源ブロックを手に持っている時だけ表示し、スキャンもその間だけ行う |
| `excludedBiomes` | キノコ島、ディープダーク | 敵が自然に湧かないバイオーム |

## 判定の前提と制限

- 基準は「2マスの高さが必要な地上モブ」（ゾンビ、スケルトンなど）です。クモなど体の形が違うモブ、スライムチャンク、構造物固有の湧きは考慮しません。
- マグマブロックはゾンビ基準では湧かない足場として扱います。
- 表示範囲はセクション（16ブロック）単位です。

## 必要なもの

- Minecraft 26.1.2 / 26.2 / 26.3 のいずれか
- Fabric Loader 0.19.5 以上
- Fabric API
- Java 25
- Mod Menu（任意）

## ビルド

```
./gradlew buildAndCollect
```

3バージョン分の jar が `build/libs/<MODバージョン>/` にまとめて出力されます。複数バージョンのビルドには [Stonecutter](https://stonecutter.kikugie.dev/) を使っています。

Modrinth への公開の手順と掲載文は [docs/MODRINTH.md](docs/MODRINTH.md) にあります。

軽さの比較方法と、他の湧き潰し表示MOD（Light Overlay / MiniHUD / Lighty）との自動ベンチマークの結果は [docs/BENCHMARK.md](docs/BENCHMARK.md) を参照してください。自動ベンチマークでは、どの場面・どのバージョンでも LightLite の追加負荷が最も小さい結果でした。ただしソフトウェア描画での相対比較なので、実機の値とは異なります。

---

## English

LightLite marks blocks where hostile mobs can spawn:

- **Red:** mobs can spawn at any time.
- **Yellow:** mobs can spawn only at night or during thunderstorms.

Markers can be translucent tiles (the default) or crosses.

It is built to be cheap:

- Results are cached per 16³ section. A section is rescanned only when vanilla reports a block or light change in it.
- Scanning is time-boxed per tick (1.5 ms by default), nearest sections first.
- Each 64×64 region's quads are uploaded to a GPU buffer once. After that, each frame only issues one draw call per region.

The spawn rules are vanilla's own: `isValidSpawn`, `NaturalSpawner.isValidEmptySpawnBlock`, and the light limits of the current dimension type.

**Keys:**

- F9: toggle the overlay
- F10: switch between tiles and crosses

**Commands:**

- `/lightlite stats`: show LightLite's own cost
- `/lightlite resetstats`: reset the measurements
- `/lightlite reload`: re-read `config/lightlite.json`

Settings are available through Mod Menu or in `config/lightlite.json` (see the table above).

**Requirements:** Minecraft 26.1.2, 26.2 or 26.3, Fabric Loader 0.19.5+, Fabric API and Java 25. Mod Menu is optional.

**License:** MIT
