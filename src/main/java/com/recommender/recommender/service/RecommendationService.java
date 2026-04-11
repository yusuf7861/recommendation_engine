package com.recommender.recommender.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.recommender.recommender.model.Product;
import com.recommender.recommender.model.RecommendationResponse;
import com.recommender.recommender.utils.MathUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private static final int DEFAULT_LIMIT = 5;

    @Value("${reco.artifactsDir:artifacts}")
    private String artifactsDir;

    @Value("${reco.topK:20}")
    private int maxLimit;

    private Path artifactsBaseDir;

    private Map<String, Integer> user2idx;
    private Map<String, Integer> item2idx;
    private Map<Integer, String> idx2item;
    private double[][] userFactors;
    private double[][] itemFactors;
    private double[][] userContent;
    private double[][] itemContent;
    private double hybridWCF;
    private double hybridWContent;

    private List<Product> items;
    private Map<String, Product> itemById = new HashMap<>();
    private Map<String, List<String>> interactionsByUser = new HashMap<>();
    private Map<String, Integer> itemInteractionCounts = new HashMap<>();
    private List<String> rankedPopularItemIds = new ArrayList<>();

    @PostConstruct
    public void loadArtifacts() {
        try {
            System.out.println("🔄 Loading recommender artifacts...");
            artifactsBaseDir = resolveArtifactsBaseDir();
            System.out.println("📁 Using artifacts dir: " + artifactsBaseDir);

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> mapping = mapper.readValue(
                    new File(resolveArtifactPath("mappings.json")),
                    new TypeReference<>() {}
            );

            // ---- Load mappings ----
            user2idx = (Map<String, Integer>) mapping.get("user2idx");
            item2idx = (Map<String, Integer>) mapping.get("item2idx");
            idx2item = new HashMap<>();
            for (Map.Entry<String, Integer> entry : item2idx.entrySet()) {
                idx2item.put(entry.getValue(), entry.getKey());
            }

            Number wcf = (Number) mapping.get("hybrid_w_cf");
            Number wcontent = (Number) mapping.get("hybrid_w_content");
            hybridWCF = wcf != null ? wcf.doubleValue() : 0.5;
            hybridWContent = wcontent != null ? wcontent.doubleValue() : 0.5;

            System.out.println("✅ Loaded mappings: users=" + user2idx.size() + ", items=" + item2idx.size());

            // ---- Load CSV matrices ----
            try {
                // ⚠️ Load all matrices
                double[][] uFac = loadMatrix(resolveArtifactPath("user_factors.csv"));
                double[][] iFac = loadMatrix(resolveArtifactPath("item_factors.csv"));
                double[][] uCont = loadMatrix(resolveArtifactPath("user_content.csv"));
                double[][] iCont = loadMatrix(resolveArtifactPath("item_content.csv"));

                // ---- Detect swapped matrices ----
                if (uFac.length < iFac.length) {
                    System.out.println("⚠️ Detected swapped matrices — auto-correcting...");
                    userFactors = iFac;
                    itemFactors = uFac;
                } else {
                    userFactors = uFac;
                    itemFactors = iFac;
                }

                userContent = uCont;
                itemContent = iCont;

            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("❌ Error reading one of the matrix CSV files: " + e.getMessage(), e);
            }

            // ---- Load item metadata ----
            items = loadItemsCsv("data/items.csv");

            // ---- Load interactions ----
            loadInteractionsCsv("data/interactions.csv");

            // ---- Print shapes for verification ----
            System.out.println("✅ Shapes:");
            System.out.println("🔍 Sample user vector norm = " + MathUtils.norm(userFactors[0]));
            System.out.println("🔍 Sample item vector norm = " + MathUtils.norm(itemFactors[0]));
            System.out.println("🔍 Sample user-content vector norm = " + MathUtils.norm(userContent[0]));
            System.out.println("🔍 Sample item-content vector norm = " + MathUtils.norm(itemContent[0]));

            System.out.printf("   userFactors = %d × %d%n", userFactors.length, userFactors[0].length);
            System.out.printf("   itemFactors = %d × %d%n", itemFactors.length, itemFactors[0].length);
            System.out.printf("   userContent = %d × %d%n", userContent.length, userContent[0].length);
            System.out.printf("   itemContent = %d × %d%n", itemContent.length, itemContent[0].length);

            // ---- Sanity auto-align check ----
            if (userFactors.length != user2idx.size())
                System.out.println("⚠️ Warning: userFactors count ≠ mapping count (some users were filtered)");
            if (itemFactors.length != item2idx.size())
                System.out.println("⚠️ Warning: itemFactors count ≠ mapping count (filtered items)");

            System.out.println("✅ Artifacts successfully loaded!");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("❌ Error initializing RecommendationService: " + e.getMessage(), e);
        }
    }

    private List<RecommendationResponse> getPopularItems(int limit) {
        if (rankedPopularItemIds.isEmpty()) {
            return items.stream()
                    .limit(limit)
                    .map(p -> new RecommendationResponse(
                            p.getItem_id(),
                            p.getTitle(),
                            p.getBrand(),
                            p.getCategory(),
                            p.getImage_url(),
                            0.0
                    ))
                    .collect(Collectors.toList());
        }

        List<RecommendationResponse> out = new ArrayList<>();
        for (String itemId : rankedPopularItemIds) {
            if (out.size() >= limit) break;
            Product p = itemById.get(itemId);
            if (p == null) continue;
            out.add(new RecommendationResponse(
                    p.getItem_id(),
                    p.getTitle(),
                    p.getBrand(),
                    p.getCategory(),
                    p.getImage_url(),
                    0.0
            ));
        }
        return out;
    }

    @Cacheable(cacheNames = "recs:popular", key = "#limit", unless = "#result == null || #result.isEmpty()")
    public List<RecommendationResponse> getPopular(int limit) {
        int normalizedLimit = normalizeLimit(limit);
        return getPopularItems(normalizedLimit);
    }

    @Cacheable(cacheNames = "recs:user", key = "#userId + ':' + #limit", unless = "#result == null || #result.isEmpty()")
    public List<RecommendationResponse> recommendForUser(String userId, int limit) {
        int normalizedLimit = normalizeLimit(limit);
        Integer uIdx = user2idx.get(userId);

        // 🔹 Handle unknown or cold-start users
        if (uIdx == null || uIdx < 0 || uIdx >= userFactors.length) {
            System.out.println("⚠️ Unknown or inactive user: " + userId + " → content-based fallback");
            return recommendContentBased(userId, normalizedLimit);
        }

        double[] cfVector = userFactors[uIdx];
        double[] contentVector = uIdx < userContent.length ? userContent[uIdx] : new double[cfVector.length];

        int itemCount = Math.min(itemFactors.length, itemContent.length);
        double[] scores = new double[itemCount];

        for (int i = 0; i < itemCount; i++) {
            double simCF = MathUtils.cosine(cfVector, itemFactors[i]);
            double simContent = MathUtils.cosine(contentVector, itemContent[i]);
            scores[i] = hybridWCF * simCF + hybridWContent * simContent;
        }

        List<RecommendationResponse> recs = getTopRecommendations(scores, normalizedLimit);
        if (recs.isEmpty()) {
            System.out.println("⚠️ Hybrid returned empty → using content-based fallback");
            return recommendContentBased(userId, normalizedLimit);
        }

        return recs;
    }

    public List<RecommendationResponse> recommendContentBased(String userId, int limit) {
        int normalizedLimit = normalizeLimit(limit);

        List<String> interactedItems = interactionsByUser.getOrDefault(userId, Collections.emptyList());
        if (interactedItems.isEmpty()) {
            System.out.println("⚠️ No interactions for user " + userId + " → showing popular items");
            return getPopularItems(normalizedLimit);
        }

        String seedItemId = interactedItems.get(0);
        Integer seedIdx = item2idx.get(seedItemId);
        if (seedIdx == null || seedIdx < 0 || seedIdx >= itemContent.length)
            return getPopularItems(normalizedLimit);

        double[] seedVec = itemContent[seedIdx];
        double[] scores = new double[itemContent.length];
        for (int i = 0; i < itemContent.length; i++) {
            scores[i] = MathUtils.cosine(seedVec, itemContent[i]);
        }

        return getTopRecommendations(scores, normalizedLimit, seedItemId);
    }

    @Cacheable(cacheNames = "recs:similar", key = "#itemId + ':' + #limit", unless = "#result == null || #result.isEmpty()")
    public List<RecommendationResponse> getSimilarItems(String itemId, int limit) {
        int normalizedLimit = normalizeLimit(limit);
        if (!item2idx.containsKey(itemId)) {
            System.out.println("⚠️ Unknown item: " + itemId);
            return Collections.emptyList();
        }

        int itemIdx = item2idx.get(itemId);

        if (itemIdx < 0 || itemIdx >= itemFactors.length) {
            System.out.println("⚠️ Invalid item index: " + itemIdx);
            return Collections.emptyList();
        }

        double[] itemVecCF = itemFactors[itemIdx];
        double[] itemVecContent = itemIdx < itemContent.length ? itemContent[itemIdx] : new double[itemVecCF.length];

        int itemCount = Math.min(itemFactors.length, itemContent.length);
        double[] scores = new double[itemCount];

        for (int i = 0; i < itemCount; i++) {
            double simCF = MathUtils.cosine(itemVecCF, itemFactors[i]);
            double simContent = MathUtils.cosine(itemVecContent, itemContent[i]);
            scores[i] = hybridWCF * simCF + hybridWContent * simContent;
        }

        return getTopRecommendations(scores, normalizedLimit, itemId);
    }


    // --------------------------
    // 🧩 Utility Helpers
    // --------------------------

    private List<RecommendationResponse> getTopRecommendations(double[] scores, int limit) {
        return getTopRecommendations(scores, limit, null);
    }

    private List<RecommendationResponse> getTopRecommendations(double[] scores, int limit, String excludeItemId) {
        if (limit <= 0) return Collections.emptyList();

        // Build indices [0..n)
        int n = scores.length;
        List<Integer> idx = new ArrayList<>(n);
        for (int i = 0; i < n; i++) idx.add(i);

        // Sort indices by score desc
        idx.sort((a, b) -> Double.compare(scores[b], scores[a]));

        List<RecommendationResponse> out = new ArrayList<>();
        Set<String> seenItems = new HashSet<>();
        for (int i : idx) {
            if (out.size() >= limit) break;

            double s = scores[i];
            if (Double.isNaN(s) || Double.isInfinite(s)) continue;

            String itemId = idx2item.get(i);
            if (itemId == null) continue;
            if (excludeItemId != null && excludeItemId.equals(itemId)) continue;
            if (!seenItems.add(itemId)) continue; // skip duplicates

            Product p = itemById.get(itemId);
            if (p == null) {
                // fallback linear scan once; cache if found
                for (Product it : items) {
                    if (itemId.equals(it.getItem_id())) { p = it; itemById.put(itemId, it); break; }
                }
            }
            if (p == null) continue;

            out.add(new RecommendationResponse(
                    itemId,
                    p.getTitle(),
                    p.getBrand(),
                    p.getCategory(),
                    p.getImage_url(),
                    s
            ));
        }
        return out;
    }

    private double[][] loadMatrix(String path) throws IOException {
        List<double[]> rows = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                double[] arr = Arrays.stream(line.split(","))
                        .mapToDouble(Double::parseDouble)
                        .toArray();
                rows.add(arr);
            }
        }
        return rows.toArray(new double[0][]);
    }

    private List<Product> loadItemsCsv(String path) throws IOException {
        List<Product> list = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(path))) {
            String[] header = reader.readNext();
            if (header == null) return list;

            // Detect header: if it doesn't contain known column names and looks like data (ASIN-like ID), treat as data
            boolean looksLikeHeader = false;
            for (String h : header) {
                String hl = h == null ? "" : h.trim().toLowerCase();
                if (hl.equals("item_id") || hl.equals("title") || hl.equals("brand") || hl.equals("category") || hl.equals("image_url")) {
                    looksLikeHeader = true; break;
                }
            }

            Map<String, Integer> idx = new HashMap<>();
            if (looksLikeHeader) {
                for (int i = 0; i < header.length; i++) idx.put(header[i].trim().toLowerCase(), i);
            } else {
                // No header → assume columns: item_id,title,brand,category,description,image_url
                // Process the first row as data
                parseItemRow(list, header, 0, 1, 2, 3, 4, 5);
            }

            String[] row;
            while ((row = reader.readNext()) != null) {
                if (looksLikeHeader) {
                    // Use header indices
                    parseItemRow(list, row,
                            idx.getOrDefault("item_id", 0),
                            idx.getOrDefault("title", 1),
                            idx.getOrDefault("brand", 2),
                            idx.getOrDefault("category", 3),
                            idx.getOrDefault("description", 4),
                            idx.getOrDefault("image_url", 5)
                    );
                } else {
                    // Positional parsing
                    parseItemRow(list, row, 0, 1, 2, 3, 4, 5);
                }
            }
        } catch (CsvValidationException e) {
            throw new RuntimeException(e);
        }

        // Build id→product map for fast lookups
        itemById.clear();
        for (Product p : list) {
            if (p.getItem_id() != null && !p.getItem_id().isEmpty()) itemById.put(p.getItem_id(), p);
        }

        System.out.println("✅ Loaded " + list.size() + " items from " + path);
        System.out.println("✅ Shapes:");
        System.out.println("   userFactors = " + userFactors.length + " × " + userFactors[0].length);
        System.out.println("   itemFactors = " + itemFactors.length + " × " + itemFactors[0].length);
        System.out.println("   userContent = " + userContent.length + " × " + userContent[0].length);
        System.out.println("   itemContent = " + itemContent.length + " × " + itemContent[0].length);

        return list;
    }

    private void parseItemRow(List<Product> out, String[] row, int idIdx, int titleIdx, int brandIdx, int catIdx, int descIdx, int imgIdx) {
        // guard against short rows
        String id = getSafe(row, idIdx);
        if (id == null || id.isEmpty()) return;
        Product p = new Product();
        p.setItem_id(id);
        p.setTitle(getSafe(row, titleIdx));
        p.setBrand(getSafe(row, brandIdx));
        p.setCategory(getSafe(row, catIdx));
        p.setDescription(getSafe(row, descIdx));
        p.setImage_url(getSafe(row, imgIdx));
        out.add(p);
    }

    private void loadInteractionsCsv(String path) {
        interactionsByUser.clear();
        itemInteractionCounts.clear();

        Path p = Paths.get(path);
        if (!Files.exists(p)) {
            System.out.println("ℹ️ interactions.csv not found at " + path + ", continuing without it.");
            rebuildPopularRanking();
            return;
        }
        long count = 0;
        try (BufferedReader br = Files.newBufferedReader(p)) {
            String headerLine = br.readLine();
            if (headerLine == null) return;
            String[] header = Arrays.stream(headerLine.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .toArray(String[]::new);
            Map<String, Integer> idx = new HashMap<>();
            for (int i = 0; i < header.length; i++) idx.put(header[i], i);

            Integer uIdx = firstNonNullIndex(idx, "user_id", "user", "uid");
            Integer iIdx = firstNonNullIndex(idx, "item_id", "item", "iid");
            if (uIdx == null || iIdx == null) {
                System.out.println("⚠️ interactions.csv missing user_id/item_id columns; detected: " + Arrays.toString(header));
                rebuildPopularRanking();
                return;
            }

            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length <= Math.max(uIdx, iIdx)) continue;
                String uid = parts[uIdx].trim();
                String iid = parts[iIdx].trim();
                if (uid.isEmpty() || iid.isEmpty()) continue;
                interactionsByUser.computeIfAbsent(uid, k -> new ArrayList<>()).add(iid);
                itemInteractionCounts.merge(iid, 1, Integer::sum);
                count++;
            }
            System.out.println("✅ Loaded interactions: " + count + ", users with interactions: " + interactionsByUser.size());
            rebuildPopularRanking();
        } catch (IOException e) {
            System.out.println("⚠️ Failed to read interactions.csv: " + e.getMessage());
            rebuildPopularRanking();
        }
    }

    private void rebuildPopularRanking() {
        if (items == null || items.isEmpty()) {
            rankedPopularItemIds = new ArrayList<>();
            return;
        }

        rankedPopularItemIds = items.stream()
                .map(Product::getItem_id)
                .filter(Objects::nonNull)
                .filter(id -> !id.isBlank())
                .distinct()
                .sorted((a, b) -> {
                    int cb = itemInteractionCounts.getOrDefault(b, 0);
                    int ca = itemInteractionCounts.getOrDefault(a, 0);
                    if (cb != ca) return Integer.compare(cb, ca);
                    return a.compareTo(b);
                })
                .collect(Collectors.toList());
    }

    private Integer firstNonNullIndex(Map<String, Integer> idx, String... keys) {
        for (String k : keys) {
            Integer v = idx.get(k);
            if (v != null) return v;
        }
        return null;
    }

    private String getSafe(String[] arr, Integer i) {
        if (i == null || i < 0 || i >= arr.length) return "";
        return arr[i].trim();
    }

    private int normalizeLimit(int requestedLimit) {
        int effectiveMax = maxLimit > 0 ? maxLimit : DEFAULT_LIMIT;
        if (requestedLimit <= 0) return DEFAULT_LIMIT;
        return Math.min(requestedLimit, effectiveMax);
    }

    private String resolveArtifactPath(String fileName) {
        if (artifactsBaseDir == null) {
            artifactsBaseDir = resolveArtifactsBaseDir();
        }
        return artifactsBaseDir.resolve(fileName).toString();
    }

    private Path resolveArtifactsBaseDir() {
        List<Path> candidates = List.of(
                Paths.get(artifactsDir),
                Paths.get("artifacts"),
                Paths.get("../artifacts")
        );

        for (Path candidate : candidates) {
            Path normalized = candidate.normalize();
            if (Files.exists(normalized.resolve("mappings.json"))) {
                return normalized;
            }
        }

        return Paths.get(artifactsDir).normalize();
    }

    public int getUserCount() { return user2idx != null ? user2idx.size() : 0; }
    public int getItemCount() { return item2idx != null ? item2idx.size() : 0; }
    public int getInteractedUserCount() { return interactionsByUser != null ? interactionsByUser.size() : 0; }
    public double getHybridWCF() { return hybridWCF; }
    public double getHybridWContent() { return hybridWContent; }

    public Map<String, Object> getHealth() {
        Map<String, Object> health = new LinkedHashMap<>();
        try {
            health.put("status", "UP");
            health.put("users", getUserCount());
            health.put("items", getItemCount());
            health.put("interactionsUsers", getInteractedUserCount());
            health.put("hybridWeights", Map.of("cf", getHybridWCF(), "content", getHybridWContent()));
        } catch (Exception e) {
            health.put("status", "ERROR");
            health.put("message", e.getMessage());
        }
        return health;
    }
}
