package com.recommender.recommender.controller;

import com.recommender.recommender.model.RecommendationResponse;
import com.recommender.recommender.service.RecommendationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class AdditionalEndpointsController {

    private final RecommendationService service;

    public AdditionalEndpointsController(RecommendationService service) {
        this.service = service;
    }

    @GetMapping("/recommendations")
    public List<RecommendationResponse> getRecommendationsGet(@RequestParam("user_id") String userId,
                                                              @RequestParam(value = "limit", defaultValue = "5") int limit) {
        return service.recommendForUser(userId, limit);
    }

    @GetMapping("/popular")
    public List<RecommendationResponse> getPopular(@RequestParam(value = "limit", defaultValue = "5") int limit) {
        return service.getPopular(limit);
    }
}
