import math

import numpy as np
from PIL import Image, ImageFilter

from .canvas import C, hg, mix, vg


def skyline(c, ybase, hmin, hmax, wmin, wmax, color, p_lit=0.3, lit=None, antenna=0.2, alpha=1.0, step=0.3, win_alpha=(110, 255), seed=0):
    r = c.r
    x = -r.uniform(0, wmin)
    blds = []
    while x < c.w:
        bw = r.uniform(wmin, wmax)
        bh = r.uniform(hmin, hmax)
        blds.append((x, ybase - bh, bw, bh, r.random() < step, r.random() < antenna))
        x += bw * r.uniform(0.82, 1.0)
    ymin = min(b[1] for b in blds) - c.u * 0.1

    def f(d):
        for x, y, bw, bh, st, an in blds:
            d.rect(x, y, x + bw, c.h + 5)
            if st:
                d.rect(x + bw * 0.18, y - bh * 0.09, x + bw * 0.82, y + 2)
            if an:
                d.rect(x + bw * 0.5 - 1.5, y - bh * 0.22, x + bw * 0.5 + 1.5, y + 2)

    c.shape(f, color, alpha, bbox=(0, ymin, c.w, c.h))
    if lit is not None:
        ww, wh = c.u * 0.0085, c.u * 0.0125
        gx, gy = c.u * 0.0185, c.u * 0.0245

        def g(d):
            for x, y, bw, bh, st, an in blds:
                ny = int((ybase - y) / gy) - 1
                nx = max(1, int((bw - gx) / gx))
                for j in range(ny):
                    for i in range(nx):
                        if r.random() < p_lit:
                            d.v = int(r.uniform(*win_alpha))
                            px = x + gx * 0.7 + i * gx
                            py = y + gy * 0.8 + j * gy
                            if py < ybase and px + ww < x + bw - gx * 0.3:
                                d.rect(px, py, px + ww, py + wh)
            d.v = 255

        c.shape(g, lit, 1.0, bbox=(0, ymin, c.w, ybase))
        c.shape(g, lit, 0.35, blur=c.u * 0.006, bbox=(0, ymin, c.w, ybase), mode="add")
    return blds


def figure(d, x, yb, h, lean=0.0, wide=1.0):
    hw = h * 0.1 * wide
    d.poly([(x - hw * 0.95, yb), (x - hw * 0.1, yb), (x + lean * 0.3, yb - h * 0.46), (x - hw * 1.1 + lean * 0.3, yb - h * 0.46)])
    d.poly([(x + hw * 0.95, yb), (x + hw * 0.1, yb), (x + lean * 0.3, yb - h * 0.46), (x + hw * 1.1 + lean * 0.3, yb - h * 0.46)])
    d.rrect(x - hw * 1.25 + lean * 0.5, yb - h * 0.82, x + hw * 1.25 + lean * 0.5, yb - h * 0.4, hw * 0.7)
    d.line([(x - hw * 1.1 + lean * 0.5, yb - h * 0.76), (x - hw * 1.3, yb - h * 0.5)], hw * 0.55)
    d.line([(x + hw * 1.1 + lean * 0.5, yb - h * 0.76), (x + hw * 1.3, yb - h * 0.5)], hw * 0.55)
    d.rect(x - hw * 0.35 + lean * 0.6, yb - h * 0.86, x + hw * 0.35 + lean * 0.6, yb - h * 0.78)
    d.ell(x + lean * 0.7, yb - h * 0.92, h * 0.065)


def pines(c, xs, ys, color, hmin, hmax, step_x=(0.6, 1.4), seed=0, alpha=1.0, tw=0.42):
    r = c.r
    trees = []
    x = float(xs[0])
    while x < xs[-1]:
        i = min(len(xs) - 1, int((x - xs[0]) / (xs[1] - xs[0])))
        trees.append((x, ys[i] + 2, r.uniform(hmin, hmax)))
        x += r.uniform(*step_x) * hmin * tw * 1.6

    def f(d):
        for x, y, h in trees:
            w = h * tw
            tiers = 4
            for k in range(tiers):
                t0 = k / tiers
                top = y - h + h * 0.78 * t0
                bot = y - h * 0.16 - (h * 0.62) * (1 - (k + 1) / tiers) * 0.0 + 0
                wk = w * (0.35 + 0.65 * (k + 1) / tiers)
                d.poly([(x, top), (x + wk * 0.5, top + h * 0.34), (x - wk * 0.5, top + h * 0.34)])
            d.rect(x - w * 0.04, y - h * 0.2, x + w * 0.04, y + 3)

    ytop = min(t[1] - t[2] for t in trees) - 4
    c.shape(f, color, alpha, bbox=(0, ytop, c.w, c.h))
    return trees


def rain(c, n, color, alpha=0.3, angle=12, length=(0.03, 0.08), width=1.0, region=None):
    r = c.r
    x0, y0, x1, y1 = region or (0, 0, c.w, c.h)
    sl = math.tan(math.radians(angle))
    lines = []
    for _ in range(n):
        x, y = r.uniform(x0 - 100, x1), r.uniform(y0, y1)
        ln = r.uniform(*length) * c.u
        lines.append((x, y, x + ln * sl, y + ln, r.uniform(0.35, 1.0)))

    def f(d):
        for x, y, xx, yy, b in lines:
            d.v = int(255 * b)
            d.line([(x, y), (xx, yy)], width)
        d.v = 255

    c.shape(f, color, alpha, bbox=(x0, y0, x1, y1))


def birds(c, n, region, color, size=0.012, alpha=0.9):
    x0, y0, x1, y1 = region
    pts = [(c.r.uniform(x0, x1), c.r.uniform(y0, y1), c.r.uniform(0.6, 1.3)) for _ in range(n)]

    def f(d):
        for x, y, s in pts:
            u = c.u * size * s
            d.line([(x - u, y - u * 0.35), (x - u * 0.35, y - u * 0.55), (x, y)], max(1.2, u * 0.12))
            d.line([(x, y), (x + u * 0.35, y - u * 0.55), (x + u, y - u * 0.35)], max(1.2, u * 0.12))

    c.shape(f, color, alpha, bbox=(x0 - 30, y0 - 30, x1 + 30, y1 + 30))


def fireflies(c, n, region, color, k=0.9):
    x0, y0, x1, y1 = region
    for _ in range(n):
        x, y = c.r.uniform(x0, x1), c.r.uniform(y0, y1)
        s = c.u * c.r.uniform(0.006, 0.02)
        c.radial(x, y, s * 3.2, color, k * c.r.uniform(0.4, 1.0), 2.0)
        c.radial(x, y, s * 0.55, (1, 1, 0.9), 1.0, 1.0)


def aurora(c, yc, th, colors, amp=0.12, k=0.9, freq=2.2):
    n = c.w
    xs = np.arange(n, dtype=np.float32)
    ln = c.line_noise(n, c.w / freq, 3, 0.55, 5)
    ln2 = c.line_noise(n, c.w / (freq * 3.5), 3, 0.5, 6)
    cen = yc + ln * amp * c.h
    stripes = 0.55 + 0.45 * (c.line_noise(n, c.w / 40, 2, 0.5, 8) * 0.5 + 0.5)
    inten = (0.5 + 0.5 * ln2) * stripes
    w4, h4 = c.w // 2, c.h // 2
    Y = np.arange(h4, dtype=np.float32)[:, None] * 2
    d = (Y - cen[None, ::2]) / th
    low = np.exp(-np.clip(d, 0, None) ** 2 * 5.0)
    up = np.exp(-np.clip(-d, 0, None) ** 2 * 0.55)
    m = np.where(d >= 0, low, up) * inten[None, ::2] * k
    m = np.clip(m, 0, 1)
    im = Image.fromarray((m * 255).astype(np.uint8)).resize((c.w, c.h), Image.BICUBIC).filter(ImageFilter.GaussianBlur(c.u * 0.004))
    mk = np.asarray(im, np.float32) / 255
    grad_t = np.clip((c.Y - (yc - th * 1.5)) / (th * 3), 0, 1)[..., None]
    col = C(colors[0]) * (1 - grad_t) + C(colors[1]) * grad_t
    c.a[:] = 1 - (1 - c.a) * (1 - col * mk[..., None])


