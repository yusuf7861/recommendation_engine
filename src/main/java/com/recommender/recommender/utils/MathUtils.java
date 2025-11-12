package com.recommender.recommender.utils;

public class MathUtils {

    public static double cosine(double[] a, double[] b) {
        if (a == null || b == null) return 0.0;
        int n = Math.min(a.length, b.length);
        if (n == 0) return 0.0;
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < n; i++) {
            double ai = a[i];
            double bi = b[i];
            dot += ai * bi;
            normA += ai * ai;
            normB += bi * bi;
        }
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public static double norm(double[] v) {
        if (v == null) return 0.0;
        double sum = 0.0;
        for (double x : v) sum += x * x;
        return Math.sqrt(sum);
    }

}
