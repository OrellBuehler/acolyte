import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

from .canvas import C, mix

FONT_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "fonts")
FILES = {
    "cinzel": "Cinzel.ttf",
    "bebas": "BebasNeue-Regular.ttf",
    "anton": "Anton-Regular.ttf",
    "oswald": "Oswald.ttf",
    "orbitron": "Orbitron.ttf",
    "playfair": "PlayfairDisplay.ttf",
    "playfair_i": "PlayfairDisplay-Italic.ttf",
    "abril": "AbrilFatface-Regular.ttf",
    "fredoka": "Fredoka.ttf",
    "pacifico": "Pacifico-Regular.ttf",
    "fraktur": "UnifrakturCook-Bold.ttf",
    "grotesk": "SpaceGrotesk.ttf",
    "cormorant": "CormorantGaramond.ttf",
    "limelight": "Limelight-Regular.ttf",
    "rye": "Rye-Regular.ttf",
    "archivo": "ArchivoBlack-Regular.ttf",
    "josefin": "JosefinSans.ttf",
    "bungee": "Bungee-Regular.ttf",
    "poiret": "PoiretOne-Regular.ttf",
    "lora": "Lora.ttf",
}
_cache = {}


def font(name, size, wght=None):
    key = (name, int(size), wght)
    if key not in _cache:
        f = ImageFont.truetype(os.path.join(FONT_DIR, FILES[name]), int(size), layout_engine=ImageFont.Layout.BASIC)
        if wght is not None:
            try:
                axes = f.get_variation_axes()
                vals = []
                for a in axes:
                    nm = a["name"]
                    nm = nm.decode() if isinstance(nm, bytes) else nm
                    vals.append(min(a["maximum"], max(a["minimum"], wght)) if nm == "Weight" else a["default"])
                f.set_variation_by_axes(vals)
            except OSError:
                pass
        _cache[key] = f
    return _cache[key]


def advance(f, a, b, track):
    if b is None:
        return f.getlength(a) + track
    return f.getlength(a + b) - f.getlength(b) + track


def width(f, text, track=0.0):
    if not text:
        return 0.0
    tot = 0.0
    for i, ch in enumerate(text):
        nxt = text[i + 1] if i + 1 < len(text) else None
        tot += advance(f, ch, nxt, track) if nxt else f.getlength(ch)
    return tot


def draw_text(d, x, y, text, f, track=0.0, fill=255):
    for i, ch in enumerate(text):
        d.text((x, y), ch, font=f, fill=fill)
        nxt = text[i + 1] if i + 1 < len(text) else None
        x += advance(f, ch, nxt, track) if nxt else f.getlength(ch)


STYLES = {
    "cinzel": dict(f="cinzel", w=700, case="upper", track=0.10, lh=1.12, maxs=0.17, ratio=0.68),
    "bebas": dict(f="bebas", w=None, case="upper", track=0.04, lh=0.98, maxs=0.28, ratio=0.5),
    "anton": dict(f="anton", w=None, case="upper", track=0.02, lh=1.0, maxs=0.26, ratio=0.5),
    "oswald": dict(f="oswald", w=300, case="upper", track=0.22, lh=1.15, maxs=0.17, ratio=0.52),
    "orbitron": dict(f="orbitron", w=800, case="upper", track=0.08, lh=1.18, maxs=0.15, ratio=0.78),
    "playfair": dict(f="playfair", w=800, case="title", track=0.0, lh=1.0, maxs=0.2, ratio=0.5),
    "playfair_i": dict(f="playfair_i", w=600, case="title", track=0.0, lh=1.04, maxs=0.19, ratio=0.5),
    "abril": dict(f="abril", w=None, case="title", track=0.01, lh=1.0, maxs=0.2, ratio=0.52),
    "fredoka": dict(f="fredoka", w=700, case="title", track=0.01, lh=1.0, maxs=0.2, ratio=0.52),
    "pacifico": dict(f="pacifico", w=None, case="title", track=0.0, lh=1.25, maxs=0.18, ratio=0.5),
    "fraktur": dict(f="fraktur", w=None, case="title", track=0.0, lh=1.0, maxs=0.22, ratio=0.5),
    "grotesk": dict(f="grotesk", w=700, case="upper", track=0.02, lh=1.02, maxs=0.2, ratio=0.62),
    "cormorant": dict(f="cormorant", w=600, case="title", track=0.0, lh=0.98, maxs=0.22, ratio=0.5),
    "limelight": dict(f="limelight", w=None, case="upper", track=0.05, lh=1.1, maxs=0.17, ratio=0.7),
    "rye": dict(f="rye", w=None, case="upper", track=0.02, lh=1.05, maxs=0.18, ratio=0.7),
    "archivo": dict(f="archivo", w=None, case="upper", track=0.0, lh=0.95, maxs=0.22, ratio=0.62),
    "josefin": dict(f="josefin", w=300, case="upper", track=0.32, lh=1.25, maxs=0.12, ratio=0.5),
    "bungee": dict(f="bungee", w=None, case="upper", track=0.02, lh=1.0, maxs=0.17, ratio=0.8),
    "poiret": dict(f="poiret", w=None, case="upper", track=0.16, lh=1.2, maxs=0.14, ratio=0.5),
    "lora": dict(f="lora", w=600, case="title", track=0.0, lh=1.05, maxs=0.17, ratio=0.5),
}