def castle(c, x, y, s, color, lit):
    def f(d):
        d.rect(x - s * 0.5, y - s * 0.55, x + s * 0.5, y + 4)
        for i in range(7):
            d.rect(x - s * 0.5 + i * s * 0.155, y - s * 0.62, x - s * 0.5 + i * s * 0.155 + s * 0.08, y - s * 0.55)
        for tx, tw, th, roof in ((-0.62, 0.2, 1.1, 0.5), (0.58, 0.22, 1.3, 0.6), (0.0, 0.26, 1.6, 0.8), (-0.3, 0.15, 0.85, 0.4), (0.3, 0.16, 0.95, 0.45)):
            cx = x + tx * s
            d.rect(cx - tw * s * 0.5, y - th * s * 0.8, cx + tw * s * 0.5, y + 4)
            d.poly([(cx - tw * s * 0.62, y - th * s * 0.8), (cx + tw * s * 0.62, y - th * s * 0.8), (cx, y - th * s * 0.8 - roof * s * 0.8)])
            d.rect(cx - 1.5, y - th * s * 0.8 - roof * s * 0.8 - s * 0.18, cx + 1.5, y - th * s * 0.8 - roof * s * 0.7)
            d.poly([(cx + 1.5, y - th * s * 0.8 - roof * s * 0.8 - s * 0.18), (cx + s * 0.1, y - th * s * 0.8 - roof * s * 0.8 - s * 0.14), (cx + 1.5, y - th * s * 0.8 - roof * s * 0.8 - s * 0.1)])

    c.shape(f, color, bbox=(x - s * 1.2, y - s * 2.2, x + s * 1.2, y + 10))

    def g(d):
        for tx, th in ((-0.62, 0.8), (0.58, 1.0), (0.0, 1.2), (-0.3, 0.55), (0.3, 0.65)):
            d.rect(x + tx * s - s * 0.012, y - th * s * 0.8 + s * 0.1, x + tx * s + s * 0.012, y - th * s * 0.8 + s * 0.17)
        for tx in (-0.3, -0.08, 0.14, 0.34):
            d.rect(x + tx * s, y - s * 0.38, x + tx * s + s * 0.03, y - s * 0.31)

    c.shape(g, lit, 0.95, bbox=(x - s * 1.2, y - s * 2.2, x + s * 1.2, y + 10))
    c.shape(g, lit, 0.5, blur=s * 0.03, bbox=(x - s * 1.2, y - s * 2.2, x + s * 1.2, y + 10), mode="add")


def cactus(d, x, yb, h):
    w = h * 0.13
    d.rrect(x - w / 2, yb - h, x + w / 2, yb + 3, w / 2)
    d.rrect(x - w * 2.0, yb - h * 0.62, x - w * 0.9, yb - h * 0.22, w / 2)
    d.rect(x - w * 2.0, yb - h * 0.34, x - w * 0.3, yb - h * 0.22)
    d.rrect(x + w * 0.9, yb - h * 0.78, x + w * 2.0, yb - h * 0.4, w / 2)
    d.rect(x + w * 0.3, yb - h * 0.52, x + w * 2.0, yb - h * 0.4)


# ---------------------------------------------------------------------------------------------- scenes


def space(c, p, v):
    c.grad([(0, p["deep"]), (0.6, mix(p["deep"], p["mid"], 0.7)), (1, p["dark"])])
    n1 = c.noise_full(c.u * 0.55, 5, 1)
    n2 = c.noise_full(c.u * 0.4, 5, 2)
    neb = 0.9 if v == "nebula" else 0.55
    c.paint(np.clip((n1 - 0.38) * 2.0, 0, 1) ** 1.5 * neb * 0.55, p["mid"] * 1.6, 1.0, mode="add")
    c.paint(np.clip((n2 - 0.5) * 2.4, 0, 1) ** 1.6 * neb * 0.5, p["alt"] * 0.7, 1.0, mode="add")
    c.stars(int(c.w * c.h / 1400), 1.0, 1.0, (1, 0.97, 0.92), big=7)
    u = c.u
    fx, fy = c.w * c.fx, c.h * c.fy
    if v == "ringed":
        r = u * (0.2 if c.poster else 0.19)
        c.radial(fx, fy, r * 2.6, p["accent"], 0.35, 2.0)
        ang = -17 if c.poster else -14
        rx = r * 2.5
        ry = rx * 0.21

        def ring(d, back):
            for i, (k, wd) in enumerate(((1.0, 7), (0.88, 14), (0.78, 4), (0.7, 9))):
                a0, a1 = (180, 360) if back else (0, 180)
                d.arc(fx, fy, rx * k, ry * k, a0, a1, wd * u / 800)

        bb = (fx - rx - 20, fy - rx - 20, fx + rx + 20, fy + rx + 20)
        c.shape(lambda d: ring(d, True), p["hi"], 0.55, bbox=bb, rot=(ang, fx, fy))
        c.sphere(fx, fy, r, p["hi"], p["deep"], (-0.6, -0.5), bands=(p["low"], p["alt"], 22, 0.5), rim=p["accent"], rimk=0.8)
        c.shape(lambda d: ring(d, False), p["hi"], 0.85, bbox=bb, rot=(ang, fx, fy))
    elif v == "moonrise":
        r = u * 0.075
        mx, my = c.w * (0.5 if c.poster else 0.7), c.h * (0.2 if c.poster else 0.24)
        c.radial(mx, my, r * 7, p["alt"], 0.35, 2.2)
        c.sphere(mx, my, r, p["hi"], p["mid"], (-0.4, -0.5))
    elif v == "station":
        c.radial(fx, fy, u * 0.6, p["accent"], 0.7, 2.0)
        c.radial(fx, fy, u * 0.25, p["hi"], 0.95, 2.0)
        rr = u * 0.3
        ang = -24

        def wheel(d):
            d.arc(fx, fy, rr, rr * 0.33, 0, 360, u * 0.02)
            d.arc(fx, fy, rr * 0.62, rr * 0.2, 0, 360, u * 0.008)
            for k in range(8):
                a = k * math.pi / 4
                d.line([(fx, fy), (fx + math.cos(a) * rr, fy + math.sin(a) * rr * 0.33)], u * 0.006)
            d.ell(fx, fy, u * 0.04, u * 0.03)

        c.shape(wheel, p["deep"], 1.0, bbox=(fx - rr - 20, fy - rr - 20, fx + rr + 20, fy + rr + 20), rot=(ang, fx, fy))
        c.radial(fx, fy, u * 0.12, p["hi"], 0.8, 3.0)
    if v in ("ringed", "moonrise", "nebula"):
        if c.poster:
            cx, cy, R = c.w * 0.38, c.h * 0.6 + u * 1.25, u * 1.25
        else:
            cx, cy, R = c.w * 0.97, c.h * 1.22, c.h * 0.92
        c.radial(cx, cy, R * 1.06, p["accent"], 0.3, 5.0, mode="screen")
        c.sphere(cx, cy, R, mix(p["mid"], p["low"], 0.5), p["deep"], (-0.45, -0.85) if c.poster else (-0.9, -0.5), bands=(p["dark"], p["mid"], 30, 0.5), rim=p["accent"], rimk=1.3, gamma=1.6)
    if v == "station" or v == "nebula":
        if v == "nebula":
            for dx in (-0.12, 0.1):
                c.radial(fx + dx * c.w, fy + dx * c.h * 0.4, u * 0.3, p["hi"], 0.9, 2.5)
        if c.poster:
            c.shade_bottom(0.55, 0.5)
    c.bloom(0.6, 0.55, 0.03)


def neon(c, p, v):
    u = c.u
    gy = int(c.h * (0.80 if c.poster else 0.74))
    c.grad([(0, p["deep"]), (0.35, p["sky"]), (gy / c.h, p["low"]), (1, p["deep"])])
    fx = c.w * c.fx
    sy = gy - u * (0.28 if c.poster else 0.22)
    R = u * (0.3 if c.poster else 0.27)
    c.radial(fx, sy, R * 3.0, p["accent"], 0.45, 2.0)
    c.stars(int(c.w * c.h / 5000), 0.4, 0.7)

    def sun(d):
        d.ell(fx, sy, R)

    c.shape(sun, vg(sy - R, sy + R, p["hi"], p["accent"]), 1.0)

    def slats(d):
        yy = sy - R * 0.05
        k = 0
        while yy < sy + R:
            hh = R * (0.04 + 0.014 * k)
            d.rect(fx - R - 5, yy, fx + R + 5, yy + hh)
            yy += R * (0.12 - 0.003 * k) + hh
            k += 1

    c.shape(slats, vg(sy, sy + R, p["sky"], p["low"]), 1.0, bbox=(fx - R - 10, sy - R, fx + R + 10, sy + R + 5))
    far = mix(p["sky"], p["low"], 0.5)
    skyline(c, gy + 4, u * 0.12, u * (0.55 if c.poster else 0.45), u * 0.05, u * 0.11, mix(far, p["dark"], 0.35), 0.25, p["accent"], seed=1, win_alpha=(60, 150))
    c.mist(gy - u * 0.06, u * 0.09, p["low"], 0.45, seed=2)
    skyline(c, gy + 4, u * 0.1, u * (0.38 if c.poster else 0.32), u * 0.06, u * 0.12, p["deep"] * 1.2, 0.3, p["alt2"], antenna=0.35, seed=2)
    c.shape(lambda d: d.rect(0, gy, c.w, c.h), p["deep"] * 0.8)
    c.reflect(gy, fade=0.5, blur=3, dark=0.6, ripple=3)
    for _ in range(26):
        x = c.r.uniform(0, c.w)
        y0 = gy + c.r.uniform(0, c.h - gy) * 0.5
        c.radial(x, y0, u * 0.03, c.r.choice([p["accent"], p["alt"]]), 0.12, 1.2, ry=u * 0.1)
    if v == "dusk":
        base = gy + (c.h - gy) * (0.35 if c.poster else 0.5)

        def fig(d):
            d.rect(0, base, c.w, c.h + 3)
            fx2 = c.w * (0.46 if c.poster else 0.64)
            hh = u * 0.2
            figure(d, fx2 - u * 0.035, base, hh, lean=-hh * 0.03)
            figure(d, fx2 + u * 0.04, base, hh * 0.94, lean=hh * 0.03)

        c.shape(fig, p["deep"] * 0.7)
    c.bloom(0.55, 0.6, 0.025)


