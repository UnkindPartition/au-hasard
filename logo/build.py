#!/usr/bin/env python3
"""Compose the launcher icon from the typeset formula and digits."""

import math
import os
import xml.etree.ElementTree as ET

BACKGROUND = "#FFFFFF"
FOREGROUND = "#000000"
DIGITS = "#d0d0d0"
# Width of the white outline around the formula, in canvas units; half of it shows.
HALO = 2.4
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


def svg_layers(digits_paths, digits_transform, paths, transform, halo_width, indent):
    """SVG markup of the full icon: background, digits, halo, formula."""
    lines = [
        f'<rect width="{SIZE}" height="{SIZE}" fill="{BACKGROUND}"/>',
        f'<g fill="{DIGITS}" transform="{digits_transform}">',
        *(f"  <path d=\"{d}\"/>" for d in digits_paths),
        "</g>",
        f'<g fill="{BACKGROUND}" stroke="{BACKGROUND}" stroke-width="{fmt(halo_width)}"'
        f' stroke-linejoin="round" transform="{transform}">',
        *(f"  <path d=\"{d}\"/>" for d in paths),
        "</g>",
        f'<g fill="{FOREGROUND}" transform="{transform}">',
        *(f"  <path d=\"{d}\"/>" for d in paths),
        "</g>",
    ]
    return "".join(indent + l + "\n" for l in lines)


