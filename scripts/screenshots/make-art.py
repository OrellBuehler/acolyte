#!/usr/bin/env python3
"""Generates the invented posters, backdrops and title logos in art/ (needs Pillow, run by hand
when the catalog changes; the generated JPEGs are committed)."""
import math
import os
import random
import sys

from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from catalog import PALETTES, TITLES  # noqa: E402

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "art")
FONTS = [
    "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf",
    "/usr/share/fonts/truetype/liberation2/LiberationSerif-Bold.ttf",
]


def rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i : i + 2], 16) for i in (0, 2, 4))


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def gradient(w, h, top, bottom):
    img = Image.new("RGB", (1, h))
    px = img.load()
    for y in range(h):
        px[0, y] = mix(top, bottom, (y / (h - 1)) ** 1.1)
    return img.resize((w, h))


def glow(img, cx, cy, radius, color, strength=0.9):
    layer = Image.new("RGB", img.size, (0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.ellipse((cx - radius, cy - radius, cx + radius, cy + radius), fill=tuple(int(c * strength) for c in color))
    layer = layer.filter(ImageFilter.GaussianBlur(radius / 2.2))
    return add(img, layer)


def add(a, b):
    return ImageChops.add(a, b)


def hills(draw, w, h, base, amp, color, rnd, step=40):
    pts = [(0, h)]
    phase = rnd.random() * 6
    freq = 1.5 + rnd.random() * 2
    for x in range(0, w + step, step):
        y = base + amp * math.sin(x / w * freq * math.pi + phase) + amp * 0.4 * math.sin(x / w * 7 + phase * 2)
        pts.append((x, y))
    pts.append((w, h))
    draw.polygon(pts, fill=color)


def scene(w, h, pal, motif, seed):
    rnd = random.Random(seed)
    top, mid, accent = (rgb(c) for c in pal)
    img = gradient(w, h, top, mid)
    cx = w * (0.35 + rnd.random() * 0.3)
    cy = h * (0.30 + rnd.random() * 0.12)
    r = min(w, h) * 0.17
    far = mix(top, mid, 0.35)
    near = mix(top, (0, 0, 0), 0.55)
    if motif in ("sun", "moon", "lantern", "waves", "trees", "mesa"):
        img = glow(img, cx, cy, r * 2.4, accent, 0.55)
    d = ImageDraw.Draw(img)
    if motif == "sun":
        d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=accent)
        hills(d, w, h, h * 0.66, h * 0.05, far, rnd)
        hills(d, w, h, h * 0.78, h * 0.05, near, rnd)
    elif motif == "moon":
        d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=accent)
        d.ellipse((cx - r * 0.55, cy - r * 1.1, cx + r * 1.35, cy + r * 0.9), fill=mix(top, mid, 0.18))
        for _ in range(60):
            x, y = rnd.random() * w, rnd.random() * h * 0.6
            s = max(1, w // 400) * (1 + rnd.random())
            d.ellipse((x, y, x + s, y + s), fill=accent)
        hills(d, w, h, h * 0.82, h * 0.03, near, rnd)
    elif motif == "lantern":
        d.ellipse((cx - r * 0.5, cy - r * 0.5, cx + r * 0.5, cy + r * 0.5), fill=accent)
        img = glow(img, cx, cy, r * 1.4, accent, 0.9)
        d = ImageDraw.Draw(img)
        hills(d, w, h, h * 0.7, h * 0.06, far, rnd)
        hills(d, w, h, h * 0.82, h * 0.04, near, rnd)
    elif motif == "rings":
        for i in range(1, 8):
            k = i / 8
            e = r * (0.6 + 2.6 * k)
            d.ellipse((cx - e, cy - e * 0.35, cx + e, cy + e * 0.35), outline=mix(mid, accent, k), width=max(2, w // 160))
        d.ellipse((cx - r * 0.5, cy - r * 0.5, cx + r * 0.5, cy + r * 0.5), fill=accent)
        hills(d, w, h, h * 0.86, h * 0.02, near, rnd)
    elif motif == "city":
        x = 0
        while x < w:
            bw = int(w * (0.05 + rnd.random() * 0.08))
            bh = int(h * (0.18 + rnd.random() * 0.34))
            col = mix(near, mid, 0.15 + rnd.random() * 0.2)
            d.rectangle((x, h - bh, x + bw, h), fill=col)
            for wy in range(h - bh + int(h * 0.02), h - int(h * 0.02), max(6, int(h * 0.028))):
                for wx in range(x + 6, x + bw - 6, max(8, int(w * 0.018))):
                    if rnd.random() < 0.35:
                        d.rectangle((wx, wy, wx + 4, wy + 6), fill=accent)
            x += bw
        d.ellipse((cx - r * 0.7, cy - r * 0.7, cx + r * 0.7, cy + r * 0.7), fill=mix(accent, mid, 0.3))
    elif motif == "waves":
        d.ellipse((cx - r * 0.8, cy - r * 0.8, cx + r * 0.8, cy + r * 0.8), fill=accent)
        for i in range(6):
            base = h * (0.55 + i * 0.07)
            hills(d, w, h, base, h * 0.025, mix(mid, near, i / 5), rnd, step=16)
    elif motif == "trees":
        d.ellipse((cx - r * 0.7, cy - r * 0.7, cx + r * 0.7, cy + r * 0.7), fill=accent)
        for row, col in ((0.72, far), (0.84, near)):
            x = -20
            while x < w + 20:
                th = h * (0.16 + rnd.random() * 0.1)
                tw = w * 0.07
                d.polygon([(x, h * row), (x + tw / 2, h * row - th), (x + tw, h * row)], fill=col)
                d.rectangle((x, h * row, x + tw, h), fill=col)
                x += tw * 0.8
    elif motif == "mesa":
        d.ellipse((cx - r * 0.8, cy - r * 0.8, cx + r * 0.8, cy + r * 0.8), fill=accent)
        for row, col in ((0.62, far), (0.78, near)):
            x = -30
            while x < w:
                bw = w * (0.12 + rnd.random() * 0.16)
                bh = h * (0.10 + rnd.random() * 0.14)
                d.polygon(
                    [(x, h * row + bh), (x + bw * 0.1, h * row), (x + bw * 0.9, h * row), (x + bw, h * row + bh)], fill=col
                )
                x += bw * 0.9
            d.rectangle((0, h * row + h * 0.1, w, h), fill=col)
    elif motif == "panes":
        gw, gh = 5, 8
        cw, ch = w / gw, h * 0.7 / gh
        for gx in range(gw):
            for gy in range(gh):
                if rnd.random() < 0.7:
                    t = rnd.random()
                    d.rectangle(
                        (gx * cw + 4, gy * ch + 4, (gx + 1) * cw - 4, (gy + 1) * ch - 4),
                        fill=mix(mid, accent, t * 0.8),
                    )
        d.rectangle((0, h * 0.7, w, h), fill=near)
    elif motif == "stars":
        for _ in range(140):
            x, y = rnd.random() * w, rnd.random() * h * 0.75
            s = max(1.5, w / 300) * (0.5 + rnd.random() * 1.5)
            d.ellipse((x, y, x + s, y + s), fill=mix(mid, accent, rnd.random()))
        d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=accent, width=max(3, w // 120))
        hills(d, w, h, h * 0.84, h * 0.03, near, rnd)
    return img


def title_lines(name, font, max_width, draw):
    words = name.split(" ")
    lines, line = [], ""
    for word in words:
        trial = (line + " " + word).strip()
        if draw.textlength(trial, font=font) <= max_width or not line:
            line = trial
        else:
            lines.append(line)
            line = word
    lines.append(line)
    return lines


def poster(t, seed):
    w, h = 800, 1200
    pal = PALETTES[t["palette"]]
    img = scene(w, h, pal, t["motif"], seed)
    shade = Image.new("L", (1, h))
    px = shade.load()
    for y in range(h):
        px[0, y] = int(235 * max(0.0, (y / h - 0.52) / 0.48) ** 1.4)
    shade = shade.resize((w, h))
    img = Image.composite(Image.new("RGB", (w, h), (6, 8, 14)), img, shade)
    d = ImageDraw.Draw(img)
    font = ImageFont.truetype(FONTS[0], 104)
    lines = title_lines(t["name"].upper(), font, w - 120, d)
    while len(lines) > 3 or max(d.textlength(line, font=font) for line in lines) > w - 100:
        font = ImageFont.truetype(FONTS[0], font.size - 8)
        lines = title_lines(t["name"].upper(), font, w - 120, d)
    line_h = font.size * 1.12
    y = h - 90 - line_h * len(lines)
    for line in lines:
        width = d.textlength(line, font=font)
        d.text(((w - width) / 2, y), line, font=font, fill=(250, 244, 232))
        y += line_h
    bar = rgb(pal[2])
    d.rectangle((w / 2 - 60, h - 70, w / 2 + 60, h - 64), fill=bar)
    return img.resize((400, 600), Image.LANCZOS)


def backdrop(t, seed):
    w, h = 1920, 1080
    pal = PALETTES[t["palette"]]
    img = scene(w, h, pal, t["motif"], seed + 99)
    return img.resize((1280, 720), Image.LANCZOS)


LOGO_FONTS = [
    ("/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf", True, 14),
    ("/usr/share/fonts/truetype/liberation2/LiberationSerif-Bold.ttf", False, 2),
    ("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", True, 10),
    ("/usr/share/fonts/truetype/liberation2/LiberationSans-Bold.ttf", True, 6),
]


def spaced(d, text, font, spacing):
    return sum(d.textlength(c, font=font) for c in text) + spacing * (len(text) - 1)


def logo(t, i):
    path, upper, spacing = LOGO_FONTS[i % len(LOGO_FONTS)]
    text = t["name"].upper() if upper else t["name"]
    size = 150
    pad = 60
    probe = ImageDraw.Draw(Image.new("RGBA", (1, 1)))
    font = ImageFont.truetype(path, size)
    words = text.split(" ")
    lines = [text]
    if len(words) > 2 and spaced(probe, text, font, spacing) > 1300:
        half = (len(words) + 1) // 2
        lines = [" ".join(words[:half]), " ".join(words[half:])]
    width = int(max(spaced(probe, line, font, spacing) for line in lines))
    line_h = int(size * 1.2)
    w, h = width + pad * 2, line_h * len(lines) + pad * 2 + 40
    layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    accent = rgb(PALETTES[t["palette"]][2])
    y = pad
    for line in lines:
        x = pad + (width - spaced(d, line, font, spacing)) / 2
        for c in line:
            d.text((x, y), c, font=font, fill=(250, 244, 232, 255))
            x += d.textlength(c, font=font) + spacing
        y += line_h
    d.rectangle((w / 2 - 90, y + 8, w / 2 + 90, y + 18), fill=accent + (255,))
    shadow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    shadow.paste((0, 0, 0, 200), mask=layer.split()[3].filter(ImageFilter.GaussianBlur(10)))
    out = Image.alpha_composite(shadow, layer)
    out = out.crop(out.getbbox())
    return out.resize((out.width // 2, out.height // 2), Image.LANCZOS)


def main():
    os.makedirs(OUT, exist_ok=True)
    for i, t in enumerate(TITLES):
        logo(t, i).save(os.path.join(OUT, f"logo-{t['slug']}.png"), optimize=True)
        poster(t, i + 1).save(os.path.join(OUT, f"poster-{t['slug']}.jpg"), quality=82, optimize=True)
        backdrop(t, i + 1).save(os.path.join(OUT, f"backdrop-{t['slug']}.jpg"), quality=80, optimize=True)
    print(f"wrote {len(TITLES) * 3} images to {OUT}")


if __name__ == "__main__":
    main()