def noir(c, p, v):
    u = c.u
    c.grad([(0, p["deep"]), (0.45, p["dark"]), (0.8, mix(p["dark"], p["sky"], 0.55)), (1, p["dark"])])
    fx = c.w * c.fx
    c.radial(fx, c.h * 0.55, u * 0.9, p["low"] * 0.7, 0.55, 2.0, ry=u * 0.6)
    gy = int(c.h * (0.88 if c.poster else 0.86))
    skyline(c, gy, u * 0.15, u * (0.7 if c.poster else 0.55), u * 0.06, u * 0.12, mix(p["dark"], p["sky"], 0.45), 0.12, p["alt2"], seed=3, win_alpha=(40, 110))
    c.mist(c.h * 0.6, u * 0.18, p["low"], 0.35, seed=1)
    skyline(c, gy, u * 0.1, u * (0.55 if c.poster else 0.42), u * 0.07, u * 0.14, mix(p["deep"], p["dark"], 0.5), 0.2, p["alt2"], seed=4, win_alpha=(50, 130))
    fw = u * (0.5 if c.poster else 0.42)
    ft = c.h * (0.2 if c.poster else 0.14)
    c.shape(lambda d: d.rect(fx - fw / 2, ft, fx + fw / 2, c.h), p["deep"] * 0.8)
    ww, wh = fw * 0.12, fw * 0.17
    gx, gyy = fw * 0.2, fw * 0.27
    lit_i, lit_j = 2, 3

    def wins(d):
        for j in range(int((c.h * 0.8 - ft) / gyy)):
            for i in range(4):
                d.rect(fx - fw / 2 + gx * 0.5 + i * gx, ft + gyy * 0.5 + j * gyy, fx - fw / 2 + gx * 0.5 + i * gx + ww, ft + gyy * 0.5 + j * gyy + wh)

    c.shape(wins, p["dark"] * 1.3, 0.8)
    lx, ly = fx - fw / 2 + gx * 0.5 + lit_i * gx, ft + gyy * 0.5 + lit_j * gyy
    c.radial(lx + ww / 2, ly + wh / 2, u * 0.32, p["accent"], 0.5, 2.0)

    def lw(d):
        d.rect(lx, ly, lx + ww, ly + wh)

    c.shape(lw, p["hi"], 1.0)

    def fg(d):
        d.v = 255
        figure(d, lx + ww / 2, ly + wh + 1, wh * 0.95)

    c.shape(fg, p["deep"], 0.95, bbox=(lx - 20, ly - 5, lx + ww + 20, ly + wh + 5))
    c.shape(lambda d: d.rect(lx + ww / 2 - 1, ly, lx + ww / 2 + 1, ly + wh), p["accent"], 0.0)
    lamp_x = c.w * (0.14 if c.poster else 0.5)
    ly0 = gy - u * 0.34
    c.radial(lamp_x, ly0, u * 0.35, p["accent"], 0.55, 2.0)

    def lamp(d):
        d.rect(lamp_x - 3, ly0, lamp_x + 3, c.h)
        d.rrect(lamp_x - 14, ly0 - 12, lamp_x + 14, ly0 + 4, 5)

    c.shape(lamp, p["deep"])
    c.shape(lambda d: d.ell(lamp_x, ly0 - 3, 9, 6), p["hi"])
    c.shape(lambda d: d.rect(0, gy, c.w, c.h), p["deep"] * 0.7)
    c.reflect(gy, fade=0.5, blur=2.5, dark=0.7, ripple=2)
    c.radial(lamp_x, gy + u * 0.04, u * 0.08, p["accent"], 0.4, 1.5, ry=u * 0.18)
    rain(c, int(c.w * c.h / 1700), p["alt2"], 0.28, 14, (0.03, 0.08), 1.0)
    c.bloom(0.55, 0.55, 0.025)


def ridges(c, p, v, flags=()):
    u = c.u
    fl = set(flags)
    horizon = c.h * (0.5 if c.poster else 0.47)
    if "night" in fl:
        c.grad([(0, p["deep"]), (0.5, p["dark"]), (0.62, mix(p["mid"], p["low"], 0.25)), (1, p["dark"])])
        c.stars(int(c.w * c.h / 2000), 0.55, 1.0, big=4)
    else:
        c.grad([(0, p["dark"]), (0.28, p["mid"]), (0.5, mix(p["sky"], p["low"], 0.7)), (0.62, p["low"]), (1, p["dark"])])
    fx, fy = c.w * c.fx, c.h * (c.fy - 0.05)
    if "aurora" in fl:
        aurora(c, c.h * 0.28, c.h * 0.2, (p["alt"], p["accent"]), 0.1, 0.9)
    if "sun" in fl:
        c.radial(fx, fy + u * 0.1, u * 0.8, p["accent"], 0.7, 2.0)
        c.radial(fx, fy + u * 0.1, u * 0.18, p["hi"], 1.0, 1.4)
        c.shape(lambda d: d.ell(fx, fy + u * 0.1, u * 0.065), p["hi"])
    if "moon" in fl:
        c.radial(fx, fy, u * 0.55, p["hi"], 0.4, 2.0)
        c.shape(lambda d: d.ell(fx, fy, u * 0.09), p["hi"])
    n = 6 if c.poster else 6
    lake = "lake" in fl
    base0 = c.h * (0.5 if c.poster else 0.5)
    base1 = c.h * (0.8 if c.poster else 0.76)
    haze = p["low"] if "night" not in fl else mix(p["mid"], p["low"], 0.35)
    last = None
    for i in range(n):
        t = i / (n - 1)
        base = base0 + (base1 - base0) * t**0.9
        amp = u * (0.1 - 0.05 * t) * (1.0 if "alpine" in fl else 0.55)
        col = mix(haze * 0.95, p["deep"], t**0.75)
        xs, ys = c.ridge(base, amp, c.w / (2.0 + 1.6 * t), col, seed=10 + i, sharp=0.65 if "alpine" in fl and t < 0.7 else 0.1, octaves=5)
        if i < n - 1:
            c.mist(base + amp * 0.6, u * (0.07 + 0.04 * t), haze, 0.5 - 0.3 * t, seed=i)
        if "castle" in fl and i == 3:
            xi = int(fx / 3)
            castle(c, fx, float(ys[xi]) + 6, u * 0.2, col * 0.8, p["accent"])
        if "pines" in fl and i >= n - 3:
            pines(c, xs, ys, col * 0.95, u * (0.05 + 0.04 * t), u * (0.09 + 0.08 * t), seed=i)
        last = (xs, ys)
    if lake:
        gy = int(c.h * 0.8)
        c.shape(lambda d: d.rect(0, gy, c.w, c.h), p["deep"] * 0.8)
        c.reflect(gy, fade=0.6, blur=4, dark=0.9, ripple=3)
    if "fireflies" in fl:
        fireflies(c, 60, (0, c.h * 0.5, c.w, c.h * 0.95), p["accent"])
    if "lantern" in fl:
        lx, ly = c.w * (0.56 if c.poster else 0.72), c.h * (0.63 if c.poster else 0.58)
        c.radial(lx, ly, u * 0.5, p["accent"], 0.75, 1.8)
        c.radial(lx, ly, u * 0.12, p["hi"], 1.0, 1.4)

        def lan(d):
            d.rrect(lx - 7, ly - 11, lx + 7, ly + 11, 3)

        c.shape(lan, p["hi"])
    if "birds" in fl:
        birds(c, 7, (c.w * 0.3, c.h * 0.12, c.w * 0.85, c.h * 0.36), p["deep"])
    c.bloom(0.6, 0.5, 0.03)


