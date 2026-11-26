#!/usr/bin/env python3
"""A small HolyArrs stand-in for the store screenshots: it answers the /api/v1
calls the screenshot screens make from the invented catalog in catalog.py.

    demo-server.py [--port 6263] [--host 0.0.0.0] [--log requests.log]

Sign in with any user name and password. The language of the texts follows the
X-HolyArrs-Language header the apps send. Anything not implemented answers 404
and is listed in the log, which is how missing endpoints are found.
"""
import argparse
import json
import os
import re
import sys
import threading
import time
from datetime import datetime, timedelta, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from catalog import BY_ID, BY_SLUG, BY_TMDB, PEOPLE, TITLES  # noqa: E402

ART = os.path.join(os.path.dirname(os.path.abspath(__file__)), "art")
NOW = datetime.now(timezone.utc).replace(microsecond=0)
LOCAL = threading.local()
ROUTES = []
OWNER = "Noor Haldane"


def lang():
    return getattr(LOCAL, "lang", "en")


def L(en, de):
    return de if lang() == "de" else en


def iso(delta=timedelta()):
    return (NOW + delta).strftime("%Y-%m-%dT%H:%M:%SZ")


def day(offset):
    return (NOW + timedelta(days=offset)).strftime("%Y-%m-%d")


def route(method, pattern):
    def wrap(fn):
        ROUTES.append((method, re.compile("^" + pattern + "$"), fn))
        return fn

    return wrap


def poster(t):
    return f"https://image.tmdb.org/t/p/w500/poster-{t['slug']}.jpg"


def backdrop(t):
    return f"https://image.tmdb.org/t/p/w780/backdrop-{t['slug']}.jpg"


def logo(t):
    return f"https://image.tmdb.org/t/p/w500/logo-{t['slug']}.png"


def overview(t):
    return t["overview_de"] if lang() == "de" else t["overview"]


def genres(t):
    return [t["genres"][1] if lang() == "de" else t["genres"][0]]


def t_(slug):
    return BY_SLUG[slug]


def owned(t):
    return t["id"] > 0


# ---------------------------------------------------------------- library state

DOWNLOADING = {"salt-and-ember": 0.41, "northern-lines": 0.88, "tidewater": 0.72}
IMPORTING = {"midnight-orchard"}
QUEUED = {"velvet-static"}


def title_state(t):
    if t["slug"] in IMPORTING or t["slug"] in DOWNLOADING:
        return "downloading"
    if t["slug"] in QUEUED:
        return "downloading"
    return "in_library"


def ask_of(slug):
    for a in asks():
        if a["tmdb_id"] == t_(slug)["tmdb"]:
            return a
    return None


# ------------------------------------------------------------------ people/asks

ASKERS = [
    {"id": 2, "name": "Lena", "display_name": "Lena Moreau"},
    {"id": 3, "name": "Marek", "display_name": "Marek Novak"},
    {"id": 4, "name": "Iris", "display_name": "Iris Calloway"},
    {"id": 5, "name": "Joaquin", "display_name": "Joaquin Reyes"},
]


def asks():
    def ask(i, slug, state, by, hours, label, **extra):
        t = t_(slug)
        who = ASKERS[by]
        base = {
            "id": i,
            "kind": t["kind"],
            "tmdb_id": t["tmdb"],
            "ref": t["tmdb"],
            "name": t["name"],
            "year": t["year"],
            "poster": poster(t),
            "state": state,
            "state_label": label,
            "asked_by": who,
            "asked_at": iso(-timedelta(hours=hours)),
            "people": [dict(who, asked_at=iso(-timedelta(hours=hours)))],
            "quality": "standard",
            "mine": False,
        }
        base.update(extra)
        return base

    return [
        ask(41, "quiet-machines", "asked", 0, 2, L("Waiting", "Wartet")),
        ask(42, "starling-court", "asked", 1, 5, L("Waiting", "Wartet"), seasons=[1, 2], season_words=L("Seasons 1-2", "Staffeln 1-2")),
        ask(43, "the-understudy", "asked", 2, 20, L("Waiting", "Wartet")),
        ask(
            44,
            "ember-tide",
            "approved",
            3,
            30,
            L("Approved", "Genehmigt"),
            title_id=0,
            adding=False,
            step={"kind": "searching", "text": L("Looking for a good copy", "Sucht nach einer guten Kopie")},
            decided_by={"id": 1, "name": "noor", "display_name": OWNER},
            decided_at=iso(-timedelta(hours=28)),
        ),
        ask(
            45,
            "lowlight",
            "approved",
            0,
            50,
            L("Approved", "Genehmigt"),
            title_id=0,
            step={"kind": "downloading", "text": L("Downloading", "Wird geladen")},
            decided_by={"id": 1, "name": "noor", "display_name": OWNER},
            decided_at=iso(-timedelta(hours=48)),
        ),
        ask(
            46,
            "fjord-and-fire",
            "available",
            2,
            120,
            L("Ready", "Bereit"),
            title_id=0,
            decided_by={"id": 1, "name": "noor", "display_name": OWNER},
            decided_at=iso(-timedelta(hours=118)),
            available_at=iso(-timedelta(hours=100)),
        ),
    ]