def write_preview(path, digits_paths, digits_transform, paths, transform, halo_width):
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

    def icon(px, mask, themed=None):
        """The full icon, or with themed = (bg, fg) the monochrome themed icon."""
        content = (
            '<use href="#full"/>'
            if themed is None
            else f'<rect width="{SIZE}" height="{SIZE}" fill="{themed[0]}"/>'
            f'<use href="#logo" fill="{themed[1]}"/>'
        )
        return (
            f'<svg width="{px}" height="{px}" viewBox="{m} {m} {VISIBLE} {VISIBLE}">'
            f'<g clip-path="url(#{ids[mask]})">{content}</g></svg>'
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
        '<g id="full">',
        svg_layers(digits_paths, digits_transform, paths, transform, halo_width, ""),
        "</g>",
        *(f'<clipPath id="{ids[n]}"><path d="{d}"/></clipPath>' for n, d in masks.items()),
        "</defs></svg>",
        "<h1>Logo preview</h1>",
        "<section><h2>Canvas</h2><div class=\"row light\">",
        cell(
            f'<svg width="324" height="324" viewBox="0 0 {SIZE} {SIZE}">'
            '<use href="#full"/>'
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
        html += [cell(icon(64, name), name) for name in masks]
        html.append(f"</div><p>{label} background</p>")
    html.append("</section><section><h2>Sizes</h2><div class=\"row wallpaper\">")
    html += [cell(icon(px, "Circle"), f"{px}px") for px in (32, 48, 64, 96, 144, 192)]
    html.append("</div></section><section><h2>Themed icons (Android 13+)</h2>")
    themes = [
        ("light", [("#d8e2ff", "#001a41"), ("#c8f0c4", "#0a3910"), ("#ffd8e4", "#3e001d")]),
        ("dark", [("#1f2d4d", "#d8e2ff"), ("#1e3a20", "#c8f0c4"), ("#4a2532", "#ffd8e4")]),
    ]
    for cls, colors in themes:
        html.append(f'<div class="row {cls}">')
        html += [cell(icon(64, "Circle", (bg, fg)), f"{bg} / {fg}") for bg, fg in colors]
        html.append("</div><p></p>")
    html.append("</section><section><h2>Google Play</h2><div class=\"row light\">")
    html += [cell(icon(px, "Rounded square"), f"{px}px") for px in (256, 96, 48)]
    html.append("</div></section></body></html>")

    with open(path, "w") as f:
        f.write("\n".join(html) + "\n")


def load(path):
    root = ET.parse(path).getroot()
    box = tuple(map(float, root.get("viewBox").split()))
    return box, [p.get("d") for p in root.iter(SVG_NS + "path")]


def vector(path, body):
    with open(path, "w") as f:
        f.write(
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            f'    android:width="{SIZE}dp"\n'
            f'    android:height="{SIZE}dp"\n'
            f'    android:viewportWidth="{SIZE}"\n'
            f'    android:viewportHeight="{SIZE}">\n'
        )
        f.write(body)
        f.write("</vector>\n")


def group(tx, ty, s, body):
    return (
        "    <group\n"
        f'        android:translateX="{fmt(tx)}"\n'
        f'        android:translateY="{fmt(ty)}"\n'
        f'        android:scaleX="{fmt(s)}"\n'
        f'        android:scaleY="{fmt(s)}">\n'
        f"{body}"
        "    </group>\n"
    )


def path_element(d, fill, stroke_width=None):
    stroke = (
        f'            android:strokeColor="{fill}"\n'
        f'            android:strokeWidth="{fmt(stroke_width)}"\n'
        '            android:strokeLineJoin="round"\n'
        if stroke_width
        else ""
    )
    return (
        "        <path\n"
        f'            android:fillColor="{fill}"\n'
        f"{stroke}"
        f'            android:pathData="{d}" />\n'
    )


def glyphs(src, dst):
    """A vector drawable of typeset glyphs, cropped to their bounding box, for tinting."""
    (x, y, w, h), paths = load(src)
    with open(dst, "w") as f:
        f.write(
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            f'    android:width="{fmt(w)}dp"\n'
            f'    android:height="{fmt(h)}dp"\n'
            f'    android:viewportWidth="{fmt(w)}"\n'
            f'    android:viewportHeight="{fmt(h)}">\n'
        )
        f.write(group(-x, -y, 1, "".join(path_element(d, "#000000") for d in paths)))
        f.write("</vector>\n")


def inline_svg(src, dst, label):
    """An SVG of typeset glyphs for inlining in HTML, cropped to their bounding box and drawn
    in the text's color."""
    (x, y, w, h), paths = load(src)
    with open(dst, "w") as f:
        f.write(
            f'<svg class="glyph" xmlns="http://www.w3.org/2000/svg" '
            f'viewBox="{fmt(x)} {fmt(y)} {fmt(w)} {fmt(h)}" fill="currentColor" '
            f'role="img" aria-label="{label}">'
        )
        f.write("".join(f'<path d="{d}"/>' for d in paths))
        f.write("</svg>\n")


def main():
    (x, y, w, h), paths = load("build/formula.svg")
    s = SAFE / math.hypot(w, h)
    tx = (SIZE - w * s) / 2 - x * s
    ty = (SIZE - h * s) / 2 - y * s
    transform = f"translate({fmt(tx)} {fmt(ty)}) scale({fmt(s)})"
    # The halo's stroke is in the formula's coordinates, which get scaled by s.
    halo_width = HALO / s

    # The digits fill the whole canvas.
    (dx, dy, dw, _), digits_paths = load("build/digits.svg")
    ds = SIZE / dw
    dtx, dty = -dx * ds, -dy * ds
    digits_transform = f"translate({fmt(dtx)} {fmt(dty)}) scale({fmt(ds)})"

    m = (SIZE - VISIBLE) // 2

    os.makedirs("out", exist_ok=True)

    write_preview(
        "out/preview.html", digits_paths, digits_transform, paths, transform, halo_width
    )

    with open("out/icon.svg", "w") as f:
        f.write(
            f'<svg xmlns="http://www.w3.org/2000/svg" width="{VISIBLE * 4}" height="{VISIBLE * 4}" '
            f'viewBox="{m} {m} {VISIBLE} {VISIBLE}">\n'
        )
        f.write(svg_layers(digits_paths, digits_transform, paths, transform, halo_width, "  "))
        f.write("</svg>\n")

    vector(
        "../res/drawable/ic_launcher_background.xml",
        "    <path\n"
        f'        android:fillColor="{BACKGROUND}"\n'
        f'        android:pathData="M0,0h{SIZE}v{SIZE}h-{SIZE}z" />\n'
        # One path per digit: aapt2 can't store strings longer than 32767 characters.
        + group(dtx, dty, ds, "".join(path_element(d, DIGITS) for d in digits_paths)),
    )
    vector(
        "../res/drawable/ic_launcher_foreground.xml",
        group(
            tx,
            ty,
            s,
            path_element("".join(paths), BACKGROUND, halo_width)
            + "".join(path_element(d, FOREGROUND) for d in paths),
        ),
    )
    glyphs("build/formula.svg", "../res/drawable/formula.xml")
    glyphs("build/u.svg", "../res/drawable/calligraphic_u.xml")
    inline_svg("build/u.svg", "../doc/calligraphic-u.svg", "𝒰")

    # For themed icons, which tint the layer's shape: the formula without the halo.
    vector(
        "../res/drawable/ic_launcher_monochrome.xml",
        group(tx, ty, s, "".join(path_element(d, FOREGROUND) for d in paths)),
    )


if __name__ == "__main__":
    main()