def toon(c, p, v):
    u = c.u
    bg = p["alt2"] if v in ("balloon", "boat") else p["sky"]
    c.fill(bg if v == "boat" else mix(p["sky"], p["accent"], 0.0))
    if v == "moon":
        c.grad([(0, p["deep"]), (1, p["mid"])])
        fx, fy = c.w * c.fx, c.h * c.fy
        R = u * 0.3
        c.shape(lambda d: d.ell(fx, fy, R * 1.6), p["mid"] * 1.3, 0.5)
        c.shape(lambda d: d.ell(fx, fy, R), p["hi"])
        c.shape(lambda d: d.ell(fx + R * 0.45, fy - R * 0.1, R * 0.92), p["mid"] * 1.15)
        for k in range(30):
            x, y = c.r.uniform(0, c.w), c.r.uniform(0, c.h * 0.65)
            s = u * c.r.uniform(0.01, 0.026)
            c.shape(lambda d, x=x, y=y, s=s: d.poly([(x, y - s), (x + s * 0.25, y - s * 0.25), (x + s, y), (x + s * 0.25, y + s * 0.25), (x, y + s), (x - s * 0.25, y + s * 0.25), (x - s, y), (x - s * 0.25, y - s * 0.25)]), p["hi"], 0.9, bbox=(x - s - 2, y - s - 2, x + s + 2, y + s + 2))
        for i in range(4):
            base = c.h * (0.7 + 0.07 * i)
            xs, ys = c.ridge(base, u * 0.05, c.w / (1.5 + i * 0.7), mix(p["dark"], p["deep"], i / 3), seed=30 + i, octaves=3)
            fold = mix(p["dark"], p["accent"], 0.35 - i * 0.07)
            c.shape(lambda d, ys=ys, xs=xs: d.line(list(zip(xs.tolist(), (ys - 2).tolist())), 3), fold, 0.9, bbox=(0, int(ys.min()) - 6, c.w, int(ys.max()) + 6))
    elif v == "boat":
        c.grad([(0, p["alt2"]), (0.6, p["hi"]), (1, p["hi"])])
        fx, fy = c.w * c.fx, c.h * 0.22
        c.shape(lambda d: d.ell(fx - u * 0.2, fy, u * 0.12), p["accent"])
        for k in range(12):
            a = k * math.pi / 6
            c.shape(lambda d, a=a: d.poly([(fx - u * 0.2 + math.cos(a - 0.08) * u * 0.16, fy + math.sin(a - 0.08) * u * 0.16), (fx - u * 0.2 + math.cos(a) * u * 0.23, fy + math.sin(a) * u * 0.23), (fx - u * 0.2 + math.cos(a + 0.08) * u * 0.16, fy + math.sin(a + 0.08) * u * 0.16)]), p["accent"], 0.9)
        sy = c.h * 0.52
        for i, col in enumerate((p["sky"], p["mid"] * 1.5, p["dark"] * 1.5)):
            base = sy + i * c.h * 0.11
            pts = [(x, base + math.sin(x / c.w * 11 + i * 1.7) * u * 0.03) for x in range(0, c.w + 8, 8)]
            c.shape(lambda d, pts=pts: d.poly([(0, c.h)] + pts + [(c.w, c.h)]), col)
        bx, by = fx + u * 0.05, sy + c.h * 0.03
        bw = u * 0.34
        c.shape(lambda d: d.poly([(bx - bw / 2, by), (bx + bw / 2, by), (bx + bw * 0.34, by + bw * 0.2), (bx - bw * 0.34, by + bw * 0.2)]), p["hi"])
        c.shape(lambda d: d.poly([(bx - bw * 0.02, by - bw * 0.62), (bx + bw * 0.36, by - bw * 0.03), (bx - bw * 0.02, by - bw * 0.03)]), p["accent"])
        c.shape(lambda d: d.poly([(bx - bw * 0.06, by - bw * 0.46), (bx - bw * 0.06, by - bw * 0.03), (bx - bw * 0.3, by - bw * 0.03)]), p["alt"])
        c.shape(lambda d: d.poly([(bx - bw / 2, by), (bx + bw / 2, by), (bx + bw * 0.34, by + bw * 0.2), (bx - bw * 0.34, by + bw * 0.2)]), p["dark"], 0.18, bbox=(bx - bw, by - 5, bx + bw, by + bw * 0.3))
    elif v == "balloon":
        c.grad([(0, p["sky"]), (1, p["alt2"])])
        for k in range(5):
            x, y = c.r.uniform(0, c.w), c.r.uniform(c.h * 0.05, c.h * 0.55)
            s = u * c.r.uniform(0.1, 0.2)
            c.shape(lambda d, x=x, y=y, s=s: (d.ell(x, y, s, s * 0.55), d.ell(x + s * 0.7, y + s * 0.1, s * 0.6, s * 0.4), d.ell(x - s * 0.7, y + s * 0.12, s * 0.55, s * 0.38)), p["hi"], 0.92, bbox=(x - s * 1.5, y - s, x + s * 1.5, y + s))
        fx, fy = c.w * c.fx, c.h * c.fy
        R = u * 0.25
        cols = [p["accent"], p["alt"], p["hi"], p["accent"], p["alt"]]
        c.shape(lambda d: d.ell(fx, fy, R, R * 1.15), p["accent"])
        for i, col in enumerate((p["alt"], p["hi"], p["alt"])):
            off = (i - 1) * R * 0.46
            c.shape(lambda d, off=off: d.ell(fx + off, fy, R * 0.22, R * 1.15), col, bbox=(fx - R, fy - R * 1.3, fx + R, fy + R * 1.3))
        c.shape(lambda d: d.poly([(fx - R * 0.45, fy + R * 1.0), (fx + R * 0.45, fy + R * 1.0), (fx + R * 0.14, fy + R * 1.5), (fx - R * 0.14, fy + R * 1.5)]), p["accent"])
        c.shape(lambda d: (d.line([(fx - R * 0.3, fy + R * 1.35), (fx - R * 0.14, fy + R * 1.5)], 3), d.line([(fx + R * 0.3, fy + R * 1.35), (fx + R * 0.14, fy + R * 1.5)], 3)), p["deep"])
        c.shape(lambda d: d.rrect(fx - R * 0.17, fy + R * 1.5, fx + R * 0.17, fy + R * 1.72, 6), p["deep"])
        for i in range(3):
            base = c.h * (0.78 + 0.07 * i)
            xs, ys = c.ridge(base, u * 0.04, c.w / 2, mix(p["mid"], p["deep"], i / 2) * 1.3, seed=50 + i, octaves=3)
    elif v == "moth":
        c.grad([(0, p["deep"]), (0.7, p["dark"]), (1, p["mid"])])
        fx, fy = c.w * c.fx, c.h * c.fy
        c.radial(fx, fy + u * 0.12, u * 0.8, p["accent"], 0.9, 2.2)
        c.radial(fx, fy + u * 0.12, u * 0.3, p["hi"], 1.0, 1.3)
        c.shape(lambda d: d.ell(fx, fy + u * 0.12, u * 0.1), p["hi"])
        c.stars(int(c.w * c.h / 9000), 0.7, 1.0, big=3)

        def moth(d):
            for sg in (-1, 1):
                d.poly([(fx + sg * u * 0.02, fy - u * 0.03), (fx + sg * u * 0.3, fy - u * 0.26), (fx + sg * u * 0.42, fy - u * 0.12), (fx + sg * u * 0.36, fy + u * 0.02), (fx + sg * u * 0.02, fy + u * 0.03)])
                d.poly([(fx + sg * u * 0.02, fy + u * 0.0), (fx + sg * u * 0.33, fy + u * 0.04), (fx + sg * u * 0.26, fy + u * 0.16), (fx + sg * u * 0.04, fy + u * 0.09)])
                d.line([(fx + sg * u * 0.01, fy - u * 0.07), (fx + sg * u * 0.1, fy - u * 0.2)], 4)
            d.ell(fx, fy, u * 0.022, u * 0.1)

        c.shape(moth, p["deep"], 0.96, bbox=(fx - u * 0.5, fy - u * 0.4, fx + u * 0.5, fy + u * 0.3))
        base = c.h * 0.84
        xs, ys = c.ridge(base, u * 0.04, c.w / 2, p["deep"], seed=60, octaves=3)
    elif v == "planes":
        c.grad([(0, p["sky"]), (0.7, p["low"]), (1, p["accent"])])
        fx, fy = c.w * c.fx, c.h * 0.7
        c.shape(lambda d: d.ell(fx, fy, u * 0.3), p["hi"], 0.95)
        for k in range(7):
            x = c.w * (0.1 + 0.8 * k / 6) + c.r.uniform(-30, 30)
            y = c.h * (0.15 + 0.4 * ((k * 5) % 7) / 7)
            s = u * c.r.uniform(0.07, 0.15)
            col = p["hi"] if k % 2 else p["deep"]
            c.shape(lambda d, x=x, y=y, s=s: (d.poly([(x - s, y + s * 0.2), (x + s, y - s * 0.2), (x - s * 0.1, y + s * 0.05)]), d.poly([(x - s, y + s * 0.2), (x + s, y - s * 0.2), (x - s * 0.2, y + s * 0.55)])), col, 1.0, bbox=(x - s - 3, y - s, x + s + 3, y + s))
        for i in range(3):
            base = c.h * (0.78 + 0.07 * i)
            xs, ys = c.ridge(base, u * 0.04, c.w / 2.2, mix(p["mid"], p["deep"], i / 2) * 1.2, seed=70 + i, octaves=3)
    c.bloom(0.85, 0.25, 0.02)


