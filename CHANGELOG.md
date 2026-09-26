# Changelog / 変更履歴

## 0.1.1 (unreleased)

### English

- Fixed: the overlay was not shown while an Iris shader pack was active. The markers now draw with LightLite's own pipeline, which is assigned to the shader pack's basic program through the Iris API.
- The `AUTO` renderer now uses the GPU-retained path with Iris installed too. It still falls back to the immediate path if the retained path fails.

### 日本語

- 修正: Iris のシェーダーパックを有効にしているとき、オーバーレイが表示されなかった。LightLite 専用のパイプラインで描くようにし、Iris の API でシェーダーパックの basic プログラムに割り当てるようにした。
- 描画方式の `AUTO` は、Iris が入っていても GPU 保持方式を使うようにした。GPU 保持方式が失敗したときは、これまでどおり逐次描画に切り替わる。

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
