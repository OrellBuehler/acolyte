#!/usr/bin/env python3
"""Draws the promo images from store/screenshots: the App Store header and
search-result assets, the README banner and the website pictures.
Needs inkscape, ImageMagick (convert) and the Inter font.

Usage: scripts/store/make-promo.py
"""
import pathlib
import subprocess
import tempfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
SHOTS = ROOT / "store" / "screenshots"
IOS = (1320, 2868)
ANDROID = (1080, 1920)

ICON = """
<g transform="translate(128 128) scale(0.86) translate(-128 -128)">
  <path fill="#F3F0E9" d="M128,16 Q138.8,59.2 182,70 Q138.8,80.8 128,124 Q117.2,80.8 74,70 Q117.2,59.2 128,16 Z"/>
  <rect x="100" y="140" width="56" height="96" fill="#BF281F"/>
  <path fill="#74150F" d="M132,140h8v8h-8zM132,156h8v8h-8zM132,172h8v8h-8zM132,188h8v8h-8zM132,204h8v8h-8zM132,220h8v8h-8zM140,148h8v8h-8zM140,164h8v8h-8zM140,180h8v8h-8zM140,196h8v8h-8zM140,212h8v8h-8zM140,228h8v8h-8zM148,140h8v96h-8z"/>
  <path fill="#F3F0E9" d="M22,36h8v8h8v8h-8v8h-8v-8h-8v-8h8zM226,96h8v8h8v8h-8v8h-8v-8h-8v-8h8zM34,180h8v8h8v8h-8v8h-8v-8h-8v-8h8zM214,26h8v8h-8zM52,116h8v8h-8zM206,196h8v8h-8z"/>
</g>
"""

STARS = [(0.42, 0.10, 1), (0.62, 0.06, 0.7), (0.96, 0.44, 1), (0.03, 0.12, 0.7), (0.05, 0.88, 1),
         (0.38, 0.90, 0.7), (0.47, 0.60, 0.7), (0.90, 0.05, 1), (0.31, 0.22, 0.7), (0.98, 0.85, 0.7)]


def phone(n, shot, size, cx, cy, height, angle):
    w = height * size[0] / size[1]
    bezel = w * 0.03
    radius = w * (0.15 if size is IOS else 0.09)
    x, y = cx - w / 2, cy - height / 2
    return f"""
<g transform="rotate({angle} {cx} {cy})">
  <rect x="{x - bezel}" y="{y - bezel}" width="{w + 2 * bezel}" height="{height + 2 * bezel}" rx="{radius + bezel}"
        fill="#000" opacity="0.55" filter="url(#shadow)"/>
  <rect x="{x - bezel}" y="{y - bezel}" width="{w + 2 * bezel}" height="{height + 2 * bezel}" rx="{radius + bezel}"
        fill="#11151C" stroke="#3A414C" stroke-width="{bezel * 0.22}"/>
  <clipPath id="c{n}"><rect x="{x}" y="{y}" width="{w}" height="{height}" rx="{radius}"/></clipPath>
  <image xlink:href="{shot}" x="{x}" y="{y}" width="{w}" height="{height}" clip-path="url(#c{n})" preserveAspectRatio="none"/>
</g>"""


