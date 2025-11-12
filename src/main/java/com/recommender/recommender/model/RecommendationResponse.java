package com.recommender.recommender.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RecommendationResponse {
    private String item_id;
    private String title;
    private String brand;
    private String category;
    private String image_url;
    private double score;
}
