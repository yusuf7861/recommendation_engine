package com.recommender.recommender.service;

import com.recommender.recommender.model.Product;
import com.recommender.recommender.model.RecommendationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private Path mockArtifactsBaseDir;

    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationService();

        // Mock the artifacts directory resolution
        ReflectionTestUtils.setField(recommendationService, "artifactsBaseDir", mockArtifactsBaseDir);
    }

    @Test
    @DisplayName("Get recommendations for known user should return hybrid recommendations")
    void getRecommendations_knownUser_returnsHybridRecommendations() {
        // Setup mock data
        Map<String, Integer> user2idx = Map.of("user1", 0);
        Map<String, Integer> item2idx = Map.of("item1", 0, "item2", 1);
        double[][] userFactors = {{1.0, 0.5}};
        double[][] itemFactors = {{1.0, 0.5}, {0.5, 1.0}};
        double[][] userContent = {{0.8, 0.6}};
        double[][] itemContent = {{0.8, 0.6}, {0.6, 0.8}};

        // Set up service with mock data
        ReflectionTestUtils.setField(recommendationService, "user2idx", user2idx);
        ReflectionTestUtils.setField(recommendationService, "item2idx", item2idx);
        ReflectionTestUtils.setField(recommendationService, "userFactors", userFactors);
        ReflectionTestUtils.setField(recommendationService, "itemFactors", itemFactors);
        ReflectionTestUtils.setField(recommendationService, "userContent", userContent);
        ReflectionTestUtils.setField(recommendationService, "itemContent", itemContent);
        ReflectionTestUtils.setField(recommendationService, "hybridWCF", 0.7);
        ReflectionTestUtils.setField(recommendationService, "hybridWContent", 0.3);

        // Mock items
        List<Product> items = List.of(
            createProduct("item1", "Product 1"),
            createProduct("item2", "Product 2")
        );
        ReflectionTestUtils.setField(recommendationService, "items", items);
        Map<String, Product> itemById = Map.of(
            "item1", items.get(0),
            "item2", items.get(1)
        );
        ReflectionTestUtils.setField(recommendationService, "itemById", itemById);

        // Execute
        List<RecommendationResponse> result = recommendationService.recommendForUser("user1", 5);

        // Verify
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.size() <= 5);
        assertTrue(result.get(0).getScore() >= 0.0);
    }

    @Test
    @DisplayName("Get recommendations for unknown user should return content-based recommendations")
    void getRecommendations_unknownUser_returnsContentBasedRecommendations() {
        // Setup mock data
        Map<String, Integer> user2idx = Map.of(); // Empty map
        Map<String, String> interactionsByUser = Map.of("user1", "item1,item2");

        ReflectionTestUtils.setField(recommendationService, "user2idx", user2idx);
        ReflectionTestUtils.setField(recommendationService, "interactionsByUser", interactionsByUser);

        // Mock content-based recommendation call
        RecommendationService spyService = spy(recommendationService);
        doReturn(List.of()).when(spyService).recommendContentBased("user1", 5);

        // Execute
        List<RecommendationResponse> result = spyService.recommendForUser("user1", 5);

        // Verify
        assertNotNull(result);
        verify(spyService).recommendContentBased("user1", 5);
    }

    @Test
    @DisplayName("Get recommendations with limit normalization")
    void getRecommendations_limitNormalization() {
        // Test with limit = 0 (should default to DEFAULT_LIMIT)
        List<RecommendationResponse> result = recommendationService.recommendForUser("user1", 0);
        // Should not throw exception and handle gracefully

        // Test with negative limit
        result = recommendationService.recommendForUser("user1", -1);
        // Should handle gracefully

        // Test with large limit (should be capped)
        result = recommendationService.recommendForUser("user1", 100);
        // Should not exceed maxLimit
    }

    @Test
    @DisplayName("Get similar items should return hybrid similarity scores")
    void getSimilarItems_returnsHybridSimilarity() {
        // Setup mock data
        Map<String, Integer> item2idx = Map.of("item1", 0, "item2", 1);
        double[][] itemFactors = {{1.0, 0.5}, {0.5, 1.0}};
        double[][] itemContent = {{0.8, 0.6}, {0.6, 0.8}};

        ReflectionTestUtils.setField(recommendationService, "item2idx", item2idx);
        ReflectionTestUtils.setField(recommendationService, "itemFactors", itemFactors);
        ReflectionTestUtils.setField(recommendationService, "itemContent", itemContent);
        ReflectionTestUtils.setField(recommendationService, "hybridWCF", 0.7);
        ReflectionTestUtils.setField(recommendationService, "hybridWContent", 0.3);

        // Mock items
        List<Product> items = List.of(
            createProduct("item1", "Product 1"),
            createProduct("item2", "Product 2")
        );
        ReflectionTestUtils.setField(recommendationService, "items", items);

        // Execute
        List<RecommendationResponse> result = recommendationService.getSimilarItems("item1", 5);

        // Verify
        assertNotNull(result);
        assertFalse(result.isEmpty());
        // First result should be the item itself with score 1.0 (but excluded)
        assertTrue(result.stream().noneMatch(r -> r.getItem_id().equals("item1")));
    }

    @Test
    @DisplayName("Get similar items for unknown item should return empty list")
    void getSimilarItems_unknownItem_returnsEmptyList() {
        Map<String, Integer> item2idx = Map.of(); // Empty map

        ReflectionTestUtils.setField(recommendationService, "item2idx", item2idx);

        List<RecommendationResponse> result = recommendationService.getSimilarItems("unknown", 5);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Get popular items should return items sorted by interaction count")
    void getPopularItems_returnsSortedByInteractions() {
        // Setup mock data
        List<Product> items = List.of(
            createProduct("item1", "Product 1"),
            createProduct("item2", "Product 2"),
            createProduct("item3", "Product 3")
        );
        Map<String, Integer> itemInteractionCounts = Map.of(
            "item1", 10,
            "item2", 5,
            "item3", 15
        );

        ReflectionTestUtils.setField(recommendationService, "items", items);
        ReflectionTestUtils.setField(recommendationService, "itemInteractionCounts", itemInteractionCounts);

        // Execute
        List<RecommendationResponse> result = recommendationService.getPopular(5);

        // Verify
        assertNotNull(result);
        assertFalse(result.isEmpty());
        // Should be sorted by interaction count descending: item3 (15), item1 (10), item2 (5)
        assertEquals("item3", result.get(0).getItem_id());
        assertEquals("item1", result.get(1).getItem_id());
        assertEquals("item2", result.get(2).getItem_id());
    }

    @Test
    @DisplayName("Content-based recommendations for user with interactions")
    void recommendContentBased_withInteractions() {
        // Setup mock data
        Map<String, String> interactionsByUser = Map.of("user1", "item1");
        Map<String, Integer> item2idx = Map.of("item1", 0, "item2", 1);
        double[][] itemContent = {{0.8, 0.6}, {0.6, 0.8}};

        ReflectionTestUtils.setField(recommendationService, "interactionsByUser", interactionsByUser);
        ReflectionTestUtils.setField(recommendationService, "item2idx", item2idx);
        ReflectionTestUtils.setField(recommendationService, "itemContent", itemContent);

        // Mock items
        List<Product> items = List.of(
            createProduct("item1", "Product 1"),
            createProduct("item2", "Product 2")
        );
        ReflectionTestUtils.setField(recommendationService, "items", items);

        // Execute
        List<RecommendationResponse> result = recommendationService.recommendContentBased("user1", 5);

        // Verify
        assertNotNull(result);
        // Should exclude the interacted item
        assertTrue(result.stream().noneMatch(r -> r.getItem_id().equals("item1")));
    }

    @Test
    @DisplayName("Content-based recommendations for user without interactions should return popular items")
    void recommendContentBased_noInteractions_returnsPopular() {
        Map<String, String> interactionsByUser = Map.of(); // No interactions

        ReflectionTestUtils.setField(recommendationService, "interactionsByUser", interactionsByUser);

        // Mock popular items call
        RecommendationService spyService = spy(recommendationService);
        doReturn(List.of()).when(spyService).getPopular(5);

        List<RecommendationResponse> result = spyService.recommendContentBased("user1", 5);

        verify(spyService).getPopular(5);
    }

    @Test
    @DisplayName("Health check should return system status")
    void getHealth_returnsSystemStatus() {
        // Setup mock data
        Map<String, Integer> user2idx = Map.of("user1", 0, "user2", 1);
        Map<String, Integer> item2idx = Map.of("item1", 0, "item2", 1, "item3", 2);
        Map<String, String> interactionsByUser = Map.of("user1", "item1", "user2", "item2");

        ReflectionTestUtils.setField(recommendationService, "user2idx", user2idx);
        ReflectionTestUtils.setField(recommendationService, "item2idx", item2idx);
        ReflectionTestUtils.setField(recommendationService, "interactionsByUser", interactionsByUser);
        ReflectionTestUtils.setField(recommendationService, "hybridWCF", 0.7);
        ReflectionTestUtils.setField(recommendationService, "hybridWContent", 0.3);

        // Execute
        Map<String, Object> health = recommendationService.getHealth();

        // Verify
        assertNotNull(health);
        assertEquals("UP", health.get("status"));
        assertEquals(2, health.get("users"));
        assertEquals(3, health.get("items"));
        assertEquals(2, health.get("interactionsUsers"));
        assertNotNull(health.get("hybridWeights"));
    }

    @Test
    @DisplayName("Normalize limit should handle edge cases")
    void normalizeLimit_edgeCases() {
        // Test with ReflectionTestUtils to access private method
        ReflectionTestUtils.setField(recommendationService, "maxLimit", 20);

        // Test various limits
        assertEquals(5, (int) ReflectionTestUtils.invokeMethod(recommendationService, "normalizeLimit", 0));
        assertEquals(5, (int) ReflectionTestUtils.invokeMethod(recommendationService, "normalizeLimit", -1));
        assertEquals(10, (int) ReflectionTestUtils.invokeMethod(recommendationService, "normalizeLimit", 10));
        assertEquals(20, (int) ReflectionTestUtils.invokeMethod(recommendationService, "normalizeLimit", 25));
    }

    private Product createProduct(String itemId, String title) {
        Product product = new Product();
        product.setItem_id(itemId);
        product.setTitle(title);
        product.setBrand("Test Brand");
        product.setCategory("Test Category");
        product.setImage_url("http://example.com/image.jpg");
        return product;
    }
}