# --------------------------------------------------------------------- shapes


def card(t, row, **extra):
    c = {
        "key": f"{row}-{t['id'] or t['tmdb']}",
        "kind": t["kind"],
        "title_id": t["id"],
        "name": t["name"],
        "year": t["year"],
        "line": "",
        "href": f"/title/{t['id']}",
        "backdrop": backdrop(t),
        "poster": poster(t),
        "logo": logo(t),
        "at": iso(-timedelta(hours=6)),
    }
    c.update(extra)
    return c


def discover_item(t, **extra):
    item = {
        "tmdb_id": t["tmdb"],
        "kind": t["kind"],
        "name": t["name"],
        "year": t["year"],
        "poster": poster(t),
        "backdrop": backdrop(t),
        "card_logo": logo(t),
        "overview": overview(t),
        "genres": genres(t),
        "in_library": owned(t),
        "title_id": t["id"],
    }
    a = ask_of(t["slug"])
    if a:
        item["ask"] = {
            "id": a["id"],
            "state": a["state"],
            "state_label": a["state_label"],
            "mine": False,
            "people": 1,
        }
    item.update(extra)
    return item


def grid_title(t):
    state = title_state(t)
    started = iso(-timedelta(days=30 + t["id"] * 9))
    slot_state = "in_library"
    download = None
    if t["slug"] in DOWNLOADING and t["kind"] == "movie":
        slot_state = "downloading"
        download = {"id": 100 + t["id"], "active": True, "progress": DOWNLOADING[t["slug"]], "state": "downloading", "label": "Downloading"}
    if t["slug"] in IMPORTING:
        slot_state = "importing"
        download = {"id": 100 + t["id"], "active": True, "progress": 1.0, "state": "downloading", "label": "Importing"}
    if t["slug"] in QUEUED:
        slot_state = "chosen"
        download = {"id": 100 + t["id"], "active": False, "progress": 0.0, "state": "queued", "label": "Queued"}
    slot = {"id": t["id"] * 10, "state": slot_state, "state_label": slot_state}
    if slot_state == "in_library":
        slot["file"] = {"ID": t["id"] * 100, "State": "ok", "Quality": {"resolution": 2160 if t["id"] in (3, 4) else 1080, "source": "WEB-DL", "codec": "HEVC"}}
    if download:
        slot["download"] = download
    out = {
        "id": t["id"],
        "kind": t["kind"],
        "name": t["name"],
        "year": t["year"],
        "overview": overview(t),
        "ids": {"tmdb": t["tmdb"]},
        "keep_up_to_date": True,
        "library_folder_id": 1,
        "folder_path": "/media/movies" if t["kind"] == "movie" else "/media/shows",
        "pick_rules_id": 1,
        "pick_rules_name": "Standard",
        "artwork": [poster(t)],
        "backdrop": backdrop(t),
        "logo": logo(t),
        "genres": genres(t),
        "runtime_minutes": t["runtime"],
        "rating": t["rating"],
        "score": t["score"],
        "votes": 1200 + t["id"] * 37,
        "added_at": started,
        "size_bytes": 8_200_000_000 + t["id"] * 410_000_000,
        "last_activity": started,
        "versions": [{"id": t["id"], "label": "main", "main": True}],
        "slots": [slot] if t["kind"] == "movie" else [],
    }
    if t["kind"] == "show":
        total = sum(t["episodes"])
        have = total - (2 if t["slug"] == "night-shift-diaries" else 0)
        dl = 1 if t["slug"] in ("tidewater", "northern-lines") else 0
        out["show"] = {
            "status": "Continuing" if t["slug"] != "northern-lines" else "Ended",
            "network": "Harbor TV",
            "seasons": [{"number": n + 1, "keep_up_to_date": True, "aired": c} for n, c in enumerate(t["episodes"])],
            "episodes": total,
            "missing": total - have - dl,
            "in_library": have - dl,
            "downloading": dl,
            "season_count": t["seasons"],
            "next_air_date": day(3) if t["slug"] == "night-shift-diaries" else "",
        }
    return out


