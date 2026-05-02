"""
Downloads all 10 Lithuanian county (admin_level=4) boundaries from Overpass API
in a single query, ensuring shared borders have exactly matching coordinates.
Then uploads them to Firestore, replacing existing data.

Requirements:
    pip install requests firebase-admin
Place serviceAccountKey.json in the same directory before running.
"""

import json
import math
import requests
import firebase_admin
from firebase_admin import credentials, firestore

# ── Overpass ──────────────────────────────────────────────────────────────────

OVERPASS_URL = "https://overpass-api.de/api/interpreter"
OVERPASS_QUERY = """
[out:json][timeout:120];
area["ISO3166-1"="LT"][admin_level="2"]->.lt;
relation(area.lt)[boundary="administrative"][admin_level="4"];
out geom;
"""

DESCRIPTIONS = {
    "Vilnius County":    "The largest Lithuanian county, home to the capital city and its surroundings.",
    "Kaunas County":     "Central Lithuania's most populous county, centered around Lithuania's second city.",
    "Klaipėda County":  "The western coastal county, home to Lithuania's only seaport and the Curonian Spit.",
    "Šiauliai County":  "Northern Lithuania's county, known for the Hill of Crosses and industrial heritage.",
    "Panevėžys County": "North-central county, one of Lithuania's largest by area.",
    "Alytus County":     "Southern Lithuania's county, known for the Dzūkija National Park and forests.",
    "Marijampolė County":"Southwestern county bordering Poland and the Kaliningrad region.",
    "Tauragė County":   "Western county known for its diverse countryside and Žemaitija heritage.",
    "Telšiai County":   "Northwestern county, heart of the Samogitian (Žemaičiai) cultural region.",
    "Utena County":      "Northeastern county, famous for the Aukštaitija National Park and its many lakes.",
}

# ── Geometry helpers ──────────────────────────────────────────────────────────

def merge_outer_ways(members):
    """Chain outer-role way segments into a single closed ring [(lat, lng), ...]."""
    segments = []
    for m in members:
        if m.get("role") != "outer":
            continue
        geom = m.get("geometry", [])
        if len(geom) < 2:
            continue
        segments.append([(n["lat"], n["lon"]) for n in geom])

    if not segments:
        return []

    ring = list(segments[0])
    used = {0}

    for _ in range(len(segments) - 1):
        tail = ring[-1]
        matched = False
        for i, seg in enumerate(segments):
            if i in used:
                continue
            if _close(seg[0], tail):
                ring.extend(seg[1:])
                used.add(i)
                matched = True
                break
            if _close(seg[-1], tail):
                ring.extend(reversed(seg[:-1]))
                used.add(i)
                matched = True
                break
        if not matched:
            break  # disjoint — take what we have

    # Close ring
    if ring and not _close(ring[0], ring[-1]):
        ring.append(ring[0])

    return ring


def _close(a, b, tol=1e-6):
    return abs(a[0] - b[0]) < tol and abs(a[1] - b[1]) < tol


def polygon_area_km2(poly):
    n = len(poly)
    area = 0.0
    for i in range(n):
        j = (i + 1) % n
        area += poly[i][1] * poly[j][0]
        area -= poly[j][1] * poly[i][0]
    area = abs(area) / 2.0
    avg_lat = sum(p[0] for p in poly) / n
    return area * 111.111 * (111.111 * math.cos(math.radians(avg_lat)))


def lat_lng_to_tile(lat, lng, zoom):
    n = 1 << zoom
    x = int((lng + 180.0) / 360.0 * n)
    lat_rad = math.radians(lat)
    y = int((1.0 - math.log(math.tan(lat_rad) + 1.0 / math.cos(lat_rad)) / math.pi) / 2.0 * n)
    return max(0, min(n - 1, x)), max(0, min(n - 1, y))


def tile_center(tx, ty, zoom):
    n = 1 << zoom
    lng = tx / n * 360.0 - 180.0 + 0.5 / n * 360.0
    lat = math.degrees((
        math.atan(math.sinh(math.pi * (1 - 2.0 * ty / n))) +
        math.atan(math.sinh(math.pi * (1 - 2.0 * (ty + 1) / n)))
    ) / 2.0)
    return lat, lng


