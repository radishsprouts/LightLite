# Modrinth 公開の準備 / Publishing on Modrinth

## 1. 手順 / Steps

**日本語**

1. Modrinth でプロジェクトを作成します（下の「2. プロジェクトの入力内容」を使います）。アイコンは設定しません。
2. Modrinth の Settings → Personal access tokens で、`Create versions` の権限だけを持つトークンを作ります。
3. GitHub リポジトリの Settings → Secrets and variables → Actions で、次の2つを登録します。
   - **Secrets** タブ: `MODRINTH_TOKEN` = 手順2のトークン（チャットなどには貼らないでください）
   - **Variables** タブ: `MODRINTH_PROJECT_ID` = プロジェクトの ID またはスラッグ（例: `lightlite`）
4. 公開済みの v0.1.0 を上げるときは、Actions → **Modrinth** → Run workflow で `v0.1.0` を指定します。
   - 以後のリリースでは、Release ワークフローの最後に自動で Modrinth へ上がります。
   - 2つの設定のどちらかがなければ、Modrinth への公開は飛ばされます（ワークフローは失敗しません）。
5. バージョンが3つ（26.1.2 / 26.2 / 26.3）そろったら、Modrinth でプロジェクトを審査に提出します。

**English**

1. Create the project on Modrinth, using the text in section 2 below. Do not set an icon.
2. On Modrinth, go to Settings → Personal access tokens and create a token with only the `Create versions` scope.
3. In the GitHub repository, go to Settings → Secrets and variables → Actions and add two entries:
   - **Secrets** tab: `MODRINTH_TOKEN`, set to the token from step 2. Do not paste it into a chat.
   - **Variables** tab: `MODRINTH_PROJECT_ID`, set to the project ID or slug (for example `lightlite`).
4. To upload the already released v0.1.0, go to Actions → **Modrinth** → Run workflow and enter `v0.1.0`.
   - Future releases are uploaded to Modrinth automatically at the end of the Release workflow.
   - If either setting is missing, the Modrinth upload is skipped. The workflow does not fail.
5. Once all three versions (26.1.2, 26.2, 26.3) are uploaded, submit the project for review on Modrinth.

**What the workflow uploads / アップロード内容**

- One Modrinth version per Minecraft version / Minecraft のバージョンごとに1つ: `0.1.0+26.1.2`, `0.1.0+26.2`, `0.1.0+26.3`.
- Loader: Fabric.
- Release channel: `beta` for `v0.x`, `release` otherwise / `v0.x` は beta、それ以外は release.
- Dependencies / 依存: Fabric API (required / 必須), Mod Menu (optional / 任意).
- Changelog: links to `CHANGELOG.md` and the GitHub release / `CHANGELOG.md` と GitHub のリリースへのリンク.
- A version that is already on Modrinth is skipped, so re-running is safe / Modrinth に同じバージョンがあれば飛ばすので、再実行しても重複しません.

## 2. プロジェクトの入力内容 / Project fields

| 項目 / Field | 値 / Value |
|---|---|
| Project type | Mod |
| Name | `LightLite` |
| URL (slug) | `lightlite` |
| Visibility | Public |
| Categories | `utility` |
| Loaders | Fabric（バージョンのアップロード時に自動で設定 / set automatically by the uploaded versions） |
| Client side | Required |
| Server side | Unsupported |
| License | MIT |
| Source code | `https://github.com/radishsprouts/LightLite` |
| Issue tracker | `https://github.com/radishsprouts/LightLite/issues` |
| Icon | なし / None |

### Summary

Modrinth の Summary は1つの欄なので、英語と日本語を1文ずつ入れます。 / The Summary is a single field, so it holds one English and one Japanese sentence.

```
Lightweight mob-spawn overlay: marks blocks where hostile mobs can spawn (red: always, yellow: at night). 軽量な湧き潰し確認MOD。
```

### Description