def owned_title(t):
    g = grid_title(t)
    out = {k: g[k] for k in ("id", "kind", "name", "year", "overview", "ids", "keep_up_to_date", "library_folder_id", "folder_path", "pick_rules_id", "pick_rules_name", "artwork", "backdrop", "logo", "genres", "runtime_minutes", "rating", "added_at", "size_bytes", "versions", "show")  if k in g}
    out["slots"] = []
    for s in g["slots"]:
        slot = dict(s, version="main", version_id=t["id"])
        if "download" in s:
            d = s["download"]
            slot["download"] = {"id": d["id"], "state": d["state"], "label": d["label"], "progress": d["progress"], "speed": 12_000_000, "eta": 600, "active": d["active"]}
        out["slots"].append(slot)
    out["stage"] = "ready"
    return out


def download_rows():
    def row(i, slug, name, progress, phase, state, speed, eta, size, **extra):
        t = t_(slug)
        r = {
            "id": i,
            "state": state,
            "label": name,
            "text": "",
            "progress": progress,
            "speed": speed,
            "up_speed": 0,
            "seeds": 42,
            "peers": 9,
            "eta": eta,
            "active": phase == "downloading",
            "phase": phase,
            "name": name,
            "title_id": t["id"],
            "title": t["name"],
            "year": t["year"],
            "downloader": "Harbor qBit",
            "downloader_id": 1,
            "hash": f"{i:040x}",
            "downloader_kind": "qbittorrent",
            "protocol": "torrent",
            "size": size,
            "started_at": iso(-timedelta(minutes=40)),
            "can_act": True,
            "kind": t["kind"],
            "client_item": True,
            "version": "main",
        }
        r.update(extra)
        return r

    return [
        row(101, "tidewater", "Tidewater.S04E05.1080p.WEB-DL.DDP5.1.H.264", 0.72, "downloading", "downloading", 21_400_000, 190, 2_800_000_000,
            episodes=[{"slot_id": 1, "season": 4, "episode": 5, "label": "S04E05", "name": L("Slack Water", "Stillwasser"), "state": "downloading"}]),
        row(102, "northern-lines", "Northern.Lines.S02.1080p.BluRay.x265", 0.88, "downloading", "downloading", 36_800_000, 95, 14_300_000_000,
            pack={"from": 1, "to": 8, "taken": 7, "total": 8}),
        row(103, "salt-and-ember", "Salt.and.Ember.2024.2160p.WEB-DL.DV.HDR.H.265", 0.41, "downloading", "downloading", 28_100_000, 640, 21_700_000_000),
        row(104, "midnight-orchard", "Midnight.Orchard.2021.1080p.BluRay.x264", 1.0, "importing", "completed", 0, 0, 9_100_000_000, active=False),
        row(105, "velvet-static", "Velvet.Static.2022.1080p.WEB-DL.DDP5.1", 0.0, "waiting", "queued", 0, 0, 7_400_000_000, active=False),
        row(106, "harbor-lights", "Harbor.Lights.2024.1080p.WEB-DL.H.264", 1.0, "done", "seeding", 0, 0, 6_800_000_000,
            active=False, seeding=True, finished_at=iso(-timedelta(hours=9)), up_speed=1_200_000, ratio=1.4),
        row(107, "orbit-nine", "Orbit.Nine.2025.2160p.WEB-DL.DV.HDR.H.265", 1.0, "done", "seeding", 0, 0, 23_800_000_000,
            active=False, seeding=True, finished_at=iso(-timedelta(hours=26)), up_speed=800_000, ratio=0.9),
    ]


