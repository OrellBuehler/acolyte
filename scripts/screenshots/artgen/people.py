import math
import random

import numpy as np
from PIL import Image

from .canvas import C, Canvas, hsv, mix, vg

SKIN = ["#f4d6c0", "#ecc2a0", "#e0a97e", "#c98b5e", "#a96b43", "#8a5233", "#6a3d25", "#4b2b1b"]
HAIR = ["#16100e", "#2b1b12", "#4a2c1a", "#7a4a26", "#b0793a", "#d7b46a", "#9a9a9e", "#e7e5e1", "#6b2a4a", "#a8441f"]
BG = [(205, 55, 0.42), (28, 45, 0.55), (165, 45, 0.38), (265, 35, 0.42), (340, 40, 0.5), (45, 55, 0.55), (225, 40, 0.38), (95, 38, 0.4), (10, 50, 0.5), (190, 50, 0.4)]
CLOTH = [(215, 0.35, 0.3), (350, 0.55, 0.55), (35, 0.5, 0.7), (160, 0.45, 0.4), (0, 0.0, 0.12), (265, 0.4, 0.45), (200, 0.05, 0.85), (20, 0.65, 0.6), (95, 0.35, 0.42), (310, 0.4, 0.5)]
HAIR_STYLES = ["short", "long", "bun", "curly", "bald", "scarf", "bob", "crop", "wave", "long"]
CLOTHES = ["crew", "vneck", "collar", "turtle", "blazer", "scoop"]


