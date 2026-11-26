import colorsys
import math
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

S = 2


def C(c):
    if isinstance(c, str):
        h = c.lstrip("#")
        return np.array([int(h[i : i + 2], 16) for i in (0, 2, 4)], np.float32) / 255
    return np.asarray(c, np.float32)


def hsv(h, s, v):
    return np.array(colorsys.hsv_to_rgb((h % 360) / 360, min(1, max(0, s)), min(1, max(0, v))), np.float32)


def mix(a, b, t):
    return C(a) + (C(b) - C(a)) * t


def P(h, h2, h3=None, s=1.0, v=1.0):
    h3 = h + 40 if h3 is None else h3
    return dict(
        deep=hsv(h, 0.8 * s, 0.07 * v),
        dark=hsv(h, 0.7 * s, 0.15 * v),
        mid=hsv(h, 0.6 * s, 0.32 * v),
        sky=hsv(h, 0.55 * s, 0.5 * v),
        low=hsv(h2, 0.55 * s, 0.85 * v),
        accent=hsv(h2, 0.8 * s, 1.0 * v),
        hi=hsv(h2, 0.18, 1.0),
        alt=hsv(h3, 0.7 * s, 0.95 * v),
        alt2=hsv(h3, 0.35 * s, 1.0),
    )


def vg(y0, y1, c0, c1):
    return ("v", y0, y1, C(c0), C(c1))


def hg(x0, x1, c0, c1):
    return ("h", x0, x1, C(c0), C(c1))


class D:
    def __init__(self, im, ox=0, oy=0):
        self.d = ImageDraw.Draw(im)
        self.ox, self.oy = ox, oy
        self.v = 255

    def _p(self, p):
        return ((p[0] - self.ox) * S, (p[1] - self.oy) * S)

    def poly(self, pts):
        self.d.polygon([self._p(p) for p in pts], fill=self.v)

    def ell(self, cx, cy, rx, ry=None):
        ry = rx if ry is None else ry
        a, b = self._p((cx - rx, cy - ry)), self._p((cx + rx, cy + ry))
        self.d.ellipse([a, b], fill=self.v)

    def rect(self, x0, y0, x1, y1):
        self.d.rectangle([self._p((x0, y0)), self._p((x1, y1))], fill=self.v)

    def rrect(self, x0, y0, x1, y1, r):
        self.d.rounded_rectangle([self._p((x0, y0)), self._p((x1, y1))], radius=r * S, fill=self.v)

    def line(self, pts, w):
        self.d.line([self._p(p) for p in pts], fill=self.v, width=max(1, int(round(w * S))), joint="curve")

    def arc(self, cx, cy, rx, ry, a0, a1, w):
        a, b = self._p((cx - rx, cy - ry)), self._p((cx + rx, cy + ry))
        self.d.arc([a, b], a0, a1, fill=self.v, width=max(1, int(round(w * S))))

    def pie(self, cx, cy, r, a0, a1):
        a, b = self._p((cx - r, cy - r)), self._p((cx + r, cy + r))
        self.d.pieslice([a, b], a0, a1, fill=self.v)


def smooth(t):
    return t * t * (3 - 2 * t)