# --------------------------------------------------------------------- routes


def not_found():
    return 404, {"error": "not found", "code": "not_found"}


@route("GET", "/api/v1/auth/state")
def auth_state(c):
    return {"needs_setup": False, "ways": ["password"], "min_password_length": 8}


@route("POST", "/api/v1/auth/login")
def auth_login(c):
    name = (c.body or {}).get("name") or "noor"
    return 200, {"name": name, "csrf": "demo-csrf-token"}, {"Set-Cookie": "holyarrs_session=demo-session; Path=/; HttpOnly; SameSite=Lax"}


@route("POST", "/api/v1/auth/logout")
def auth_logout(c):
    return 204, None


@route("GET", "/api/v1/auth/me")
def auth_me(c):
    return {
        "id": 1,
        "name": "noor",
        "via": "session",
        "csrf": "demo-csrf-token",
        "role": "owner",
        "perms": ["ask", "approve", "manage_library", "settings"],
        "auth_method": "password",
        "has_password": True,
        "held": [],
        "max_rating": "",
        "rating_country": "US",
        "views": {"welcome_seen": "1"},
    }


@route("GET", "/api/v1/parts")
def parts(c):
    def part(key, name, on=True):
        return {"part": key, "name": name, "on": on, "state": "on" if on else "off"}

    return {
        "parts": [
            part("core", "Core"),
            part("movies", "Movies"),
            part("shows", "Shows"),
            part("music", "Music", False),
            part("subtitles", "Subtitles", False),
            part("wishlist", "Requests"),
            part("sources", "Sources", False),
        ]
    }


@route("GET", "/api/v1/my/profile")
def my_profile(c):
    return {"display_name": OWNER, "username": "noor", "language": lang()}


@route("GET", "/api/v1/app/about")
def about(c):
    return {"version": "1.3.0", "uptime_s": 864000, "started_at": iso(-timedelta(days=10))}


@route("GET", "/api/v1/home")
def home(c):
    arrived = [
        card(t_("orbit-nine"), "arrived", at=iso(-timedelta(hours=3))),
        card(t_("glasshouse"), "arrived", line=L("Season 3 · Episode 6", "Staffel 3 · Folge 6"), at=iso(-timedelta(hours=5))),
        card(t_("paper-moons"), "arrived", at=iso(-timedelta(hours=8))),
        card(t_("harbor-lights"), "arrived", at=iso(-timedelta(hours=9))),
    ]
    new_seasons = [
        card(t_("night-shift-diaries"), "season", line=L("Season 2", "Staffel 2"), season=2, at=iso(-timedelta(hours=4))),
        card(t_("the-cartographers"), "season", line=L("Season 1", "Staffel 1"), season=1, at=iso(-timedelta(hours=12))),
    ]
    following = [card(t_("tidewater"), "following", line=L("Season 4 · Episode 5", "Staffel 4 · Folge 5"))]
    soon = [card(t_("northern-lines"), "soon", line=L("Season 3", "Staffel 3"), at=iso(timedelta(days=4)))]
    return {
        "rows": ["new_seasons", "arrived", "following", "soon"],
        "owner": True,
        "arrived": arrived,
        "new_seasons": new_seasons,
        "following": following,
        "soon": soon,
        "ask_progress": {},
        "library": {
            "movie": {"titles": 8, "parts": 8, "items": 8, "on_disk": 6, "missing": 0, "downloading": 3, "size": 118_000_000_000},
            "show": {"titles": 5, "parts": 14, "items": 112, "on_disk": 108, "missing": 2, "downloading": 2, "size": 396_000_000_000},
        },
    }


@route("GET", "/api/v1/health")
def health(c):
    return {
        "version": "1.3.0",
        "uptime_s": 864000,
        "ok": True,
        "sources": [],
        "downloaders": [{"name": "Harbor qBit", "state": "ok"}],
        "schema": 41,
    }


@route("GET", "/api/v1/health/checks")
def health_checks(c):
    return {"checks": [{"id": 1, "key": "disk:/media", "kind": "disk_space", "subject": "/media", "state": "ok", "message": ""}], "worst": "ok"}


@route("GET", "/api/v1/needs-decision")
def needs_decision(c):
    return {"items": [], "count": 0}


