import json
import firebase_admin
from firebase_admin import credentials, firestore

# Path to your downloaded service account key
cred = credentials.Certificate("serviceAccountKey.json")
firebase_admin.initialize_app(cred)
db = firestore.client()

with open("utena county_firestore.json") as f:
    doc = json.load(f)

region_id = doc["id"]

# Firestore doesn't allow nested arrays — convert [[lat,lng],...] to [{"lat":...,"lng":...},...]
doc["boundary"] = [{"lat": p[0], "lng": p[1]} for p in doc["boundary"]]

db.collection("regions").document(region_id).set(doc, merge=True)
print(f"Uploaded '{region_id}' with {len(doc['boundary'])} boundary points and {doc['goal_tiles']:,} goal_tiles")
