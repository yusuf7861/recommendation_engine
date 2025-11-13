import gzip
import json
import pandas as pd
from tqdm import tqdm
import os
import re

RAW_DIR = "./data/raw"
OUT_DIR = "./data"
os.makedirs(OUT_DIR, exist_ok=True)

TOP_K_ITEMS = 10_000

print("Loading reviews...")
reviews = []
with gzip.open(f"{RAW_DIR}/Electronics_5.json.gz", "rb") as f:
    for line in tqdm(f):
        reviews.append(json.loads(line))

df_reviews = pd.DataFrame(reviews)[["reviewerID", "asin", "overall", "unixReviewTime"]]
df_reviews.columns = ["user_id", "item_id", "rating", "ts"]

df_reviews["event_type"] = df_reviews["rating"].apply(lambda x: "purchase" if x >= 4 else "view")
df_reviews["event_value"] = df_reviews["rating"].astype(float)
df_reviews["ts"] = pd.to_datetime(df_reviews["ts"], unit="s")

item_popularity = df_reviews.groupby("item_id").size().sort_values(ascending=False)
top_items = set(item_popularity.head(TOP_K_ITEMS).index)
df_reviews = df_reviews[df_reviews["item_id"].isin(top_items)]

print(f"Filtered to top {TOP_K_ITEMS} items.")
print(f"Remaining interactions: {len(df_reviews):,}")

print("Loading metadata...")
meta = []
with gzip.open(f"{RAW_DIR}/meta_Electronics.json.gz", "rb") as f:
    for line in tqdm(f):
        meta.append(json.loads(line))

df_meta = pd.DataFrame(meta)[
    ["asin", "title", "brand", "category", "description", "imageURLHighRes"]
]
df_meta.columns = ["item_id", "title", "brand", "category", "description", "image_url"]

def flatten_category(cat):
    if isinstance(cat, list):
        try:
            if isinstance(cat[0], list):
                cat = cat[-1]
            return " > ".join(map(str, cat))
        except Exception:
            return " > ".join(map(str, cat))
    return str(cat) if cat else ""

def clean_image(images):
    if isinstance(images, list) and len(images) > 0:
        return images[0]
    elif isinstance(images, str) and images.startswith("http"):
        return images
    return ""

df_meta["category"] = df_meta["category"].apply(flatten_category)
df_meta["title"] = df_meta["title"].fillna("").apply(lambda x: re.sub(r"[\n\r\t]", " ", x))
df_meta["brand"] = df_meta["brand"].fillna("Unknown").astype(str)
df_meta["description"] = df_meta["description"].fillna("").astype(str)
df_meta["image_url"] = df_meta["image_url"].apply(clean_image)
df_meta["tags"] = df_meta["brand"] + " " + df_meta["category"]

df_meta = df_meta[df_meta["item_id"].isin(top_items)]

df_reviews.drop_duplicates(subset=["user_id", "item_id"], inplace=True)
df_reviews.to_csv(f"{OUT_DIR}/interactions.csv", index=False)
df_meta.to_csv(f"{OUT_DIR}/items.csv", index=False)

print("\nDone!")
print(f" - interactions.csv ({len(df_reviews):,} rows)")
print(f" - items.csv ({len(df_meta):,} items)")