@route("GET", "/api/v1/library-folders")
def library_folders(c):
    return {
        "library_folders": [
            {"id": 1, "path": "/media/movies", "kind": "movie", "label": "Movies", "free_space": 2_400_000_000_000, "health": "ok"},
            {"id": 2, "path": "/media/shows", "kind": "show", "label": "Shows", "free_space": 2_400_000_000_000, "health": "ok"},
        ]
    }


@route("GET", "/api/v1/downloads/view")
def downloads_view(c):
    rows = download_rows()
    return {
        "downloads": rows,
        "totals": {"speed": sum(r["speed"] for r in rows), "up_speed": 2_000_000},
        "clients": [{"id": 1, "name": "Harbor qBit", "health": "ok"}],
    }


@route("GET", "/api/v1/downloads/clients")
def download_clients(c):
    return {"clients": [{"id": 1, "name": "Harbor qBit", "kind": "qbittorrent", "protocol": "torrent", "controls": True, "torrent_controls": True,
                         "status": {"speed": 86_300_000, "up_speed": 2_000_000, "free_space": 2_400_000_000_000, "connection": "connected"}}]}


@route("GET", "/api/v1/asks")
def asks_list(c):
    rows = asks()
    state = c.query.get("state")
    if state:
        rows = [r for r in rows if r["state"] == state]
    return {
        "asks": rows,
        "counts": {"pending": 3, "processing": 2, "done": 1, "failed": 0},
        "total": len(rows),
    }


@route("GET", "/api/v1/asks/limit")
def asks_limit(c):
    return {"no_limit": True}


@route("GET", "/api/v1/messages")
def messages(c):
    return {"messages": [], "unread": 0}


@route("GET", "/api/v1/titles")
def titles(c):
    rows = [grid_title(t) for t in TITLES if owned(t)]
    q = c.query.get("q", "").lower()
    if q:
        rows = [r for r in rows if q in r["name"].lower()]
    return {"titles": rows}


@route("GET", r"/api/v1/titles/(?P<id>\d+)")
def title_one(c):
    t = BY_ID.get(int(c.m["id"]))
    return owned_title(t) if t else not_found()


@route("GET", "/api/v1/titles/owned")
def titles_owned(c):
    items = []
    for t in TITLES:
        if owned(t):
            state = title_state(t)
            items.append({"kind": t["kind"], "tmdb": t["tmdb"], "id": t["id"], "state": state,
                          "percent": DOWNLOADING.get(t["slug"], 0) * 100, "genres": genres(t)})
    return {"items": items}


@route("GET", "/api/v1/my/watched")
def my_watched(c):
    return {"enabled": False}


@route("GET", "/api/v1/collections")
def collections(c):
    return {"collections": []}


CALENDAR_PLAN = [
    (2, "glasshouse", "episode", 3, 7, "The Draft in Room Four"),
    (2, "tidewater", "episode", 4, 3, "Slack Water"),
    (2, "the-understudy", "cinema", 0, 0, ""),
    (4, "night-shift-diaries", "episode", 2, 5, "Double Shift"),
    (4, "northern-lines", "episode", 3, 1, "Last Train Out"),
    (4, "moth-and-lamp", "cinema", 0, 0, ""),
    (7, "starling-court", "episode", 2, 4, "Conflict of Interest"),
    (7, "lowlight", "episode", 1, 6, "Long Exposure"),
    (9, "the-cartographers", "episode", 1, 8, "The Edge of the Sheet"),
    (9, "glasshouse", "episode", 3, 8, "Condensation"),
    (11, "fjord-and-fire", "cinema", 0, 0, ""),
    (12, "tidewater", "episode", 4, 4, "Neap"),
    (14, "night-shift-diaries", "episode", 2, 6, "Skeleton Crew"),
    (14, "quiet-machines", "cinema", 0, 0, ""),
    (16, "starling-court", "episode", 2, 5, "Closing Arguments"),
    (18, "ember-tide", "cinema", 0, 0, ""),
    (19, "lowlight", "episode", 1, 7, "Contact Sheet"),
    (21, "the-cartographers", "episode", 1, 9, "Terra Incognita"),
    (23, "glasshouse", "episode", 3, 9, "Greenhouse Effect"),
    (26, "northern-lines", "episode", 3, 2, "Terminus"),
]


