package com.recommender.recommender.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class RecommendationResponseTest {

    @Test
    @DisplayName("RecommendationResponse should be created with all fields")
    void recommendationResponse_creation_withAllFields() {
        RecommendationResponse response = new RecommendationResponse(
            "B001234",
            "Test Product",
            "Test Brand",
            "Electronics",
            "http://example.com/image.jpg",
            0.95
        );

        assertEquals("B001234", response.getItem_id());
        assertEquals("Test Product", response.getTitle());
        assertEquals("Test Brand", response.getBrand());
        assertEquals("Electronics", response.getCategory());
        assertEquals("http://example.com/image.jpg", response.getImage_url());
        assertEquals(0.95, response.getScore(), 0.001);
    }

    @Test
    @DisplayName("RecommendationResponse should handle null values")
    void recommendationResponse_handlesNullValues() {
        RecommendationResponse response = new RecommendationResponse(
            null, null, null, null, null, 0.0
        );

        assertNull(response.getItem_id());
        assertNull(response.getTitle());
        assertNull(response.getBrand());
        assertNull(response.getCategory());
        assertNull(response.getImage_url());
        assertEquals(0.0, response.getScore());
    }

    @Test
    @DisplayName("RecommendationResponse toString should work")
    void recommendationResponse_toString() {
        RecommendationResponse response = new RecommendationResponse(
            "B001234", "Test Product", "Test Brand", "Electronics",
            "http://example.com/image.jpg", 0.95
        );

        String toString = response.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("B001234"));
        assertTrue(toString.contains("Test Product"));
        assertTrue(toString.contains("0.95"));
    }

    @Test
    @DisplayName("RecommendationResponse equals and hashCode should work")
    void recommendationResponse_equalsAndHashCode() {
        RecommendationResponse response1 = new RecommendationResponse(
            "B001234", "Test Product", "Test Brand", "Electronics",
            "http://example.com/image.jpg", 0.95
        );

        RecommendationResponse response2 = new RecommendationResponse(
            "B001234", "Test Product", "Test Brand", "Electronics",
            "http://example.com/image.jpg", 0.95
        );

        RecommendationResponse response3 = new RecommendationResponse(
            "B005678", "Different Product", "Different Brand", "Books",
            "http://different.com/image.jpg", 0.87
        );

        assertEquals(response1, response2);
        assertNotEquals(response1, response3);
        assertEquals(response1.hashCode(), response2.hashCode());
        assertNotEquals(response1.hashCode(), response3.hashCode());
    }

    @Test
    @DisplayName("RecommendationResponse should handle floating point precision")
    void recommendationResponse_floatingPointPrecision() {
        RecommendationResponse response = new RecommendationResponse(
            "B001234", "Test", "Brand", "Category", "url", 0.123456789
        );

        assertEquals(0.123456789, response.getScore(), 0.000000001);
    }
}
