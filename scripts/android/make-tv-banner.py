#!/usr/bin/env python3
"""Generates the Android TV launcher banner (320x180 xhdpi) in
androidTvApp/src/androidMain/res/drawable-xhdpi from assets/icon. Needs Pillow."""
import pathlib

from PIL import Image, ImageDraw, ImageFont

repo = pathlib.Path(__file__).resolve().parents[2]
target = repo / "androidTvApp" / "src" / "androidMain" / "res" / "drawable-xhdpi" / "tv_banner.png"
background = (5, 8, 13)
cream = (243, 240, 233)
font_path = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"

width, height = 320, 180
art = Image.open(repo / "assets" / "icon" / "adaptive-foreground-1024.png").convert("RGBA").resize((150, 150), Image.LANCZOS)
banner = Image.new("RGBA", (width, height), background + (255,))
banner.alpha_composite(art, (0, 15))
draw = ImageDraw.Draw(banner)
font = ImageFont.truetype(font_path, 36)
draw.text((124, height // 2), "Acolyte", font=font, fill=cream, anchor="lm")
target.parent.mkdir(parents=True, exist_ok=True)
banner.convert("RGB").save(target, optimize=True)