def haunt(c, p, v):
    u = c.u
    c.grad([(0, p["deep"]), (0.5, p["dark"]), (0.8, mix(p["mid"], p["low"], 0.25)), (1, p["dark"])])
    mx, my = c.w * (0.62 if c.poster else 0.72), c.h * (0.24 if c.poster else 0.28)
    c.radial(mx, my, u * 0.8, p["accent"], 0.45, 2.0)
    c.shape(lambda d: d.ell(mx, my, u * 0.11), p["hi"])
    c.sphere(mx, my, u * 0.11, p["hi"], p["low"] * 0.9, (-0.5, -0.4))
    n = c.noise_full(u * 0.12, 4, 4)
    c.paint(np.clip(n - 0.45, 0, 1) * 0.8, p["dark"] * 1.5, 0.6, mode="over")
    c.stars(int(c.w * c.h / 7000), 0.5, 0.8)
    birds(c, 5, (c.w * 0.3, c.h * 0.15, c.w * 0.75, c.h * 0.4), p["deep"], 0.014)
    xs, ys = c.ridge(c.h * 0.7, u * 0.04, c.w / 1.4, mix(p["mid"], p["deep"], 0.6), seed=80, octaves=3)
    c.mist(c.h * 0.7, u * 0.1, p["low"], 0.5, seed=3)
    hx = c.w * (0.34 if c.poster else 0.58)
    hy = float(ys[int(hx / 3)]) + 4
    s = u * 0.3

    def house(d):
        d.rect(hx - s * 0.5, hy - s * 0.55, hx + s * 0.5, hy + 3)
        d.poly([(hx - s * 0.58, hy - s * 0.55), (hx + s * 0.58, hy - s * 0.55), (hx + s * 0.18, hy - s * 1.0), (hx - s * 0.18, hy - s * 1.0)])
        d.rect(hx + s * 0.28, hy - s * 1.2, hx + s * 0.4, hy - s * 0.8)
        d.rect(hx - s * 0.46, hy - s * 0.85, hx - s * 0.26, hy - s * 0.55)
        d.poly([(hx - s * 0.5, hy - s * 0.85), (hx - s * 0.22, hy - s * 0.85), (hx - s * 0.36, hy - s * 1.2)])

    def win(d):
        d.rect(hx - s * 0.32, hy - s * 0.42, hx - s * 0.22, hy - s * 0.3)
        d.rect(hx + s * 0.1, hy - s * 0.42, hx + s * 0.2, hy - s * 0.3)
        d.rect(hx - s * 0.05, hy - s * 0.82, hx + s * 0.05, hy - s * 0.68)

    orchard = v == "orchard"
    if not orchard:
        c.shape(house, p["deep"] * 0.9, bbox=(hx - s, hy - s * 1.4, hx + s, hy + 6))
        c.shape(win, p["accent"], 1.0, bbox=(hx - s, hy - s * 1.4, hx + s, hy + 6))
        c.shape(win, p["accent"], 0.6, blur=s * 0.04, bbox=(hx - s, hy - s * 1.4, hx + s, hy + 6), mode="add")
    tips = []

    def tree(d, x, y, ln, ang, w, depth):
        if depth == 0 or ln < 4:
            tips.append((x, y))
            return
        x2, y2 = x + math.sin(ang) * ln, y - math.cos(ang) * ln
        d.line([(x, y), (x2, y2)], w)
        tree(d, x2, y2, ln * 0.72, ang - 0.5 + c.r.uniform(-0.2, 0.2), w * 0.7, depth - 1)
        tree(d, x2, y2, ln * 0.7, ang + 0.55 + c.r.uniform(-0.2, 0.2), w * 0.7, depth - 1)

    tx = hx + s * 1.5 if not orchard else c.w * 0.5
    ty = float(ys[min(len(ys) - 1, int(tx / 3))]) + 4
    ts = s * 0.65 if not orchard else s * 1.1
    c.shape(lambda d: tree(d, tx, ty, ts, 0.05, ts * 0.11, 7), p["deep"] * 0.9, bbox=(tx - ts * 3, ty - ts * 4.2, tx + ts * 3, ty + 6))
    if orchard:
        for x, y in tips[::2]:
            c.radial(x, y, u * 0.035, p["accent"], 0.8, 1.6)
            c.shape(lambda d, x=x, y=y: d.ell(x, y, u * 0.007), p["hi"], 1.0, bbox=(x - 10, y - 10, x + 10, y + 10))
    xs2, ys2 = c.ridge(c.h * 0.8, u * 0.03, c.w / 1.8, p["deep"] * 0.7, seed=81, octaves=3)
    c.mist(c.h * 0.8, u * 0.09, p["low"], 0.55, seed=4)
    c.mist(c.h * 0.92, u * 0.08, p["low"], 0.5, seed=5)
    c.bloom(0.6, 0.45, 0.03)


def frame(c, p, v):
    u = c.u
    c.fill(p["deep"])
    # damask lattice
    cell = u * 0.11
    n = c.noise_full(u * 0.5, 3, 5)
    c.paint(np.clip(n - 0.3, 0, 1), p["dark"], 1.0)

    def lattice(d):
        for j in range(-1, int(c.h / cell) + 2):
            for i in range(-1, int(c.w / cell) + 2):
                x = i * cell + (cell / 2 if j % 2 else 0)
                y = j * cell * 0.9
                d.poly([(x, y - cell * 0.36), (x + cell * 0.22, y), (x, y + cell * 0.36), (x - cell * 0.22, y)])
                d.ell(x, y, cell * 0.07)
                d.ell(x + cell * 0.5, y + cell * 0.45, cell * 0.05)

    c.shape(lattice, p["mid"], 0.5)
    if c.poster and v != "hall":
        cx, cy = c.w / 2, c.h * 0.39
        c.radial(cx, cy, u * 0.8, p["accent"], 0.35, 2.0)
        R = u * 0.3

        def medallion(d):
            d.ell(cx, cy, R * 1.12)

        c.shape(medallion, p["deep"] * 1.1, 1.0)
        c.shape(lambda d: d.ell(cx, cy, R * 1.1), p["accent"], 0.9)
        c.shape(lambda d: d.ell(cx, cy, R * 1.04), p["deep"] * 1.2)
        c.shape(lambda d: d.ell(cx, cy, R * 0.97), vg(cy - R, cy + R, p["mid"] * 1.2, p["dark"]))

        def rosette(d):
            for k in range(16):
                a = k * math.pi / 8
                d.poly([(cx + math.cos(a - 0.12) * R * 0.2, cy + math.sin(a - 0.12) * R * 0.2), (cx + math.cos(a) * R * 0.88, cy + math.sin(a) * R * 0.88), (cx + math.cos(a + 0.12) * R * 0.2, cy + math.sin(a + 0.12) * R * 0.2)])
            for k in range(8):
                a = k * math.pi / 4 + math.pi / 8
                d.ell(cx + math.cos(a) * R * 0.62, cy + math.sin(a) * R * 0.62, R * 0.07)

        c.shape(rosette, vg(cy - R, cy + R, p["hi"], p["accent"]), 0.95)
        c.shape(lambda d: d.ell(cx, cy, R * 0.18), p["deep"])
        c.shape(lambda d: d.ell(cx, cy, R * 0.12), p["accent"])
        # bird
        bx, by = cx, cy
        for sy in (-1,):
            pass
    else:
        arch_xs = [c.w * 0.5] if c.poster else np.linspace(c.w * 0.3, c.w * 1.0, 4)
        for k, x in enumerate(arch_xs):
            top = c.h * 0.14
            w = u * (0.62 if c.poster else 0.28)

            def arch(d, x=x, w=w, top=top):
                d.rect(x - w / 2, top + w / 2, x + w / 2, c.h * 0.85)
                d.ell(x, top + w / 2, w / 2)

            c.radial(x, c.h * 0.5, u * 0.5, p["accent"], 0.18, 2.0)
            c.shape(arch, vg(top, c.h * 0.85, p["accent"], p["low"] * 0.6), 0.5)
            c.shape(lambda d, x=x, w=w: d.rect(x - 2, top + w / 2 - 2, x + 2, c.h * 0.85), p["deep"], 0.8)
            c.shape(lambda d, x=x, w=w: d.rect(x - w / 2, c.h * 0.5 - 2, x + w / 2, c.h * 0.5 + 2), p["deep"], 0.8)
            c.shape(lambda d, x=x, w=w: d.rect(x - w / 2 - 14, c.h * 0.0, x - w / 2 + 2, c.h), p["dark"], 0.95)
        c.shape(lambda d: d.rect(0, c.h * 0.85, c.w, c.h), p["deep"] * 0.7)
        c.reflect(int(c.h * 0.85), 0.4, 6, 0.5)
        fireflies(c, 40, (c.w * 0.25, c.h * 0.1, c.w, c.h * 0.8), p["accent"], 0.7)
    if c.poster:
        m = u * 0.035
        for k, (inset, wd, col) in enumerate(((m, 6, p["accent"]), (m * 2.1, 2.5, p["accent"]))):
            c.shape(lambda d, i=inset, w=wd: (d.line([(i, i), (c.w - i, i), (c.w - i, c.h - i), (i, c.h - i), (i, i)], w)), col, 0.9)
        for (x, y, sx, sy) in ((m, m, 1, 1), (c.w - m, m, -1, 1), (m, c.h - m, 1, -1), (c.w - m, c.h - m, -1, -1)):
            c.shape(lambda d, x=x, y=y, sx=sx, sy=sy: (d.ell(x + sx * u * 0.045, y + sy * u * 0.045, u * 0.02), d.ell(x + sx * u * 0.045, y + sy * u * 0.045, u * 0.013)), p["accent"], 0.95)
    c.bloom(0.65, 0.45, 0.03)


