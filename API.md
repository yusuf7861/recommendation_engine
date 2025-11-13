# Recommender API Reference

**Base URL:** `http://localhost:8080`

**Version:** v1

**Content-Type:** `application/json`

**CORS:** Enabled for all origins (`*`)

---

## Table of Contents

1. [Common Data Types](#common-data-types)
2. [Authentication](#authentication)
3. [Error Handling](#error-handling)
4. [API Endpoints](#api-endpoints)
   - [Get User Recommendations (POST)](#1-post-apiv1recommendations)
   - [Get User Recommendations (GET)](#2-get-apiv1recommendations)
   - [Get Similar Items](#3-get-apiv1itemsitemidsimilar)
   - [Get Popular Items](#4-get-apiv1popular)
   - [Health Check](#5-get-apiv1health)
5. [Frontend Integration Examples](#frontend-integration-examples)

---

## Common Data Types

### RecommendationResponse

Represents a single recommendation item returned by the API.

| Field | Type | Description |
|-------|------|-------------|
| `item_id` | string | Unique identifier for the item |
| `title` | string | Item title/name |
| `brand` | string | Brand name of the item |
| `category` | string | Item category (e.g., "Electronics") |
| `image_url` | string | Full URL to the item image |
| `score` | number | Relevance/similarity score (0.0 to 1.0). Returns 0.0 for popular items fallback |

**Example:**
```json
{
  "item_id": "B000WL0I1I",
  "title": "Everstone APSAMB 32-65\" LCD TV Wall Mount Bracket",
  "brand": "Everstone",
  "category": "Electronics",
  "image_url": "https://images-na.ssl-images-amazon.com/images/I/41rd%2BMghyVL.jpg",
  "score": 0.6864113076666485
}
```

---

## Authentication

Currently, the API does not require authentication. All endpoints are publicly accessible.

---

## Error Handling

### HTTP Status Codes

| Status Code | Description |
|-------------|-------------|
| 200 OK | Request successful |
| 400 Bad Request | Invalid request parameters |
| 404 Not Found | Endpoint not found |
| 500 Internal Server Error | Server error occurred |

### Error Response Format

When an error occurs, the API may return an error response:

```json
{
  "timestamp": "2025-11-13T11:04:15.223Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Error description",
  "path": "/api/v1/recommendations"
}
```

### Behavioral Notes

- For unknown/empty users, the service falls back to content-based or popular items and returns **200 OK** with a (possibly empty) array
- For unknown `itemId` in "similar" endpoint, the service returns **200 OK** with an empty array `[]`
- Default limit is **5** unless specified
- Invalid or negative limit values will use the default value

---

## API Endpoints

---

### 1) POST /api/v1/recommendations

Get personalized hybrid (Collaborative Filtering + Content-based) recommendations for a user.

**Endpoint:** `POST /api/v1/recommendations`

**Request Headers:**
```
Content-Type: application/json
```

**Request Body:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `user_id` | string | Yes | - | Unique identifier for the user |
| `limit` | integer | No | 5 | Maximum number of recommendations to return (1-100) |

**Request Example:**
```json
{
  "user_id": "A3SGXH7AUHU8GW",
  "limit": 5
}
```

**Response:**

- **Status Code:** 200 OK
- **Content-Type:** application/json
- **Body:** Array of `RecommendationResponse` objects

**Success Response Example:**
```json
[
  {
    "item_id": "B000WL0I1I",
    "title": "Everstone APSAMB 32-65\" LCD TV Wall Mount Bracket",
    "brand": "Everstone",
    "category": "Electronics",
    "image_url": "https://images-na.ssl-images-amazon.com/images/I/41rd%2BMghyVL.jpg",
    "score": 0.6864113076666485
  },
  {
    "item_id": "B000VCTQZ2",
    "title": "Cheetah Mounts APTMMB TV Wall Mount Bracket",
    "brand": "Cheetah",
    "category": "Electronics",
    "image_url": "https://images-na.ssl-images-amazon.com/images/I/51spYQZ99oL.jpg",
    "score": 0.6240004201838252
  },
  {
    "item_id": "B001234567",
    "title": "Sample Product Title",
    "brand": "Sample Brand",
    "category": "Electronics",
    "image_url": "https://images-na.ssl-images-amazon.com/images/I/sample.jpg",
    "score": 0.5123456789012345
  }
]
```

**Empty Result Example (Unknown User):**
```json
[]
```

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/v1/recommendations \
  -H "Content-Type: application/json" \
  -d '{"user_id": "A3SGXH7AUHU8GW", "limit": 5}'
```

---

### 2) GET /api/v1/recommendations

Get personalized recommendations via query parameters. Same functionality as POST variant, but more convenient for simple requests.

**Endpoint:** `GET /api/v1/recommendations`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `user_id` | string | Yes | - | Unique identifier for the user |
| `limit` | integer | No | 5 | Maximum number of recommendations to return (1-100) |

**Request Example:**
```
GET /api/v1/recommendations?user_id=A3SGXH7AUHU8GW&limit=3
```

**Response:**

- **Status Code:** 200 OK
- **Content-Type:** application/json
- **Body:** Array of `RecommendationResponse` objects

**Success Response Example:**
```json
[
  {
    "item_id": "B000WL0I1I",
    "title": "Everstone APSAMB 32-65\" LCD TV Wall Mount Bracket",
    "brand": "Everstone",
    "category": "Electronics",
    "image_url": "https://images-na.ssl-images-amazon.com/images/I/41rd%2BMghyVL.jpg",
    "score": 0.6864113076666485
  },
  {
    "item_id": "B000VCTQZ2",
    "title": "Cheetah Mounts APTMMB TV Wall Mount Bracket",
    "brand": "Cheetah",
    "category": "Electronics",
    "image_url": "https://images-na.ssl-images-amazon.com/images/I/51spYQZ99oL.jpg",
    "score": 0.6240004201838252
  },
  {
    "item_id": "B001234567",
    "title": "Sample Product Title",
    "brand": "Sample Brand",
    "category": "Electronics",
    "image_url": "https://images-na.ssl-images-amazon.com/images/I/sample.jpg",
    "score": 0.5123456789012345
  }
]
```

**cURL Example:**
```bash
curl -X GET "http://localhost:8080/api/v1/recommendations?user_id=A3SGXH7AUHU8GW&limit=3"
```

---

### 3) GET /api/v1/items/{itemId}/similar

Get items similar to a given item using hybrid similarity (collaborative filtering + content-based).

**Endpoint:** `GET /api/v1/items/{itemId}/similar`

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `itemId` | string | Yes | Unique identifier of the item to find similar items for |

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `limit` | integer | No | 5 | Maximum number of similar items to return (1-100) |

**Request Example:**
```
GET /api/v1/items/B000WL0I1I/similar?limit=3
```

**Response:**

- **Status Code:** 200 OK
- **Content-Type:** application/json
- **Body:** Array of `RecommendationResponse` objects

**Success Response Example:**
```json
[
  {
    "item_id": "B000VCTQZ2",
    "title": "Cheetah Mounts APTMMB TV Wall Mount Bracket",
    "brand": "Cheetah",
    "category": "Electronics",
    "image_url": "https://images-na.ssl-images-amazon.com/images/I/51spYQZ99oL.jpg",
    "score": 0.8745623401838252
  },
  {
    "item_id": "B001234567",
    "title": "VideoSecu ML531BE TV Wall Mount",
    "brand": "VideoSecu",
    "category": "Electronics",
    "image_url": "https://images-na.ssl-images-amazon.com/images/I/sample2.jpg",
    "score": 0.8234567890123456
  },
  {
    "item_id": "B009876543",
    "title": "Mount-It! TV Wall Mount Bracket",
    "brand": "Mount-It!",
    "category": "Electronics",
    "image_url": "https://images-na.ssl-images-amazon.com/images/I/sample3.jpg",
    "score": 0.7567890123456789
  }
]
```

**Empty Result Example (Unknown Item):**
```json
[]
```

**Note:** If the `itemId` does not exist in the system, the API returns an empty array `[]` with status 200 OK.

**cURL Example:**
```bash
curl -X GET "http://localhost:8080/api/v1/items/B000WL0I1I/similar?limit=3"
```

---

### 4) GET /api/v1/popular

Get popular items as a fallback recommendation list. Useful for new users or when personalized recommendations are not available.

**Endpoint:** `GET /api/v1/popular`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `limit` | integer | No | 5 | Maximum number of popular items to return (1-100) |

**Request Example:**
```
GET /api/v1/popular?limit=3
```

**Response:**

- **Status Code:** 200 OK
- **Content-Type:** application/json
- **Body:** Array of `RecommendationResponse` objects

**Success Response Example:**
```json
[
  {
    "item_id": "B000WL0I1I",
    "title": "Everstone APSAMB 32-65\" LCD TV Wall Mount Bracket",
    "brand": "Everstone",
    "category": "Electronics",
    "image_url": "https://images-na.ssl-images-amazon.com/images/I/41rd%2BMghyVL.jpg",
    "score": 0.0
  },
  {
    "item_id": "B000VCTQZ2",
    "title": "Cheetah Mounts APTMMB TV Wall Mount Bracket",
    "brand": "Cheetah",
    "category": "Electronics",
    "image_url": "https://images-na.ssl-images-amazon.com/images/I/51spYQZ99oL.jpg",
    "score": 0.0
  },
  {
    "item_id": "B001234567",
    "title": "Sample Product Title",
    "brand": "Sample Brand",
    "category": "Electronics",
    "image_url": "https://images-na.ssl-images-amazon.com/images/I/sample.jpg",
    "score": 0.0
  }
]
```

**Note:** The `score` field for popular items is typically `0.0` as these are not personalized recommendations.

**cURL Example:**
```bash
curl -X GET "http://localhost:8080/api/v1/popular?limit=3"
```

---

### 5) GET /api/v1/health

Health check endpoint to verify service status and get system statistics.

**Endpoint:** `GET /api/v1/health`

**Request Parameters:** None

**Request Example:**
```
GET /api/v1/health
```

**Response:**

- **Status Code:** 200 OK
- **Content-Type:** application/json
- **Body:** Health status object

**Success Response Example:**
```json
{
  "status": "UP",
  "users": 1234,
  "items": 10000,
  "interactionsUsers": 567,
  "hybridWeights": {
    "cf": 0.7,
    "content": 0.3
  }
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `status` | string | Service status: "UP" or "ERROR" |
| `users` | integer | Total number of users in the system |
| `items` | integer | Total number of items in the catalog |
| `interactionsUsers` | integer | Number of users with recorded interactions |
| `hybridWeights` | object | Weights used for hybrid recommendations |
| `hybridWeights.cf` | number | Weight for collaborative filtering (0.0-1.0) |
| `hybridWeights.content` | number | Weight for content-based filtering (0.0-1.0) |

**Error Response Example:**
```json
{
  "status": "ERROR",
  "message": "Failed to load model artifacts: File not found"
}
```

**Note:** Even if the service returns an error status, the HTTP status code is still 200 OK. Check the `status` field in the response body.

**cURL Example:**
```bash
curl -X GET "http://localhost:8080/api/v1/health"
```

---

## Frontend Integration Examples

Below are code examples for integrating the Recommendation API into frontend applications.

### JavaScript (Fetch API)

#### Get User Recommendations (POST)
```javascript
async function getUserRecommendations(userId, limit = 5) {
  try {
    const response = await fetch('http://localhost:8080/api/v1/recommendations', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        user_id: userId,
        limit: limit
      })
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const recommendations = await response.json();
    return recommendations;
  } catch (error) {
    console.error('Error fetching recommendations:', error);
    throw error;
  }
}

// Usage
getUserRecommendations('A3SGXH7AUHU8GW', 5)
  .then(recommendations => {
    console.log('Recommendations:', recommendations);
    // Display recommendations in UI
  })
  .catch(error => {
    console.error('Failed to load recommendations:', error);
  });
```

#### Get User Recommendations (GET)
```javascript
async function getUserRecommendationsGET(userId, limit = 5) {
  try {
    const url = new URL('http://localhost:8080/api/v1/recommendations');
    url.searchParams.append('user_id', userId);
    url.searchParams.append('limit', limit);

    const response = await fetch(url);

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const recommendations = await response.json();
    return recommendations;
  } catch (error) {
    console.error('Error fetching recommendations:', error);
    throw error;
  }
}

// Usage
getUserRecommendationsGET('A3SGXH7AUHU8GW', 5)
  .then(recommendations => console.log(recommendations));
```

#### Get Similar Items
```javascript
async function getSimilarItems(itemId, limit = 5) {
  try {
    const url = new URL(`http://localhost:8080/api/v1/items/${itemId}/similar`);
    url.searchParams.append('limit', limit);

    const response = await fetch(url);

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const similarItems = await response.json();
    return similarItems;
  } catch (error) {
    console.error('Error fetching similar items:', error);
    throw error;
  }
}

// Usage
getSimilarItems('B000WL0I1I', 3)
  .then(items => console.log('Similar items:', items));
```

#### Get Popular Items
```javascript
async function getPopularItems(limit = 5) {
  try {
    const url = new URL('http://localhost:8080/api/v1/popular');
    url.searchParams.append('limit', limit);

    const response = await fetch(url);

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const popularItems = await response.json();
    return popularItems;
  } catch (error) {
    console.error('Error fetching popular items:', error);
    throw error;
  }
}

// Usage
getPopularItems(5)
  .then(items => console.log('Popular items:', items));
```

#### Health Check
```javascript
async function checkHealth() {
  try {
    const response = await fetch('http://localhost:8080/api/v1/health');

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const health = await response.json();
    return health;
  } catch (error) {
    console.error('Error checking health:', error);
    throw error;
  }
}

// Usage
checkHealth()
  .then(health => {
    if (health.status === 'UP') {
      console.log('Service is healthy');
      console.log(`Users: ${health.users}, Items: ${health.items}`);
    } else {
      console.error('Service error:', health.message);
    }
  });
```

---

### JavaScript (Axios)

First, install axios:
```bash
npm install axios
```

#### Get User Recommendations (POST)
```javascript
import axios from 'axios';

async function getUserRecommendations(userId, limit = 5) {
  try {
    const response = await axios.post('http://localhost:8080/api/v1/recommendations', {
      user_id: userId,
      limit: limit
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching recommendations:', error.response?.data || error.message);
    throw error;
  }
}

// Usage
getUserRecommendations('A3SGXH7AUHU8GW', 5)
  .then(recommendations => console.log(recommendations));
```

#### Get Similar Items
```javascript
import axios from 'axios';

async function getSimilarItems(itemId, limit = 5) {
  try {
    const response = await axios.get(`http://localhost:8080/api/v1/items/${itemId}/similar`, {
      params: { limit }
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching similar items:', error.response?.data || error.message);
    throw error;
  }
}

// Usage
getSimilarItems('B000WL0I1I', 3)
  .then(items => console.log('Similar items:', items));
```

---

### React Hook Example

```javascript
import { useState, useEffect } from 'react';
import axios from 'axios';

function useRecommendations(userId, limit = 5) {
  const [recommendations, setRecommendations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!userId) {
      setLoading(false);
      return;
    }

    const fetchRecommendations = async () => {
      try {
        setLoading(true);
        const response = await axios.post('http://localhost:8080/api/v1/recommendations', {
          user_id: userId,
          limit: limit
        });
        setRecommendations(response.data);
        setError(null);
      } catch (err) {
        setError(err.message);
        setRecommendations([]);
      } finally {
        setLoading(false);
      }
    };

    fetchRecommendations();
  }, [userId, limit]);

  return { recommendations, loading, error };
}

// Usage in a component
function RecommendationsComponent({ userId }) {
  const { recommendations, loading, error } = useRecommendations(userId, 5);

  if (loading) return <div>Loading recommendations...</div>;
  if (error) return <div>Error: {error}</div>;

  return (
    <div>
      <h2>Recommended for you</h2>
      <div className="recommendations-grid">
        {recommendations.map(item => (
          <div key={item.item_id} className="recommendation-card">
            <img src={item.image_url} alt={item.title} />
            <h3>{item.title}</h3>
            <p>{item.brand}</p>
            <p>Score: {item.score.toFixed(2)}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
```

---

### TypeScript Types

```typescript
// Type definitions for TypeScript projects
export interface RecommendationResponse {
  item_id: string;
  title: string;
  brand: string;
  category: string;
  image_url: string;
  score: number;
}

export interface HealthResponse {
  status: 'UP' | 'ERROR';
  users?: number;
  items?: number;
  interactionsUsers?: number;
  hybridWeights?: {
    cf: number;
    content: number;
  };
  message?: string;
}

export interface RecommendationRequest {
  user_id: string;
  limit?: number;
}

// Example API service class
export class RecommendationService {
  private baseUrl: string;

  constructor(baseUrl: string = 'http://localhost:8080') {
    this.baseUrl = baseUrl;
  }

  async getUserRecommendations(
    userId: string,
    limit: number = 5
  ): Promise<RecommendationResponse[]> {
    const response = await fetch(`${this.baseUrl}/api/v1/recommendations`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ user_id: userId, limit }),
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return response.json();
  }

  async getSimilarItems(
    itemId: string,
    limit: number = 5
  ): Promise<RecommendationResponse[]> {
    const url = new URL(`${this.baseUrl}/api/v1/items/${itemId}/similar`);
    url.searchParams.append('limit', limit.toString());

    const response = await fetch(url);

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return response.json();
  }

  async getPopularItems(limit: number = 5): Promise<RecommendationResponse[]> {
    const url = new URL(`${this.baseUrl}/api/v1/popular`);
    url.searchParams.append('limit', limit.toString());

    const response = await fetch(url);

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return response.json();
  }

  async checkHealth(): Promise<HealthResponse> {
    const response = await fetch(`${this.baseUrl}/api/v1/health`);

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return response.json();
  }
}
```

---

### PowerShell Examples

#### GET Similar Items
```powershell
powershell -NoProfile -Command "Invoke-WebRequest -UseBasicParsing 'http://localhost:8080/api/v1/items/B000WL0I1I/similar?limit=3' | Select-Object -ExpandProperty Content"
```

#### GET Recommendations
```powershell
powershell -NoProfile -Command "Invoke-WebRequest -UseBasicParsing 'http://localhost:8080/api/v1/recommendations?user_id=A3SGXH7AUHU8GW&limit=3' | Select-Object -ExpandProperty Content"
```

#### POST Recommendations
```powershell
powershell -NoProfile -Command "$b=ConvertTo-Json @{user_id='A3SGXH7AUHU8GW';limit=3}; Invoke-WebRequest -UseBasicParsing -Method Post -ContentType 'application/json' -Body $b http://localhost:8080/api/v1/recommendations | Select-Object -ExpandProperty Content"
```

#### GET Popular Items
```powershell
powershell -NoProfile -Command "Invoke-WebRequest -UseBasicParsing 'http://localhost:8080/api/v1/popular?limit=3' | Select-Object -ExpandProperty Content"
```

#### GET Health
```powershell
powershell -NoProfile -Command "Invoke-WebRequest -UseBasicParsing 'http://localhost:8080/api/v1/health' | Select-Object -ExpandProperty Content"
```

---

## Additional Notes

### CORS Configuration
The API has CORS enabled for all origins (`*`). This allows frontend applications running on any domain to make requests to the API. In production, consider restricting this to specific domains.

### Rate Limiting
Currently, there is no rate limiting implemented. Consider implementing rate limiting in production environments.

### Caching
Consider implementing client-side caching for:
- Popular items (can be cached for longer periods)
- Similar items (relatively stable)
- Health check responses

### Error Handling Best Practices
1. Always check the HTTP status code
2. For health endpoint, check both HTTP status and `status` field in response
3. Handle empty results gracefully (empty user history, unknown items)
4. Implement retry logic for network failures
5. Show fallback content (popular items) when personalized recommendations fail

### Performance Tips
1. Use appropriate `limit` values to avoid over-fetching
2. Implement pagination if displaying many items
3. Use GET endpoints for simple requests (easier caching)
4. Batch requests when possible
5. Consider implementing a debounce mechanism for user-triggered requests

