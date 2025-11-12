import gzip
import json
import pandas as pd
import os
import re
from tqdm import tqdm

RAW_DIR = "./data/raw"
OUT_DIR = "./data"
os.makedirs(OUT_DIR, exist_ok=True)

TOP_K_ITEMS = 10000

# ---------- 1️⃣ Load Reviews ----------
print("📥 Loading Electronics_5.json.gz ...")
reviews = []
with gzip.open(os.path.join(RAW_DIR, "Electronics_5.json.gz"), "rb") as f:
    for line in tqdm(f):
        d = json.loads(line)
        reviews.append({
            "user_id": d["reviewerID"],
            "item_id": d["asin"],
            "rating": float(d["overall"]),
            "ts": int(d["unixReviewTime"])
        })

df_reviews = pd.DataFrame(reviews)
df_reviews["event_type"] = df_reviews["rating"].apply(lambda x: "purchase" if x >= 4 else "view")
df_reviews["event_value"] = df_reviews["rating"]
df_reviews["ts"] = pd.to_datetime(df_reviews["ts"], unit="s")

print("✅ Reviews loaded:", len(df_reviews))

# ---------- 2️⃣ Load Metadata ----------
print("📦 Loading meta_Electronics.json.gz ...")
meta = []
with gzip.open(os.path.join(RAW_DIR, "meta_Electronics.json.gz"), "rb") as f:
    for line in tqdm(f):
        m = json.loads(line)
        meta.append({
            "item_id": m.get("asin", ""),
            "title": m.get("title", ""),
            "brand": m.get("brand", "Unknown"),
            "category": " > ".join(m.get("categories", [["Electronics"]])[-1]),
            "description": m.get("description", ""),
            "image_url": m.get("imageURLHighRes", m.get("imUrl", ""))
        })

df_meta = pd.DataFrame(meta)
df_meta.fillna("", inplace=True)
print("✅ Metadata loaded:", len(df_meta))

# ---------- 3️⃣ Filter Top-K Popular Items ----------
popularity = df_reviews.groupby("item_id").size().sort_values(ascending=False)
top_items = set(popularity.head(TOP_K_ITEMS).index)

df_reviews = df_reviews[df_reviews["item_id"].isin(top_items)]
df_meta = df_meta[df_meta["item_id"].isin(top_items)]

print(f"📊 Filtered to top {TOP_K_ITEMS} items, {len(df_reviews)} interactions left.")

# ---------- 4️⃣ Merge ----------
df_merged = df_reviews.merge(df_meta, on="item_id", how="left")
df_merged["brand"].replace("", "Unknown", inplace=True)
df_merged["title"].replace("", "Untitled Product", inplace=True)

# ---------- 5️⃣ Export ----------
interactions = df_merged[["user_id", "item_id", "event_type", "event_value", "ts"]].drop_duplicates()
items = df_meta.drop_duplicates(subset=["item_id"])

# fill missing image URLs
items["image_url"] = items["image_url"].apply(
    lambda x: x[0] if isinstance(x, list) and len(x) > 0 else
    ("https://via.placeholder.com/300x300?text=" + re.sub(r'\s+', '+', str(items.get('title', 'Item'))))
)

# save
interactions.to_csv(os.path.join(OUT_DIR, "interactions.csv"), index=False)
items.to_csv(os.path.join(OUT_DIR, "items.csv"), index=False)

print("\n✅ Saved:")
print(f" - data/interactions.csv ({len(interactions):,} rows)")
print(f" - data/items.csv ({len(items):,} items)")
