package com.recommender.recommender.controller;

import com.recommender.recommender.model.RecommendationResponse;
import com.recommender.recommender.service.RecommendationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RecommendationController.class)
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecommendationService recommendationService;

    @Test
    @DisplayName("POST /api/v1/recommendations should return recommendations")
    void getRecommendations_returnsRecommendations() throws Exception {
        // Setup mock data
        List<RecommendationResponse> mockRecommendations = Arrays.asList(
            createMockRecommendation("item1", "Product 1", 0.95),
            createMockRecommendation("item2", "Product 2", 0.87)
        );

        when(recommendationService.recommendForUser("user123", 5))
            .thenReturn(mockRecommendations);

        // Execute and verify
        mockMvc.perform(post("/api/v1/recommendations")
                .contentType("application/json")
                .content("{\"user_id\":\"user123\",\"limit\":5}"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].item_id").value("item1"))
            .andExpect(jsonPath("$[0].title").value("Product 1"))
            .andExpect(jsonPath("$[0].score").value(0.95));
    }

    @Test
    @DisplayName("POST /api/v1/recommendations with default limit")
    void getRecommendations_defaultLimit() throws Exception {
        List<RecommendationResponse> mockRecommendations = Arrays.asList(
            createMockRecommendation("item1", "Product 1", 0.95)
        );

        when(recommendationService.recommendForUser("user123", 5))
            .thenReturn(mockRecommendations);

        mockMvc.perform(post("/api/v1/recommendations")
                .contentType("application/json")
                .content("{\"user_id\":\"user123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/recommendations with missing user_id should return bad request")
    void getRecommendations_missingUserId_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/recommendations")
                .contentType("application/json")
                .content("{\"limit\":5}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/items/{itemId}/similar should return similar items")
    void getSimilarItems_returnsSimilarItems() throws Exception {
        List<RecommendationResponse> mockSimilar = Arrays.asList(
            createMockRecommendation("item2", "Similar Product", 0.82),
            createMockRecommendation("item3", "Another Similar", 0.75)
        );

        when(recommendationService.getSimilarItems("item1", 5))
            .thenReturn(mockSimilar);

        mockMvc.perform(get("/api/v1/items/item1/similar")
                .param("limit", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].item_id").value("item2"))
            .andExpect(jsonPath("$[0].score").value(0.82));
    }

    @Test
    @DisplayName("GET /api/v1/items/{itemId}/similar with default limit")
    void getSimilarItems_defaultLimit() throws Exception {
        List<RecommendationResponse> mockSimilar = Arrays.asList(
            createMockRecommendation("item2", "Similar Product", 0.82)
        );

        when(recommendationService.getSimilarItems("item1", 5))
            .thenReturn(mockSimilar);

        mockMvc.perform(get("/api/v1/items/item1/similar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Controller should handle service exceptions gracefully")
    void controller_handlesServiceExceptions() throws Exception {
        when(recommendationService.recommendForUser(anyString(), anyInt()))
            .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(post("/api/v1/recommendations")
                .contentType("application/json")
                .content("{\"user_id\":\"user123\",\"limit\":5}"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Controller should handle invalid limit parameter")
    void controller_handlesInvalidLimit() throws Exception {
        List<RecommendationResponse> mockRecommendations = Arrays.asList(
            createMockRecommendation("item1", "Product 1", 0.95)
        );

        when(recommendationService.recommendForUser("user123", 5))
            .thenReturn(mockRecommendations);

        // Test with invalid limit (should be handled by service)
        mockMvc.perform(post("/api/v1/recommendations")
                .contentType("application/json")
                .content("{\"user_id\":\"user123\",\"limit\":\"invalid\"}"))
            .andExpect(status().isBadRequest());
    }

    private RecommendationResponse createMockRecommendation(String itemId, String title, double score) {
        return new RecommendationResponse(
            itemId,
            title,
            "Test Brand",
            "Test Category",
            "http://example.com/image.jpg",
            score
        );
    }
}