```markdown
**English** | [日本語](#日本語)

LightLite marks the blocks where hostile mobs can spawn.

- **Red:** mobs can spawn at any time.
- **Yellow:** mobs can spawn only at night or during thunderstorms.

Markers are translucent tiles by default. You can switch to crosses.

## Why it is light

- **It only recalculates what changed.** Results are cached per 16×16×16 section. A section is rescanned only when vanilla reports a block or light change in it.
- **Scanning has a per-tick time limit.** By default it uses at most 1.5 ms per tick, nearest sections first.
- **It does not rebuild vertices every frame.** Markers are uploaded to the GPU once per 64×64 area. Each frame only issues draw calls for what is on screen.
- **It draws less far away.** Chunk columns outside the view are skipped. Beyond `gridDistance` (16 blocks by default), neighbouring tiles of the same color are merged into one.

In an automated benchmark with software rendering, the added frame time was +1.1 ms for LightLite, against +3.5 to +4.7 ms for Light Overlay (lugosieben) and +3.5 to +7.0 ms for MiniHUD. This is a relative comparison; results on real hardware will differ. Details and the method are in [docs/BENCHMARK.md](https://github.com/radishsprouts/LightLite/blob/main/docs/BENCHMARK.md).

## Controls

- **F9:** toggle the overlay
- **F10:** switch between tiles and crosses
- `/lightlite stats`: show LightLite's own cost

Settings are in Mod Menu (optional) or `config/lightlite.json`.

## Limits

- The check is for 2-block-tall ground mobs such as zombies and skeletons. Spiders, slime chunks and structure-specific spawns are not covered.
- Light level numbers are not shown.

## Requirements

- Minecraft 26.1.2, 26.2 or 26.3
- Fabric Loader 0.19.5 or later
- Fabric API
- Mod Menu (optional)

---

## 日本語

敵モブが湧けるブロックの上にマーカーを表示します。

- **赤:** 昼でも夜でも湧く。
- **黄:** 夜（や雷雨）の間だけ湧く。

表示は半透明のタイルが既定で、X 印にも切り替えられます。

## 軽さのための設計

- **変化があった所だけを計算し直します。** 16×16×16 のセクションごとに結果を保存し、バニラからブロックや明るさの変化が通知されたセクションだけを再計算します。
- **1ティックの処理時間に上限があります。** 既定では 1ティックあたり 1.5ms までで、プレイヤーに近い所から処理します。
- **毎フレーム頂点を作り直しません。** マーカーは 64×64 の範囲ごとに一度だけ GPU に送り、毎フレームは画面に見えている分の描画命令を出すだけです。
- **遠くは描く量を減らします。** 画面外のチャンク列は描きません。`gridDistance`（既定16ブロック）より遠くは、隣り合う同じ色のタイルを1枚にまとめます。

ソフトウェア描画での自動ベンチマークでは、1フレームあたりの増加は LightLite が +1.1ms、Light Overlay（lugosieben 版）が +3.5〜4.7ms、MiniHUD が +3.5〜7.0ms でした。これは相対比較で、実機での値とは異なります。詳しい条件と方法は [docs/BENCHMARK.md](https://github.com/radishsprouts/LightLite/blob/main/docs/BENCHMARK.md) にあります。

## 操作

- **F9:** 表示の ON/OFF
- **F10:** タイルと X 印の切り替え
- `/lightlite stats`: LightLite 自身の負荷を表示

設定は Mod Menu（任意）か `config/lightlite.json` で変更できます。

## 判定の範囲

- 判定の基準は、ゾンビやスケルトンのような2マスの高さの地上モブです。クモ、スライムチャンク、構造物固有の湧きは対象外です。
- 明るさの数値は表示しません。

## 必要なもの

- Minecraft 26.1.2 / 26.2 / 26.3
- Fabric Loader 0.19.5 以上
- Fabric API
- Mod Menu（任意）
```
