# 軽さの比較手順（実機）

LightLite と他の湧き潰し表示MODの負荷を、同じ条件で比べるための手順です。CI 上の自動ベンチはソフトウェア描画で動くので、GPU の負荷が実機と違います。最終的な判断は、この実機手順の結果で行ってください。

## 比較対象（26.x に対応した Fabric 版があるもの）

| MOD | 必要な依存MOD | 表示の有効化 |
|---|---|---|
| LightLite | Fabric API | F9 |
| Light Overlay（lugosieben 版） | OverlayLib、YACL | F9（LightLite と同じキーなので、どちらかを変更する） |
| Lighty | Fabric API | 既定キー、または Mod Menu から |
| MiniHUD | MaLiLib | 設定画面で `Light level` のオーバーレイを有効にする |

- shedaniel 版の Light Overlay には 26.x 版がないので、比較できません。
- 依存MODも含めて入れた状態で測ってください。利用者が実際に入れる構成で比べるためです。

## 条件をそろえる

1. 毎回まったく同じ構成で起動します。
   - MOD は「比較するMOD 1つ + その依存MOD + Fabric API（+ Sodium を使うなら Sodium）」だけにする。
   - 描画距離、シミュレーション距離、画面サイズ、フルスクリーンかどうか、FPS 上限（「無制限」にする）、VSync（OFF）をそろえる。
2. 同じワールドを使います。シードを決めて新しく作るか、同じワールドフォルダをコピーして使います。
3. 同じ位置と向きから測ります。
   - `/tp @s <x> <y> <z> <yaw> <pitch>` で移動する。
   - 湧き潰し前の広い洞窟や地下の平地など、マーカーが数千個出る場所を選ぶ。
4. 表示範囲はできるだけ近づけます。LightLite は `horizontalRange` / `verticalRange` で合わせます。

## 測る

測定は次の3つの状態で行います。

- MODなし
- 表示OFF（MODは入っているが表示していない）
- 表示ON

それぞれ次の手順で測ります。

1. 位置についたら 30 秒待ちます。チャンクの読み込みと初回スキャンを終わらせるためです。
2. **F3** のデバッグ画面で、FPS とフレーム時間のグラフを 60 秒ほど記録します。平均と最低値をメモします。
3. **spark** を入れている場合は、`/spark profiler start --timeout 60` を実行します。レポートで各MODのパッケージが使ったCPU時間の割合を見ます。
   - LightLite: `io.github.radishsprouts.lightlite`
   - Light Overlay: `dev.lugo.lightoverlay` など
4. LightLite の場合は、`/lightlite resetstats` → 60 秒待つ → `/lightlite stats` で自身の負荷も記録します。
5. 動いているときの負荷も測ります。同じ経路を `/tp` で順番に移動するか、エリトラで直線に飛ぶなどして、F3 のフレーム時間の最大値を比べます。

## 記録表の例

| 条件 | 平均FPS | 1% Low | spark のMOD割合 | 備考 |
|---|---|---|---|---|
| MODなし | | | - | |
| LightLite 表示OFF | | | | |
| LightLite 表示ON | | | | `/lightlite stats` の値 |
| Light Overlay 表示ON | | | | |
| Lighty 表示ON | | | | |
| MiniHUD 表示ON | | | | |

## `/lightlite stats` の見方

- `scan`: 1ティックあたりのスキャン時間です。
  - 平均値は、普段はほぼ 0 になります（変化がない所は再計算しないため）。
  - 瞬間値（peak）が `tickBudgetMs`（既定 1.5ms）を大きく超えることはありません。
- `render`: 1フレームあたりの描画にかかる CPU 時間です。メッシュの再構築時間も含みます。
- `draw calls` / `quads`: 1フレームあたりの描画命令の数と、描いた四角形の数です。画面外のチャンク列は描かない（`columns culled` の分）ので、見ている方向によって変わります。
- `gpu`: GPU バッファに置いているマーカーのデータ量です。

## 自動ベンチマークの結果（参考値）

`OverlayBenchmark`（client gametest）で測った結果です。

