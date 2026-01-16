# Recommender System – Project Guide

A single, practical document to understand, run, extend, and troubleshoot this project end‑to‑end.

- Tech stack: Spring Boot (Java 17), Maven, Python (offline training), CSV artifacts
- Core idea: Hybrid recommendations = Collaborative Filtering (CF) + Content features
- Inputs: Prebuilt matrices and mappings in `artifacts/`, catalog and interactions in `data/`
- Outputs: REST endpoints returning ranked item recommendations

---

## 1) What this service does

- Loads precomputed artifacts (embeddings, content vectors, ID mappings) at startup
- Serves recommendations via HTTP
  - Known users: Hybrid CF + Content scoring
  - Cold‑start users: Content‑based similarity from past interactions
  - No data: Popular items fallback

---

## 2) Repository map (what matters)

- `src/main/java/com/recommender/recommender/`
  - `RecommenderApplication.java` – Spring Boot entry point
  - `controller/`
    - `RecommendationController.java` – Recommend/similar endpoints
    - `AdditionalEndpointsController.java` – `GET /api/v1/recommendations`, `GET /api/v1/popular`
    - `HealthController.java` – Liveness/basic health
  - `service/RecommendationService.java` – Business logic; loads artifacts on startup, scoring & fallbacks
  - `model/` – DTOs (`Product`, `RecommendationResponse`)
  - `utils/MathUtils.java` – Cosine similarity, vector ops
- `src/main/resources/application.yaml` – Basic config
- `artifacts/` – Pretrained model outputs (produced by Python):
  - `mappings.json` – `user2idx`, `item2idx`, weights (hybrid mixing)
  - `user_factors.csv`, `item_factors.csv` – CF embeddings
  - `user_content.csv`, `item_content.csv` – Content vectors
- `data/` – Runtime data used by the service
  - `items.csv` – Product catalog
  - `interactions.csv` – User ↔ item interactions (for fallback / popularity)
  - `raw/` – Source datasets used by Python scripts
- Python (offline training / prep)
  - `prepare_amazon_*.py` – Data preparation utilities
  - `train_hybrid.py` – Trains/exports matrices and mappings
- Build
  - `pom.xml`, `mvnw.cmd` – Maven build wrapper (Windows)

---

## 3) How it works (high‑level)

- On startup, `RecommendationService` reads artifacts and data into in‑memory maps/matrices
- For a recommendation request:
  1) If the user is known (has vectors), compute hybrid scores: `score = w_cf*cf + w_content*content`
  2) Else if the user has history, compute content similarity for items related to their history
  3) Else return popular items (from interactions/catalog)
- Results are ranked, deduplicated, and returned as `List<RecommendationResponse>`

---

## 4) Quickstart (Windows)

Prerequisites
- Java 17, Maven (wrapper included), Git
- Optional (for training): Python 3.11 (a venv is already included as `venv311/`)

Run (dev)
```cmd
mvnw.cmd spring-boot:run
```

Build a fat JAR and run
```cmd
mvnw.cmd -DskipTests package
java -jar target\recommender-0.0.1-SNAPSHOT.jar
```

Default server port is typically 8080.

Smoke test in another terminal
```cmd
curl -s "http://localhost:8080/actuator/health"
curl -s "http://localhost:8080/api/v1/popular?limit=5"
```

---

## 5) API reference (essentials)

Base path: `/api/v1`

Quick examples:
- Popular items
  - `GET /api/v1/popular?limit=5`
  - Returns: `[{ item_id, title, brand, category, image_url, score }]`

- Recommendations for a user
  - `GET /api/v1/recommendations?user_id=U123&limit=5`
  - Returns: `[{ item_id, title, brand, category, image_url, score }]`

For complete API documentation including:
- Detailed request/response schemas
- All 5 endpoints with full examples
- Frontend integration code (JavaScript, React, TypeScript)
- Error handling and best practices
- **See [API.md](API.md) for comprehensive documentation**

Notes
- CORS is open (`@CrossOrigin("*")`) for quick testing
- Swagger UI (springdoc): `/swagger-ui/index.html`

---

## 6) Data & artifacts explained