def apply_case(text, case):
    return text.upper() if case == "upper" else text


def balanced(words, n):
    if n == 1 or len(words) == 1:
        return [" ".join(words)]
    best, bs = None, 1e9
    import itertools

    for cuts in itertools.combinations(range(1, len(words)), min(n - 1, len(words) - 1)):
        parts, last = [], 0
        for c in cuts + (len(words),):
            parts.append(" ".join(words[last:c]))
            last = c
        sc = max(len(p) for p in parts) + 0.3 * (max(len(p) for p in parts) - min(len(p) for p in parts))
        if sc < bs:
            best, bs = parts, sc
    return best


def layout_title(name, style, max_w, max_size, max_lines=3, max_h=None, bias=(1.0, 0.92, 0.85)):
    st = STYLES[style]
    text = apply_case(name, st["case"])
    words = text.split(" ")
    best = None
    for n in range(1, min(max_lines, len(words)) + 1):
        lines = balanced(words, n)
        size = max_size
        while size > 10:
            f = font(st["f"], size, st["w"])
            wmax = max(width(f, ln, st["track"] * size) for ln in lines)
            asc, desc = f.getmetrics()
            hh = int(size * st["lh"]) * (len(lines) - 1) + asc + desc
            if wmax <= max_w and (max_h is None or hh <= max_h):
                break
            size -= 2
        score = size * bias[n - 1]
        if best is None or score > best[0] + 0.01:
            best = (score, lines, size)
    _, lines, size = best
    return lines, size


def render_title_mask(lines, style, size, align="c", pad=0, fixed_w=None):
    st = STYLES[style]
    f = font(st["f"], size, st["w"])
    tr = st["track"] * size
    ws = [width(f, ln, tr) for ln in lines]
    wmax = int(max(ws) if fixed_w is None else fixed_w)
    lh = int(size * st["lh"])
    asc, desc = f.getmetrics()
    h = lh * (len(lines) - 1) + asc + desc
    W, H = wmax + pad * 2, int(h) + pad * 2
    im = Image.new("L", (W, H), 0)
    d = ImageDraw.Draw(im)
    for i, ln in enumerate(lines):
        x = pad + ((wmax - ws[i]) / 2 if align == "c" else 0 if align == "l" else wmax - ws[i])
        draw_text(d, x, pad + i * lh, ln, f, tr, 255)
    return im


def tight(im):
    bb = im.getbbox()
    return bb


def paste_mask(canvas, im, x, y, color, alpha=1.0, shadow=0.0, shadow_blur=6, shadow_off=(0, 3), outline=None, mode="over"):
    w, h = im.size
    full = Image.new("L", (canvas.w, canvas.h), 0)
    full.paste(im, (int(x), int(y)))
    bbox = canvas.bb(x - 40, y - 40, x + w + 40, y + h + 40)
    sub = full.crop(bbox)
    if shadow:
        sh = Image.new("L", sub.size, 0)
        sh.paste(sub, shadow_off)
        sh = sh.filter(ImageFilter.GaussianBlur(shadow_blur))
        canvas.paint(np.asarray(sh, np.float32) / 255 * shadow, (0.0, 0.0, 0.02), 1.0, bbox)
    if outline:
        col, wd = outline
        ol = sub.filter(ImageFilter.MaxFilter(int(wd) * 2 + 1))
        canvas.paint(np.asarray(ol, np.float32) / 255, col, alpha, bbox)
    canvas.paint(np.asarray(sub, np.float32) / 255, color, alpha, bbox, mode)


def small_caps_line(canvas, text, cx, y, name, size, color, alpha=0.85, track=0.25, wght=None, shadow=0.0):
    f = font(name, size, wght)
    tr = track * size
    w = width(f, text, tr)
    asc, desc = f.getmetrics()
    im = Image.new("L", (int(w) + 4, asc + desc + 4), 0)
    draw_text(ImageDraw.Draw(im), 2, 2, text, f, tr, 255)
    paste_mask(canvas, im, cx - w / 2, y, color, alpha, shadow=shadow)
    return asc + desc


def credit_block(canvas, lines, cx, y, size, color, alpha=0.7):
    f = font("oswald", size, 300)
    yy = y
    for ln in lines:
        w = width(f, ln, size * 0.08)
        asc, desc = f.getmetrics()
        im = Image.new("L", (int(w) + 4, asc + desc + 4), 0)
        draw_text(ImageDraw.Draw(im), 2, 2, ln, f, size * 0.08, 255)
        h = im.size[1]
        im = im.resize((int(im.size[0] * 0.86), int(h * 1.22)), Image.LANCZOS)
        paste_mask(canvas, im, cx - im.size[0] / 2, yy, color, alpha)
        yy += size * 1.35
    return yy - y
