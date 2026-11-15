---
version: alpha
name: mashangliang-nordic
description: "碼上亮的設計系統：以全域預設 DESIGN.md（中性灰階＋單一藍色重點）為基底，走北歐簡約：淺底、白卡片、細線分層、大量留白。條碼區一律純黑條＋純白底，收銀掃描器才好讀。"

colors:
  primary: "#1F2937"
  secondary: "#4B5563"
  accent: "#2563EB"
  on-accent: "#FFFFFF"
  neutral: "#F9FAFB"
  surface: "#FFFFFF"
  border: "#E5E7EB"
  error: "#DC2626"
  barcode-ink: "#000000"
  barcode-paper: "#FFFFFF"
  icon-1: "#3E5C76"
  icon-2: "#4F6B58"
  icon-3: "#8B5A3C"
  icon-4: "#6B4E71"
  icon-5: "#2F6B6B"
  icon-6: "#7A5C1E"

typography:
  h3:
    fontFamily: "Roboto, Noto Sans TC, system-ui, sans-serif"
    fontSize: 1.5rem
    fontWeight: 700
    lineHeight: 1.3
  body-md:
    fontFamily: "Roboto, Noto Sans TC, system-ui, sans-serif"
    fontSize: 1rem
    fontWeight: 400
    lineHeight: 1.6
  body-sm:
    fontFamily: "Roboto, Noto Sans TC, system-ui, sans-serif"
    fontSize: 0.875rem
    fontWeight: 400
    lineHeight: 1.5
  caption:
    fontFamily: "Roboto, Noto Sans TC, system-ui, sans-serif"
    fontSize: 0.75rem
    fontWeight: 400
    lineHeight: 1.4
  micro:
    fontFamily: "Roboto, Noto Sans TC, system-ui, sans-serif"
    fontSize: 0.625rem
    fontWeight: 400
    lineHeight: 1.4
  mono:
    fontFamily: "Roboto Mono, ui-monospace, monospace"
    fontSize: 1.25rem
    fontWeight: 700
    letterSpacing: 0.06em

rounded:
  md: 8px
  lg: 12px

spacing:
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px

components:
  page:
    backgroundColor: "{colors.neutral}"
    textColor: "{colors.primary}"
    typography: "{typography.body-md}"
  card:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.primary}"
    rounded: "{rounded.lg}"
    padding: 16px
  hairline:
    backgroundColor: "{colors.border}"
    height: 1px
  button-primary:
    backgroundColor: "{colors.accent}"
    textColor: "{colors.on-accent}"
    typography: "{typography.body-md}"
    rounded: "{rounded.md}"
    padding: 0 16px
  button-secondary:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.primary}"
    typography: "{typography.body-md}"
    rounded: "{rounded.md}"
    padding: 0 16px
  button-danger:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.error}"
    typography: "{typography.body-md}"
    rounded: "{rounded.md}"
    padding: 0 16px
  input:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.primary}"
    rounded: "{rounded.md}"
    padding: 12px 16px
  widget:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.primary}"
    rounded: "{rounded.lg}"
    padding: 1px
  sun-button:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.secondary}"
    width: 48px
    height: 32px
  sun-button-on:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.accent}"
    width: 48px
    height: 32px
  barcode:
    backgroundColor: "{colors.barcode-paper}"
    textColor: "{colors.barcode-ink}"
  barcode-name:
    textColor: "{colors.secondary}"
    typography: "{typography.micro}"
  barcode-value:
    textColor: "{colors.primary}"
    typography: "{typography.mono}"
  icon-letter:
    backgroundColor: "{colors.icon-1}"
    textColor: "{colors.on-accent}"
    rounded: "{rounded.md}"
  # 文字圖示的其他底色：各列一個，lint 才會逐一檢查白字對比
  icon-letter-2: { backgroundColor: "{colors.icon-2}", textColor: "{colors.on-accent}" }
  icon-letter-3: { backgroundColor: "{colors.icon-3}", textColor: "{colors.on-accent}" }
  icon-letter-4: { backgroundColor: "{colors.icon-4}", textColor: "{colors.on-accent}" }
  icon-letter-5: { backgroundColor: "{colors.icon-5}", textColor: "{colors.on-accent}" }
  icon-letter-6: { backgroundColor: "{colors.icon-6}", textColor: "{colors.on-accent}" }
