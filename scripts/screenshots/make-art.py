#!/usr/bin/env python3
"""Draws the invented posters, backdrops, title logos and cast portraits in art/ for the demo catalog.

    make-art.py [slug ...]        (no arguments: everything)

Needs Pillow and NumPy. Everything is drawn here from shapes, gradients and noise with fixed seeds and the open-licence
fonts in fonts/ (see fonts/README.md), so a re-run produces the same files. The generated files are committed.
"""
import os
import sys
import zlib
from concurrent.futures import ProcessPoolExecutor

import numpy as np
from PIL import Image, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from artgen import scenes, typo  # noqa: E402
from artgen.canvas import Canvas, P, mix  # noqa: E402
from artgen.people import portrait  # noqa: E402
from catalog import BY_SLUG, PEOPLE, TITLES, cast_of  # noqa: E402

OUT = os.path.join(HERE, "art")
POSTER = (800, 1200)
POSTER_OUT = (600, 900)
BACKDROP = (1920, 1080)
PEOPLE_SIZE = 360


def A(scene, variant, flags, pal, style, **kw):
    return dict(scene=scene, v=variant, flags=flags, pal=pal, style=style, **kw)


SPEC = {
    "harbor-lights": A("sea", "lighthouse", (), (210, 42, 200), "playfair"),
    "the-last-lantern": A("ridges", "", ("night", "fireflies", "lantern", "pines"), (265, 38, 170), "cormorant"),
    "paper-moons": A("toon", "moon", (), (262, 330, 50), "fredoka"),
    "orbit-nine": A("space", "ringed", (), (225, 28, 300), "oswald"),
    "velvet-static": A("noir", "", (), (220, 345, 200), "limelight"),
    "salt-and-ember": A("sea", "ice", (), (200, 20, 185), "anton"),
    "midnight-orchard": A("haunt", "orchard", (), (275, 32, 140), "fraktur"),
    "copper-canyon": A("mesa", "", (), (12, 30, 45), "rye"),
    "cinder-hollow": A("haunt", "", (), (355, 18, 40), "bebas", lay="top"),
    "a-hundred-small-suns": A("neon", "dusk", (), (335, 24, 200), "pacifico"),
    "the-weight-of-rain": A("noir", "", (), (190, 45, 170, 0.9, 1.0), "playfair_i"),
    "kestrel-run": A("neon", "", (), (285, 18, 190), "bungee"),
    "wild-meridian": A("ridges", "", ("night", "aurora", "alpine", "pines"), (215, 150, 280), "josefin", lay="top"),
    "pip-and-the-paper-boat": A("toon", "boat", (), (200, 14, 50), "abril"),
    "the-glass-crown": A("ridges", "", ("sun", "alpine", "castle", "pines"), (250, 35, 200), "cinzel"),
    "hollow-mile": A("forest", "road", (), (170, 70, 90, 0.9, 1.0), "grotesk"),
    "sundial": A("space", "nebula", (), (285, 45, 190), "poiret"),
    "gold-plated-alibi": A("blocks", "", (), (45, 8, 200), "archivo"),
    "the-third-movement": A("frame", "hall", (), (240, 48, 20), "playfair"),
    "ironwood": A("mesa", "night", (), (232, 26, 40), "rye"),
    "quiet-machines": A("space", "station", (), (195, 180, 220, 0.8, 1.0), "grotesk"),
    "ember-tide": A("sea", "sunset", (), (10, 28, 350), "bebas"),
    "the-understudy": A("curtain", "", (), (350, 22, 40), "limelight"),
    "fjord-and-fire": A("ridges", "", ("sun", "alpine", "lake", "pines"), (190, 12, 40), "oswald"),
    "moth-and-lamp": A("toon", "moth", (), (150, 52, 100), "fredoka"),
    "aurora-protocol": A("space", "moonrise", (), (255, 165, 320), "orbitron"),
    "foxglove-summer": A("ridges", "", ("sun", "birds"), (295, 38, 330), "cormorant"),
    "brass-and-thunder": A("sea", "ship", (), (225, 38, 200, 0.9, 1.0), "cinzel"),
    "mister-bumblewick": A("toon", "balloon", (), (190, 12, 50), "pacifico"),
    "the-quiet-hours": A("haunt", "", (), (200, 165, 40, 0.9, 0.9), "lora"),
    "saffron-road": A("mesa", "", (), (25, 42, 12), "abril"),
    "glasshouse": A("glass", "", (), (165, 95, 60), "playfair"),
    "night-shift-diaries": A("blocks", "", (), (310, 352, 200), "bungee"),
    "tidewater": A("sea", "sunset", (), (200, 175, 40, 0.8, 0.9), "cormorant"),
    "the-cartographers": A("mapchart", "parch", (), (35, 28, 20), "cinzel"),
    "northern-lines": A("rail", "", (), (215, 35, 190), "oswald"),
    "cardigan-club": A("blocks", "", (), (20, 175, 60), "abril"),
    "the-long-thaw": A("ridges", "", ("night", "moon", "alpine", "pines"), (205, 190, 270, 0.9, 1.0), "josefin"),
    "deep-field": A("space", "nebula", (), (175, 290, 40), "orbitron"),
    "harrow-and-finch": A("noir", "", (), (30, 38, 20, 0.8, 0.9), "anton"),
    "wildlight": A("ridges", "", ("sun", "alpine", "lake"), (205, 22, 50), "lora"),
    "crownless": A("ridges", "", ("night", "castle", "moon", "pines"), (350, 40, 20, 1.0, 0.9), "cinzel"),
    "lowlight": A("neon", "", (), (270, 330, 190), "bebas"),
    "starling-court": A("frame", "", (), (345, 40, 15), "playfair_i"),
    "paper-planes": A("toon", "planes", (), (220, 18, 45), "fredoka"),
    "signal-fire": A("ridges", "", ("night", "lantern", "pines", "alpine"), (215, 25, 35), "bebas"),
    "neon-parish": A("neon", "", (), (300, 175, 330), "limelight"),
    "greenwake": A("forest", "rays", (), (150, 85, 60), "poiret"),
    "salt-marsh": A("ridges", "", ("night", "moon", "lake"), (170, 150, 60, 0.7, 0.9), "josefin"),
}

