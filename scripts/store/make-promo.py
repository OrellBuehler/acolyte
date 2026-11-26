#!/usr/bin/env python3
"""Draws every promo image from store/screenshots and the app icon: the README banner and platform strips, the
website pictures (site/assets/img), the sharing picture, the store header images in assets/store and the captioned
store screenshots in assets/store/captioned. Devices are drawn here (plain bezels, no manufacturer artwork).
Needs Pillow and numpy; the Inter font comes from scripts/store/fonts.

Usage: scripts/store/make-promo.py [readme] [site] [store] [captioned]   (no argument draws everything)
"""
import functools
import pathlib
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = pathlib.Path(__file__).resolve().parents[2]
SHOTS = ROOT / "store" / "screenshots"
FONT = pathlib.Path(__file__).resolve().parent / "fonts" / "InterVariable.ttf"
LOCALES = ("en-US", "de-DE")

BG = (5, 8, 13)
RED = (191, 40, 31)
RED_HOT = (229, 53, 43)
TEXT = (245, 245, 247)
MUTED = (161, 161, 166)
CREAM = (243, 240, 233)
BLUE = (64, 156, 255)
RIM = (62, 69, 80)
BODY = (15, 19, 26)

TAGLINE = {"en-US": "Your HolyArrs server, on every screen", "de-DE": "Dein HolyArrs-Server auf jedem Bildschirm"}
PLATFORMS = ("iPhone", "Android", "Apple TV", "Android TV")

PHONE_SCREENS = ("01-home", "02-explore", "03-title", "04-library", "05-downloads", "06-requests")
TV_SCREENS = ("01-home", "02-explore", "03-title", "04-new-and-popular", "05-calendar", "06-requests")


@functools.lru_cache(maxsize=None)
def font(size, weight=500):
    f = ImageFont.truetype(str(FONT), max(1, round(size)))
    f.set_variation_by_axes([weight])
    return f


def shot_path(platform, locale, name):
    folder = {
        "ios": f"ios/{locale}/iphone-6.9",
        "android": f"android/{locale}/phone",
        "tvos": f"tvos/{locale}",
        "androidtv": f"androidtv/{locale}",
    }[platform]
    return SHOTS / folder / f"{name}.png"


@functools.lru_cache(maxsize=32)
def load(path):
    return Image.open(path).convert("RGB")


def scaled(path, width, height):
    return load(path).resize((round(width), round(height)), Image.LANCZOS)


def rounded_mask(w, h, r, ss=4):
    m = Image.new("L", (w * ss, h * ss), 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, w * ss - 1, h * ss - 1), radius=r * ss, fill=255)
    return m.resize((w, h), Image.LANCZOS)


def over(dst, src, x, y):
    x, y = round(x), round(y)
    sx, sy = max(0, -x), max(0, -y)
    ex, ey = min(src.width, dst.width - x), min(src.height, dst.height - y)
    if ex <= sx or ey <= sy:
        return
    dst.alpha_composite(src.crop((sx, sy, ex, ey)), (x + sx, y + sy))


def shadow(dst, x, y, w, h, r, blur, dy=0, alpha=0.6, color=(0, 0, 0)):
    pad = round(blur * 3)
    w, h = round(w), round(h)
    m = Image.new("L", (w + 2 * pad, h + 2 * pad), 0)
    ImageDraw.Draw(m).rounded_rectangle((pad, pad, pad + w, pad + h), radius=r, fill=round(255 * alpha))
    m = m.filter(ImageFilter.GaussianBlur(blur))
    layer = Image.new("RGBA", m.size, color + (0,))
    layer.putalpha(m)
    over(dst, layer, x - pad, y - pad + dy)