def blocks(c, p, v):
    u = c.u
    cols = [p["accent"], p["alt"], p["hi"], p["low"], p["alt2"], p["mid"]]
    c.fill(p["dark"] if v != "bright" else p["hi"])
    r = c.r
    rects = []

    def split(x0, y0, x1, y1, depth):
        if depth == 0 or (x1 - x0) < u * 0.25 or (y1 - y0) < u * 0.25:
            rects.append((x0, y0, x1, y1))
            return
        if (x1 - x0) > (y1 - y0) * r.uniform(0.7, 1.3):
            s = r.uniform(0.3, 0.7)
            xm = x0 + (x1 - x0) * s
            split(x0, y0, xm, y1, depth - 1)
            split(xm, y0, x1, y1, depth - 1)
        else:
            s = r.uniform(0.3, 0.7)
            ym = y0 + (y1 - y0) * s
            split(x0, y0, x1, ym, depth - 1)
            split(x0, ym, x1, y1, depth - 1)

    split(0, 0, c.w, c.h, 4)
    order = list(range(len(cols)))
    for i, (x0, y0, x1, y1) in enumerate(rects):
        col = cols[(i * 7 + 2) % len(cols)]
        c.shape(lambda d, x0=x0, y0=y0, x1=x1, y1=y1: d.rect(x0, y0, x1, y1), col, 1.0, bbox=(x0, y0, x1, y1))
    fx, fy = c.w * c.fx + c.r.uniform(-0.1, 0.04) * c.w, c.h * c.fy + c.r.uniform(-0.05, 0.05) * c.h
    R = u * c.r.uniform(0.22, 0.32)
    c.shape(lambda d: d.ell(fx, fy, R), p["deep"], 0.92)
    c.shape(lambda d: d.ell(fx + R * 0.08, fy - R * 0.08, R * 0.88), p["hi"])

    def stripes(d):
        for k in range(9):
            d.rect(fx - R * 0.9, fy - R * 0.1 + k * R * 0.11, fx + R * 0.9, fy - R * 0.1 + k * R * 0.11 + R * 0.05)

    c.shape(stripes, p["accent"], 1.0, bbox=(fx - R, fy - R, fx + R, fy + R), erase=None)
    dots = []
    for j in range(8):
        for i in range(8):
            dots.append((c.w * 0.04 + i * u * 0.038, c.h * 0.78 + j * u * 0.038))

    def halftone(d):
        for k, (x, y) in enumerate(dots):
            d.ell(x, y, u * 0.012)

    c.shape(halftone, p["deep"], 0.9)
    c.shape(lambda d: d.poly([(c.w * 0.62, c.h * 0.0), (c.w * 0.72, c.h * 0.0), (c.w * 0.5, c.h * 0.18)]), p["deep"], 0.9)
    c.grain(0.01)


def sea(c, p, v):
    u = c.u
    gy = int(c.h * (0.6 if c.poster else 0.56))
    night = v in ("lighthouse", "ship")
    if night:
        c.grad([(0, p["deep"]), (gy / c.h, mix(p["mid"], p["low"], 0.35)), (1, p["deep"])])
        c.stars(int(c.w * c.h / 3500), gy / c.h, 0.9, big=3)
    else:
        c.grad([(0, p["dark"]), (0.25, p["mid"]), (gy / c.h, p["low"]), (1, p["deep"])])
    fx = c.w * c.fx
    if v in ("sunset", "ice"):
        sy = gy - u * 0.06
        c.radial(fx, sy, u * 0.9, p["accent"], 0.8, 2.0)
        c.radial(fx, sy, u * 0.2, p["hi"], 1.0, 1.5)
        c.shape(lambda d: d.ell(fx, sy, u * 0.095), p["hi"])
    if v in ("lighthouse", "ship"):
        mx, my = c.w * (0.3 if c.poster else 0.52), c.h * 0.2
        c.radial(mx, my, u * 0.5, p["hi"], 0.4, 2.0)
        c.sphere(mx, my, u * 0.07, p["hi"], p["low"], (-0.5, -0.4))
    c.mist(gy, u * 0.06, p["hi"], 0.35, seed=7)
    c.shape(lambda d: d.rect(0, gy, c.w, c.h), vg(gy, c.h, mix(p["dark"], p["low"], 0.3), p["deep"]))
    c.reflect_sun = None
    if v in ("sunset", "ice"):
        for k in range(40):
            y = gy + 4 + k * (c.h - gy) / 46
            wd = u * (0.03 + 0.18 * (k / 46) ** 1.2) * c.r.uniform(0.6, 1.2)
            x = fx + c.r.uniform(-1, 1) * u * 0.03 * (1 + k / 8)
            c.shape(lambda d, x=x, y=y, wd=wd: d.rrect(x - wd, y, x + wd, y + max(2, u * 0.004), 2), p["hi"], 0.75 - k / 70, bbox=(x - wd - 4, y - 4, x + wd + 4, y + 10))
    if v == "ship" or v == "ice":
        sx, sb = c.w * (0.62 if c.poster else 0.7), gy + u * 0.015
        sz = u * 0.22

        def ship(d):
            d.poly([(sx - sz * 0.6, sb - sz * 0.08), (sx + sz * 0.62, sb - sz * 0.08), (sx + sz * 0.42, sb + sz * 0.1), (sx - sz * 0.42, sb + sz * 0.1)])
            d.poly([(sx + sz * 0.62, sb - sz * 0.08), (sx + sz * 0.95, sb - sz * 0.2), (sx + sz * 0.64, sb - sz * 0.02)])
            for mxo, mh in ((-0.28, 0.8), (0.04, 1.0), (0.32, 0.72)):
                mxp = sx + mxo * sz
                d.rect(mxp - 1.5, sb - mh * sz, mxp + 1.5, sb - sz * 0.08)
                for k, (y0, y1, wd) in enumerate(((0.95, 0.62, 0.26), (0.58, 0.28, 0.32))):
                    top, bot = sb - mh * sz * y0, sb - mh * sz * y1
                    d.poly([(mxp - sz * wd * 0.5, bot), (mxp + sz * wd * 0.5, bot), (mxp + sz * wd * 0.42, top), (mxp - sz * wd * 0.42, top)])
            d.line([(sx - sz * 0.5, sb - sz * 0.1), (sx - sz * 0.28, sb - sz * 0.8)], 2)

        c.shape(ship, p["deep"], 0.95, bbox=(sx - sz, sb - sz * 1.2, sx + sz, sb + sz * 0.3))
    if v == "lighthouse":
        lx = c.w * (0.62 if c.poster else 0.72)
        lb = gy + u * 0.07
        lh = u * 0.42
        c.shape(lambda d: d.poly([(lx - u * 0.18, lb + u * 0.05), (lx - u * 0.1, lb - u * 0.04), (lx + u * 0.12, lb - u * 0.05), (lx + u * 0.2, lb + u * 0.05)]), p["deep"])

        def tower(d):
            d.poly([(lx - u * 0.035, lb), (lx + u * 0.035, lb), (lx + u * 0.022, lb - lh), (lx - u * 0.022, lb - lh)])
            d.rect(lx - u * 0.03, lb - lh - u * 0.012, lx + u * 0.03, lb - lh)
            d.rect(lx - u * 0.02, lb - lh - u * 0.04, lx + u * 0.02, lb - lh - u * 0.012)
            d.poly([(lx - u * 0.028, lb - lh - u * 0.04), (lx + u * 0.028, lb - lh - u * 0.04), (lx, lb - lh - u * 0.07)])

        c.shape(tower, p["deep"] * 1.1)
        ly = lb - lh - u * 0.026
        c.radial(lx, ly, u * 0.3, p["hi"], 0.9, 2.0)
        c.shape(lambda d: d.poly([(lx, ly), (c.w * 1.1 if lx < c.w * 0.5 else -c.w * 0.1, ly - u * 0.12), (c.w * 1.1 if lx < c.w * 0.5 else -c.w * 0.1, ly + u * 0.12)]), p["hi"], 0.22, blur=u * 0.01, mode="add")
        c.shape(lambda d: d.poly([(lx, ly), (-c.w * 0.1, ly - u * 0.16), (-c.w * 0.1, ly + u * 0.06)]), p["hi"], 0.2, blur=u * 0.01, mode="add")
    n = 9
    for k in range(n):
        t = k / (n - 1)
        y = gy + (c.h - gy) * (0.02 + 0.98 * t**1.3)
        amp = u * (0.004 + 0.03 * t)
        pts = [(x, y + math.sin(x / (u * (0.08 + 0.2 * t)) + k * 1.9) * amp + math.sin(x / (u * 0.03) + k) * amp * 0.3) for x in range(-8, c.w + 16, 8)]
        col = mix(p["mid"], p["deep"], t) if night else mix(p["low"], p["deep"], 0.35 + 0.55 * t)
        c.shape(lambda d, pts=pts: d.poly([(-8, c.h + 5)] + pts + [(c.w + 8, c.h + 5)]), col * (0.9 if v in ("lighthouse", "ship") else 1.0), 0.85, bbox=(0, y - amp * 2, c.w, c.h))
        c.shape(lambda d, pts=pts: d.line(pts, max(1.5, u * 0.003)), p["hi"], 0.22 * (1 - t * 0.6), bbox=(0, y - amp * 2, c.w, y + amp * 2 + 4))
    if v == "ice":
        for k in range(9):
            x = c.r.uniform(0, c.w)
            y = gy + c.r.uniform(0.05, 0.4) * (c.h - gy)
            s = u * c.r.uniform(0.05, 0.12)
            c.shape(lambda d, x=x, y=y, s=s: d.poly([(x - s, y), (x - s * 0.4, y - s * 0.12), (x + s * 0.6, y - s * 0.08), (x + s, y + s * 0.02), (x + s * 0.3, y + s * 0.1), (x - s * 0.7, y + s * 0.08)]), p["hi"], 0.85, bbox=(x - s - 2, y - s * 0.2, x + s + 2, y + s * 0.2))
    c.bloom(0.55, 0.55, 0.03)


