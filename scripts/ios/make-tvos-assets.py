#!/usr/bin/env python3
"""Generates the tvOS brand assets (layered app icon and Top Shelf images) in
iosApp/AcolyteTV/Assets.xcassets from assets/icon. Needs Pillow."""
import json
import pathlib
import shutil

from PIL import Image

repo = pathlib.Path(__file__).resolve().parents[2]
catalog = repo / "iosApp" / "AcolyteTV" / "Assets.xcassets"
brand = catalog / "App Icon & Top Shelf Image.brandassets"
background = (5, 8, 13)
front = Image.open(repo / "assets" / "icon" / "adaptive-foreground-1024.png").convert("RGBA")
info = {"author": "xcode", "version": 1}


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")


def front_layer(width, height, scale):
    side = round(height * scale)
    art = front.resize((side, side), Image.LANCZOS)
    layer = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    layer.paste(art, ((width - side) // 2, (height - side) // 2), art)
    return layer


def back_layer(width, height):
    return Image.new("RGB", (width, height), background)


def imageset(folder, name, images):
    entries = []
    for scale, image in images:
        filename = f"{name}{'' if scale == '1x' else '@' + scale}.png"
        folder.mkdir(parents=True, exist_ok=True)
        image.save(folder / filename)
        entries.append({"idiom": "tv", "filename": filename, "scale": scale})
    write(folder / "Contents.json", {"images": entries, "info": info})


def stack(name, width, height, scales):
    root = brand / f"{name}.imagestack"
    write(root / "Contents.json", {"layers": [{"filename": "Front.imagestacklayer"}, {"filename": "Back.imagestacklayer"}], "info": info})
    for layer, make in (("Front", lambda w, h: front_layer(w, h, 1.35)), ("Back", back_layer)):
        folder = root / f"{layer}.imagestacklayer"
        write(folder / "Contents.json", {"info": info})
        imageset(folder / "Content.imageset", "Content", [(s, make(width * int(s[0]), height * int(s[0]))) for s in scales])


def shelf(name, width, height):
    images = []
    for scale in ("1x", "2x"):
        factor = int(scale[0])
        canvas = back_layer(width * factor, height * factor).convert("RGBA")
        art = front_layer(width * factor, height * factor, 1.6)
        canvas.alpha_composite(art)
        images.append((scale, canvas.convert("RGB")))
    imageset(brand / f"{name}.imageset", name, images)


if catalog.exists():
    shutil.rmtree(brand, ignore_errors=True)
stack("App Icon - App Store", 1280, 768, ["1x"])
stack("App Icon", 400, 240, ["1x", "2x"])
shelf("Top Shelf Image", 1920, 720)
shelf("Top Shelf Image Wide", 2320, 720)
write(
    brand / "Contents.json",
    {
        "assets": [
            {"size": "1280x768", "idiom": "tv", "filename": "App Icon - App Store.imagestack", "role": "primary-app-icon"},
            {"size": "400x240", "idiom": "tv", "filename": "App Icon.imagestack", "role": "primary-app-icon"},
            {"size": "2320x720", "idiom": "tv", "filename": "Top Shelf Image Wide.imageset", "role": "top-shelf-image-wide"},
            {"size": "1920x720", "idiom": "tv", "filename": "Top Shelf Image.imageset", "role": "top-shelf-image"},
        ],
        "info": info,
    },
)
write(catalog / "Contents.json", {"info": info})
src = repo / "iosApp" / "Acolyte" / "Assets.xcassets"
for name in ("AccentColor.colorset", "BrandMark.imageset"):
    shutil.rmtree(catalog / name, ignore_errors=True)
    shutil.copytree(src / name, catalog / name)
