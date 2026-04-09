import json
import math

ZOOM = 20

def lat_lng_to_tile(lat, lng, zoom):
    n = 1 << zoom
    x = int((lng + 180.0) / 360.0 * n)
    lat_rad = math.radians(lat)
    y = int((1.0 - math.log(math.tan(lat_rad) + 1.0 / math.cos(lat_rad)) / math.pi) / 2.0 * n)
    return max(0, min(n - 1, x)), max(0, min(n - 1, y))

def tile_center_lat_lng(tx, ty, zoom):
    n = 1 << zoom
    lng = tx / n * 360.0 - 180.0 + (0.5 / n * 360.0)
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

# ── Load ──────────────────────────────────────────────────────────────────────

with open('utena.json') as f:
    data = json.load(f)

feature   = data['features'][0]
props     = feature['properties']
geom_type = feature['geometry']['type']
raw_coords = feature['geometry']['coordinates']

# Handle both Polygon and MultiPolygon — pick the largest ring
if geom_type == 'Polygon':
    rings = raw_coords
elif geom_type == 'MultiPolygon':
    rings = [r[0] for r in raw_coords]  # outer ring of each part
else:
    raise ValueError(f"Unsupported geometry type: {geom_type}")

largest_ring = max(rings, key=len)

# GeoJSON is [lng, lat] — swap to [lat, lng]
polygon = [(lat, lng) for lng, lat in largest_ring]

lats = [p[0] for p in polygon]
lngs = [p[1] for p in polygon]
min_lat, max_lat = min(lats), max(lats)
min_lng, max_lng = min(lngs), max(lngs)

area_km2 = polygon_area_km2(polygon)
city_name = props.get('name', 'Unknown')

print(f"City       : {city_name}")
print(f"Polygon    : {len(polygon)} vertices")
print(f"Area       : {area_km2:.1f} km²")
print(f"Bbox       : lat [{min_lat:.6f}, {max_lat:.6f}]  lng [{min_lng:.6f}, {max_lng:.6f}]")

# ── Count tiles inside polygon ────────────────────────────────────────────────

tx_min, ty_max = lat_lng_to_tile(min_lat, min_lng, ZOOM)
tx_max, ty_min = lat_lng_to_tile(max_lat, max_lng, ZOOM)

total_bbox = (tx_max - tx_min + 1) * (ty_max - ty_min + 1)
print(f"\nScanning {total_bbox:,} tiles in bounding box (zoom {ZOOM})...")
print("This may take 2–5 minutes for a large city.\n")

goal_tiles = 0
total_rows = ty_max - ty_min + 1

for row_i, ty in enumerate(range(ty_min, ty_max + 1)):
    if row_i % 100 == 0:
        pct = row_i / total_rows * 100
        print(f"  {pct:.1f}%  ({goal_tiles:,} tiles so far)", end='\r')
    for tx in range(tx_min, tx_max + 1):
        clat, clng = tile_center_lat_lng(tx, ty, ZOOM)
        if point_in_polygon(clat, clng, polygon):
            goal_tiles += 1

print(f"\n\ngoal_tiles : {goal_tiles:,}")

# ── Build Firestore document ──────────────────────────────────────────────────

doc = {
    "id": city_name.lower(),
    "name": city_name,
    "description": "",
    "min_lat": round(min_lat, 7),
    "max_lat": round(max_lat, 7),
    "min_lng": round(min_lng, 7),
    "max_lng": round(max_lng, 7),
    "goal_tiles": goal_tiles,
    "boundary": [[round(lat, 7), round(lng, 7)] for lat, lng in polygon]
}

out_path = f"{city_name.lower()}_firestore.json"
with open(out_path, 'w') as f:
    json.dump(doc, f, indent=2)

print(f"Saved to   : {out_path}")
print("\nFirst 3 boundary points (lat, lng):")
for p in doc['boundary'][:3]:
    print(f"  {p}")
