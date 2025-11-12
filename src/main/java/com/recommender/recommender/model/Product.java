package com.recommender.recommender.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Product {
    private String item_id;
    private String title;
    private String brand;
    private String category;
    private String description;
    private String image_url;
}
