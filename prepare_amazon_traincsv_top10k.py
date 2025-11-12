import gzip
import pandas as pd
from tqdm import tqdm
import os
import re

RAW_DIR = "./data/raw"
OUT_DIR = "./data"
os.makedirs(OUT_DIR, exist_ok=True)

TOP_K_ITEMS = 10000

# ---------- 1. Load Electronics.train.csv.gz ----------
print("📥 Loading Electronics.train.csv.gz ...")
df = pd.read_csv(os.path.join(RAW_DIR, "Electronics.train.csv.gz"), compression='gzip')

print("✅ Loaded:", len(df), "rows")
print("Columns:", list(df.columns))

# ---------- 2. Standardize column names ----------
df.rename(columns={
    'reviewerID': 'user_id',
    'asin': 'item_id',
    'rating': 'rating',
    'unixReviewTime': 'ts',
    'category': 'category',
    'summary': 'title'
}, inplace=True)

df["brand"] = df.get("brand", "Unknown")
df["description"] = df.get("reviewText", "")
df["tags"] = df["category"].astype(str)

# ---------- 3. Clean ----------
df["ts"] = pd.to_datetime(df["ts"], unit="s", errors="coerce")
df["event_value"] = df["rating"].astype(float)
df["event_type"] = df["rating"].apply(lambda x: "purchase" if x >= 4 else "view")

# ---------- 4. Filter Top-K Items ----------
item_popularity = df.groupby("item_id").size().sort_values(ascending=False)
top_items = set(item_popularity.head(TOP_K_ITEMS).index)
df = df[df["item_id"].isin(top_items)]

print(f"📊 Filtered to top {TOP_K_ITEMS} most popular items")
print(f"Remaining interactions: {len(df)}")

# ---------- 5. Extract interactions ----------
interactions = df[["user_id", "item_id", "event_type", "event_value", "ts"]].drop_duplicates()

# ---------- 6. Extract item metadata ----------
meta_cols = ["item_id", "title", "brand", "category", "tags", "description"]
df_meta = df[meta_cols].drop_duplicates(subset=["item_id"]).copy()

# optional image placeholder (no image in new dataset)
df_meta["image_url"] = "https://via.placeholder.com/300x300?text=" + df_meta["title"].fillna("").str.replace(" ", "+")

# ---------- 7. Save ----------
interactions.to_csv(os.path.join(OUT_DIR, "interactions.csv"), index=False)
df_meta.to_csv(os.path.join(OUT_DIR, "items.csv"), index=False)

print("\n✅ Saved:")
print(f" - data/interactions.csv ({len(interactions)} rows)")
print(f" - data/items.csv ({len(df_meta)} products)")
