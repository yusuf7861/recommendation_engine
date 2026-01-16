# Improvement Plan: Recommendation Engine

**Date:** November 13, 2025  
**Based on:** Comprehensive Code Review

---

## Quick Wins (1-2 Hours Each)

### 1. Fix CORS Configuration
**Current:**
```java
@CrossOrigin(origins = "*")
```

**Improved:**
```java
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:3000}")
```

**File:** All controller classes  
**Priority:** 🔴 Critical

---

### 2. Add Input Validation
**Add to Controllers:**
```java
import jakarta.validation.constraints.*;

@RequestParam @NotBlank @Size(min=1, max=100) String userId,
@RequestParam @Min(1) @Max(100) int limit
```

**Add to pom.xml:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

**Priority:** 🔴 Critical

---

### 3. Replace System.out with SLF4J Logger
**Current:**
```java
System.out.println("✅ Loaded mappings...");
```

**Improved:**
```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RecommendationService {
    // ...
    log.info("Loaded mappings: users={}, items={}", user2idx.size(), item2idx.size());
}
```

**Files:** All service and controller classes  
**Priority:** 🟡 High

---

### 4. Use Configuration Properties
**Create:**
```java
@ConfigurationProperties(prefix = "reco")
@Component
public class RecommendationProperties {
    private String artifactsDir = "./artifacts";
    private String dataDir = "./data";
    private Hybrid hybrid = new Hybrid();
    private int defaultLimit = 5;
    
    @Data
    public static class Hybrid {
        private double wcf = 0.7;
        private double wContent = 0.3;
    }
    // getters and setters
}
```

**Inject into Service:**
```java
@Autowired
private RecommendationProperties properties;
```

**Priority:** 🟡 High

---

## Medium Improvements (Half Day Each)

### 5. Add Test Profile Configuration

**Create:** `src/test/resources/application-test.yaml`
```yaml
server:
  port: 0

reco:
  artifactsDir: src/test/resources/fixtures/artifacts
  dataDir: src/test/resources/fixtures/data
```

**Create Test Fixtures:**
- Minimal `mappings.json` with 5 users, 5 items
- Small matrices (5x3 dimensions)
- Sample `items.csv` with 5 products
- Sample `interactions.csv` with 10 interactions

**Update Test:**
```java
@SpringBootTest
@ActiveProfiles("test")
class RecommenderApplicationTests {
    @Test
    void contextLoads() {
        // Now passes!
    }
}
```

**Priority:** 🔴 Critical

---

### 6. Add Unit Tests for MathUtils

**Create:** `src/test/java/com/recommender/recommender/utils/MathUtilsTest.java`
```java
@Test
void cosine_identicalVectors_returns1() {
    double[] v = {1, 2, 3};
    assertEquals(1.0, MathUtils.cosine(v, v), 0.0001);
}

@Test
void cosine_orthogonalVectors_returns0() {
    double[] a = {1, 0, 0};
    double[] b = {0, 1, 0};
    assertEquals(0.0, MathUtils.cosine(a, b), 0.0001);
}

@Test
void cosine_nullVectors_returns0() {
    assertEquals(0.0, MathUtils.cosine(null, null));
}
```

**Priority:** 🟡 High

---

### 7. Add Health Check Enhancements

**Add Spring Boot Actuator:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**Configure:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
```

**Priority:** 🟡 High

---

### 8. Add Exception Handling

**Create:** `GlobalExceptionHandler.java`
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity
            .badRequest()
            .body(new ErrorResponse("Invalid input: " + e.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("An error occurred processing your request"));
    }
}
```

**Priority:** 🔴 Critical

---

## Larger Improvements (1-2 Days Each)

### 9. Refactor RecommendationService

**Split into:**

**ArtifactLoaderService:**
- `loadMappings()`
- `loadMatrices()`
- Matrix validation

**DataService:**
- `loadItems()`
- `loadInteractions()`
- Data caching

**ScoringService:**
- `calculateHybridScore()`
- `calculateContentScore()`
- `calculateCFScore()`

**RecommendationService:**
- `recommendForUser()`
- `getSimilarItems()`
- Orchestration

**Priority:** 🟢 Medium

