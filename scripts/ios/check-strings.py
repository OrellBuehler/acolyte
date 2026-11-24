#!/usr/bin/env python3
"""Checks the iOS string tables: en and de have the same keys, every key the
Swift code names exists, and German uses Swiss spelling (no eszett)."""
import pathlib
import re
import sys

root = pathlib.Path(__file__).resolve().parents[2] / "iosApp" / "Acolyte"
sources = [root, root.parent / "AcolyteTV"]
entry = re.compile(r'^"((?:[^"\\]|\\.)+)"\s*=\s*"((?:[^"\\]|\\.)*)";\s*$')


def table(lang):
    keys = {}
    for line in (root / f"{lang}.lproj" / "Localizable.strings").read_text(encoding="utf-8").splitlines():
        match = entry.match(line.strip())
        if match:
            if match.group(1) in keys:
                print(f"{lang}: duplicate key {match.group(1)}")
                sys.exit(1)
            keys[match.group(1)] = match.group(2)
    return keys


en, de = table("en"), table("de")
problems = []
problems += [f"missing in de: {k}" for k in sorted(en.keys() - de.keys())]
problems += [f"missing in en: {k}" for k in sorted(de.keys() - en.keys())]
problems += [f"eszett in de: {k}" for k, v in de.items() if "ß" in v]
used = set()
for swift in (path for source in sources for path in source.rglob("*.swift")):
    text = swift.read_text(encoding="utf-8")
    used.update(re.findall(r'L10n\.(?:tr|format)\(\s*"([^"]+)"', text))
    for a, b in re.findall(r'L10n\.tr\(\s*[^")]*\?\s*"([^"]+)"\s*:\s*"([^"]+)"', text):
        used.update([a, b])
problems += [f"used but not defined: {k}" for k in sorted(used - en.keys())]
for p in problems:
    print(p)
sys.exit(1 if problems else 0)