PROD = "HARBOR PICTURES"


def seed_of(slug):
    return zlib.crc32(slug.encode()) % 100000


def palette(spec):
    h, h2, h3, *sv = spec["pal"]
    return P(h, h2, h3, *sv)


def draw_scene(c, spec):
    p = palette(spec)
    fn = getattr(scenes, spec["scene"])
    if spec["scene"] == "ridges":
        fn(c, p, spec["v"], spec["flags"])
    else:
        fn(c, p, spec["v"])
    return p


def title_colors(p):
    return mix(p["hi"], (1, 1, 1), 0.55), p["accent"]


def make_poster(t, spec):
    w, h = POSTER
    c = Canvas(w, h, seed_of(t["slug"]), "poster")
    p = draw_scene(c, spec)
    lay = spec.get("lay", "bottom")
    if lay == "top":
        c.shade_top(0.38, 0.8)
        c.shade_bottom(0.78, 0.75)
    else:
        c.shade_bottom(0.5 if spec["scene"] == "blocks" else 0.58, 0.96 if spec["scene"] == "blocks" else 0.88, power=1.35)
        c.shade_top(0.12, 0.35)
    c.vignette(0.3)
    c.grain(0.014)
    fill, accent = title_colors(p)
    st = typo.STYLES[spec["style"]]
    cast = [PEOPLE[i].upper() for i, _ in cast_of(t)]
    top_line = "   ".join(cast[:3]) if t["kind"] == "movie" else f"A {PROD} ORIGINAL SERIES"
    typo.small_caps_line(c, top_line, w / 2, 50 if lay != "top" else h - 205, "oswald", 20, fill, 0.8, 0.28, 300, shadow=0.6)
    lines, size = typo.layout_title(t["name"], spec["style"], w * 0.84, int(st["maxs"] * w * 0.95), 3, h * 0.2)
    im = typo.render_title_mask(lines, spec["style"], size, "c", pad=8)
    tw, th = im.size
    if lay == "top":
        y = 120
    elif lay == "mid":
        y = int(h * 0.5 - th / 2 + 40)
    else:
        y = int(h * 0.855 - th)
    typo.paste_mask(c, im, (w - tw) / 2, y, fill, 1.0, shadow=0.75, shadow_blur=10, shadow_off=(0, 5))
    tag_y = y + th + 14
    tsz = 21
    while typo.width(typo.font("josefin", tsz, 500), t["tagline"].upper(), 0.32 * tsz) > w * 0.86:
        tsz -= 1
    typo.small_caps_line(c, t["tagline"].upper(), w / 2, tag_y, "josefin", tsz, accent, 0.95, 0.32, 500, shadow=0.7)
    ry = tag_y - 12
    c.shape(lambda d: d.rect(w / 2 - 36, ry, w / 2 + 36, ry + 2), accent, 0.9, bbox=(w / 2 - 60, ry - 4, w / 2 + 60, ry + 8))
    b = f"DIRECTED BY {cast[4]}   WRITTEN BY {cast[5]}   MUSIC BY {cast[6]}"
    e = f"A {PROD} {'ORIGINAL SERIES' if t['kind'] == 'show' else 'PRODUCTION'}   {t['year']}"
    typo.credit_block(c, [b, e], w / 2, h - 96, 17, fill, 0.62)
    return c.image().resize(POSTER_OUT, Image.LANCZOS)


