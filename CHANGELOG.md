# Changelog / 変更履歴

## Unreleased

### English

- Fixed red markers on soul sand and mud even right next to a torch. Like vanilla, a spot now counts only if the mob's body does not collide with blocks, which also removes false markers on carpets, fences, farmland and similar blocks.

### 日本語

- 松明のすぐ隣でも、ソウルサンドと泥の上に赤いマーカーが出ていた不具合を直しました。バニラと同じく、モブの体がブロックにぶつかる場所は湧く場所として扱わなくなりました。カーペット・フェンス・耕地などに出ていた誤ったマーカーも出なくなります。

## 0.1.0 (2026-09-26)

### English

- First release for Minecraft 26.1.2, 26.2 and 26.3 (Fabric).
- Spawn overlay with red (spawns at any time) and yellow (spawns only at night) markers, shown as tiles or crosses.
- Scanning is cached per section, updated from vanilla's block and light change notifications, and limited in time per tick.
- Region meshes are kept on the GPU, and chunk columns outside the view are not drawn. Beyond `gridDistance`, neighbouring markers are merged into rectangles. With shader mods, an immediate fallback is used instead.
- Mod Menu settings screen, `config/lightlite.json`, and the `/lightlite stats`, `/lightlite resetstats` and `/lightlite reload` commands.

### 日本語

- Minecraft 26.1.2 / 26.2 / 26.3（Fabric）向けの最初のリリースです。
- 湧き潰しの表示: 赤（いつでも湧く）と黄（夜だけ湧く）のマーカーを、タイルか X 印で表示します。
- スキャン結果はセクション単位で保存し、バニラのブロックと明るさの変化の通知で更新します。1ティックあたりの処理時間に上限があります。
- 領域ごとのメッシュを GPU に保持し、画面外のチャンク列は描きません。`gridDistance` より遠くは、隣り合うマーカーを長方形にまとめます。シェーダーMODを使っているときは、代わりに immediate 方式で描きます。
- Mod Menu の設定画面、`config/lightlite.json`、コマンド `/lightlite stats`・`/lightlite resetstats`・`/lightlite reload`。
