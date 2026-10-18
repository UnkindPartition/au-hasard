#!/usr/bin/env python3
"""Compose the launcher icon from the typeset formula."""

import math
import os
import xml.etree.ElementTree as ET

BACKGROUND = "#FFFFFF"
FOREGROUND = "#000000"
# Adaptive icon canvas (dp). Launchers show at most the central VISIBLE dp and
# may mask it down to a SAFE dp circle, so the formula must fit inside that circle.
SIZE = 108
VISIBLE = 72
SAFE = 66

SVG_NS = "{http://www.w3.org/2000/svg}"


def fmt(v):
    return f"{v:.4f}".rstrip("0").rstrip(".")


def rounded_square(x, y, size, tl, tr, br, bl):
    return (
        f"M{x + tl},{y}H{x + size - tr}A{tr},{tr} 0 0 1 {x + size},{y + tr}"
        f"V{y + size - br}A{br},{br} 0 0 1 {x + size - br},{y + size}"
        f"H{x + bl}A{bl},{bl} 0 0 1 {x},{y + size - bl}"
        f"V{y + tl}A{tl},{tl} 0 0 1 {x + tl},{y}Z"
    )


def squircle(cx, cy, r, n=5, steps=96):
    pts = []
    for i in range(steps):
        t = 2 * math.pi * i / steps
        c, s = math.cos(t), math.sin(t)
        pts.append(
            (
                cx + r * math.copysign(abs(c) ** (2 / n), c),
                cy + r * math.copysign(abs(s) ** (2 / n), s),
            )
        )
    return "M" + "L".join(f"{fmt(px)},{fmt(py)}" for px, py in pts) + "Z"


def write_preview(path, paths, transform):
    m = (SIZE - VISIBLE) // 2
    c = SIZE / 2
    r = VISIBLE / 2
    masks = {
        "Circle": f"M{m},{c}A{r},{r} 0 1 0 {m + VISIBLE},{c}A{r},{r} 0 1 0 {m},{c}Z",
        "Squircle": squircle(c, c, r),
        "Rounded square": rounded_square(m, m, VISIBLE, 14, 14, 14, 14),
        "Teardrop": rounded_square(m, m, VISIBLE, r, r, 10, r),
        "Square": rounded_square(m, m, VISIBLE, 0, 0, 0, 0),
    }
    ids = {name: f"mask{i}" for i, name in enumerate(masks)}

    def icon(px, mask, bg, fg):
        return (
            f'<svg width="{px}" height="{px}" viewBox="{m} {m} {VISIBLE} {VISIBLE}">'
            f'<g clip-path="url(#{ids[mask]})"><rect width="{SIZE}" height="{SIZE}" fill="{bg}"/>'
            f'<use href="#logo" fill="{fg}"/></g></svg>'
        )

    def cell(content, label):
        return f'<figure>{content}<figcaption>{label}</figcaption></figure>'

    html = [
        "<!doctype html>",
        '<html><head><meta charset="utf-8"><title>Logo preview</title><style>',
        "body { font-family: sans-serif; margin: 24px; background: #fafafa; color: #222; }",
        "section { margin-bottom: 32px; }",
        ".row { display: flex; flex-wrap: wrap; align-items: flex-end; gap: 24px;"
        " padding: 20px; border-radius: 12px; }",
        "figure { margin: 0; text-align: center; }",
        "figcaption { font-size: 12px; margin-top: 6px; }",
        ".light { background: #e8e8e8; }",
        ".dark { background: #121212; color: #ddd; }",
        ".wallpaper { background: linear-gradient(135deg, #f6a38b, #7b5ac9, #2bc0e4); color: #fff; }",
        "</style></head><body>",
        '<svg width="0" height="0" style="position: absolute"><defs>',
        f'<g id="logo" transform="{transform}">',
        *(f'<path d="{d}"/>' for d in paths),
        "</g>",
        *(f'<clipPath id="{ids[n]}"><path d="{d}"/></clipPath>' for n, d in masks.items()),
        "</defs></svg>",
        "<h1>Logo preview</h1>",
        "<section><h2>Canvas</h2><div class=\"row light\">",
        cell(
            f'<svg width="324" height="324" viewBox="0 0 {SIZE} {SIZE}">'
            f'<rect width="{SIZE}" height="{SIZE}" fill="{BACKGROUND}"/>'
            f'<use href="#logo" fill="{FOREGROUND}"/>'
            f'<rect x="{m}" y="{m}" width="{VISIBLE}" height="{VISIBLE}" fill="none"'
            ' stroke="#e53935" stroke-width="0.4" stroke-dasharray="2 1"/>'
            f'<circle cx="{c}" cy="{c}" r="{SAFE / 2}" fill="none"'
            ' stroke="#1e88e5" stroke-width="0.4" stroke-dasharray="2 1"/></svg>',
            f"Full {SIZE}dp canvas: red = visible area, blue = safe zone",
        ),
        "</div></section>",
        "<section><h2>Launcher masks</h2>",
    ]
    for cls, label in [("light", "Light"), ("dark", "Dark"), ("wallpaper", "Wallpaper")]:
        html.append(f'<div class="row {cls}">')
        html += [cell(icon(64, name, BACKGROUND, FOREGROUND), name) for name in masks]
        html.append(f"</div><p>{label} background</p>")
    html.append("</section><section><h2>Sizes</h2><div class=\"row wallpaper\">")
    html += [cell(icon(px, "Circle", BACKGROUND, FOREGROUND), f"{px}px") for px in (32, 48, 64, 96, 144, 192)]
    html.append("</div></section><section><h2>Themed icons (Android 13+)</h2>")
    themes = [
        ("light", [("#d8e2ff", "#001a41"), ("#c8f0c4", "#0a3910"), ("#ffd8e4", "#3e001d")]),
        ("dark", [("#1f2d4d", "#d8e2ff"), ("#1e3a20", "#c8f0c4"), ("#4a2532", "#ffd8e4")]),
    ]
    for cls, colors in themes:
        html.append(f'<div class="row {cls}">')
        html += [cell(icon(64, "Circle", bg, fg), f"{bg} / {fg}") for bg, fg in colors]
        html.append("</div><p></p>")
    html.append("</section><section><h2>Google Play</h2><div class=\"row light\">")
    html += [
        cell(icon(px, "Rounded square", BACKGROUND, FOREGROUND), f"{px}px")
        for px in (256, 96, 48)
    ]
    html.append("</div></section></body></html>")

    with open(path, "w") as f:
        f.write("\n".join(html) + "\n")