- Dataset source: https://cseweb.ucsd.edu/~jmcauley/datasets/amazon_v2/ (Electronics 5-Core and Electronics metadata)
  - Local raw files referenced here:
    - `data/raw/Electronics_5.json.gz`
    - `data/raw/meta_Electronics.json.gz`
- `mappings.json`
  - `user2idx`, `item2idx`: string IDs → integer indices (row/col in matrices)
  - `hybrid` weights: how to mix CF and content scores
- `*_factors.csv` (CF)
  - Dense vectors trained offline (user/item latent factors)
- `*_content.csv`
  - Feature vectors derived from item/user content (e.g., text/category features)
- `items.csv`
  - Columns: id, title, brand, category, image URL, etc.
- `interactions.csv`
  - User ↔ item events (implicit or ratings); used for fallback/popularity

---

## 7) Minimal mental model (diagram)

Activity flow (compact)

[Diagram code removed]

Simple architecture (compact)

[Diagram code removed]

You can paste these in any PlantUML renderer to visualize.

---

## 8) Training pipeline (optional, offline)

The included Python environment `venv311/` contains libs for matrix factorization (implicit, numpy, pandas, sklearn).

Typical steps
1) Prepare raw data
   - Use `prepare_amazon_*` scripts to generate cleaned CSVs under `data/` and `data/raw/`
2) Train hybrid model
   - Run `train_hybrid.py` to fit CF + content and export matrices to `artifacts/`
3) Restart the Spring Boot app so it reloads updated artifacts at startup

Run with the existing venv
```cmd
venv311\Scripts\activate.bat
python train_hybrid.py
python prepare_amazon_top10k.py
```
Deactivate when done
```cmd
venv311\Scripts\deactivate.bat
```

Outputs go to `artifacts/` and `data/`.

---

## 9) Configuration

- `src/main/resources/application.yaml`
  - Server port, logging, and any custom toggles can go here
- Environment variables
  - Not strictly required; artifacts are read from the repo paths by default

---

## 10) Development tips

- Fast iteration: keep the app running with `spring-boot:run` and edit Java files; Spring DevTools can be added if hot reload is needed
- Testing endpoints
  - Browser/Swagger UI
  - `curl` from the command line (examples above)
- Data refresh
  - Replace CSVs/JSONs, then restart the app to reload

---

## 11) Troubleshooting

Common issues

- App starts but returns empty lists
  - Ensure `artifacts/*.csv` and `mappings.json` exist and match your `items.csv`/`interactions.csv`
- Port already in use (8080)
  - Change `server.port` in `application.yaml` or free the port
- Wrong Java version
  - Use Java 17 (`java -version`); build uses Spring Boot 3.5.x
- PlantUML errors while rendering the snippets
  - Make sure your file begins with `@startuml` and ends with `@enduml`; avoid mixing state/action syntaxes
- Windows path issues
  - Use `\\` in paths (e.g., `target\\recommender-0.0.1-SNAPSHOT.jar`)

If you hit something not covered here, search the code:
- Controllers: what endpoints exist and what they call
- `RecommendationService`: how artifacts are loaded and scores computed

---

## 12) Extend the system

- Add a new endpoint
  - Create a method in a controller and call into `RecommendationService`
- Adjust hybrid weights
  - Update weights in `mappings.json` and restart the app
- Add new features to content vectors
  - Retrain with `train_hybrid.py` and regenerate `item_content.csv` / `user_content.csv`

---

## 13) License & authorship

- See project `pom.xml`/headers for licensing information.

---

## 14) Quick reference commands (Windows)

```cmd
:: Run
mvnw.cmd spring-boot:run

:: Build & run JAR
mvnw.cmd -DskipTests package
java -jar target\\recommender-0.0.1-SNAPSHOT.jar

:: Call APIs
curl -s "http://localhost:8080/api/v1/popular?limit=5"
curl -s "http://localhost:8080/api/v1/recommendations?user_id=U123&limit=5"

:: Train (optional)
venv311\\Scripts\\activate.bat
python train_hybrid.py
venv311\\Scripts\\deactivate.bat
```

---

That’s it — you now have the big picture, how to run it, how to retrain and update artifacts, and how to extend the API safely.
