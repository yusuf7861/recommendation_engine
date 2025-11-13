# Recommender

Spring Boot + Java service that serves hybrid (CF + content) recommendations over REST.

Key endpoints (see API.md for full details):
- POST /api/v1/recommendations
- GET  /api/v1/recommendations
- GET  /api/v1/items/{itemId}/similar
- GET  /api/v1/popular
- GET  /api/v1/health

## Quick start

Prereqs: Java 17+, Maven (or use included Maven wrapper).

Build:
```bat
mvnw.cmd -DskipTests package
```
Run:
```bat
java -jar target\recommender-0.0.1-SNAPSHOT.jar
```

Test (PowerShell examples):
```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8080/api/v1/health | Select-Object -ExpandProperty Content
Invoke-WebRequest -UseBasicParsing 'http://localhost:8080/api/v1/popular?limit=3' | Select-Object -ExpandProperty Content
```

## Data & artifacts
- Place model artifacts under `artifacts/` (user_factors.csv, item_factors.csv, user_content.csv, item_content.csv, mappings.json)
- Items file under `data/items.csv`
- Optional interactions under `data/interactions.csv`

## Dataset
- Source: https://cseweb.ucsd.edu/~jmcauley/datasets/amazon_v2/
- Used subsets: Electronics 5-Core and Electronics metadata
- Local raw files (already referenced in this repo):
  - `data/raw/Electronics_5.json.gz`
  - `data/raw/meta_Electronics.json.gz`
- Prep/ETL scripts: `prepare_amazon_*.py` and `train_hybrid.py`

## Notes
- See `API.md` for schema and examples.
- See `src/main/resources/application.yaml` for config (port, names, etc.).

## 📋 Code Review Available
A comprehensive code review has been completed for this project:
- **[REVIEW_INDEX.md](REVIEW_INDEX.md)** - Start here! Navigation guide for all review documents
- **[REVIEW_SUMMARY.md](REVIEW_SUMMARY.md)** - Executive summary (Rating: ⭐⭐⭐⭐ 4/5)
- **[CODE_REVIEW.md](CODE_REVIEW.md)** - Detailed technical analysis (24 findings)
- **[IMPROVEMENT_PLAN.md](IMPROVEMENT_PLAN.md)** - Step-by-step improvement roadmap

The review identifies critical security issues, provides actionable fixes, and includes a 3-week implementation plan.
