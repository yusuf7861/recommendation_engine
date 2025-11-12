# Recommender API Reference

Base URL: `http://localhost:8080`

All responses are JSON. CORS is open (origins = *).

Common object: RecommendationResponse
- item_id: string
- title: string
- brand: string
- category: string
- image_url: string (URL)
- score: number (relevance score; 0.0 for simple popular fallback)

Notes and behaviors
- For unknown/empty users, the service falls back to content-based or popular items and still returns 200 with a (possibly empty) array.
- For unknown itemId in "similar", the service returns 200 with an empty array.
- Default limit is 5 unless specified.

---

1) POST /api/v1/recommendations
Purpose: Get hybrid (CF + content) recommendations for a user.

Request
- Headers: `Content-Type: application/json`
- Body (JSON):
  {
    "user_id": "string",  // required
    "limit": 5             // optional (default 5)
  }

Response (200)
- Body: Array<RecommendationResponse>
- Example:
  [
    {
      "item_id": "B000WL0I1I",
      "title": "Everstone APSAMB 32-65\" LCD TV Wall Mount Bracket...",
      "brand": "Everstone",
      "category": "Electronics",
      "image_url": "https://images-na.ssl-images-amazon.com/images/I/41rd%2BMghyVL.jpg",
      "score": 0.6864113076666485
    },
    {
      "item_id": "B000VCTQZ2",
      "title": "Cheetah Mounts APTMMB TV Wall Mount Bracket...",
      "brand": "Cheetah",
      "category": "Electronics",
      "image_url": "https://images-na.ssl-images-amazon.com/images/I/51spYQZ99oL.jpg",
      "score": 0.6240004201838252
    }
  ]

---

2) GET /api/v1/recommendations
Purpose: Same as POST variant but via query parameters.

Request
- Query params:
  - user_id: string (required)
  - limit: number (optional, default 5)
- Example: `/api/v1/recommendations?user_id=userA&limit=3`

Response (200)
- Body: Array<RecommendationResponse>

---

3) GET /api/v1/items/{itemId}/similar
Purpose: Get items similar to a given item (hybrid similarity).

Request
- Path params:
  - itemId: string (required)
- Query params:
  - limit: number (optional, default 5)
- Example: `/api/v1/items/0972683275/similar?limit=3`

Response (200)
- Body: Array<RecommendationResponse>
- Unknown itemId → returns `[]` (empty array)

---

4) GET /api/v1/popular
Purpose: Get a simple popular/fallback list (currently first N items).

Request
- Query params:
  - limit: number (optional, default 5)
- Example: `/api/v1/popular?limit=3`

Response (200)
- Body: Array<RecommendationResponse> (scores may be 0.0 for this endpoint)

---

5) GET /api/v1/health
Purpose: Quick service and data sanity signal.

Request
- No parameters

Response (200)
- Body (object):
  {
    "status": "UP",
    "users": 1234,
    "items": 10000,
    "interactionsUsers": 567,
    "hybridWeights": { "cf": 0.7, "content": 0.3 }
  }
- On initialization error:
  {
    "status": "ERROR",
    "message": "error details..."
  }

---

Examples (PowerShell)
- GET similar
  powershell -NoProfile -Command "Invoke-WebRequest -UseBasicParsing 'http://localhost:8080/api/v1/items/0972683275/similar?limit=3' | Select-Object -ExpandProperty Content"

- GET recommendations
  powershell -NoProfile -Command "Invoke-WebRequest -UseBasicParsing 'http://localhost:8080/api/v1/recommendations?user_id=userA&limit=3' | Select-Object -ExpandProperty Content"

- POST recommendations
  powershell -NoProfile -Command "$b=ConvertTo-Json @{user_id='userA';limit=3}; Invoke-WebRequest -UseBasicParsing -Method Post -ContentType 'application/json' -Body $b http://localhost:8080/api/v1/recommendations | Select-Object -ExpandProperty Content"

- GET popular
  powershell -NoProfile -Command "Invoke-WebRequest -UseBasicParsing 'http://localhost:8080/api/v1/popular?limit=3' | Select-Object -ExpandProperty Content"

- GET health
  powershell -NoProfile -Command "Invoke-WebRequest -UseBasicParsing 'http://localhost:8080/api/v1/health' | Select-Object -ExpandProperty Content"

