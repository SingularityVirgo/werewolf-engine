# 可选背景图（Image2 / 本地生成）

项目默认使用 `bg-chamber.svg` 与 CSS 纹理，**无需图片即可运行**。若希望更写实的中世纪氛围，将生成图放入本目录并按下表命名，页面会自动叠加（见 `BrandBackdrop.tsx`）。

## 许可建议

- 自用 / 课题演示：AI 生成通常无商用纠纷
- 若开源发布：请使用自有生成图，或 Unsplash / Wikimedia **CC0 / CC-BY** 并保留署名

## 提示词（Image2）

### 1. `bg-chamber.webp`（Home / Setup 全屏背景）

```
Medieval stone chamber interior at night, empty round wooden table with twelve chairs implied but not shown, three wax candles with warm golden flame, deep shadows, green-black fog near floor, gothic arch in background, painterly oil texture, muted palette olive and umber, no people, no text, no modern objects, cinematic wide 16:9
```

负向：`neon, purple, cyberpunk, cartoon, anime, text, watermark, logo, bright daylight, modern furniture`

### 2. `bg-table-felt.webp`（Game 中央区 subtle 底纹，可选）

```
Top-down view of dark worn green felt on old wooden table, subtle fabric weave, candlelight rim light from one corner, macro texture, no cards, no objects, seamless tileable, muted, 1:1
```

负向：`text, logo, bright colors, plastic`

### 3. `parchment-scroll.webp`（Setup 板子卡 / 结算屏，可选）

```
Aged parchment paper texture, irregular edges, foxing spots, warm sepia, flat scan, no writing, no symbols, tileable center, high resolution
```

## 文件放置

```
frontend/public/assets/
  bg-chamber.webp      ← 覆盖在 SVG 之上，opacity ~0.45
  bg-table-felt.webp   ← Game 中间区（未实现自动检测时可手动接）
  parchment-scroll.webp
```

生成后建议 WebP，宽度 1920（背景）或 1024（纹理），压缩至 200–400 KB。