def draw(out, width, height, phones, tagline, text_x=0.06, scale=1.0):
    u = height * scale
    icon = u * 0.24
    ty = height * 0.5
    stars = "".join(
        f'<rect x="{width * x}" y="{height * y}" width="{u * 0.011 * s}" height="{u * 0.011 * s}"/>' for x, y, s in STARS
    )
    body = "".join(phone(i, SHOTS / shot, size, width * cx, height * cy, height * h, angle)
                   for i, (shot, size, cx, cy, h, angle) in enumerate(phones))
    tx = width * text_x
    svg = f"""<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 {width} {height}" width="{width}" height="{height}">
<defs>
  <radialGradient id="glow" cx="{tx + icon}" cy="{ty}" r="{width * 0.5}" gradientUnits="userSpaceOnUse">
    <stop offset="0" stop-color="#BF281F" stop-opacity="0.46"/>
    <stop offset="0.55" stop-color="#BF281F" stop-opacity="0.09"/>
    <stop offset="1" stop-color="#BF281F" stop-opacity="0"/>
  </radialGradient>
  <radialGradient id="glow2" cx="{width * 0.78}" cy="{height * 1.05}" r="{width * 0.4}" gradientUnits="userSpaceOnUse">
    <stop offset="0" stop-color="#409CFF" stop-opacity="0.16"/>
    <stop offset="1" stop-color="#409CFF" stop-opacity="0"/>
  </radialGradient>
  <filter id="shadow" x="-30%" y="-30%" width="160%" height="160%"><feGaussianBlur stdDeviation="{u * 0.03}"/></filter>
</defs>
<rect width="{width}" height="{height}" fill="#05080D"/>
<rect width="{width}" height="{height}" fill="url(#glow)"/>
<rect width="{width}" height="{height}" fill="url(#glow2)"/>
<g fill="#F3F0E9" opacity="0.5">{stars}</g>
{body}
<g transform="translate({tx} {ty - icon * 1.45}) scale({icon / 256})">{ICON}</g>
<text x="{tx + icon * 0.08}" y="{ty + u * 0.085}" font-family="Inter" font-weight="700" font-size="{u * 0.15}" letter-spacing="{-u * 0.004}" fill="#F5F5F7">Acolyte</text>
<text x="{tx + icon * 0.1}" y="{ty + u * 0.16}" font-family="Inter" font-weight="500" font-size="{u * 0.045}" fill="#A1A1A6">{tagline}</text>
<rect x="{tx + icon * 0.1}" y="{ty + u * 0.2}" width="{u * 0.1}" height="{u * 0.009}" fill="#BF281F"/>
</svg>"""
    out.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        src = pathlib.Path(tmp) / "promo.svg"
        raw = pathlib.Path(tmp) / "promo.png"
        src.write_text(svg)
        subprocess.run(["inkscape", str(src), "-o", str(raw), "-w", str(width), "-h", str(height)],
                       check=True, capture_output=True)
        subprocess.run(["convert", str(raw), "-background", "#05080D", "-alpha", "remove", "-alpha", "off",
                        "-strip", f"PNG24:{out}"], check=True)
    print(out.relative_to(ROOT))


def ios(name, locale="en-US"):
    return f"ios/{locale}/iphone-6.9/{name}.png"


def android(name, locale="en-US"):
    return f"android/{locale}/phone/{name}.png"


def web(locale):
    for platform, folder in (("ios", f"ios/{locale}/iphone-6.9"), ("android", f"android/{locale}/phone")):
        for shot in sorted((SHOTS / folder).glob("*.png")):
            out = ROOT / "site" / "assets" / "screens" / f"{platform}-{locale[:2]}-{shot.stem}.jpg"
            out.parent.mkdir(parents=True, exist_ok=True)
            subprocess.run(["convert", str(shot), "-resize", "540x", "-strip", "-quality", "82", str(out)], check=True)


TAGLINE = "Your HolyArrs server, in your pocket"

draw(ROOT / "assets/store/appstore-header-5244x2950.png", 5244, 2950, [
    (ios("04-library"), IOS, 0.575, 0.58, 0.62, -8),
    (ios("02-explore"), IOS, 0.895, 0.58, 0.62, 8),
    (ios("01-home"), IOS, 0.735, 0.52, 0.76, 0),
], TAGLINE, text_x=0.07, scale=0.86)

draw(ROOT / "assets/store/appstore-search-3840x1646.png", 3840, 1646, [
    (ios("04-library"), IOS, 0.59, 0.74, 1.0, -8),
    (ios("03-title"), IOS, 0.91, 0.74, 1.0, 8),
    (ios("01-home"), IOS, 0.75, 0.70, 1.15, 0),
], TAGLINE, text_x=0.06)

draw(ROOT / "assets/readme/banner.png", 2400, 900, [
    (android("04-library"), ANDROID, 0.60, 0.78, 0.95, -7),
    (ios("03-title"), IOS, 0.90, 0.78, 1.08, 7),
    (ios("01-home"), IOS, 0.75, 0.72, 1.15, 0),
], TAGLINE, text_x=0.05)

draw(ROOT / "site/assets/og.png", 1200, 630, [
    (ios("03-title"), IOS, 0.94, 0.80, 1.05, 7),
    (ios("01-home"), IOS, 0.76, 0.74, 1.15, 0),
], TAGLINE, text_x=0.05, scale=0.92)

web("en-US")
web("de-DE")
