package com.recommender.recommender.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class MathUtilsTest {

    @Test
    @DisplayName("Cosine similarity of identical vectors should be 1.0")
    void cosine_identicalVectors_returnsOne() {
        double[] vec = {1.0, 2.0, 3.0};
        double result = MathUtils.cosine(vec, vec);
        assertEquals(1.0, result, 0.0001);
    }

    @Test
    @DisplayName("Cosine similarity of orthogonal vectors should be 0.0")
    void cosine_orthogonalVectors_returnsZero() {
        double[] vec1 = {1.0, 0.0};
        double[] vec2 = {0.0, 1.0};
        double result = MathUtils.cosine(vec1, vec2);
        assertEquals(0.0, result, 0.0001);
    }

    @Test
    @DisplayName("Cosine similarity of opposite vectors should be -1.0")
    void cosine_oppositeVectors_returnsNegativeOne() {
        double[] vec1 = {1.0, 2.0};
        double[] vec2 = {-1.0, -2.0};
        double result = MathUtils.cosine(vec1, vec2);
        assertEquals(-1.0, result, 0.0001);
    }

    @ParameterizedTest
    @CsvSource({
        "1.0, 0.0, 0.0, 1.0, 0.0",
        "1.0, 1.0, 1.0, 1.0, 1.0",
        "3.0, 4.0, 1.0, 0.0, 0.0"
    })
    @DisplayName("Cosine similarity with various vector combinations")
    void cosine_variousVectors(double x1, double y1, double x2, double y2, double expected) {
        double[] vec1 = {x1, y1};
        double[] vec2 = {x2, y2};
        double result = MathUtils.cosine(vec1, vec2);
        assertEquals(expected, result, 0.0001);
    }

    @Test
    @DisplayName("Cosine similarity with null vectors should return 0.0")
    void cosine_nullVectors_returnsZero() {
        double[] vec1 = {1.0, 2.0};
        double result1 = MathUtils.cosine(null, vec1);
        double result2 = MathUtils.cosine(vec1, null);
        double result3 = MathUtils.cosine(null, null);

        assertEquals(0.0, result1);
        assertEquals(0.0, result2);
        assertEquals(0.0, result3);
    }

    @Test
    @DisplayName("Cosine similarity with empty vectors should return 0.0")
    void cosine_emptyVectors_returnsZero() {
        double[] vec1 = {};
        double[] vec2 = {1.0};
        double result = MathUtils.cosine(vec1, vec2);
        assertEquals(0.0, result);
    }

    @Test
    @DisplayName("Cosine similarity with zero vectors should return 0.0")
    void cosine_zeroVectors_returnsZero() {
        double[] vec1 = {0.0, 0.0};
        double[] vec2 = {1.0, 2.0};
        double result = MathUtils.cosine(vec1, vec2);
        assertEquals(0.0, result);
    }

    @Test
    @DisplayName("Cosine similarity with different length vectors should use minimum length")
    void cosine_differentLengths_usesMinimumLength() {
        double[] vec1 = {1.0, 2.0, 3.0};
        double[] vec2 = {1.0, 2.0};
        double result = MathUtils.cosine(vec1, vec2);
        // Should be cos([1,2], [1,2]) = 1.0
        assertEquals(1.0, result, 0.0001);
    }

    @Test
    @DisplayName("Cosine similarity calculation with floating point precision")
    void cosine_floatingPointPrecision() {
        double[] vec1 = {1.0000000001, 2.0000000001};
        double[] vec2 = {1.0000000002, 2.0000000002};
        double result = MathUtils.cosine(vec1, vec2);
        assertTrue(result > 0.9999); // Should be very close to 1.0
    }
}