@route("GET", "/api/v1/calendar")
def calendar(c):
    start = datetime.strptime(c.query.get("from", day(0))[:10], "%Y-%m-%d")
    mid = start + timedelta(days=15)
    today = NOW.strftime("%Y-%m-%d")
    rows = []
    for i, (dom, slug, kind, season, ep, name) in enumerate(CALENDAR_PLAN):
        t = t_(slug)
        date = f"{mid.year:04d}-{mid.month:02d}-{dom:02d}"
        state = "in_library" if date < today else ""
        if slug == "tidewater" and ep == 4:
            state = "searching" if date >= today else state
        rows.append({
            "title_id": t["id"], "name": t["name"], "year": t["year"], "kind": kind, "date": date,
            "item_id": 7000 + i if kind == "episode" else 0, "season": season, "episode": ep, "episode_name": name,
            "tmdb": t["tmdb"], "air_day": date if kind == "episode" else "", "timed": False,
            "runtime": t["runtime"], "network": "Harbor Stream" if kind == "episode" else "", "state": state,
            "versions": [], "poster": poster(t), "backdrop": backdrop(t), "genres": genres(t),
            "studio": "Harbor Pictures" if kind != "episode" else "", "following": False,
        })
    return {"from": c.query.get("from", ""), "to": c.query.get("to", ""), "dates": rows, "my_follows": 0, "people": 1}


@route("GET", "/api/v1/explore/head")
def explore_head(c):
    def row(rtype, title, slugs, subtitle=""):
        return {"id": rtype, "type": rtype, "title": title, "subtitle": subtitle, "wide": True,
                "items": [discover_item(t_(s)) for s in slugs]}

    return {
        "type": c.query.get("type", "all"),
        "hero": [discover_item(t_(s)) for s in ("ember-tide", "starling-court", "quiet-machines")],
        "head": [
            row("cinemas", L("In cinemas now", "Jetzt im Kino"),
                ["fjord-and-fire", "the-understudy", "moth-and-lamp", "ember-tide", "quiet-machines", "orbit-nine", "velvet-static"]),
            row("because_have", L("Because you have Orbit Nine", "Weil du Orbit Nine hast"),
                ["the-last-lantern", "paper-moons", "salt-and-ember", "midnight-orchard", "copper-canyon", "harbor-lights"]),
            row("returning", L("Shows that are back", "Serien sind zurück"),
                ["tidewater", "glasshouse", "night-shift-diaries", "northern-lines", "the-cartographers"]),
            row("new_seasons", L("New seasons", "Neue Staffeln"), ["lowlight", "starling-court", "the-cartographers", "night-shift-diaries"]),
        ],
    }


@route("GET", "/api/v1/explore/tail")
def explore_tail(c):
    return {"type": c.query.get("type", "all"), "page": 0, "more": False, "rows": []}


@route("GET", "/api/v1/explore/strip")
def explore_strip(c):
    return {"type": c.query.get("type", "all"), "tiles": []}


GENRE_CARDS = [
    (18, "Drama", "harbor-lights"), (14, "Fantasy", "the-last-lantern"), (16, "Animation", "paper-moons"),
    (878, "Science Fiction", "orbit-nine"), (53, "Thriller", "velvet-static"), (12, "Adventure", "salt-and-ember"),
    (9648, "Mystery", "midnight-orchard"), (37, "Western", "copper-canyon"), (35, "Comedy", "night-shift-diaries"),
    (80, "Crime", "tidewater"),
]


@route("GET", "/api/v1/explore/genre-row")
def explore_genre_row(c):
    kind = "show" if c.query.get("type") == "shows" else "movie"
    return {"type": c.query.get("type", "all"),
            "genres": [{"kind": kind, "id": gid, "name": name, "backdrop": backdrop(t_(slug)), "from": "", "both": False}
                       for gid, name, slug in GENRE_CARDS]}


