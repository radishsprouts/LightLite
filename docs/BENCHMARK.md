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
- `draw calls`: 1フレームあたりの描画命令の数です。表示範囲内の領域（64×64ブロック）の数と同じになります。
- `gpu`: GPU バッファに置いているマーカーのデータ量です。
