package com.recommender.recommender.controller;

import com.recommender.recommender.service.RecommendationService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class HealthController {

    private final RecommendationService service;

    public HealthController(RecommendationService service) {
        this.service = service;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> m = new LinkedHashMap<>();
        try {
            m.put("status", "UP");
            m.put("users", service.getUserCount());
            m.put("items", service.getItemCount());
            m.put("interactionsUsers", service.getInteractedUserCount());
            m.put("hybridWeights", Map.of("cf", service.getHybridWCF(), "content", service.getHybridWContent()));
        } catch (Exception e) {
            m.put("status", "ERROR");
            m.put("message", e.getMessage());
        }
        return m;
    }
}