@route("GET", "/api/v1/explore/new")
def explore_new(c):
    def row(rtype, title, slugs):
        return {"id": rtype, "type": rtype, "title": title, "subtitle": "", "wide": True,
                "items": [discover_item(t_(s)) for s in slugs]}

    def entry(slug, what):
        return discover_item(t_(slug), what=what)

    rail = [
        {"day": day(-1), "items": [entry("glasshouse", "On Harbor Stream"), entry("the-understudy", "Out")]},
        {"day": day(0), "items": [entry("fjord-and-fire", "Out"), entry("starling-court", "On Harbor Stream"), entry("lowlight", "On Harbor Stream")]},
        {"day": day(1), "items": [entry("moth-and-lamp", "Coming"), entry("ember-tide", "Coming")]},
        {"day": day(2), "items": [entry("quiet-machines", "Coming"), entry("the-cartographers", "On Harbor Stream")]},
    ]
    return {
        "type": c.query.get("type", "all"),
        "page": 0,
        "more": False,
        "hero": [discover_item(t_(s)) for s in ("starling-court", "ember-tide", "lowlight")],
        "rail": rail,
        "rows": [
            row("popular_streaming", L("Popular on Harbor Stream", "Beliebt auf Harbor Stream"),
                ["starling-court", "lowlight", "glasshouse", "tidewater", "the-cartographers", "northern-lines"]),
            row("popular_cinemas", L("Popular in cinemas", "Beliebt im Kino"),
                ["fjord-and-fire", "the-understudy", "ember-tide", "quiet-machines", "moth-and-lamp"]),
        ],
    }


@route("POST", "/api/v1/explore/counts")
def explore_counts(c):
    return 204, None


@route("POST", "/api/v1/explore/strip/counts")
def explore_strip_counts(c):
    return 204, None


@route("GET", r"/api/v1/metadata/(?P<kind>movie|show)/(?P<tmdb>\d+)")
def metadata(c):
    t = BY_TMDB.get(c.m["tmdb"])
    if not t:
        return not_found()
    people = [
        {"name": PEOPLE[i], "role": "cast", "character": ch, "profile": f"https://image.tmdb.org/t/p/w185/profile-{i + 1}.jpg", "id": str(5000 + i)}
        for i, ch in enumerate(["Captain Ines Varga", "Elias Thorne", "The Keeper", "Mayor Dunmore", "Petra", "Old Jonas"])
    ]
    seasons = [{"number": n + 1, "episode_count": ec, "year": t["year"] + n} for n, ec in enumerate(t.get("episodes", ()))]
    out = {
        "tmdb_id": t["tmdb"],
        "kind": t["kind"],
        "name": t["name"],
        "year": t["year"],
        "poster": poster(t),
        "backdrop": backdrop(t),
        "logo": logo(t),
        "overview": overview(t),
        "genres": genres(t),
        "runtime_minutes": t["runtime"] if t["kind"] == "movie" else 0,
        "episode_runtime": t["runtime"] if t["kind"] == "show" else 0,
        "rating": t["rating"],
        "score": t["score"],
        "votes": 1200,
        "people": people,
        "status": "Released" if t["kind"] == "movie" else "Returning Series",
        "tagline": L("Some places keep what you leave behind.", "Manche Orte bewahren, was du zurücklässt."),
        "studios": ["Harbor Pictures"],
        "seasons": seasons,
        "similar": [
            {"tmdb_id": s["tmdb"], "kind": s["kind"], "name": s["name"], "year": s["year"], "poster": poster(s), "backdrop": backdrop(s),
             "in_library": owned(s), "title_id": s["id"]}
            for s in TITLES if s is not t and s["kind"] == t["kind"]
        ][:8],
        "in_library": owned(t),
        "title_id": t["id"],
    }
    a = ask_of(t["slug"])
    if a:
        out["ask"] = {"id": a["id"], "state": a["state"], "state_label": a["state_label"], "mine": False, "people": 1}
    return out


@route("GET", "/api/v1/titles/lookup")
def lookup_movies(c):
    t = BY_TMDB.get(c.query.get("term", "").replace("tmdb:", ""))
    if not t or t["kind"] != "movie":
        return {"movies": []}
    return {"movies": [{"name": t["name"], "year": t["year"], "ids": {"tmdb": t["tmdb"]}, "poster": poster(t), "title_id": t["id"],
                        "overview": overview(t), "backdrop": backdrop(t), "genres": genres(t)}]}


