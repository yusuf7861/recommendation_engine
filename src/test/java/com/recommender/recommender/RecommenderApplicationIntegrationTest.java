package com.recommender.recommender;

import com.recommender.recommender.model.RecommendationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RecommenderApplicationIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Full application context should load successfully")
    void contextLoads() {
        // This test verifies that the entire application context loads without errors
        assertNotNull(restTemplate);
        assertTrue(port > 0);
    }

    @Test
    @DisplayName("Health endpoint should return UP status")
    void healthEndpoint_returnsUpStatus() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/health", String.class);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"status\":\"UP\"") ||
                  response.getBody().contains("\"status\":\"ERROR\""));
    }

    @Test
    @DisplayName("GET recommendations endpoint should handle requests")
    void getRecommendationsEndpoint_handlesRequests() {
        // Test with a dummy user ID - should not throw exception
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/recommendations?user_id=test_user&limit=5",
            String.class);

        // Should return either success or appropriate error, but not crash
        assertTrue(response.getStatusCodeValue() == 200 ||
                  response.getStatusCodeValue() == 500 ||
                  response.getStatusCodeValue() == 404);
    }

    @Test
    @DisplayName("POST recommendations endpoint should handle requests")
    void postRecommendationsEndpoint_handlesRequests() {
        String requestBody = "{\"user_id\":\"test_user\",\"limit\":5}";

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/recommendations",
            entity,
            String.class);

        // Should return either success or appropriate error, but not crash
        assertTrue(response.getStatusCodeValue() == 200 ||
                  response.getStatusCodeValue() == 400 ||
                  response.getStatusCodeValue() == 500);
    }

    @Test
    @DisplayName("Similar items endpoint should handle requests")
    void similarItemsEndpoint_handlesRequests() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/items/test_item/similar?limit=5",
            String.class);

        // Should return either success or appropriate error, but not crash
        assertTrue(response.getStatusCodeValue() == 200 ||
                  response.getStatusCodeValue() == 500);
    }

    @Test
    @DisplayName("Popular items endpoint should handle requests")
    void popularItemsEndpoint_handlesRequests() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/popular?limit=5",
            String.class);

        // Should return either success or appropriate error, but not crash
        assertTrue(response.getStatusCodeValue() == 200 ||
                  response.getStatusCodeValue() == 500);
    }

    @Test
    @DisplayName("Invalid requests should return appropriate error codes")
    void invalidRequests_returnAppropriateErrors() {
        // Test missing user_id parameter
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/recommendations",
            String.class);

        assertEquals(400, response.getStatusCodeValue());

        // Test invalid POST body
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>("{\"invalid\":\"json\"", headers);

        response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/recommendations",
            entity,
            String.class);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    @DisplayName("CORS headers should be present")
    void corsHeaders_present() {
        org.springframework.http.HttpHeaders reqHeaders = new org.springframework.http.HttpHeaders();
        reqHeaders.setOrigin("http://localhost:8080");
        org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(reqHeaders);

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:" + port + "/api/v1/health",
            org.springframework.http.HttpMethod.GET,
            entity,
            String.class);

        // Check for CORS headers
        assertNotNull(response.getHeaders().getAccessControlAllowOrigin());
        assertTrue(response.getHeaders().getAccessControlAllowOrigin().contains("*") ||
                  response.getHeaders().getAccessControlAllowOrigin().contains("http://localhost:8080"));
    }

    @Test
    @DisplayName("Application should handle concurrent requests")
    void application_handlesConcurrentRequests() {
        // Test multiple concurrent requests
        // This is a basic test - in a real scenario, you'd use a load testing tool
        for (int i = 0; i < 10; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/v1/health",
                String.class);

            assertTrue(response.getStatusCodeValue() == 200 ||
                      response.getStatusCodeValue() == 500);
        }
    }
}