### 計測条件
- **環境:** Xvfb と llvmpipe によるソフトウェア描画。GPU の仕事も CPU で肩代わりするので、頂点数や塗る面積の負荷が実機より大きく出ます。ここでの数値は**相対比較**です。
- **ワールド:** 平坦ワールドの地上で、600ティック測りました。数値は「同じ回に測った『MODなし』との1フレームあたりの差（ms）」です。
- **場面:**
  - 止まった場面: その場で1回転する。
  - 移動: 1ティックに0.5ブロックずつ直進する。
  - ブロック変化: 4ティックごとに周囲へ松明を置く／外す。
- **比較MODの版（Modrinth から取得し、SHA-512 を照合済み）:**

  | MOD | 26.1.2 | 26.2 | 26.3 |
  |---|---|---|---|
  | Light Overlay（lugosieben） | 2.9.2 | 2.12.0 | 2.12.0 |
  | MiniHUD | 0.39.11 | 0.40.7 | 0.41.1 |
  | Lighty | 4.0.1 | 4.0.1 | 4.0.1 |

- **比較MODの設定:** どれも既定のまま、依存MOD込みで測りました。
  - Light Overlay: X印、4チャンク
  - MiniHUD: 範囲24
  - 比較MODの値は1回だけ計測した記録で、LightLite の値は変更のたびに測り直しています。

### 止まった場面（1フレームあたりの増加、ms）

| 条件 | 26.1.2 | 26.2 | 26.3 |
|---|---|---|---|
| **LightLite 既定（タイル、範囲32）** | **+1.06** | **+1.10** | **+1.10** |
| LightLite タイル、範囲64 | +0.76 | +1.48 | +1.92 |
| LightLite X印、範囲64 | +0.56 | +1.23 | +1.47 |
| Light Overlay | +3.65 | +3.49 | +4.68 |
| MiniHUD | +3.48 | +3.66 | +6.97 |
| Lighty | +34.3 | +35.4 | +35.2 |

### 移動・ブロック変化（26.1.2、1フレームあたりの増加、ms）

| 場面 | LightLite | Light Overlay | MiniHUD | Lighty |
|---|---|---|---|---|
| 移動 | **+0.90** | +3.33 | +3.21 | +26.7 |
| ブロック変化 | **+0.23** | +4.71 | +2.99 | +26.8 |

どの場面でも、ティックの処理時間の増加は 0.05ms 以下でした。

### MOD 自身の CPU 使用（26.1.2、JFR）
- ゲームループ中のサンプルのうち、各MODのコードが動いていた割合です。GPU ドライバ内の処理は含みません。
- この計測は、遠くのタイルをまとめる改良の前に取ったものです。

| MOD | ゲームループ内の割合 | 別スレッド |
|---|---|---|
| LightLite 既定 | 0.45% | ほぼなし |
| MiniHUD | 10.9% | 少量 |
| Light Overlay | 14.8% | OverlayLib の専用スレッドで常時処理 |
| Lighty | 0% | ワーカースレッドで少量（重さは描画側） |

### LightLite が軽い理由
- **スキャン:** 変化したセクションだけを計算し直すので、平均は約 0.001ms/tick です。
- **描画:**
  - GPU に保持したメッシュを、見えているチャンク列の分だけ描きます。
  - `gridDistance` より遠くは、隣り合うマスを1枚にまとめます。
  - その結果、既定の範囲の平坦ワールドで、描く四角形はマーカー 6,400個に対して 1,289枚です。

### 再計測の方法

```
./gradlew :26.1.2:runClientGameTest -PlightliteBench=lightlite [-PlightliteBenchRange=64] [-PlightliteBenchMode=cross] [-PlightliteBenchScenario=static|move|edit]
```

- 比較MODを測るときは、そのMODの jar と依存MODを `bench-mods/<バージョン>/<target>/` に置き、`-PlightliteBench=lightoverlay|minihud|lighty` を指定します。
- 26.3 を Xvfb 上で動かすときは、`SDL_VIDEO_FORCE_EGL=1` が必要です。
- 結果は `build/bench/results.jsonl` に出力されます。
