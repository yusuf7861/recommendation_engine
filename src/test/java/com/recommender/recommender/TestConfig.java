package com.recommender.recommender;

import com.recommender.recommender.service.RecommendationService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@TestConfiguration
@Profile("test")
public class TestConfig {

    @Bean
    @Primary
    public RecommendationService recommendationService() {
        // Return a mock or test implementation of RecommendationService
        return new RecommendationService();
    }
}