def main():
    root = ET.parse("build/formula.svg").getroot()
    x, y, w, h = map(float, root.get("viewBox").split())
    paths = [p.get("d") for p in root.iter(SVG_NS + "path")]

    s = SAFE / math.hypot(w, h)
    tx = (SIZE - w * s) / 2 - x * s
    ty = (SIZE - h * s) / 2 - y * s

    m = (SIZE - VISIBLE) // 2

    os.makedirs("out", exist_ok=True)

    write_preview("out/preview.html", paths, f"translate({fmt(tx)} {fmt(ty)}) scale({fmt(s)})")

    with open("out/icon.svg", "w") as f:
        f.write(
            f'<svg xmlns="http://www.w3.org/2000/svg" width="{VISIBLE * 4}" height="{VISIBLE * 4}" '
            f'viewBox="{m} {m} {VISIBLE} {VISIBLE}">\n'
            f'  <rect width="{SIZE}" height="{SIZE}" fill="{BACKGROUND}"/>\n'
            f'  <g fill="{FOREGROUND}" transform="translate({fmt(tx)} {fmt(ty)}) scale({fmt(s)})">\n'
        )
        for d in paths:
            f.write(f'    <path d="{d}"/>\n')
        f.write("  </g>\n</svg>\n")

    header = (
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        f'    android:width="{SIZE}dp"\n'
        f'    android:height="{SIZE}dp"\n'
        f'    android:viewportWidth="{SIZE}"\n'
        f'    android:viewportHeight="{SIZE}">\n'
    )

    with open("../res/drawable/ic_launcher_foreground.xml", "w") as f:
        f.write(header)
        f.write(
            "    <group\n"
            f'        android:translateX="{fmt(tx)}"\n'
            f'        android:translateY="{fmt(ty)}"\n'
            f'        android:scaleX="{fmt(s)}"\n'
            f'        android:scaleY="{fmt(s)}">\n'
        )
        for d in paths:
            f.write(
                "        <path\n"
                f'            android:fillColor="{FOREGROUND}"\n'
                f'            android:pathData="{d}" />\n'
            )
        f.write("    </group>\n</vector>\n")

    with open("../res/drawable/ic_launcher_background.xml", "w") as f:
        f.write(header)
        f.write(
            "    <path\n"
            f'        android:fillColor="{BACKGROUND}"\n'
            f'        android:pathData="M0,0h{SIZE}v{SIZE}h-{SIZE}z" />\n'
            "</vector>\n"
        )


if __name__ == "__main__":
    main()
