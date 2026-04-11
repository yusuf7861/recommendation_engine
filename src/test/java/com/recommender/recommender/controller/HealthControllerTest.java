package com.recommender.recommender.controller;

import com.recommender.recommender.service.RecommendationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecommendationService recommendationService;

    @Test
    @DisplayName("GET /api/v1/health should return system health status")
    void getHealth_returnsHealthStatus() throws Exception {
        // Setup mock health data
        when(recommendationService.getUserCount()).thenReturn(1000);
        when(recommendationService.getItemCount()).thenReturn(5000);
        when(recommendationService.getInteractedUserCount()).thenReturn(800);
        when(recommendationService.getHybridWCF()).thenReturn(0.7);
        when(recommendationService.getHybridWContent()).thenReturn(0.3);

        // Execute and verify
        mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.users").value(1000))
            .andExpect(jsonPath("$.items").value(5000))
            .andExpect(jsonPath("$.interactionsUsers").value(800))
            .andExpect(jsonPath("$.hybridWeights.cf").value(0.7))
            .andExpect(jsonPath("$.hybridWeights.content").value(0.3));
    }

    @Test
    @DisplayName("GET /api/v1/health should handle service exceptions")
    void getHealth_handlesServiceExceptions() throws Exception {
        when(recommendationService.getUserCount())
            .thenThrow(new RuntimeException("Service unavailable"));

        mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("GET /api/v1/health should return error status when service fails")
    void getHealth_returnsErrorStatusOnFailure() throws Exception {
        when(recommendationService.getUserCount())
            .thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isInternalServerError());
    }
}