---

## Overview

條碼工具：打開就是要給收銀員掃，畫面安靜、好找、好掃。淺灰底（`{colors.neutral}`）上放白卡片（`{colors.surface}`），細線（`{colors.border}`）分層，不用陰影。整套只有一個重點色 `{colors.accent}`：主要按鈕、選取中的選項、調亮中的太陽鈕。

## Colors

- 條碼一律 `{colors.barcode-ink}` 條、`{colors.barcode-paper}` 底，不跟主題變色、不加透明度：對比最大，掃描器才讀得到。
- `{colors.error}` 只用在錯誤訊息和刪除。
- 沒選圖片的店家圖示，用名稱第一個字配 `icon-1`～`icon-6` 其中一色（依名稱固定），都是低彩度的北歐色，白字對比都過 AA。
- 對比（WCAG AA，一般文字 ≥ 4.5:1）：

  | 前景／背景 | 對比 |
  |---|---|
  | `{colors.primary}`／白 | 14.7 |
  | `{colors.secondary}`／白 | 7.6 |
  | `{colors.secondary}`／`{colors.neutral}` | 7.2 |
  | 白／`{colors.accent}`、`{colors.accent}`／白 | 5.2 |
  | `{colors.error}`／白 | 4.8 |
  | 白／`icon-1`～`icon-6` | 5.8～7.1 |

  輸入框的提示字也用 `{colors.secondary}`（不用更淺的灰）。

## Typography

系統字體（Roboto＋Noto Sans CJK）。標題 `{typography.h3}`，內文 `{typography.body-md}`。條碼下面那一行：名稱比號碼小。小工具（與編輯畫面的預覽）字要小，條碼才大：名稱 `{typography.micro}`、號碼 `{typography.caption}` 大小的等寬字。全螢幕時名稱 `{typography.body-sm}`、號碼 `{typography.mono}`。字級都用 sp，跟著系統字型大小縮放。

## Layout

4px 為基本單位，間距用 `{spacing.*}`。畫面外距 `{spacing.lg}`，卡片內距 `{spacing.md}`。可點的東西至少 48dp 高；例外是小工具的太陽鈕 48×32dp（仍高於 WCAG 2.2 AA 的 24dp），下面那行才不會吃掉條碼的高度。店家圖示 48dp，多店家小工具每格寬 72dp。

## Elevation & Depth

不用陰影，靠 `{colors.border}` 細線（1dp）分層。

## Shapes

- `{rounded.md}`：按鈕、輸入框、店家圖示。
- `{rounded.lg}`：卡片、小工具。

## Components

- `hairline`：卡片、輸入框、小工具、次要按鈕的 1dp 細框。
- `button-primary`：每個畫面最多一個（新增、儲存）。
- `button-secondary`：其他動作（編輯、選擇圖片、前往允許）。
- `button-danger`：只用在刪除。
- `widget`：兩種桌面小工具的底，白色＋細框。條碼（`barcode`）撐滿整個小工具，只讓出 1dp 細框、上方留 `{spacing.sm}`；左右的白邊就是條碼的靜區。
- 單一圖示小工具（4×1）：平常背景透明，只放一個店家圖示（`icon-letter` 或自訂圖片，48dp）；靠左／置中／靠右分別落在第一格、正中間、最後一格的中心，跟桌面上的 App 圖示對齊。展開時換成 `widget` 白底條碼（同單一條碼小工具，沒有太陽鈕）。沒選條碼時是白底的「選擇條碼」，字才不會直接壓在桌布上。
- `sun-button`：單一條碼小工具右下的調亮鈕，觸控範圍 48×32dp、圖示 24dp 空心太陽；調亮中換成 `sun-button-on`（實心、重點色）。點條碼本身平常不做事，免得誤觸調亮。
- `barcode-name`、`barcode-value`：條碼下面那一行，名稱在前、號碼在後。
- `icon-letter`：沒選圖片時的店家圖示。

## Do's and Don'ts

- Do：顏色、字級、間距、圓角一律引用 token；原生端在 `res/values/colors.xml`、`dimens.xml`，改 token 先改這份再同步過去。
- Do：條碼保留左右靜區（空白），不要裁掉。
- Don't：不要加第二個重點色，條碼不要套主題色或深色模式。