@route("GET", "/api/v1/shows/lookup")
def lookup_shows(c):
    t = BY_TMDB.get(c.query.get("term", "").replace("tmdb:", ""))
    if not t or t["kind"] != "show":
        return {"shows": []}
    return {"shows": [{"source": "tmdb", "name": t["name"], "year": t["year"], "ids": {"tmdb": t["tmdb"]}, "poster": poster(t),
                       "title_id": t["id"], "overview": overview(t), "backdrop": backdrop(t), "genres": genres(t)}]}


@route("GET", r"/api/v1/extras/(?P<kind>movie|show)/(?P<tmdb>\d+)")
def extras(c):
    t = BY_TMDB.get(c.m["tmdb"])
    if not t:
        return not_found()
    return {"scores": [{"source": "tmdb", "value": t["score"], "max": 10, "votes": 1200}]}


@route("GET", r"/api/v1/artwork/tmdb/(?P<size>[^/]+)/(?P<file>[A-Za-z0-9_.-]+)")
def artwork(c):
    path = os.path.join(ART, os.path.basename(c.m["file"]))
    if not os.path.isfile(path):
        return not_found()
    with open(path, "rb") as f:
        kind = "image/png" if path.endswith(".png") else "image/jpeg"
        return 200, f.read(), {"Content-Type": kind, "Cache-Control": "max-age=3600"}


@route("GET", "/live")
def live(c):
    return "stream"


@route("POST", "/api/v1/live/interest")
def live_interest(c):
    return {"ok": True}


# ---------------------------------------------------------------------- server


class Ctx:
    def __init__(self, query, body):
        self.query = {k: v[0] for k, v in query.items()}
        self.body = body
        self.m = {}


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"
    server_version = "demo"

    def log_message(self, fmt, *args):
        pass

    def _log(self, status, note=""):
        line = f"{self.command} {self.path} -> {status} {note}".rstrip()
        with LOG_LOCK:
            print(line, flush=True)

    def _handle(self):
        parsed = urlparse(self.path)
        length = int(self.headers.get("Content-Length") or 0)
        raw = self.rfile.read(length) if length else b""
        try:
            body = json.loads(raw) if raw else None
        except ValueError:
            body = None
        LOCAL.lang = (self.headers.get("X-HolyArrs-Language") or "en").lower()[:2]
        ctx = Ctx(parse_qs(parsed.query), body)
        for method, pattern, fn in ROUTES:
            if method != self.command:
                continue
            m = pattern.match(parsed.path)
            if not m:
                continue
            ctx.m = m.groupdict()
            result = fn(ctx)
            if result == "stream":
                return self._stream()
            headers = {}
            if isinstance(result, tuple):
                status, payload = result[0], result[1]
                headers = result[2] if len(result) > 2 else {}
            else:
                status, payload = 200, result
            return self._send(status, payload, headers)
        self._log(404, "(not implemented)")
        self._send(*not_found(), quiet=True)

    def _send(self, status, payload, headers=None, quiet=False):
        headers = dict(headers or {})
        if isinstance(payload, bytes):
            data = payload
        elif payload is None:
            data = b""
        else:
            data = json.dumps(payload).encode()
            headers.setdefault("Content-Type", "application/json")
        if not quiet:
            self._log(status)
        self.send_response(status)
        for k, v in headers.items():
            self.send_header(k, v)
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        if self.command != "HEAD":
            self.wfile.write(data)

    def _stream(self):
        self._log(200, "(event stream)")
        self.send_response(200)
        self.send_header("Content-Type", "text/event-stream")
        self.send_header("Cache-Control", "no-cache")
        self.send_header("Connection", "keep-alive")
        self.end_headers()
        try:
            self.wfile.write(b": connected\n\n")
            self.wfile.flush()
            while True:
                time.sleep(15)
                self.wfile.write(b": heartbeat\n\n")
                self.wfile.flush()
        except (BrokenPipeError, ConnectionResetError, OSError):
            pass

    do_GET = do_POST = do_PUT = do_DELETE = do_PATCH = do_HEAD = _handle


LOG_LOCK = threading.Lock()


class Server(ThreadingHTTPServer):
    daemon_threads = True
    allow_reuse_address = True


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--host", default="0.0.0.0")
    p.add_argument("--port", type=int, default=6263)
    a = p.parse_args()
    server = Server((a.host, a.port), Handler)
    print(f"demo server on http://{a.host}:{a.port}", flush=True)
    server.serve_forever()


if __name__ == "__main__":
    main()