def portrait(i, size=360):
    r = random.Random(1000 + i * 7)
    s = size
    c = Canvas(s, s, 2000 + i, "portrait")
    bh, bs, bv = BG[(i * 3 + 1) % len(BG)]
    c.grad([(0, hsv(bh, bs / 100 * 1.4, bv * 1.15)), (1, hsv(bh + 14, bs / 100 * 1.5, bv * 0.62))])
    c.radial(s * 0.5, s * 0.4, s * 0.75, hsv(bh, 0.25, 1.0), 0.25, 1.6)
    for _ in range(7):
        c.radial(r.uniform(0, s), r.uniform(0, s * 0.7), s * r.uniform(0.06, 0.16), hsv(bh + r.uniform(-20, 20), 0.3, 1.0), 0.12, 1.2)
    skin = C(SKIN[(i * 5 + 2) % len(SKIN)])
    skin_d = skin * 0.82
    hair = C(HAIR[(i * 7 + 3) % len(HAIR)])
    style = HAIR_STYLES[(i * 3 + i // 5) % len(HAIR_STYLES)]
    cth, cs, cv = CLOTH[(i * 4 + 1) % len(CLOTH)]
    cloth = hsv(cth, cs, cv)
    kind = CLOTHES[(i * 5 + i // 3) % len(CLOTHES)]
    male = i % 2 == 0
    cx = s * 0.5 + r.uniform(-0.01, 0.01) * s
    hy = s * 0.43
    hrx, hry = s * (0.155 if not male else 0.17), s * 0.2
    glasses = i % 5 == 2
    beard = male and i % 4 == 0 and style != "scarf"
    if style == "scarf":
        style_back = "scarf"
    else:
        style_back = style

    def back(d):
        if style in ("long", "wave"):
            d.rrect(cx - hrx * 1.28, hy - hry * 0.9, cx + hrx * 1.28, s * (0.86 if style == "long" else 0.74), hrx * 0.7)
        if style == "bob":
            d.rrect(cx - hrx * 1.3, hy - hry * 0.9, cx + hrx * 1.3, hy + hry * 0.85, hrx * 0.6)
        if style == "bun":
            d.ell(cx, hy - hry * 1.15, hrx * 0.45)
        if style == "curly":
            for k in range(22):
                a = math.pi * (0.9 + k * 0.1 * 0.97) if k < 22 else 0
                ang = -math.pi * 0.05 + (k / 21) * math.pi * 1.1
                d.ell(cx + math.cos(math.pi + ang) * hrx * 1.25, hy - hry * 0.1 + math.sin(math.pi + ang) * hry * 1.2, hrx * 0.33)
            d.ell(cx, hy - hry * 0.4, hrx * 1.2, hry * 1.15)
        if style == "scarf":
            d.ell(cx, hy, hrx * 1.28, hry * 1.2)
            d.rrect(cx - hrx * 1.28, hy, cx + hrx * 1.28, s * 0.74, hrx * 0.5)

    c.shape(back, hair if style != "scarf" else hsv(cth + 20, 0.5, 0.55), 1.0)
    # shoulders & clothes
    sw = s * (0.36 if male else 0.31)
    top = s * 0.7

    def body(d):
        d.rrect(cx - sw, top, cx + sw, s * 1.2, s * 0.14)
        d.poly([(cx - sw * 0.5, top + s * 0.06), (cx + sw * 0.5, top + s * 0.06), (cx + sw * 0.2, top - s * 0.02), (cx - sw * 0.2, top - s * 0.02)])

    c.shape(body, vg(top, s, cloth * 1.15, cloth * 0.7), 1.0)
    # neck
    nw = hrx * 0.5

    def neck(d):
        d.rrect(cx - nw, hy + hry * 0.55, cx + nw, top + s * 0.07, nw * 0.4)

    c.shape(neck, skin_d, 1.0)
    # neckline
    if kind == "crew":
        c.shape(lambda d: d.ell(cx, top + s * 0.015, nw * 1.35, s * 0.045), skin_d)
    elif kind == "vneck":
        c.shape(lambda d: d.poly([(cx - nw * 1.4, top - s * 0.01), (cx + nw * 1.4, top - s * 0.01), (cx, top + s * 0.17)]), skin_d)
    elif kind == "scoop":
        c.shape(lambda d: d.ell(cx, top - s * 0.01, nw * 1.8, s * 0.09), skin_d)
    elif kind == "turtle":
        c.shape(lambda d: d.rrect(cx - nw * 1.35, top - s * 0.07, cx + nw * 1.35, top + s * 0.06, s * 0.03), cloth * 1.25)
    elif kind == "collar":
        c.shape(lambda d: d.poly([(cx - nw * 1.25, top - s * 0.04), (cx, top + s * 0.14), (cx + nw * 1.25, top - s * 0.04), (cx + nw * 2.0, top + s * 0.02), (cx, top + s * 0.22), (cx - nw * 2.0, top + s * 0.02)]), (0.93, 0.93, 0.95))
        c.shape(lambda d: d.poly([(cx - nw * 1.25, top - s * 0.04), (cx, top + s * 0.14), (cx + nw * 1.25, top - s * 0.04)]), skin_d)
    elif kind == "blazer":
        c.shape(lambda d: d.poly([(cx - nw * 1.3, top - s * 0.03), (cx + nw * 1.3, top - s * 0.03), (cx, top + s * 0.25)]), (0.92, 0.92, 0.94))
        c.shape(lambda d: d.poly([(cx - nw * 1.3, top - s * 0.03), (cx - nw * 0.2, top + s * 0.2), (cx - nw * 1.3, top + s * 0.3), (cx - nw * 2.6, top + s * 0.1)]), cloth * 0.75)
        c.shape(lambda d: d.poly([(cx + nw * 1.3, top - s * 0.03), (cx + nw * 0.2, top + s * 0.2), (cx + nw * 1.3, top + s * 0.3), (cx + nw * 2.6, top + s * 0.1)]), cloth * 0.75)
        c.shape(lambda d: d.poly([(cx - nw * 1.3, top - s * 0.03), (cx + nw * 1.3, top - s * 0.03), (cx, top + s * 0.1)]), skin_d)
    # ears and head
    c.shape(lambda d: (d.ell(cx - hrx * 0.98, hy + hry * 0.05, hrx * 0.14, hry * 0.17), d.ell(cx + hrx * 0.98, hy + hry * 0.05, hrx * 0.14, hry * 0.17)), skin_d * 0.95)
    c.shape(lambda d: d.ell(cx, hy, hrx, hry), vg(hy - hry, hy + hry, skin * 1.04, skin * 0.96))
    # soft shadow on one side
    c.shape(lambda d: d.ell(cx + hrx * 0.5, hy + hry * 0.05, hrx * 0.62, hry * 0.98), skin_d * 0.88, 0.45, blur=s * 0.012, bbox=(cx - hrx * 1.3, hy - hry * 1.3, cx + hrx * 1.3, hy + hry * 1.3))
    if style == "scarf":
        c.shape(lambda d: (d.ell(cx, hy, hrx * 1.2, hry * 1.12), d.rect(cx - hrx * 1.2, hy - hry * 1.1, cx + hrx * 1.2, hy - hry * 0.2)), hsv(cth + 20, 0.5, 0.55), 1.0, erase=lambda d: d.ell(cx, hy + hry * 0.28, hrx * 0.86, hry * 0.84))
        c.shape(lambda d: d.ell(cx, hy + hry * 0.28, hrx * 0.86, hry * 0.84), vg(hy - hry, hy + hry, skin * 1.04, skin * 0.96), 0.0)
    # hair front
    def front(d):
        if style in ("short", "crop", "wave", "long", "bob", "curly"):
            d.poly([(cx - hrx * 1.02, hy - hry * 0.05), (cx - hrx * 0.95, hy - hry * 0.7), (cx - hrx * 0.5, hy - hry * 1.05), (cx + hrx * 0.5, hy - hry * 1.08), (cx + hrx * 0.95, hy - hry * 0.7), (cx + hrx * 1.02, hy - hry * 0.05), (cx + hrx * 0.82, hy - hry * 0.45), (cx + hrx * 0.2, hy - hry * 0.62), (cx - hrx * 0.3, hy - hry * 0.52), (cx - hrx * 0.82, hy - hry * 0.3)])
            d.ell(cx, hy - hry * 0.72, hrx * 0.98, hry * 0.45)
        if style == "bun":
            d.ell(cx, hy - hry * 0.7, hrx * 1.0, hry * 0.45)
        if style == "short":
            d.ell(cx - hrx * 0.4, hy - hry * 0.78, hrx * 0.7, hry * 0.3)

    if style != "bald" and style != "scarf":
        c.shape(front, hair * 1.05, 1.0)
        c.shape(lambda d: d.ell(cx + hrx * 0.45, hy - hry * 0.8, hrx * 0.55, hry * 0.25), hair * 1.5, 0.35, blur=s * 0.008, bbox=(cx - hrx * 1.3, hy - hry * 1.3, cx + hrx * 1.3, hy))
    # beard
    if beard:
        c.shape(lambda d: d.ell(cx, hy + hry * 0.55, hrx * 0.88, hry * 0.5), hair, 0.95, erase=lambda d: d.ell(cx, hy + hry * 0.32, hrx * 0.4, hry * 0.13))
    # features
    ey = hy + hry * 0.02
    ex = hrx * 0.42
    ink = (0.12, 0.08, 0.08)
    c.shape(lambda d: (d.ell(cx - ex, ey, hrx * 0.11, hry * 0.075), d.ell(cx + ex, ey, hrx * 0.11, hry * 0.075)), ink, 0.95)
    c.shape(lambda d: (d.ell(cx - ex + hrx * 0.03, ey - hry * 0.02, hrx * 0.035), d.ell(cx + ex + hrx * 0.03, ey - hry * 0.02, hrx * 0.035)), (1, 1, 1), 0.85)
    brow = hair * 0.85 if style not in ("bald", "scarf") else C(HAIR[1])
    c.shape(lambda d: (d.line([(cx - ex - hrx * 0.17, ey - hry * 0.17), (cx - ex + hrx * 0.2, ey - hry * 0.19)], s * 0.011), d.line([(cx + ex - hrx * 0.2, ey - hry * 0.19), (cx + ex + hrx * 0.17, ey - hry * 0.17)], s * 0.011)), brow, 0.9)
    c.shape(lambda d: d.line([(cx + hrx * 0.04, ey + hry * 0.1), (cx - hrx * 0.04, ey + hry * 0.3), (cx + hrx * 0.1, ey + hry * 0.33)], s * 0.008), skin_d * 0.7, 0.9)
    mood = (i // 2) % 3
    my = hy + hry * 0.56
    if mood == 0:
        c.shape(lambda d: d.arc(cx, my - hry * 0.12, hrx * 0.28, hry * 0.17, 20, 160, s * 0.011), (0.5, 0.2, 0.2) if not beard else (0.9, 0.7, 0.65), 0.95)
    elif mood == 1:
        c.shape(lambda d: d.line([(cx - hrx * 0.2, my), (cx + hrx * 0.2, my - hry * 0.01)], s * 0.011), (0.5, 0.2, 0.2) if not beard else (0.9, 0.7, 0.65), 0.95)
    else:
        c.shape(lambda d: d.pie(cx, my - hry * 0.08, hrx * 0.25, 10, 170), (0.95, 0.93, 0.92), 0.95)
        c.shape(lambda d: d.arc(cx, my - hry * 0.08, hrx * 0.25, hrx * 0.25, 10, 170, s * 0.009), (0.5, 0.2, 0.2), 0.9)
    c.shape(lambda d: (d.ell(cx - ex * 1.25, my - hry * 0.2, hrx * 0.14, hry * 0.09), d.ell(cx + ex * 1.25, my - hry * 0.2, hrx * 0.14, hry * 0.09)), (0.9, 0.35, 0.35), 0.14, blur=s * 0.01, bbox=(0, 0, s, s))
    if glasses:
        def gl(d):
            for sg in (-1, 1):
                d.arc(cx + sg * ex, ey, hrx * 0.27, hrx * 0.23, 0, 360, s * 0.008)
            d.line([(cx - ex + hrx * 0.27, ey), (cx + ex - hrx * 0.27, ey)], s * 0.008)

        c.shape(gl, (0.1, 0.08, 0.1), 0.9)
    c.vignette(0.35)
    c.grain(0.012)
    return c.image()