def mesa(c, p, v):
    u = c.u
    night = v == "night"
    if night:
        c.grad([(0, p["deep"]), (0.55, p["dark"]), (0.7, mix(p["mid"], p["low"], 0.3)), (1, p["dark"])])
        c.stars(int(c.w * c.h / 2500), 0.6, 1.0, big=4)
    else:
        c.grad([(0, p["dark"]), (0.3, p["mid"]), (0.55, mix(p["sky"], p["low"], 0.6)), (0.68, p["low"]), (1, p["dark"])])
    fx, fy = c.w * c.fx, c.h * (c.fy + 0.03)
    if night:
        c.radial(fx, fy, u * 0.5, p["hi"], 0.4, 2.0)
        c.shape(lambda d: d.ell(fx, fy, u * 0.075), p["hi"])
    else:
        c.radial(fx, fy, u * 0.9, p["accent"], 0.75, 2.0)
        c.radial(fx, fy, u * 0.16, p["hi"], 1.0, 1.4)
        c.shape(lambda d: d.ell(fx, fy, u * 0.085), p["hi"])
    c.mist(c.h * 0.6, u * 0.1, p["low"], 0.4, seed=8)
    n = 4
    for i in range(n):
        t = i / (n - 1)
        base = c.h * (0.56 + 0.18 * t)
        col = mix(p["low"] * 0.6 if not night else p["mid"], p["deep"], t**0.8)
        butte = []
        x = -u * 0.1
        while x < c.w:
            bw = c.r.uniform(0.14, 0.38) * u * (1.0 + 0.4 * (1 - t))
            bh = c.r.uniform(0.07, 0.2) * u * (1.2 - 0.4 * t)
            butte.append((x, bw, bh))
            x += bw * c.r.uniform(0.8, 1.3)

        def f(d, butte=butte, base=base):
            for x, bw, bh in butte:
                top = base - bh
                d.poly([(x - bw * 0.45, base + u * 0.6), (x + bw * 0.02, base - bh * 0.32), (x + bw * 0.1, base - bh * 0.46), (x + bw * 0.12, top + bh * 0.06), (x + bw * 0.18, top), (x + bw * 0.82, top), (x + bw * 0.88, top + bh * 0.06), (x + bw * 0.9, base - bh * 0.46), (x + bw * 0.98, base - bh * 0.32), (x + bw * 1.45, base + u * 0.6)])

        c.shape(f, col, 1.0, bbox=(0, base - u * 0.3, c.w, c.h))

        def strata(d, butte=butte, base=base):
            for x, bw, bh in butte:
                for k in range(1, 4):
                    y = base - bh + bh * k * 0.14
                    d.rect(x + bw * 0.13, y, x + bw * 0.87, y + 2 + u * 0.003)

        c.shape(strata, p["deep"], 0.28, bbox=(0, base - u * 0.3, c.w, c.h))
        c.mist(base, u * 0.05, p["low"] if not night else p["mid"], 0.4 - 0.2 * t, seed=20 + i)
    gy = c.h * 0.9
    xs, ys = c.ridge(gy, u * 0.025, c.w / 2.5, p["deep"] * 0.8, seed=90, octaves=3)
    cx = [c.w * 0.1, c.w * 0.22, c.w * 0.8] if c.poster else [c.w * 0.08, c.w * 0.18, c.w * 0.9]
    c.shape(lambda d: [cactus(d, x, gy + u * 0.02, u * (0.18 - 0.03 * i)) for i, x in enumerate(cx)], p["deep"] * 0.75, bbox=(0, gy - u * 0.3, c.w, c.h))
    c.bloom(0.6, 0.5, 0.03)


def glass(c, p, v):
    u = c.u
    c.grad([(0, p["dark"]), (0.4, p["mid"]), (0.7, p["low"] * 0.8), (1, p["dark"])])
    fx = c.w * c.fx
    c.radial(fx - u * 0.1, c.h * 0.15, u * 1.0, p["hi"], 0.55, 2.0)
    n = c.noise_full(u * 0.2, 4, 9)
    c.paint(np.clip(n - 0.5, 0, 1), p["hi"], 0.15)
    vx, vy = c.w * (0.5 if c.poster else 0.66), c.h * 0.9

    def ribs(d):
        n_r = 13
        for k in range(n_r):
            a = math.pi * (0.04 + 0.92 * k / (n_r - 1))
            tx = vx + math.cos(a) * c.w * 1.0
            ty = vy - math.sin(a) * c.h * 1.15
            d.line([(vx, vy), (tx, ty)], u * 0.006)
        for rr in (0.18, 0.34, 0.52, 0.78, 1.1):
            d.arc(vx, vy, c.w * rr, c.h * 1.15 * rr * 0.9, 180, 360, u * 0.007)

    c.shape(ribs, p["deep"], 0.85)
    rays = []
    for k in range(5):
        x0 = c.r.uniform(0, c.w)
        rays.append((x0, c.r.uniform(0.05, 0.12) * c.w))

    def lightrays(d):
        for x0, w0 in rays:
            d.poly([(x0, 0), (x0 + w0, 0), (x0 + w0 * 2.6 - c.w * 0.1, c.h), (x0 + w0 * 1.2 - c.w * 0.2, c.h)])

    c.shape(lightrays, p["hi"], 0.14, blur=u * 0.01, mode="add")
    plants = [(c.w * (i + c.r.uniform(0.2, 0.8)) / 7, c.h * c.r.uniform(0.96, 1.04), u * c.r.uniform(0.28, 0.5)) for i in range(7)]
    for k, (x, y, L) in enumerate(plants):
        t = k / 6
        col = mix(p["mid"], p["deep"], 0.5 + 0.5 * (k % 2))

        def fronds(d, x=x, y=y, L=L):
            for j in range(11):
                a = math.radians(-80 + j * 16 + c.r.uniform(-5, 5))
                ln = L * c.r.uniform(0.6, 1.0) * (1 - abs(j - 5) * 0.07)
                ex, ey = x + math.sin(a) * ln, y - math.cos(a) * ln
                mx, my = x + math.sin(a) * ln * 0.5 - math.cos(a) * ln * 0.12, y - math.cos(a) * ln * 0.5 - math.sin(a) * ln * 0.12
                d.poly([(x, y), (mx - math.cos(a) * ln * 0.09, my - math.sin(a) * ln * 0.09), (ex, ey), (mx + math.cos(a) * ln * 0.09, my + math.sin(a) * ln * 0.09)])

        c.shape(fronds, col, 0.97, bbox=(x - L * 1.1, y - L * 1.1, x + L * 1.1, y + 10))
    c.mist(c.h * 0.75, u * 0.12, p["hi"], 0.4, seed=11)
    c.bloom(0.6, 0.5, 0.03)