def glow(dst, cx, cy, rx, ry, color, alpha):
    sw, sh = max(8, dst.width // 8), max(8, dst.height // 8)
    yy, xx = np.mgrid[0:sh, 0:sw]
    d = np.sqrt(((xx * 8 - cx) / rx) ** 2 + ((yy * 8 - cy) / ry) ** 2)
    a = np.clip(1 - d, 0, 1) ** 2.2 * alpha
    layer = np.zeros((sh, sw, 4), np.uint8)
    layer[..., :3] = color
    layer[..., 3] = (a * 255).astype(np.uint8)
    over(dst, Image.fromarray(layer, "RGBA").resize(dst.size, Image.BICUBIC), 0, 0)


def stars(dst, seed, count, area=None):
    rng = np.random.default_rng(seed)
    d = ImageDraw.Draw(dst)
    x0, y0, x1, y1 = area or (0, 0, dst.width, dst.height)
    unit = max(2, dst.height / 380)
    for _ in range(count):
        x, y = rng.uniform(x0, x1), rng.uniform(y0, y1)
        s = unit * rng.choice([0.7, 1.0, 1.0, 1.4])
        c = CREAM + (int(rng.uniform(70, 170)),)
        if rng.random() < 0.3:
            d.rectangle((x - s * 1.5, y - s * 0.5, x + s * 1.5, y + s * 0.5), fill=c)
            d.rectangle((x - s * 0.5, y - s * 1.5, x + s * 0.5, y + s * 1.5), fill=c)
        else:
            d.rectangle((x - s * 0.5, y - s * 0.5, x + s * 0.5, y + s * 0.5), fill=c)


def background(w, h, glows=(), seed=7, star_count=26, star_area=None):
    img = Image.new("RGBA", (w, h), BG + (255,))
    for g in glows:
        glow(img, *g)
    stars(img, seed, star_count, star_area)
    return img


def ambient(dst, path, x, y, w, h, alpha=0.5, spread=1.45):
    small = load(path).resize((24, 14), Image.BILINEAR)
    gw, gh = round(w * spread), round(h * spread * 1.1)
    base = small.resize((gw, gh), Image.BICUBIC).filter(ImageFilter.GaussianBlur(gw * 0.04))
    yy, xx = np.mgrid[0:gh, 0:gw]
    d = np.sqrt(((xx - gw / 2) / (gw / 2)) ** 2 + ((yy - gh / 2) / (gh / 2)) ** 2)
    a = (np.clip(1 - d, 0, 1) ** 1.8 * alpha * 255).astype(np.uint8)
    layer = base.convert("RGBA")
    layer.putalpha(Image.fromarray(a, "L"))
    over(dst, layer, x + w / 2 - gw / 2, y + h / 2 - gh / 2)


def phone_size(ratio, height):
    sw = height / (ratio + 0.058)
    return sw, sw * ratio


def phone(path, height, kind):
    ratio = 2868 / 1320 if kind == "ios" else 1920 / 1080
    sw, sh = phone_size(ratio, height)
    b = sw * 0.029
    radius = sw * (0.135 if kind == "ios" else 0.075)
    w, h = round(sw + 2 * b), round(sh + 2 * b)
    side = max(2, round(sw * 0.012))
    img = Image.new("RGBA", (w + 2 * side, h), (0, 0, 0, 0))
    outer = round(radius + b)
    frame = Image.new("RGBA", (w, h), RIM + (255,))
    frame.paste(Image.new("RGBA", (w - 4, h - 4), BODY + (255,)), (2, 2), rounded_mask(w - 4, h - 4, outer - 2))
    frame.putalpha(rounded_mask(w, h, outer))
    over(img, frame, side, 0)
    d = ImageDraw.Draw(img)
    for top, length, left in ((0.19, 0.075, True), (0.3, 0.075, True), (0.26, 0.11, False)):
        x = 0 if left else side + w
        d.rounded_rectangle((x, h * top, x + side, h * (top + length)), radius=side / 2, fill=RIM + (255,))
    screen = scaled(path, sw, sh).convert("RGBA")
    screen.putalpha(rounded_mask(round(sw), round(sh), round(radius)))
    over(img, screen, side + b, b)
    return img, side


def tv(path, width, stand=True):
    w = round(width)
    sh = round(w * 9 / 16)
    b = max(3, round(w * 0.0065))
    radius = round(w * 0.006)
    h = sh + 2 * b
    neck = round(w * 0.055) if stand else 0
    foot = round(w * 0.013) if stand else 0
    img = Image.new("RGBA", (w, h + neck + foot), (0, 0, 0, 0))
    if stand:
        nw = round(w * 0.1)
        grad = np.linspace(46, 22, neck).astype(np.uint8)[:, None].repeat(nw, 1)
        neck_img = Image.fromarray(np.stack([grad, grad + 3, grad + 8, np.full_like(grad, 255)], -1), "RGBA")
        over(img, neck_img, (w - nw) / 2, h - 1)
        fw = round(w * 0.3)
        foot_img = Image.new("RGBA", (fw, foot), (50, 56, 66, 255))
        foot_img.putalpha(rounded_mask(fw, foot, foot // 2))
        over(img, foot_img, (w - fw) / 2, h + neck - 1)
    frame = Image.new("RGBA", (w, h), RIM + (255,))
    frame.paste(Image.new("RGBA", (w - 2, h - 2), BODY + (255,)), (1, 1), rounded_mask(w - 2, h - 2, radius - 1))
    frame.putalpha(rounded_mask(w, h, radius))
    over(img, frame, 0, 0)
    screen = scaled(path, w - 2 * b, sh).convert("RGBA")
    screen.putalpha(rounded_mask(w - 2 * b, sh, max(1, radius - b)))
    over(img, screen, b, b)
    return img, h


def place_phone(dst, path, x, y, height, kind, with_shadow=True):
    img, side = phone(path, height, kind)
    if with_shadow:
        b = img.width - 2 * side
        shadow(dst, x + side, y, b, img.height, round(b * 0.15), height * 0.035, dy=round(height * 0.03), alpha=0.7)
    over(dst, img, x, y)
    return img.size


def place_tv(dst, path, x, y, width, stand=True, glow_alpha=0.5):
    img, screen_h = tv(path, width, stand)
    if glow_alpha:
        ambient(dst, path, x, y, width, screen_h, glow_alpha)
    shadow(dst, x, y, width, screen_h, round(width * 0.006), width * 0.02, dy=round(width * 0.025), alpha=0.65)
    over(dst, img, x, y)
    return img.size


def text_block(d, x, y, text, size, weight, fill, max_width, line=1.22):
    f = font(size, weight)
    lines, cur = [], ""
    for word in text.split():
        trial = (cur + " " + word).strip()
        if cur and f.getlength(trial) > max_width:
            lines.append(cur)
            cur = word
        else:
            cur = trial
    lines.append(cur)
    for l in lines:
        d.text((x, y), l, font=f, fill=fill, anchor="la")
        y += size * line
    return y


def wordmark(dst, x, y, size):
    icon = Image.open(ROOT / "assets" / "icon" / "icon-1024.png").convert("RGBA")
    isz = round(size * 0.9)
    icon = icon.resize((isz, isz), Image.LANCZOS)
    icon.putalpha(rounded_mask(isz, isz, round(isz * 0.225)))
    shadow(dst, x, y, isz, isz, round(isz * 0.225), isz * 0.12, dy=round(isz * 0.08), alpha=0.8, color=(120, 10, 5))
    over(dst, icon, x, y)
    ImageDraw.Draw(dst).text((x + isz + size * 0.28, y + isz / 2), "Acolyte", font=font(size * 0.78, 800), fill=TEXT, anchor="lm")
    return y + isz


def chips(dst, x, y, size, labels=PLATFORMS):
    d = ImageDraw.Draw(dst)
    f = font(size, 600)
    pad = size * 0.8
    for label in labels:
        w = f.getlength(label) + 2 * pad + size * 0.5
        d.rounded_rectangle(
            (x, y, x + w, y + size * 2.3), radius=size * 1.15, outline=(70, 77, 90), width=max(1, round(size * 0.08)), fill=(17, 21, 28)
        )
        cx, cy = x + pad * 0.85, y + size * 1.15
        d.ellipse((cx - size * 0.14, cy - size * 0.14, cx + size * 0.14, cy + size * 0.14), fill=RED_HOT)
        d.text((cx + size * 0.45, cy), label, font=f, fill=TEXT, anchor="lm")
        x += w + size * 0.5
    return x


def finish(img, path, quality=90, noise=True):
    path = pathlib.Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    rgb = img.convert("RGB")
    suffix = path.suffix.lower()
    if suffix in (".jpg", ".webp") and noise:
        arr = np.asarray(rgb).astype(np.int16)
        rng = np.random.default_rng(3)
        arr += rng.integers(-1, 2, size=arr.shape[:2], dtype=np.int16)[..., None]
        rgb = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8))
    if suffix == ".jpg":
        rgb.save(path, "JPEG", quality=quality, optimize=True, progressive=True, subsampling=0)
    elif suffix == ".webp":
        rgb.save(path, "WEBP", quality=quality, method=6)
    else:
        rgb.save(path, "PNG", optimize=True)
    print(f"{path.relative_to(ROOT)}  {rgb.width}x{rgb.height}  {path.stat().st_size // 1024} KB")


def hero_devices(dst, x, y, w, h, locale):
    n = shot_path
    tv_w = w * 0.52
    place_tv(dst, n("tvos", locale, "01-home"), x, y + h * 0.02, tv_w)
    place_tv(dst, n("androidtv", locale, "02-explore"), x + w * 0.48, y + h * 0.2, tv_w)
    ph = h * 0.8
    sw, _ = phone_size(2868 / 1320, ph)
    px = x + w * 0.27
    place_phone(dst, n("ios", locale, "01-home"), px, y + h - ph, ph, "ios")
    ah = h * 0.68
    place_phone(dst, n("android", locale, "04-library"), px + sw + w * 0.035, y + h - ah, ah, "android")


def banner(locale, path, W=2400, H=900):
    tagline = {"en-US": "Your HolyArrs server, on every screen", "de-DE": "Dein HolyArrs-Server auf jedem Bildschirm"}[locale]
    img = background(W, H, [(W * 0.74, H * 0.55, W * 0.5, H * 0.95, RED, 0.5), (W * 0.1, H * 1.0, W * 0.45, H * 0.6, BLUE, 0.1)])
    hero_devices(img, W * 0.37, H * 0.05, W * 0.62, H * 0.9, locale)
    wordmark(img, W * 0.055, H * 0.2, H * 0.15)
    d = ImageDraw.Draw(img)
    y = text_block(d, W * 0.055, H * 0.47, tagline, H * 0.052, 650, TEXT, W * 0.27, line=1.18)
    d.rectangle((W * 0.055, y + H * 0.01, W * 0.055 + H * 0.09, y + H * 0.02), fill=RED_HOT)
    chips(img, W * 0.055, y + H * 0.07, H * 0.026)
    finish(img, path, 90)


def glows_for(W, H, right=0.74):
    return [(W * right, H * 0.55, W * 0.5, H * 0.95, RED, 0.5), (W * 0.1, H * 1.0, W * 0.45, H * 0.6, BLUE, 0.1)]


def phone_strip(kind, locale, path, W=2400, quality=88):
    ratio = 2868 / 1320 if kind == "ios" else 1920 / 1080
    unit = 0.474 if kind == "ios" else 0.5766
    ph = W * 0.94 / (6 * unit + 5 * 0.06)
    H = round(ph * 1.2)
    img = background(W, H, [(W * 0.5, H * 0.55, W * 0.55, H * 0.8, RED, 0.38), (W * 0.05, H, W * 0.4, H * 0.7, BLUE, 0.08)], star_count=18)
    x = W * 0.03
    for name in PHONE_SCREENS:
        size = place_phone(img, shot_path(kind, locale, name), x, (H - ph) / 2, ph, kind)
        x += size[0] + ph * 0.06
    finish(img, path, quality)


def tv_strip(kind, locale, path, W=2400, quality=88):
    H = round(W * 0.4)
    img = background(W, H, [(W * 0.5, H * 0.5, W * 0.55, H * 0.8, RED, 0.34), (W * 0.9, H, W * 0.4, H * 0.7, BLUE, 0.08)], star_count=18)
    side = {"tvos": ("02-explore", "04-new-and-popular"), "androidtv": ("02-explore", "05-calendar")}[kind]
    for name, x in ((side[0], W * 0.0), (side[1], W * 0.6)):
        layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
        place_tv(layer, shot_path(kind, locale, name), x, H * 0.13, W * 0.4, glow_alpha=0)
        layer.putalpha(layer.getchannel("A").point(lambda v: round(v * 0.72)))
        img.alpha_composite(layer)
    place_tv(img, shot_path(kind, locale, "01-home"), W * 0.23, H * 0.07, W * 0.54, glow_alpha=0.55)
    finish(img, path, quality)


def phone_cutouts(kind, locale, folder, height=640):
    for i, name in enumerate(PHONE_SCREENS, 1):
        img, side = phone(shot_path(kind, locale, name), height, kind)
        out = pathlib.Path(folder) / f"{kind}-{locale[:2]}-{i}.webp"
        out.parent.mkdir(parents=True, exist_ok=True)
        img.save(out, "WEBP", quality=84, method=6, alpha_quality=92)
        print(f"{out.relative_to(ROOT)}  {img.width}x{img.height}  {out.stat().st_size // 1024} KB")


def hero_picture(locale, path, W):
    H = round(W * 0.5)
    img = background(W, H, [(W * 0.55, H * 0.5, W * 0.6, H * 0.9, RED, 0.45)], star_count=14)
    hero_devices(img, W * 0.04, H * 0.04, W * 0.92, H * 0.92, locale)
    finish(img, path, 86)


def og(locale, path):
    W, H = 1200, 630
    tag = {"en-US": "Your HolyArrs server, on every screen", "de-DE": "Dein HolyArrs-Server auf jedem Bildschirm"}[locale]
    img = background(W, H, glows_for(W, H, 0.7), star_count=18)
    hero_devices(img, W * 0.4, H * 0.08, W * 0.6, H * 0.88, locale)
    wordmark(img, 56, 108, 78)
    d = ImageDraw.Draw(img)
    y = text_block(d, 56, 250, tag, 40, 650, TEXT, 440, line=1.2)
    d.rectangle((56, y + 10, 56 + 70, y + 16), fill=RED_HOT)
    chips(img, 56, y + 44, 15, PLATFORMS[:2])
    chips(img, 56, y + 94, 15, PLATFORMS[2:])
    finish(img, path, 88)


def phone_row(img, kind, locale, items, x0, x1, cy, base):
    gap = base * 0.03
    widths = [phone_size(2868 / 1320 if kind == "ios" else 1920 / 1080, base * h)[0] * 1.058 + base * h * 0.02 for _, h in items]
    k = min(1.0, (x1 - x0 - gap * (len(items) - 1)) / sum(widths))
    x = x0
    for (name, h), w in zip(items, widths):
        ph = base * h * k
        place_phone(img, shot_path(kind, locale, name), x, cy - ph * 0.5, ph, kind)
        x += w * k + gap


def phones_scene(W, H, locale, shots, path, tagline, text_x=0.06):
    img = background(W, H, glows_for(W, H, 0.7), star_count=28)
    phone_row(img, "ios", locale, list(zip(shots, (0.8, 0.92, 0.8))), W * 0.40, W * 0.98, H * 0.52, H)
    wordmark(img, W * text_x, H * 0.24, H * 0.14)
    d = ImageDraw.Draw(img)
    y = text_block(d, W * text_x, H * 0.46, tagline, H * 0.055, 650, TEXT, W * 0.32, line=1.18)
    d.rectangle((W * text_x, y + H * 0.012, W * text_x + H * 0.09, y + H * 0.022), fill=RED_HOT)
    finish(img, path, noise=False)


def feature_graphic(path):
    W, H = 1024, 500
    img = background(W, H, glows_for(W, H, 0.7), star_count=16)
    x = W * 0.5
    for name, top in (("03-title", 0.1), ("01-home", 0.02)):
        ph = H * 1.0
        size = place_phone(img, shot_path("android", "en-US", name), x, H * top + (H * 0.12 if name == "03-title" else 0), ph, "android")
        x += size[0] + H * 0.03
    wordmark(img, 52, 120, 64)
    d = ImageDraw.Draw(img)
    y = text_block(d, 52, 235, "Your HolyArrs server, in your pocket", 30, 650, TEXT, 330, line=1.2)
    d.rectangle((52, y + 8, 52 + 50, y + 12), fill=RED_HOT)
    finish(img, path, noise=False)


def tv_banner(path):
    W, H = 1280, 720
    img = background(W, H, [(W * 0.5, H * 0.52, W * 0.55, H * 0.85, RED, 0.5), (W * 0.5, H * 1.1, W * 0.6, H * 0.5, BLUE, 0.08)], star_count=30)
    isz = 250
    icon = Image.open(ROOT / "assets" / "icon" / "icon-1024.png").convert("RGBA").resize((isz, isz), Image.LANCZOS)
    icon.putalpha(rounded_mask(isz, isz, round(isz * 0.225)))
    f = font(170, 800)
    d = ImageDraw.Draw(img)
    tw = d.textlength("Acolyte", font=f)
    gap = 44
    x0 = (W - (isz + gap + tw)) / 2
    shadow(img, x0, (H - isz) / 2, isz, isz, round(isz * 0.225), 28, dy=18, alpha=0.8, color=(120, 10, 5))
    over(img, icon, x0, (H - isz) / 2)
    d.text((x0 + isz + gap, H / 2 + 4), "Acolyte", font=f, fill=TEXT, anchor="lm")
    finish(img, path, noise=False)


def captioned(kind, locale, name, caption, path):
    tv_kind = kind in ("tvos", "androidtv")
    W, H = {"ios": (1320, 2868), "android": (1080, 1920), "tvos": (1920, 1080), "androidtv": (1920, 1080)}[kind]
    img = background(W, H, [(W * 0.5, H * 0.62, W * 0.9, H * 0.6, RED, 0.34), (W * 0.1, H * 1.0, W * 0.6, H * 0.4, BLUE, 0.07)], star_count=16)
    d = ImageDraw.Draw(img)
    if tv_kind:
        size = H * 0.058
        f = font(size, 720)
        lines = wrap(caption, f, W * 0.78)
        y = H * 0.075
        for l in lines:
            d.text((W / 2, y), l, font=f, fill=TEXT, anchor="ma")
            y += size * 1.18
        tw = W * 0.8
        place_tv(img, shot_path(kind, locale, name), (W - tw) / 2, y + H * 0.045, tw, stand=False, glow_alpha=0.5)
    else:
        size = W * 0.074
        f = font(size, 760)
        y = H * 0.045
        for l in wrap(caption, f, W * 0.86):
            d.text((W / 2, y), l, font=f, fill=TEXT, anchor="ma")
            y += size * 1.16
        cut, side = phone(shot_path(kind, locale, name), H * 0.9, kind)
        x = (W - (cut.width - 2 * side)) / 2 - side
        top = y + H * 0.03
        b = cut.width - 2 * side
        shadow(img, x + side, top, b, cut.height, round(b * 0.15), H * 0.03, dy=round(H * 0.02), alpha=0.7)
        over(img, cut, x, top)
    finish(img, path, 85, noise=False)


def wrap(text, f, max_width):
    lines, cur = [], ""
    for word in text.split():
        trial = (cur + " " + word).strip()
        if cur and f.getlength(trial) > max_width:
            lines.append(cur)
            cur = word
        else:
            cur = trial
    lines.append(cur)
    return lines


def main(args):
    want = set(args) or {"readme", "site", "store", "captioned"}
    if "readme" in want:
        banner("en-US", ROOT / "assets/readme/banner.jpg")
        for kind in ("ios", "android"):
            phone_strip(kind, "en-US", ROOT / f"assets/readme/{kind}.jpg", 2000)
        for kind in ("tvos", "androidtv"):
            tv_strip(kind, "en-US", ROOT / f"assets/readme/{kind}.jpg", 2000)
    if "site" in want:
        out = ROOT / "site/assets/img"
        for locale in LOCALES:
            l = locale[:2]
            hero_picture(locale, out / f"hero-{l}-1200.webp", 1200)
            hero_picture(locale, out / f"hero-{l}-2400.webp", 2400)
            for kind in ("tvos", "androidtv"):
                tv_strip(kind, locale, out / f"{kind}-{l}-1200.webp", 1200, 84)
                tv_strip(kind, locale, out / f"{kind}-{l}-2400.webp", 2400, 82)
            for kind in ("ios", "android"):
                phone_cutouts(kind, locale, out)
            og(locale, out / f"og-{l}.jpg")
    if "store" in want:
        st = ROOT / "assets/store"
        phones_scene(5244, 2950, "en-US", ("04-library", "01-home", "03-title"), st / "appstore-header-5244x2950.png", STORE_TAGLINE, 0.07)
        phones_scene(3840, 1646, "en-US", ("04-library", "01-home", "03-title"), st / "appstore-search-3840x1646.png", STORE_TAGLINE)
        feature_graphic(st / "feature-graphic.png")
        tv_banner(st / "androidtv-banner-1280x720.png")
    if "captioned" in want:
        base = ROOT / "assets/store/captioned"
        for locale in LOCALES:
            for kind in ("ios", "android"):
                for i, name in enumerate(PHONE_SCREENS):
                    captioned(kind, locale, name, CAPTIONS[locale][i], base / kind / locale / f"{name}.jpg")
            for kind in ("tvos", "androidtv"):
                for i, name in enumerate(TV_SCREENS):
                    captioned(kind, locale, name, TV_CAPTIONS[locale][i], base / kind / locale / f"{name}.jpg")


STORE_TAGLINE = "Your HolyArrs server, in your pocket"

CAPTIONS = {
    "en-US": (
        "Everything new on your server, at a glance",
        "Browse by genre, cast or collection",
        "Cast, seasons and trailers in one place",
        "Your whole library, filtered your way",
        "Follow every download as it happens",
        "Ask for a title, approve with a tap",
    ),
    "de-DE": (
        "Alles Neue auf deinem Server auf einen Blick",
        "Stöbern nach Genre, Besetzung oder Sammlung",
        "Besetzung, Staffeln und Trailer an einem Ort",
        "Deine Bibliothek, so gefiltert, wie du willst",
        "Jeden Download live verfolgen",
        "Titel anfragen, mit einem Tipp genehmigen",
    ),
}
TV_CAPTIONS = {
    "en-US": (
        "Your server on the big screen",
        "Browse by genre, cast or collection",
        "Title pages made for the couch",
        "New and popular, one press away",
        "See what is coming up",
        "Requests, approved from the sofa",
    ),
    "de-DE": (
        "Dein Server auf dem grossen Bildschirm",
        "Stöbern nach Genre, Besetzung oder Sammlung",
        "Titelseiten fürs Sofa gemacht",
        "Neu und beliebt, einen Klick entfernt",
        "Sehen, was demnächst kommt",
        "Anfragen vom Sofa aus genehmigen",
    ),
}

if __name__ == "__main__":
    main(sys.argv[1:])
