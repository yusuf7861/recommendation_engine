package com.recommender.recommender.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    @Test
    @DisplayName("Product should be created with all fields")
    void product_creation_withAllFields() {
        Product product = new Product();
        product.setItem_id("B001234");
        product.setTitle("Test Product");
        product.setBrand("Test Brand");
        product.setCategory("Electronics");
        product.setDescription("A test product description");
        product.setImage_url("http://example.com/image.jpg");

        assertEquals("B001234", product.getItem_id());
        assertEquals("Test Product", product.getTitle());
        assertEquals("Test Brand", product.getBrand());
        assertEquals("Electronics", product.getCategory());
        assertEquals("A test product description", product.getDescription());
        assertEquals("http://example.com/image.jpg", product.getImage_url());
    }

    @Test
    @DisplayName("Product should handle null values gracefully")
    void product_handlesNullValues() {
        Product product = new Product();

        // Test with null values
        product.setItem_id(null);
        product.setTitle(null);
        product.setBrand(null);
        product.setCategory(null);
        product.setDescription(null);
        product.setImage_url(null);

        assertNull(product.getItem_id());
        assertNull(product.getTitle());
        assertNull(product.getBrand());
        assertNull(product.getCategory());
        assertNull(product.getDescription());
        assertNull(product.getImage_url());
    }

    @Test
    @DisplayName("Product toString should work")
    void product_toString() {
        Product product = new Product();
        product.setItem_id("B001234");
        product.setTitle("Test Product");

        String toString = product.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("B001234"));
        assertTrue(toString.contains("Test Product"));
    }

    @Test
    @DisplayName("Product equals and hashCode should work")
    void product_equalsAndHashCode() {
        Product product1 = new Product();
        product1.setItem_id("B001234");
        product1.setTitle("Test Product");

        Product product2 = new Product();
        product2.setItem_id("B001234");
        product2.setTitle("Test Product");

        Product product3 = new Product();
        product3.setItem_id("B005678");
        product3.setTitle("Different Product");

        assertEquals(product1, product2);
        assertNotEquals(product1, product3);
        assertEquals(product1.hashCode(), product2.hashCode());
        assertNotEquals(product1.hashCode(), product3.hashCode());
    }
}