def point_in_polygon(lat, lng, poly):
    inside = False
    j = len(poly) - 1
    for i in range(len(poly)):
        lat_i, lng_i = poly[i]
        lat_j, lng_j = poly[j]
        if ((lng_i > lng) != (lng_j > lng) and
                lat < (lat_j - lat_i) * (lng - lng_i) / (lng_j - lng_i) + lat_i):
            inside = not inside
        j = i
    return inside


def count_goal_tiles(polygon, zoom=20):
    lats = [p[0] for p in polygon]
    lngs = [p[1] for p in polygon]
    tx_min, ty_max = lat_lng_to_tile(min(lats), min(lngs), zoom)
    tx_max, ty_min = lat_lng_to_tile(max(lats), max(lngs), zoom)
    total = (tx_max - tx_min + 1) * (ty_max - ty_min + 1)
    print(f"    Scanning {total:,} bbox tiles…", end="", flush=True)
    count = 0
    rows = ty_max - ty_min + 1
    for ri, ty in enumerate(range(ty_min, ty_max + 1)):
        if ri % 200 == 0:
            print(f" {ri/rows*100:.0f}%", end="", flush=True)
        for tx in range(tx_min, tx_max + 1):
            clat, clng = tile_center(tx, ty, zoom)
            if point_in_polygon(clat, clng, polygon):
                count += 1
    print()
    return count


# ── Main ──────────────────────────────────────────────────────────────────────

print("Fetching Lithuanian county boundaries from Overpass API…")
resp = requests.post(OVERPASS_URL, data={"data": OVERPASS_QUERY}, timeout=180)
resp.raise_for_status()
elements = resp.json()["elements"]
print(f"Received {len(elements)} relations\n")

cred = credentials.Certificate("serviceAccountKey.json")
firebase_admin.initialize_app(cred)
db = firestore.client()

uploaded = 0
for el in elements:
    tags = el.get("tags", {})
    name_en = tags.get("name:en", "")
    lt_name = tags.get("name", "")

    # Accept only county-level relations
    if "apskritis" not in lt_name.lower() and "county" not in name_en.lower():
        continue

    # Normalise to "X County"
    if name_en:
        county_name = name_en if "County" in name_en else name_en + " County"
    else:
        # Derive from Lithuanian name: "Kauno apskritis" → "Kaunas County"
        county_name = lt_name  # fallback; ideally name:en is present

    region_id = county_name.lower().replace(" ", "-").replace("ė", "e").replace("ū", "u") \
        .replace("ą", "a").replace("č", "c").replace("š", "s").replace("ž", "z") \
        .replace("ę", "e").replace("į", "i")

    print(f"Processing: {county_name}  (id={region_id})")

    ring = merge_outer_ways(el.get("members", []))
    if len(ring) < 4:
        print(f"  ⚠ Could not build polygon, skipping\n")
        continue

    lats = [p[0] for p in ring]
    lngs = [p[1] for p in ring]
    area = polygon_area_km2(ring)
    print(f"  {len(ring)} vertices · {area:.1f} km²")

    goal = count_goal_tiles(ring)
    print(f"  goal_tiles = {goal:,}")

    boundary = [{"lat": round(p[0], 7), "lng": round(p[1], 7)} for p in ring]

    doc = {
        "id": region_id,
        "name": county_name,
        "description": DESCRIPTIONS.get(county_name, ""),
        "min_lat": round(min(lats), 7),
        "max_lat": round(max(lats), 7),
        "min_lng": round(min(lngs), 7),
        "max_lng": round(max(lngs), 7),
        "goal_tiles": goal,
        "boundary": boundary,
    }

    db.collection("regions").document(region_id).set(doc)
    print(f"  ✓ Uploaded '{region_id}'\n")
    uploaded += 1

print(f"Done. Uploaded {uploaded} counties.")
