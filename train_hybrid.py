import os, json
import pandas as pd, numpy as np
from scipy import sparse
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.preprocessing import normalize
from sklearn.decomposition import TruncatedSVD
from implicit.als import AlternatingLeastSquares
from tqdm import tqdm
from scipy.sparse import csr_matrix, vstack

# -----------------------------#
# Configuration
# -----------------------------#
os.environ["OPENBLAS_NUM_THREADS"] = "1"  # limit threadpool to avoid slowdown

DATA_DIR = "./data"
ARTIFACTS_DIR = "./artifacts"
os.makedirs(ARTIFACTS_DIR, exist_ok=True)

ALS_FACTORS, ALS_REG, ALS_ITERS, ALS_ALPHA = 64, 0.02, 20, 40
HYBRID_W_CF, HYBRID_W_CONTENT = 0.7, 0.3
TFIDF_MAX_FEATURES = 20000
SVD_K = 128

# -----------------------------#
# Load data
# -----------------------------#
print("Loading data...")
interactions = pd.read_csv(f"{DATA_DIR}/interactions.csv")
items = pd.read_csv(f"{DATA_DIR}/items.csv")

interactions["weight"] = interactions["event_value"]

# -----------------------------#
# Encode users/items
# -----------------------------#
unique_users = interactions["user_id"].astype(str).unique()
unique_items = items["item_id"].astype(str).unique()

user2idx = {u: i for i, u in enumerate(sorted(unique_users))}
item2idx = {p: i for i, p in enumerate(sorted(unique_items))}
idx2user = {i: u for u, i in user2idx.items()}
idx2item = {i: p for p, i in item2idx.items()}

# -----------------------------#
# Clean + map indices safely
# -----------------------------#
interactions["user_idx"] = interactions["user_id"].astype(str).map(user2idx)
interactions["item_idx"] = interactions["item_id"].astype(str).map(item2idx)

before = len(interactions)
interactions = interactions.dropna(subset=["user_idx", "item_idx"]).copy()
after = len(interactions)
print(f"Filtered invalid mappings: dropped {before - after} rows ({before - after:.2f} / {before} total)")

rows = interactions["user_idx"].astype(int).values
cols = interactions["item_idx"].astype(int).values
vals = interactions["weight"].astype(float).values

UI = sparse.coo_matrix((vals, (rows, cols)), shape=(len(user2idx), len(item2idx))).tocsr()
IU = UI.T.tocsr()
print(f"Final matrix shape: users={UI.shape[0]}, items={UI.shape[1]}, nnz={UI.nnz}")

# -----------------------------#
# Train ALS (Collaborative)
# -----------------------------#
print("Training ALS collaborative model...")
als = AlternatingLeastSquares(factors=ALS_FACTORS, regularization=ALS_REG, iterations=ALS_ITERS)
als.fit((IU * ALS_ALPHA).astype("double"))

item_factors = normalize(als.item_factors)
user_factors = normalize(als.user_factors)
print("ALS model trained.")

# -----------------------------#
# TF-IDF Content Embeddings
# -----------------------------#
expected_fields = ["title", "brand", "category", "tags", "description"]
text_fields = [f for f in expected_fields if f in items.columns]

if not text_fields:
    raise ValueError("No valid text fields found in items.csv!")

items[text_fields] = items[text_fields].fillna("")
corpus = items[text_fields].apply(lambda x: " ".join(x.astype(str)), axis=1)

print(f"Building TF-IDF features from: {', '.join(text_fields)} ({len(corpus):,} items)")
vectorizer = TfidfVectorizer(max_features=TFIDF_MAX_FEATURES, ngram_range=(1, 2))
X_content = normalize(vectorizer.fit_transform(corpus))
print(f"TF-IDF shape: {X_content.shape}")

# -----------------------------#
# Sparse User Profile Building (memory-safe)
# -----------------------------#
print("Building user content profiles (sparse averaging)...")
user_profiles = []
for uid, grp in tqdm(interactions.groupby("user_id"), total=len(interactions["user_id"].unique())):
    uidx = user2idx.get(uid)
    item_idxs = grp["item_id"].map(item2idx).dropna().astype(int).values
    if len(item_idxs) == 0:
        user_profiles.append(csr_matrix((1, X_content.shape[1])))
        continue
    vec = X_content[item_idxs].sum(axis=0)
    vec = vec / len(item_idxs)
    user_profiles.append(csr_matrix(vec))

user_profiles = vstack(user_profiles)
user_profiles = normalize(user_profiles)
print(f"User content profiles built: shape={user_profiles.shape}")

# -----------------------------#
# Dimensionality Reduction
# -----------------------------#
print(f"Applying TruncatedSVD (k={SVD_K})...")
svd = TruncatedSVD(n_components=SVD_K, random_state=42)
item_content_proj = normalize(svd.fit_transform(X_content))
user_content_proj = normalize(user_profiles @ svd.components_.T)
print("SVD projection complete.")

# -----------------------------#
# Save artifacts
# -----------------------------#
np.savetxt(f"{ARTIFACTS_DIR}/user_factors.csv", user_factors, delimiter=",", fmt="%.6f")
np.savetxt(f"{ARTIFACTS_DIR}/item_factors.csv", item_factors, delimiter=",", fmt="%.6f")
np.savetxt(f"{ARTIFACTS_DIR}/user_content.csv", user_content_proj, delimiter=",", fmt="%.6f")
np.savetxt(f"{ARTIFACTS_DIR}/item_content.csv", item_content_proj, delimiter=",", fmt="%.6f")

with open(f"{ARTIFACTS_DIR}/mappings.json", "w") as f:
    json.dump({
        "user2idx": user2idx,
        "item2idx": item2idx,
        "idx2user": idx2user,
        "idx2item": idx2item,
        "hybrid_w_cf": HYBRID_W_CF,
        "hybrid_w_content": HYBRID_W_CONTENT
    }, f)

print("\nArtifacts saved to ./artifacts/")
print("Training complete — model ready for deployment!")