class Canvas:
    def __init__(self, w, h, seed, kind):
        self.w, self.h, self.kind = w, h, kind
        self.r = random.Random(seed)
        self.g = np.random.default_rng(seed)
        self.seed = seed
        self.a = np.zeros((h, w, 3), np.float32)
        self.u = float(min(w, h))
        self.poster = kind == "poster"
        self.fx, self.fy = (0.5, 0.38) if self.poster else (0.68, 0.40)
        self.hz = 0.70 if self.poster else 0.66
        yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
        self.Y, self.X = yy, xx

    def fill(self, c):
        self.a[:] = C(c)

    def grad(self, stops, axis=0):
        n = self.h if axis == 0 else self.w
        t = np.linspace(0, 1, n)
        pos = [s[0] for s in stops]
        cols = [C(s[1]) for s in stops]
        ch = np.stack([np.interp(t, pos, [c[k] for c in cols]) for k in range(3)], 1).astype(np.float32)
        if axis == 0:
            self.a[:] = ch[:, None, :]
        else:
            self.a[:] = ch[None, :, :]

    def _paint_spec(self, spec, x0, y0, x1, y1):
        if isinstance(spec, tuple) and spec and spec[0] in ("v", "h"):
            kind, a0, a1, c0, c1 = spec
            if kind == "v":
                t = np.clip((np.arange(y0, y1, dtype=np.float32) - a0) / max(1e-6, a1 - a0), 0, 1)[:, None, None]
            else:
                t = np.clip((np.arange(x0, x1, dtype=np.float32) - a0) / max(1e-6, a1 - a0), 0, 1)[None, :, None]
            return c0 + (c1 - c0) * t
        return C(spec)

    def paint(self, mask, color, alpha=1.0, bbox=None, mode="over"):
        x0, y0, x1, y1 = bbox or (0, 0, self.w, self.h)
        m = mask[..., None] * alpha
        reg = self.a[y0:y1, x0:x1]
        col = self._paint_spec(color, x0, y0, x1, y1)
        if mode == "over":
            reg[:] = reg * (1 - m) + col * m
        elif mode == "add":
            reg[:] = reg + col * m
        elif mode == "screen":
            reg[:] = 1 - (1 - reg) * (1 - col * m)
        elif mode == "mul":
            reg[:] = reg * (1 - m + col * m)

    def bb(self, x0, y0, x1, y1, pad=0):
        return (
            max(0, int(math.floor(x0 - pad))),
            max(0, int(math.floor(y0 - pad))),
            min(self.w, int(math.ceil(x1 + pad))),
            min(self.h, int(math.ceil(y1 + pad))),
        )

    def shape(self, fn, color, alpha=1.0, blur=0, bbox=None, rot=None, mode="over", erase=None):
        x0, y0, x1, y1 = bbox or (0, 0, self.w, self.h)
        x0, y0, x1, y1 = self.bb(x0, y0, x1, y1, blur * 3)
        bw, bh = x1 - x0, y1 - y0
        if bw <= 0 or bh <= 0:
            return
        im = Image.new("L", (bw * S, bh * S), 0)
        d = D(im, x0, y0)
        fn(d)
        if erase:
            d.v = 0
            erase(d)
        if rot:
            deg, cx, cy = rot
            im = im.rotate(deg, resample=Image.BICUBIC, center=((cx - x0) * S, (cy - y0) * S))
        m = im.resize((bw, bh), Image.BOX)
        if blur:
            m = m.filter(ImageFilter.GaussianBlur(blur))
        self.paint(np.asarray(m, np.float32) / 255, color, alpha, (x0, y0, x1, y1), mode)

    def radial(self, cx, cy, r, color, k=1.0, p=2.0, mode="screen", ry=None):
        ry = r if ry is None else ry
        x0, y0, x1, y1 = self.bb(cx - r, cy - ry, cx + r, cy + ry)
        if x1 <= x0 or y1 <= y0:
            return
        X, Y = self.X[y0:y1, x0:x1], self.Y[y0:y1, x0:x1]
        d = np.sqrt(((X - cx) / r) ** 2 + ((Y - cy) / ry) ** 2)
        m = np.clip(1 - d, 0, 1) ** p * k
        self.paint(np.clip(m, 0, 1), color, 1.0, (x0, y0, x1, y1), mode)

    def fbm(self, w, h, scale, octaves=4, persist=0.5, seed=0):
        rng = np.random.default_rng(self.seed * 7919 + seed)
        out = np.zeros((h, w), np.float32)
        amp, tot = 1.0, 0.0
        for o in range(octaves):
            gw = max(2, int(w / scale * 2**o) + 2)
            gh = max(2, int(h / scale * 2**o) + 2)
            g = rng.random((gh, gw)).astype(np.float32)
            im = Image.fromarray((g * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC)
            out += amp * np.asarray(im, np.float32) / 255
            tot += amp
            amp *= persist
        out /= tot
        lo, hi = out.min(), out.max()
        return (out - lo) / max(1e-6, hi - lo)

    def noise_full(self, scale, octaves=4, seed=0, persist=0.5):
        w, h = max(8, self.w // 3), max(8, self.h // 3)
        f = self.fbm(w, h, scale / 3, octaves, persist, seed)
        im = Image.fromarray((f * 255).astype(np.uint8)).resize((self.w, self.h), Image.BICUBIC)
        return np.asarray(im, np.float32) / 255

    def line_noise(self, n, scale, octaves=5, persist=0.5, seed=0):
        rng = np.random.default_rng(self.seed * 104729 + seed)
        out = np.zeros(n, np.float64)
        amp, tot = 1.0, 0.0
        for o in range(octaves):
            k = max(3, int(n / scale * 2**o) + 3)
            pts = rng.random(k)
            x = np.linspace(0, k - 3, n)
            i = np.floor(x).astype(int)
            f = x - i
            f = (1 - np.cos(np.pi * f)) / 2
            out += amp * (pts[i] * (1 - f) + pts[i + 1] * f)
            tot += amp
            amp *= persist
        out /= tot
        out = (out - out.min()) / max(1e-9, out.max() - out.min())
        return out * 2 - 1

    def ridge(self, ybase, amp, scale, color, seed=0, sharp=0.0, octaves=5, step=3, x_tilt=0.0, ret_only=False):
        xs = np.arange(0, self.w + step, step)
        n = len(xs)
        ln = self.line_noise(n, scale, octaves, 0.5, seed)
        if sharp:
            ridged = (1 - np.abs(ln)) * 2 - 1
            ln = ln * (1 - sharp) + ridged * sharp
        ys = ybase + amp * ln + x_tilt * (xs - self.w / 2)
        if not ret_only:
            ytop = float(ys.min())

            def f(d):
                d.poly([(0, self.h + 5)] + list(zip(xs.tolist(), ys.tolist())) + [(self.w, self.h + 5)])

            self.shape(f, color, bbox=(0, int(ytop) - 2, self.w, self.h))
        return xs, ys

    def mist(self, y, thick, color, alpha=0.5, scale=260, seed=0, k=1.0):
        w4, h4 = self.w // 4, self.h // 4
        f = self.fbm(w4, h4, scale / 4, 4, 0.55, 31 + seed)
        yy = np.arange(h4, dtype=np.float32)[:, None] * 4
        prof = np.exp(-(((yy - y) / thick) ** 2))
        m = np.clip(prof * (0.25 + 1.2 * f) * k, 0, 1)
        im = Image.fromarray((m * 255).astype(np.uint8)).resize((self.w, self.h), Image.BICUBIC)
        self.paint(np.asarray(im, np.float32) / 255, color, alpha)

    def stars(self, n, ymax=1.0, bright=1.0, tint=(1, 1, 1), big=0, xmin=0.0):
        layer = np.zeros((self.h, self.w), np.float32)
        ys = (self.g.random(n) ** 1.3 * self.h * ymax).astype(int)
        xs = (self.g.random(n) * self.w * (1 - xmin) + self.w * xmin).astype(int)
        b = self.g.random(n) ** 2.5 * 0.9 + 0.1
        ys = np.clip(ys, 0, self.h - 1)
        xs = np.clip(xs, 0, self.w - 1)
        layer[ys, xs] = b
        im = Image.fromarray((layer * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(0.8))
        m = np.clip(np.asarray(im, np.float32) / 255 * 3.2 * bright, 0, 1)
        self.paint(m, tint, 1.0, mode="screen")
        for _ in range(big):
            x = self.r.uniform(self.w * xmin, self.w)
            y = self.r.uniform(0, self.h * ymax)
            self.radial(x, y, self.u * 0.018, tint, 0.9, 2.5)

            def f(d, x=x, y=y):
                s = self.u * 0.014
                d.poly([(x, y - s), (x + s * 0.12, y - s * 0.12), (x + s, y), (x + s * 0.12, y + s * 0.12), (x, y + s), (x - s * 0.12, y + s * 0.12), (x - s, y), (x - s * 0.12, y - s * 0.12)])

            self.shape(f, tint, 0.85, bbox=(x - 30, y - 30, x + 30, y + 30), mode="screen")

    def sphere(self, cx, cy, r, light, dark, ldir=(-0.55, -0.6), bands=None, rim=None, rimk=1.0, gamma=0.9):
        x0, y0, x1, y1 = self.bb(cx - r - 3, cy - r - 3, cx + r + 3, cy + r + 3)
        if x1 <= x0 or y1 <= y0:
            return
        X, Y = self.X[y0:y1, x0:x1], self.Y[y0:y1, x0:x1]
        nx, ny = (X - cx) / r, (Y - cy) / r
        rho = np.sqrt(nx**2 + ny**2)
        nz = np.sqrt(np.clip(1 - rho**2, 0, 1))
        lx, ly = ldir
        lz = math.sqrt(max(0.05, 1 - lx * lx - ly * ly))
        lam = np.clip(nx * lx + ny * ly + nz * lz, 0, 1) ** gamma
        t = lam[..., None]
        col = C(dark) + (C(light) - C(dark)) * t
        if bands is not None:
            c1, c2, freq, k = bands
            band = 0.5 + 0.5 * np.sin(ny * freq + 2.2 * np.sin(nx * 2.1 + ny * 1.3))
            col = col * (1 - k) + (C(c1) + (C(c2) - C(c1)) * band[..., None]) * col.max(axis=2, keepdims=True) * k * 1.4
        if rim is not None:
            col = col + C(rim) * (np.clip(rho, 0, 1) ** 7 * rimk * (0.35 + 0.65 * lam))[..., None]
        m = np.clip((1 - rho) * r / 1.2, 0, 1)
        reg = self.a[y0:y1, x0:x1]
        reg[:] = reg * (1 - m[..., None]) + np.clip(col, 0, 1.4) * m[..., None]

    def vignette(self, k=0.4, cy=0.5):
        dx = (self.X / self.w - 0.5) * 2
        dy = (self.Y / self.h - cy) * 2
        d = np.clip(np.sqrt(dx**2 * 0.8 + dy**2 * 0.8) - 0.35, 0, 1.5)
        self.a *= (1 - k * np.clip(d, 0, 1) ** 1.6)[..., None]

    def shade_bottom(self, start, strength=0.9, color=(0.01, 0.015, 0.03), power=1.5):
        t = np.clip((self.Y / self.h - start) / (1 - start), 0, 1) ** power * strength
        self.a[:] = self.a * (1 - t[..., None]) + C(color) * t[..., None]

    def shade_top(self, end, strength=0.8, color=(0.01, 0.015, 0.03), power=1.5):
        t = np.clip((end - self.Y / self.h) / end, 0, 1) ** power * strength
        self.a[:] = self.a * (1 - t[..., None]) + C(color) * t[..., None]

    def shade_left(self, end, strength=0.75, color=(0.01, 0.015, 0.03), power=1.4):
        t = np.clip((end - self.X / self.w) / end, 0, 1) ** power * strength
        self.a[:] = self.a * (1 - t[..., None]) + C(color) * t[..., None]

    def bloom(self, thr=0.7, strength=0.5, radius=0.03):
        small = Image.fromarray((np.clip(self.a, 0, 1) * 255).astype(np.uint8)).resize((self.w // 4, self.h // 4), Image.BILINEAR)
        arr = np.asarray(small, np.float32) / 255
        bright = np.clip(arr - thr, 0, 1) / (1 - thr)
        out = np.zeros_like(self.a)
        for rr, kk in ((radius, 1.0), (radius * 3, 0.8)):
            im = Image.fromarray((bright * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(rr * self.u / 4))
            up = Image.fromarray(np.asarray(im)).resize((self.w, self.h), Image.BICUBIC)
            out += np.asarray(up, np.float32) / 255 * kk
        self.a[:] = 1 - (1 - self.a) * (1 - np.clip(out * strength, 0, 1))

    def grain(self, amount=0.02):
        n = self.g.normal(0, amount, (self.h, self.w, 1)).astype(np.float32)
        c = self.g.normal(0, amount * 0.3, (self.h, self.w, 3)).astype(np.float32)
        self.a += n + c

    def grade(self, contrast=1.06, lift=0.0):
        self.a[:] = np.clip(self.a, 0, 1)
        self.a[:] = np.clip((self.a - 0.5) * contrast + 0.5 + lift, 0, 1)

    def image(self):
        return Image.fromarray((np.clip(self.a, 0, 1) * 255 + 0.5).astype(np.uint8))

    def reflect(self, ygr, fade=0.55, blur=2.0, dark=0.55, tint=None, ripple=0.0):
        h = self.h
        n = h - ygr
        if n <= 0:
            return
        src = self.a[max(0, ygr - n) : ygr][::-1]
        if src.shape[0] < n:
            pad = np.repeat(src[-1:], n - src.shape[0], 0)
            src = np.concatenate([src, pad], 0)
        im = Image.fromarray((np.clip(src, 0, 1) * 255).astype(np.uint8))
        im = im.filter(ImageFilter.GaussianBlur(blur))
        arr = np.asarray(im, np.float32) / 255
        if ripple:
            shift = (np.sin(np.arange(n)[:, None] * 0.9 + self.g.random() * 6) * ripple * (np.arange(n)[:, None] / n + 0.2)).astype(int)
            idx = (np.arange(self.w)[None, :] + shift) % self.w
            arr = arr[np.arange(n)[:, None], idx]
        t = np.clip(1 - np.arange(n, dtype=np.float32) / n * (1 / fade), 0, 1)[:, None, None]
        base = self.a[ygr:]
        col = arr * dark
        if tint is not None:
            col = col + C(tint) * 0.06
        base[:] = base * (1 - t * 0.9) + col * t * 0.9 + base * t * 0.1 * 0