def mapchart(c, p, v):
    u = c.u
    chart = v == "chart"
    if chart:
        c.grad([(0, p["deep"]), (1, p["dark"])])
    else:
        c.grad([(0, p["low"] * 0.8), (1, p["sky"])])
    f = c.noise_full(u * 0.45, 5, 12)
    levels = 18
    q = np.floor(f * levels)
    edge = np.zeros_like(f, bool)
    edge[:, 1:] |= q[:, 1:] != q[:, :-1]
    edge[1:, :] |= q[1:, :] != q[:-1, :]
    ed = Image.fromarray((edge * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(0.7))
    m = np.asarray(ed, np.float32) / 255
    idx = (f > 0.55).astype(np.float32)
    c.paint(np.clip(m * 1.6, 0, 1), p["accent"] if chart else p["deep"], 0.55 if chart else 0.5)
    major = (q % 4 == 0) & edge
    mm = np.asarray(Image.fromarray((major * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(1.0)), np.float32) / 255
    c.paint(np.clip(mm * 2, 0, 1), p["hi"] if chart else p["deep"], 0.7)
    # land tint
    c.paint(np.clip((f - 0.5) * 3, 0, 1), p["mid"] if chart else p["hi"], 0.4)
    grid = []
    for i in range(0, 40):
        grid.append(i * u * 0.09)

    def gr(d):
        for g in grid:
            d.line([(g, 0), (g, c.h)], 1)
            d.line([(0, g), (c.w, g)], 1)

    c.shape(gr, p["hi"], 0.07)
    fx, fy = c.w * c.fx, c.h * (c.fy + 0.02)
    R = u * 0.26

    def rose(d):
        d.arc(fx, fy, R, R, 0, 360, 3)
        d.arc(fx, fy, R * 0.82, R * 0.82, 0, 360, 1.5)
        for k in range(16):
            a = k * math.pi / 8
            L = R * (1.18 if k % 4 == 0 else 0.98 if k % 2 == 0 else 0.86)
            w = 0.1 if k % 2 == 0 else 0.06
            d.poly([(fx + math.cos(a - w) * R * 0.1, fy + math.sin(a - w) * R * 0.1), (fx + math.cos(a) * L, fy + math.sin(a) * L), (fx + math.cos(a + w) * R * 0.1, fy + math.sin(a + w) * R * 0.1)])

    c.radial(fx, fy, R * 2.5, p["accent"], 0.35, 2.0)
    c.shape(rose, p["hi"], 0.95, bbox=(fx - R * 1.3, fy - R * 1.3, fx + R * 1.3, fy + R * 1.3))
    pts = []
    x, y = c.w * 0.1, c.h * 0.82
    for k in range(9):
        pts.append((x, y))
        x += c.w * 0.1 + c.r.uniform(-10, 30)
        y += c.r.uniform(-1, 0.4) * c.h * 0.09

    def route(d):
        for a, b in zip(pts[:-1], pts[1:]):
            L = math.hypot(b[0] - a[0], b[1] - a[1])
            n = int(L / (u * 0.022))
            for k in range(0, n, 2):
                t0, t1 = k / n, min(1, (k + 1) / n)
                d.line([(a[0] + (b[0] - a[0]) * t0, a[1] + (b[1] - a[1]) * t0), (a[0] + (b[0] - a[0]) * t1, a[1] + (b[1] - a[1]) * t1)], u * 0.005)
        ex, ey = pts[-1]
        d.line([(ex - u * 0.03, ey - u * 0.03), (ex + u * 0.03, ey + u * 0.03)], u * 0.012)
        d.line([(ex - u * 0.03, ey + u * 0.03), (ex + u * 0.03, ey - u * 0.03)], u * 0.012)

    c.shape(route, p["alt"] if chart else p["deep"], 0.95)
    c.bloom(0.7, 0.4, 0.02)


def rail(c, p, v):
    u = c.u
    vx, vy = c.w * c.fx, c.h * (0.5 if c.poster else 0.52)
    c.grad([(0, p["deep"]), (0.5, mix(p["dark"], p["sky"], 0.5)), (0.52, p["low"] * 0.6), (1, p["deep"])])
    c.stars(int(c.w * c.h / 4000), 0.45, 0.8)
    c.radial(vx, vy, u * 0.9, p["accent"], 0.7, 2.0)
    xs, ys = c.ridge(vy + u * 0.02, u * 0.04, c.w / 1.5, mix(p["dark"], p["deep"], 0.5), seed=100, octaves=4, sharp=0.2)
    c.shape(lambda d: d.poly([(0, vy), (c.w, vy), (c.w, c.h), (0, c.h)]), vg(vy, c.h, p["low"] * 0.75, p["deep"]))
    c.mist(vy, u * 0.06, p["hi"], 0.5, seed=13)
    gauge = c.w * 0.5

    def tracks(d):
        for sgn in (-1, 1):
            d.poly([(vx + sgn * u * 0.004, vy), (vx + sgn * (gauge * 0.17), c.h), (vx + sgn * (gauge * 0.17 + u * 0.06), c.h), (vx + sgn * u * 0.006, vy)])

    c.shape(tracks, p["hi"], 0.55, bbox=(0, vy, c.w, c.h))
    ties = []
    k = 0
    t = 0.02
    while t < 1.0:
        y = vy + (c.h - vy) * t**1.9
        ties.append((y, t))
        t += 0.028 + t * 0.03
    c.shape(lambda d: [d.rect(vx - gauge * 0.28 * (y - vy) / (c.h - vy) - 1, y, vx + gauge * 0.28 * (y - vy) / (c.h - vy) + 1, y + max(1.5, (y - vy) * 0.03)) for y, t in ties], p["deep"], 0.95, bbox=(0, vy, c.w, c.h))
    for side in (-1, 1):
        px = []
        t = 0.05
        while t < 1.2:
            y = vy + (c.h - vy) * t**1.9
            x = vx + side * (gauge * 0.55 + c.w * 0.25 * ((y - vy) / (c.h - vy))) * (y - vy) / (c.h - vy) * 1.4
            px.append((x, y, (y - vy) / (c.h - vy)))
            t += 0.18
        c.shape(lambda d, px=px: [(d.rect(x - 2 - s * 6, y - s * u * 0.7, x + 2 + s * 6, y), d.rect(x - s * u * 0.1, y - s * u * 0.66, x + s * u * 0.1, y - s * u * 0.64)) for x, y, s in px], p["deep"] * 0.7, bbox=(0, 0, c.w, c.h))
    c.radial(vx, vy, u * 0.2, p["hi"], 1.0, 1.6)
    c.radial(vx, vy, u * 0.04, (1, 1, 1), 1.0, 1.0)
    cone = [(vx, vy), (vx - c.w * 0.55, c.h), (vx + c.w * 0.55, c.h)]
    c.shape(lambda d: d.poly(cone), p["hi"], 0.12, blur=u * 0.02, mode="add")
    snow = c.g.random((c.h, c.w)) < 0.0012
    im = Image.fromarray((snow * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(1.1))
    c.paint(np.clip(np.asarray(im, np.float32) / 255 * 6, 0, 1), (1, 1, 1), 0.8, mode="screen")
    c.bloom(0.55, 0.55, 0.03)


def curtain(c, p, v):
    u = c.u
    c.fill(p["deep"])
    fold = c.line_noise(c.w, c.w / 7, 2, 0.5, 14)
    t = 0.5 + 0.5 * np.sin(c.X / c.w * (22 if c.poster else 34) + fold[None, :] * 1.2 + 0.4 * np.sin(c.Y / c.h * 3))
    shade = (0.2 + 0.8 * t ** 1.3)[..., None]
    base = p["accent"] * 0.62 + p["dark"] * 0.4
    drape = base * shade
    x0 = c.w * (0.34 if c.poster else 0.5)
    mask = np.ones((c.h, c.w), np.float32)
    open_left = np.clip((c.X - c.w * 0.0) / 1.0, 0, 1)
    w1 = c.w * (0.27 if c.poster else 0.16)
    left = 1 - np.clip((c.X - (w1 + (1 - c.Y / c.h) * 0.0 + np.sin(c.Y / c.h * 3.2) * c.w * 0.03)) / 4, 0, 1)
    right = np.clip((c.X - (c.w - w1 * 0.9 - np.sin(c.Y / c.h * 3.2 + 1) * c.w * 0.03)) / 4, 0, 1)
    cm = np.clip(left + right, 0, 1)
    c.a[:] = c.a * (1 - cm[..., None]) + drape * cm[..., None]
    c.shape(lambda d: [d.ell(c.w * k / 5 + c.w * 0.1, -u * 0.05, c.w * 0.14, u * 0.14) for k in range(0, 6)], p["accent"] * 0.7, 1.0, bbox=(0, 0, c.w, u * 0.2))
    sx = c.w * (0.5 if c.poster else 0.68)
    sy = c.h * 0.08

    def cone(d):
        d.poly([(sx - u * 0.03, sy), (sx + u * 0.03, sy), (sx + u * (0.34 if c.poster else 0.3), c.h * 0.86), (sx - u * (0.34 if c.poster else 0.3), c.h * 0.86)])

    c.shape(cone, p["hi"], 0.3, blur=u * 0.008, mode="add")
    c.radial(sx, c.h * 0.86, u * 0.4, p["hi"], 0.8, 1.8, ry=u * 0.09)
    c.shape(lambda d: d.ell(sx, c.h * 0.86, u * 0.3, u * 0.05), p["hi"], 0.35, mode="add")
    c.shape(lambda d: d.rect(0, c.h * 0.86, c.w, c.h), p["deep"] * 0.7, 0.5)
    c.mist(c.h * 0.6, u * 0.2, p["hi"], 0.25, seed=14)
    # audience
    heads = []
    for k in range(26):
        x = c.w * (k + c.r.uniform(-0.3, 0.3)) / 25
        y = c.h * (0.95 + c.r.uniform(-0.01, 0.03))
        heads.append((x, y, u * c.r.uniform(0.04, 0.06)))
    c.shape(lambda d: [(d.ell(x, y, s), d.ell(x, y + s * 1.9, s * 1.6, s * 1.3)) for x, y, s in heads], p["deep"] * 0.7, 1.0, bbox=(0, c.h * 0.85, c.w, c.h))
    c.bloom(0.6, 0.5, 0.03)


def forest(c, p, v):
    u = c.u
    c.grad([(0, p["deep"]), (0.45, p["dark"]), (0.7, mix(p["mid"], p["low"], 0.3)), (1, p["deep"])])
    fx = c.w * c.fx
    c.radial(fx, c.h * 0.45, u * 0.9, p["accent"], 0.45, 2.0, ry=u * 0.6)
    n = 5
    for i in range(n):
        t = i / (n - 1)
        base = c.h * (0.62 + 0.22 * t)
        xs, ys = c.ridge(base, u * 0.03, c.w / 1.5, mix(p["low"] * 0.5, p["deep"], t**0.8), seed=110 + i, octaves=3)
        pines(c, xs, ys, mix(p["low"] * 0.5, p["deep"], t**0.8) * 0.95, u * (0.08 + 0.12 * t), u * (0.14 + 0.18 * t), seed=i, tw=0.35)
        c.mist(base, u * (0.06 + 0.03 * t), p["low"], 0.5 - 0.3 * t, seed=30 + i)
    if v == "rays":
        rx = c.w * (0.62 if c.poster else 0.72)

        def rays(d):
            for k in range(6):
                x0 = rx + (k - 2.5) * u * 0.08
                d.poly([(x0, -10), (x0 + u * 0.03, -10), (x0 + u * (0.3 if k > 2 else -0.15) + u * 0.12, c.h), (x0 + u * (0.3 if k > 2 else -0.15) - u * 0.1, c.h)])

        c.shape(rays, p["hi"], 0.14, blur=u * 0.012, mode="add")
    else:
        hx, hy = fx, c.h * 0.7
        c.radial(hx - u * 0.07, hy, u * 0.5, p["hi"], 0.55, 2.0)
        c.radial(hx + u * 0.07, hy, u * 0.5, p["hi"], 0.55, 2.0)
        c.shape(lambda d: (d.ell(hx - u * 0.07, hy, u * 0.012), d.ell(hx + u * 0.07, hy, u * 0.012)), (1, 1, 0.95), 1.0)
    c.mist(c.h * 0.9, u * 0.1, p["low"], 0.5, seed=40)
    c.bloom(0.6, 0.5, 0.03)
