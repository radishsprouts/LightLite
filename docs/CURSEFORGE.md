# Publishing on CurseForge / CurseForge への公開

[日本語は下にあります](#日本語)

## English

### Steps

1. Read CurseForge's rules for authors, the [Moderation Policies](https://support.curseforge.com/support/solutions/articles/9000197279-moderation-policies) and the [Mod Authors Terms](https://legal.overwolf.com/docs/curseforge/mod-authors-terms/), and check what they say about AI-generated content.
2. Create the project on CurseForge (Minecraft → Mods), using the fields and text in "Project fields" below.
3. Note the numeric **Project ID** shown on the project page.
4. Create an API token for uploads. It is under API Tokens in your CurseForge account settings (historically https://legacy.curseforge.com/account/api-tokens).
5. In the GitHub repository, go to Settings → Secrets and variables → Actions and add two entries:
   - **Secrets** tab: `CURSEFORGE_TOKEN`, set to the token from step 4. Do not paste it into a chat or anywhere else.
   - **Variables** tab: `CURSEFORGE_PROJECT_ID`, set to the Project ID from step 3.
6. To upload the already released v0.1.0, go to Actions → **CurseForge** → Run workflow and enter `v0.1.0`.
   - Future releases are uploaded to CurseForge automatically at the end of the Release workflow.
   - If either of the two settings is missing, the CurseForge upload is skipped. The workflow does not fail.
   - The upload API cannot list files that are already on CurseForge. Running the workflow twice for the same tag uploads the files twice.
7. New projects and files are reviewed by CurseForge before they become public.

### What the workflow uploads

- One file per Minecraft version: `LightLite 0.1.0+26.1.2`, `LightLite 0.1.0+26.2` and `LightLite 0.1.0+26.3`. The files are the jars attached to the GitHub release.
- Game versions of each file: its Minecraft version, Fabric, Client and Java 25. Their CurseForge IDs are looked up when the workflow runs.
- Release type: `beta` for `v0.x` tags, `release` otherwise.
- Relations: Fabric API (`fabric-api`, required dependency) and Mod Menu (`modmenu`, optional dependency).
- Changelog: the section for that version in `CHANGELOG.md` (English and Japanese), followed by a link to the GitHub release.

### Project fields

| Field | Value |
|---|---|
| Game / class | Minecraft / Mods |
| Name | `LightLite` |
| Summary | See below |
| Main category | Utility & QoL |
| Additional category | Map and Information |
| License | MIT License |
| Source | `https://github.com/radishsprouts/LightLite` |
| Issues | `https://github.com/radishsprouts/LightLite/issues` |
| Logo | None |
| Description | See "Description text" at the end of this file |

#### Summary

The Summary is a single field, so it holds one English sentence followed by one Japanese sentence:

```
Lightweight mob-spawn overlay: marks blocks where hostile mobs can spawn (red: always, yellow: at night). 軽量な湧き潰し確認MOD。
```

#### Description

The Description is also a single field. Choose Markdown as the format and paste the whole block in "Description text" at the end of this file. It contains the full English text followed by the full Japanese text.

---

## 日本語

### 手順

1. CurseForge の作者向けの規約（[Moderation Policies](https://support.curseforge.com/support/solutions/articles/9000197279-moderation-policies) と [Mod Authors Terms](https://legal.overwolf.com/docs/curseforge/mod-authors-terms/)）を読み、AI で生成した内容についての記述を確認します。
2. CurseForge でプロジェクトを作成します（Minecraft → Mods）。下の「プロジェクトの入力内容」の項目と文章を使います。
3. プロジェクトのページに表示される数字の **Project ID** を控えます。
4. アップロード用の API トークンを作ります。CurseForge のアカウント設定の API Tokens にあります（以前の場所は https://legacy.curseforge.com/account/api-tokens ）。
5. GitHub リポジトリの Settings → Secrets and variables → Actions で、次の2つを登録します。
   - **Secrets** タブ: `CURSEFORGE_TOKEN` に手順4のトークンを入れます。チャットなどには貼らないでください。
   - **Variables** タブ: `CURSEFORGE_PROJECT_ID` に手順3の Project ID を入れます。
6. 公開済みの v0.1.0 を上げるときは、Actions → **CurseForge** → Run workflow で `v0.1.0` を指定します。
   - 以後のリリースでは、Release ワークフローの最後に自動で CurseForge へ上がります。
   - 2つの設定のどちらかがなければ、CurseForge への公開は飛ばされます。ワークフローは失敗しません。
   - アップロード用の API では、CurseForge にすでにあるファイルの一覧を取れません。同じタグで2回実行すると、ファイルが2回上がります。
7. 新しいプロジェクトとファイルは、CurseForge の審査を経てから公開されます。

### ワークフローがアップロードする内容

- Minecraft のバージョンごとに1ファイル: `LightLite 0.1.0+26.1.2`、`LightLite 0.1.0+26.2`、`LightLite 0.1.0+26.3`。ファイルは GitHub のリリースに添付された jar です。
- 各ファイルのゲームバージョン: その Minecraft のバージョン、Fabric、Client、Java 25。CurseForge 上の ID は、ワークフローの実行時に調べます。
- リリースの種類: `v0.x` のタグは `beta`、それ以外は `release`。
- 関連プロジェクト: Fabric API（`fabric-api`、必須の依存）、Mod Menu（`modmenu`、任意の依存）。
- 変更履歴: `CHANGELOG.md` のそのバージョンの節（英語と日本語）と、GitHub のリリースへのリンク。

### プロジェクトの入力内容

| 項目 | 値 |
|---|---|
| ゲーム / 種類 | Minecraft / Mods |
| 名前 | `LightLite` |
| Summary | 下記を参照 |
| メインカテゴリ | Utility & QoL |
| 追加カテゴリ | Map and Information |
| ライセンス | MIT License |
| ソース | `https://github.com/radishsprouts/LightLite` |
| Issues | `https://github.com/radishsprouts/LightLite/issues` |
| ロゴ | なし |
| 説明文 | このファイルの最後の「Description text」を参照 |

#### Summary

Summary は1つの欄なので、英語の文と日本語の文を1つずつ、英語を先にして入れます。

```
Lightweight mob-spawn overlay: marks blocks where hostile mobs can spawn (red: always, yellow: at night). 軽量な湧き潰し確認MOD。
```

#### 説明文

説明文も1つの欄です。形式は Markdown を選び、このファイルの最後の「Description text」のブロックを丸ごと貼り付けます。英語の全文のあとに日本語の全文が続きます。

---

## Description text


```markdown
**English** | **日本語** (below / 下にあります)

# English

## Overview

LightLite shows colored markers on the blocks where hostile mobs can spawn.

| Color | Meaning |
|---|---|
| Red | Mobs can spawn by day and by night (the sky light is not enough either) |
| Yellow | Mobs can spawn only at night (or during thunderstorms) |

Markers can be translucent tiles (the default) or crosses.

## How it stays light

- **Only what changed is recalculated.** Results are stored per 16×16×16 section, and only sections whose blocks or light changed are recalculated. Changes are picked up from the notifications vanilla uses to update chunk rendering.
- **Each tick has a time limit.** By default, scanning uses at most 1.5 ms per tick, starting with the sections closest to the player.
- **No vertices are built every frame.** Markers are sent to a GPU buffer once for each 64×64-block area. Every frame, the only work is issuing draw calls for what is visible.
- **Less is drawn out of view and far away.** Chunk columns outside the screen are not drawn. Beyond `gridDistance` (16 blocks by default), neighbouring tiles of the same color are merged into one, which greatly reduces the vertex count.
- **The spawn check is vanilla's own.** Floor checks (`isValidSpawn`) and space checks (`NaturalSpawner.isValidEmptySpawnBlock`) call vanilla functions. The light conditions are read from the dimension settings, so the result is also correct in the Nether and other dimensions.

## Performance comparison

Automated benchmark with software rendering (Xvfb and llvmpipe), on the surface of a superflat world. Each number is the added time per frame compared with playing without the mod, in ms. The compared mods were measured with their default settings and their required mods. Because the CPU also does the GPU's work here, this is a relative comparison; values on real hardware will differ.

Static (turning around once in place):

| Condition | 26.1.2 | 26.2 | 26.3 |
|---|---|---|---|
| **LightLite default (tiles, range 32)** | **+1.06** | **+1.10** | **+1.10** |
| LightLite tiles, range 64 | +0.76 | +1.48 | +1.92 |
| LightLite crosses, range 64 | +0.56 | +1.23 | +1.47 |
| Light Overlay (lugosieben) | +3.65 | +3.49 | +4.68 |
| MiniHUD | +3.48 | +3.66 | +6.97 |
| Lighty | +34.3 | +35.4 | +35.2 |

Moving and block edits (26.1.2):

| Scenario | LightLite | Light Overlay | MiniHUD | Lighty |
|---|---|---|---|---|
| Moving | **+0.90** | +3.33 | +3.21 | +26.7 |
| Block edits | **+0.23** | +4.71 | +2.99 | +26.8 |

The conditions, the mod versions and the steps for comparing on real hardware are in [docs/BENCHMARK.md](https://github.com/radishsprouts/LightLite/blob/main/docs/BENCHMARK.md).

## Controls

| Key | Action |
|---|---|
| F9 | Toggle the overlay on/off |
| F10 | Switch between tiles and crosses |

Keys can be changed in Options → Controls.

Commands:

- `/lightlite stats`: show LightLite's own cost (scan ms/tick, render µs/frame, GPU memory, and so on).
- `/lightlite resetstats`: reset the measurements.
- `/lightlite reload`: re-read `config/lightlite.json`.

## Settings

If Mod Menu is installed, the settings screen opens from the mod list. Every setting can also be changed in `config/lightlite.json`.

| Setting | Default | Description |
|---|---|---|
| `enabled` | `true` | Overlay on/off |
| `mode` | `TILE` | `TILE` (tiles) / `CROSS` (crosses) |
| `horizontalRange` | `32` | Horizontal range (8–128 blocks; rounded up to a multiple of 16) |
| `verticalRange` | `16` | Vertical range up and down (4–64 blocks) |
| `alwaysColor` / `nightColor` | `70FF2A2A` / `70FFD21E` | Colors as ARGB hex |
| `tickBudgetMs` | `1.5` | Maximum scan time per tick |
| `backend` | `AUTO` | `RETAINED` (kept on the GPU, fastest) / `IMMEDIATE` (most compatible) / `AUTO` (`IMMEDIATE` if Iris is installed) |
| `gridDistance` | `16` | Within this distance (blocks), each block gets its own tile or cross; beyond it, neighbouring blocks are merged into one tile (0 always merges) |
| `onlyWhenHoldingLight` | `false` | Show the overlay, and scan, only while holding a light-emitting block |
| `excludedBiomes` | Mushroom Fields, Deep Dark | Biomes where hostile mobs do not spawn naturally |

## Assumptions and limits

- The check is for ground mobs that need 2 blocks of height (zombies, skeletons, and so on). Mobs with other body shapes such as spiders, slime chunks, and structure-specific spawns are not considered.
- Magma blocks are treated as floors where mobs cannot spawn, following the zombie rules.
- The range works in sections (16 blocks).
- Light level numbers are not shown.

## Requirements

- Minecraft 26.1.2, 26.2 or 26.3
- Fabric Loader 0.19.5 or later
- Fabric API
- Java 25
- Mod Menu (optional)

## Source code and license

- Source code: https://github.com/radishsprouts/LightLite
- Bug reports: https://github.com/radishsprouts/LightLite/issues
- License: MIT

---

# 日本語

## 概要

敵モブが湧けるブロックの上に色付きのマーカーを表示します。

| 色 | 意味 |
|---|---|
| 赤 | 昼でも夜でも湧く（空の光も足りていない） |
| 黄 | 夜（や雷雨）の間だけ湧く |

表示は半透明のタイル（既定）と X 印を切り替えられます。

## 軽さのための設計

- **変化があった所だけを計算する。** 16×16×16 のセクション単位で結果を保存し、ブロックや明るさが変わったセクションだけを計算し直します。変化はバニラがチャンク描画を更新するときの通知から拾います。
- **1ティックの処理時間に上限がある。** スキャンは既定で 1ティックあたり 1.5ms までで、プレイヤーに近い所から処理します。
- **毎フレームの頂点生成がない。** マーカーは 64×64 ブロックの領域ごとにGPUのバッファへ一度だけ送ります。毎フレームの処理は、見えている範囲の描画命令だけです。
- **見えない所・遠い所は描く量を減らす。** 画面外のチャンク列は描きません。`gridDistance`（既定16ブロック）より遠くは、隣り合う同じ色のタイルを1枚にまとめ、頂点数を大きく減らします。
- **判定はバニラそのもの。** 足場の判定（`isValidSpawn`）と空間の判定（`NaturalSpawner.isValidEmptySpawnBlock`）はバニラの関数を使います。明るさの条件はディメンションの設定から読むので、ネザーなどでも正しく判定します。

## 軽さの比較

ソフトウェア描画（Xvfb と llvmpipe）での自動ベンチマークで、平坦ワールドの地上で測りました。数値は、MODなしと比べた1フレームあたりの増加（ms）です。比較MODは既定の設定のまま、依存MOD込みで測りました。ここでは GPU の仕事も CPU で肩代わりするので相対比較であり、実機での値とは異なります。

止まった場面（その場で1回転する）:

| 条件 | 26.1.2 | 26.2 | 26.3 |
|---|---|---|---|
| **LightLite 既定（タイル、範囲32）** | **+1.06** | **+1.10** | **+1.10** |
| LightLite タイル、範囲64 | +0.76 | +1.48 | +1.92 |
| LightLite X印、範囲64 | +0.56 | +1.23 | +1.47 |
| Light Overlay（lugosieben 版） | +3.65 | +3.49 | +4.68 |
| MiniHUD | +3.48 | +3.66 | +6.97 |
| Lighty | +34.3 | +35.4 | +35.2 |

移動とブロック変化（26.1.2）:

| 場面 | LightLite | Light Overlay | MiniHUD | Lighty |
|---|---|---|---|---|
| 移動 | **+0.90** | +3.33 | +3.21 | +26.7 |
| ブロック変化 | **+0.23** | +4.71 | +2.99 | +26.8 |

計測条件、比較MODの版、実機での比較手順は [docs/BENCHMARK.md](https://github.com/radishsprouts/LightLite/blob/main/docs/BENCHMARK.md) にあります。

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
| `backend` | `AUTO` | `RETAINED`（GPU保持、最速）/ `IMMEDIATE`（互換性重視）/ `AUTO`（Iris があれば `IMMEDIATE`） |
| `gridDistance` | `16` | この距離（ブロック）以内は1マスずつ（タイルまたはX印）、それより遠くは隣り合うマスを1枚のタイルにまとめて描く（0で常にまとめる） |
| `onlyWhenHoldingLight` | `false` | 光源ブロックを手に持っている時だけ表示し、スキャンもその間だけ行う |
| `excludedBiomes` | キノコ島、ディープダーク | 敵が自然に湧かないバイオーム |

## 判定の前提と制限

- 基準は「2マスの高さが必要な地上モブ」（ゾンビ、スケルトンなど）です。クモなど体の形が違うモブ、スライムチャンク、構造物固有の湧きは考慮しません。
- マグマブロックはゾンビ基準では湧かない足場として扱います。
- 表示範囲はセクション（16ブロック）単位です。
- 明るさの数値は表示しません。

## 必要なもの

- Minecraft 26.1.2 / 26.2 / 26.3 のいずれか
- Fabric Loader 0.19.5 以上
- Fabric API
- Java 25
- Mod Menu（任意）

## ソースコードとライセンス

- ソースコード: https://github.com/radishsprouts/LightLite
- 不具合の報告: https://github.com/radishsprouts/LightLite/issues
- ライセンス: MIT
```