def make_backdrop(t, spec):
    w, h = BACKDROP
    c = Canvas(w, h, seed_of(t["slug"]) + 7, "backdrop")
    draw_scene(c, spec)
    lum = float(np.concatenate([c.a[:, : w // 3].ravel(), c.a[int(h * 0.65) :].ravel()]).mean())
    k = min(0.3, max(0.0, (lum - 0.2) * 2.0))
    c.shade_left(0.8 if k > 0.05 else 0.64, 0.62 + k, power=1.1 if k > 0.05 else 1.4)
    c.shade_bottom(0.5, 0.78 + k * 0.5, power=1.4)
    c.shade_top(0.12, 0.3)
    c.vignette(0.3)
    c.grain(0.016)
    return c.image()


def make_logo(t, spec):
    p = palette(spec)
    st = typo.STYLES[spec["style"]]
    fill, accent = title_colors(p)
    W = 820
    lines, size = typo.layout_title(t["name"], spec["style"], W, 190, 3, None, (0.62, 1.0, 0.95))
    im = typo.render_title_mask(lines, spec["style"], size, "c", pad=36)
    if spec["style"] in ("poiret", "josefin"):
        im = im.filter(ImageFilter.MaxFilter(3))
    bb = im.getbbox()
    im = im.crop((max(0, bb[0] - 24), max(0, bb[1] - 24), min(im.width, bb[2] + 24), min(im.height, bb[3] + 24)))
    m = np.asarray(im, np.float32) / 255
    sha = np.asarray(im.filter(ImageFilter.GaussianBlur(7)), np.float32) / 255 * 0.7
    yy = np.linspace(0, 1, im.height)[:, None, None]
    top = np.minimum(fill, 1.0) * 255
    bot = np.minimum(mix(fill, accent, 0.45), 1.0) * 255
    col = top + (bot - top) * np.clip(yy * 1.2 - 0.1, 0, 1)
    out = np.zeros((im.height, im.width, 4), np.float32)
    out[..., :3] = col * m[..., None] + 8 * (1 - m[..., None])
    out[..., 3] = np.clip(m + sha * (1 - m), 0, 1) * 255
    rgba = Image.fromarray(out.astype(np.uint8), "RGBA")
    if rgba.width > 600:
        rgba = rgba.resize((600, int(rgba.height * 600 / rgba.width)), Image.LANCZOS)
    return rgba


def render(slug):
    t = BY_SLUG[slug]
    spec = SPEC[slug]
    make_poster(t, spec).save(os.path.join(OUT, f"poster-{slug}.jpg"), quality=82, optimize=True, progressive=True)
    make_backdrop(t, spec).save(os.path.join(OUT, f"backdrop-{slug}.jpg"), quality=73, optimize=True, progressive=True)
    make_logo(t, spec).save(os.path.join(OUT, f"logo-{slug}.png"), optimize=True)
    return slug


def render_person(i):
    portrait(i, PEOPLE_SIZE).save(os.path.join(OUT, f"profile-{i + 1}.jpg"), quality=86, optimize=True)
    return i


def main():
    os.makedirs(OUT, exist_ok=True)
    only = sys.argv[1:]
    missing = [t["slug"] for t in TITLES if t["slug"] not in SPEC]
    if missing:
        sys.exit(f"no art spec for {missing}")
    slugs = only or [t["slug"] for t in TITLES]
    with ProcessPoolExecutor() as pool:
        for s in pool.map(render, slugs):
            print("title", s, flush=True)
        if not only:
            list(pool.map(render_person, range(len(PEOPLE))))
    if not only:
        keep = {f"{k}-{t['slug']}.{e}" for t in TITLES for k, e in (("poster", "jpg"), ("backdrop", "jpg"), ("logo", "png"))}
        keep |= {f"profile-{i + 1}.jpg" for i in range(len(PEOPLE))} | {"NOTICE.md"}
        for f in os.listdir(OUT):
            if f not in keep:
                os.remove(os.path.join(OUT, f))
    print(f"done: {len(slugs)} titles")


if __name__ == "__main__":
    main()
