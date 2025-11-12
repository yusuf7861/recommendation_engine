package com.recommender.recommender.controller;

import com.recommender.recommender.model.RecommendationResponse;
import com.recommender.recommender.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    @PostMapping("/recommendations")
    public List<RecommendationResponse> getRecommendations(@RequestBody UserRequest request) {
        return recommendationService.recommendForUser(request.getUser_id(), request.getLimit());
    }

    @GetMapping("/items/{itemId}/similar")
    public List<RecommendationResponse> getSimilarItems(@PathVariable String itemId,
                                                        @RequestParam(defaultValue = "5") int limit) {
        return recommendationService.getSimilarItems(itemId, limit);
    }

    // Simple DTO
    public static class UserRequest {
        private String user_id;
        private int limit = 5;

        public String getUser_id() { return user_id; }
        public void setUser_id(String user_id) { this.user_id = user_id; }
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
    }
}

