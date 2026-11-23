#!/usr/bin/env python3
"""Compares the pinned HolyArrs spec with newer ones and reports what changed,
highlighting the operations the app calls (shared/build/reports/api/client-endpoints.json,
written by ApiContractTest).

Usage: drift.py --pinned api/openapi.json --pinned-label 1.3.0 \
                --compare new.json --compare-label "release 1.4.0" [--compare ... --compare-label ...] \
                --used client-endpoints.json [--out drift.md] [--github-output $GITHUB_OUTPUT]
Always exits 0; the outputs say whether the app's surface changed."""
import argparse
import hashlib
import json

METHODS = ("get", "post", "put", "patch", "delete")


def plugin(path):
    return path.startswith("/api/v1/plugins") or path.startswith("/api/v1/plugin-")


def resolve(spec, node, depth=0):
    if depth > 15:
        return node
    if isinstance(node, dict):
        if "$ref" in node:
            name = node["$ref"].rsplit("/", 1)[-1]
            return resolve(spec, spec.get("components", {}).get("schemas", {}).get(name, {}), depth + 1)
        return {k: resolve(spec, v, depth + 1) for k, v in node.items() if k not in ("description", "example", "examples", "summary")}
    if isinstance(node, list):
        return [resolve(spec, v, depth + 1) for v in node]
    return node


def digest(value):
    return hashlib.sha256(json.dumps(value, sort_keys=True).encode()).hexdigest()[:12]


def operations(spec):
    out = {}
    for path, item in spec.get("paths", {}).items():
        if plugin(path):
            continue
        for method in METHODS:
            op = item.get(method)
            if not op:
                continue
            params = sorted(
                f"{p.get('in')}:{p.get('name')}{'!' if p.get('required') else ''}"
                for p in resolve(spec, op.get("parameters", []))
            )
            responses = {code: resolve(spec, r.get("content", {})) for code, r in op.get("responses", {}).items() if code.startswith("2")}
            out[f"{method.upper()} {path}"] = {
                "permission": "public" if op.get("security") == [] else op.get("x-holyarrs-permission"),
                "part": op.get("x-holyarrs-part"),
                "scope": op.get("x-holyarrs-scope"),
                "deprecated": bool(op.get("deprecated")),
                "params": params,
                "request": digest(resolve(spec, op.get("requestBody", {}))),
                "response": digest(responses),
            }
    return out


def changes(old, new):
    what = []
    for field in ("permission", "part", "scope", "deprecated"):
        if old[field] != new[field]:
            what.append(f"{field} {old[field]} → {new[field]}")
    if old["params"] != new["params"]:
        added = sorted(set(new["params"]) - set(old["params"]))
        removed = sorted(set(old["params"]) - set(new["params"]))
        what.append("params" + (f" +{added}" if added else "") + (f" -{removed}" if removed else ""))
    if old["request"] != new["request"]:
        what.append("request body schema")
    if old["response"] != new["response"]:
        what.append("response schema")
    return what


def report(pinned, pinned_label, other, label, used):
    old, new = operations(pinned), operations(other)
    added = sorted(new.keys() - old.keys())
    removed = sorted(old.keys() - new.keys())
    changed = {k: changes(old[k], new[k]) for k in sorted(old.keys() & new.keys())}
    changed = {k: v for k, v in changed.items() if v}
    used_removed = [k for k in removed if k in used]
    used_changed = {k: v for k, v in changed.items() if k in used}
    lines = [f"### {pinned_label} → {label}", ""]
    lines.append(
        f"{len(added)} added, {len(removed)} removed, {len(changed)} changed operations "
        f"(spec version {pinned.get('info', {}).get('version')} → {other.get('info', {}).get('version')})."
    )
    lines.append("")
    if used_removed or used_changed:
        lines += ["**Operations the app uses that changed:**", "", "| Operation | Change |", "| --- | --- |"]
        lines += [f"| `{k}` | removed |" for k in used_removed]
        lines += [f"| `{k}` | {'; '.join(v)} |" for k, v in used_changed.items()]
        lines.append("")
    else:
        lines += ["No operation the app uses changed.", ""]
    for title, keys in (("Added", added), ("Removed", removed)):
        if keys:
            lines += [f"<details><summary>{title} ({len(keys)})</summary>", ""] + [f"- `{k}`" for k in keys] + ["", "</details>", ""]
    other_changed = [k for k in changed if k not in used]
    if other_changed:
        lines += [f"<details><summary>Changed, not used by the app ({len(other_changed)})</summary>", ""]
        lines += [f"- `{k}`: {'; '.join(changed[k])}" for k in other_changed] + ["", "</details>", ""]
    return "\n".join(lines), bool(used_removed or used_changed), bool(added or removed or changed)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pinned", required=True)
    parser.add_argument("--pinned-label", required=True)
    parser.add_argument("--compare", action="append", required=True)
    parser.add_argument("--compare-label", action="append", required=True)
    parser.add_argument("--used", required=True)
    parser.add_argument("--out")
    parser.add_argument("--github-output")
    args = parser.parse_args()
    pinned = json.load(open(args.pinned))
    used = {f"{e['method']} {e['path']}" for e in json.load(open(args.used))}
    sections = [f"## HolyArrs API drift (app pinned to {args.pinned_label}, {len(used)} operations used)", ""]
    surface_changed = False
    any_changed = False
    for path, label in zip(args.compare, args.compare_label):
        text, used_changed, changed = report(pinned, args.pinned_label, json.load(open(path)), label, used)
        sections.append(text)
        surface_changed |= used_changed
        any_changed |= changed
    text = "\n".join(sections)
    print(text)
    if args.out:
        open(args.out, "w").write(text + "\n")
    if args.github_output:
        with open(args.github_output, "a") as out:
            out.write(f"surface_changed={'true' if surface_changed else 'false'}\n")
            out.write(f"spec_changed={'true' if any_changed else 'false'}\n")


if __name__ == "__main__":
    main()