---

### 10. Add Docker Support

**Create:** `Dockerfile`
```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/recommender-0.0.1-SNAPSHOT.jar app.jar
COPY artifacts/ /app/artifacts/
COPY data/ /app/data/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Create:** `docker-compose.yml`
```yaml
version: '3.8'
services:
  recommender:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=production
    volumes:
      - ./artifacts:/app/artifacts
      - ./data:/app/data
```

**Priority:** 🟡 High

---

### 11. Add CI/CD Pipeline

**Create:** `.github/workflows/ci.yml`
```yaml
name: CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    - uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    - name: Build with Maven
      run: ./mvnw clean verify
    - name: Run tests
      run: ./mvnw test
    - name: Upload coverage
      uses: codecov/codecov-action@v3
```

**Priority:** 🟡 High

---

### 12. Add Comprehensive Integration Tests

**Create:** `RecommendationControllerIntegrationTest.java`
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RecommendationControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void getRecommendations_validUser_returnsRecommendations() {
        ResponseEntity<List> response = restTemplate.getForEntity(
            "/api/v1/recommendations?user_id=testUser&limit=3",
            List.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
    
    @Test
    void getSimilarItems_validItem_returnsSimilarItems() {
        // ...
    }
}
```

**Priority:** 🔴 Critical

---

### 13. Add Python Requirements File

**Create:** `requirements.txt`
```
pandas==2.1.4
numpy==1.26.2
scipy==1.11.4
scikit-learn==1.3.2
implicit==0.7.2
tqdm==4.66.1
```

**Create:** `scripts/setup.sh`
```bash
#!/bin/bash
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

**Priority:** 🟢 Medium

---

## Optional Enhancements

### 14. Add Caching Layer
- Use Caffeine for in-memory caching
- Cache popular queries
- TTL-based invalidation

### 15. Add Swagger/OpenAPI Annotations
```java
@Operation(summary = "Get recommendations for a user")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Success"),
    @ApiResponse(responseCode = "400", description = "Invalid input")
})
@GetMapping("/recommendations")
public List<RecommendationResponse> getRecommendations(...)
```

### 16. Add Prometheus Metrics
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### 17. Add Rate Limiting
- Use Bucket4j
- Configure per-endpoint limits
- Add rate limit headers

### 18. Add Request/Response Logging
- Log all API requests
- Include timing information
- Sanitize sensitive data

---

## Implementation Order

**Week 1: Critical Fixes**
1. Fix CORS (Day 1)
2. Add input validation (Day 1)
3. Add test profile and fixtures (Day 2)
4. Add exception handling (Day 2)
5. Fix logging (Day 3)
6. Add basic unit tests (Day 3-5)

**Week 2: Quality & DevOps**
1. Add integration tests (Day 1-2)
2. Add Docker support (Day 3)
3. Add CI/CD pipeline (Day 4)
4. Refactor service layer (Day 4-5)
5. Update dependencies (Day 5)

**Week 3: Enhancements**
1. Add monitoring/metrics
2. Add caching
3. Add API documentation
4. Performance testing
5. Documentation updates

---

## Testing Strategy

### Unit Tests (Target: 70% coverage)
- All utility methods
- Service layer business logic
- DTO validation

### Integration Tests (Target: Major flows)
- All API endpoints
- Error scenarios
- Edge cases

### Performance Tests
- Concurrent users
- Large datasets
- Memory profiling

---

## Success Metrics

- [ ] Test coverage >70%
- [ ] All critical security issues fixed
- [ ] CI/CD pipeline green
- [ ] Docker build successful
- [ ] API documentation complete
- [ ] No high/critical vulnerabilities
- [ ] Response time <200ms p95
- [ ] Zero downtime deployments possible

---

## Resources Needed

**Tools:**
- Java 17 JDK
- Maven 3.8+
- Docker Desktop
- Python 3.11+
- IDE (IntelliJ IDEA / VS Code)

**Time Estimate:**
- Critical fixes: 1 week
- Quality improvements: 1 week
- Enhancements: 1 week
- **Total: 3 weeks** (one developer)

---

**Next Action:** Start with "Quick Wins" section, implement in order listed.
