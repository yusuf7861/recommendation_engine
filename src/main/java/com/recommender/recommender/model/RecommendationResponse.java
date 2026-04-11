package com.recommender.recommender.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class RecommendationResponse {
    private String item_id;
    private String title;
    private String brand;
    private String category;
    private String image_url;
    private double score;
}
