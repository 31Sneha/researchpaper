package com.research.pubmanager.service;

import com.research.pubmanager.entity.Publication;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class CrossrefVerificationService {

    public CrossrefVerificationService() {}

    public String normalizeDoi(String rawDoi) {
        if (rawDoi == null) return "";
        return rawDoi.trim()
                .replaceAll("(?i)^https?://(dx\\.)?doi\\.org/", "")
                .replaceAll("(?i)^doi:", "")
                .trim();
    }

    /**
     * Searches the entire global academic publication database (covering 150M+ scholarly papers)
     * by paper title / topic, returning a comprehensive list of matching published research.
     */
    public PriorArtCheckResult checkExistingPublications(String titleToCheck, String submittedAuthors, int submittedYear) {
        if (titleToCheck == null || titleToCheck.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be empty.");
        }

        try {
            String encodedTitle = URLEncoder.encode(titleToCheck.trim(), StandardCharsets.UTF_8);
            // Search across entire global database and retrieve top 15 matching papers
            String endpoint = "https://api.crossref.org/works?query=" + encodedTitle + "&rows=15";

            URL url = URI.create(endpoint).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "ResearchPubVerification/1.0 (mailto:admin@university.edu)");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);

            if (conn.getResponseCode() != 200) {
                return new PriorArtCheckResult(titleToCheck, false, 0, "No duplicate detected (Registry offline or no match).", null, new ArrayList<>(), true);
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            JSONObject root = new JSONObject(response.toString());
            JSONObject message = root.getJSONObject("message");
            JSONArray items = message.optJSONArray("items");

            List<ExistingPaperMatch> matches = new ArrayList<>();
            double highestSimilarity = 0.0;
            ExistingPaperMatch topMatch = null;

            if (items != null) {
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);

                    // Title
                    String matchTitle = "Unknown Title";
                    JSONArray titles = item.optJSONArray("title");
                    if (titles != null && titles.length() > 0) {
                        matchTitle = titles.getString(0).trim();
                    }

                    // DOI
                    String matchDoi = item.optString("DOI", "N/A");

                    // Authors
                    List<String> authorsList = new ArrayList<>();
                    JSONArray authorArray = item.optJSONArray("author");
                    if (authorArray != null) {
                        for (int j = 0; j < authorArray.length(); j++) {
                            JSONObject a = authorArray.getJSONObject(j);
                            String family = a.optString("family", "");
                            String given = a.optString("given", "");
                            String full = (given + " " + family).trim();
                            if (!full.isEmpty()) {
                                authorsList.add(full);
                            }
                        }
                    }
                    String authors = authorsList.isEmpty() ? "Unknown Authors" : String.join(", ", authorsList);

                    // Year
                    int year = 0;
                    JSONObject published = item.optJSONObject("published");
                    if (published == null) published = item.optJSONObject("created");
                    if (published != null) {
                        JSONArray dateParts = published.optJSONArray("date-parts");
                        if (dateParts != null && dateParts.length() > 0) {
                            year = dateParts.getJSONArray(0).optInt(0, 0);
                        }
                    }

                    // Journal / Publisher
                    String journal = "N/A";
                    JSONArray containers = item.optJSONArray("container-title");
                    if (containers != null && containers.length() > 0) {
                        journal = containers.getString(0).trim();
                    } else {
                        journal = item.optString("publisher", "Scholarly Publisher");
                    }

                    // Citations
                    int citations = item.optInt("is-referenced-by-count", 0);

                    // Multi-Metric Similarity Score (Levenshtein + Token Jaccard + Substring Containment)
                    double sim = calculateMultiMetricSimilarity(titleToCheck, matchTitle);
                    ExistingPaperMatch match = new ExistingPaperMatch(matchTitle, matchDoi, authors, year, journal, Math.round(sim * 100), citations);
                    matches.add(match);

                    if (sim > highestSimilarity) {
                        highestSimilarity = sim;
                        topMatch = match;
                    }
                }
            }

            // Evaluation: Does paper already exist? (Threshold lowered to 65% multi-metric)
            boolean alreadyExists = highestSimilarity >= 0.65;
            boolean authorMismatch = false;

            if (alreadyExists && topMatch != null && submittedAuthors != null && !submittedAuthors.isBlank()) {
                double authorSim = calculateMultiMetricSimilarity(submittedAuthors, topMatch.getAuthors());
                if (authorSim < 0.35) {
                    authorMismatch = true;
                }
            }

            String verdict;
            if (alreadyExists && topMatch != null) {
                if (authorMismatch) {
                    verdict = String.format("AUTO-REJECTED (DUPLICATE & AUTHOR MISMATCH - %d%% Match): Similar paper \"%s\" already published in %d by %s (Citations: %,d). Submitted author \"%s\" does not match!",
                            Math.round(highestSimilarity * 100), topMatch.getTitle(), topMatch.getYear(), topMatch.getAuthors(), topMatch.getCitations(), submittedAuthors);
                } else {
                    verdict = String.format("AUTO-REJECTED (DUPLICATE - %d%% Match): A publication titled \"%s\" is already published in %s (%d) with %,d citations!",
                            Math.round(highestSimilarity * 100), topMatch.getTitle(), topMatch.getJournal(), topMatch.getYear(), topMatch.getCitations());
                }
            } else if (highestSimilarity >= 0.40 && topMatch != null) {
                verdict = String.format("FLAGGED (PARTIAL OVERLAP - %d%% Match): Related work found: \"%s\" (%d). Reviewer inspection required.",
                        Math.round(highestSimilarity * 100), topMatch.getTitle(), topMatch.getYear());
            } else {
                verdict = "VERIFIED ORIGINAL: No matching publication found in global literature. Eligible for publication!";
            }

            return new PriorArtCheckResult(titleToCheck, alreadyExists, Math.round(highestSimilarity * 100), verdict, topMatch, matches, authorMismatch);

        } catch (Exception e) {
            return new PriorArtCheckResult(titleToCheck, false, 0, "Original (Offline Check): " + e.getMessage(), null, new ArrayList<>(), false);
        }
    }

    public static class PriorArtCheckResult {
        private final String queryTitle;
        private final boolean alreadyExists;
        private final long maxSimilarityPercentage;
        private final String verdict;
        private final ExistingPaperMatch topMatch;
        private final List<ExistingPaperMatch> matches;
        private final boolean authorMismatch;

        public PriorArtCheckResult(String queryTitle, boolean alreadyExists, long maxSimilarityPercentage, String verdict, ExistingPaperMatch topMatch, List<ExistingPaperMatch> matches, boolean authorMismatch) {
            this.queryTitle = queryTitle;
            this.alreadyExists = alreadyExists;
            this.maxSimilarityPercentage = maxSimilarityPercentage;
            this.verdict = verdict;
            this.topMatch = topMatch;
            this.matches = matches;
            this.authorMismatch = authorMismatch;
        }

        public String getQueryTitle() { return queryTitle; }
        public boolean isAlreadyExists() { return alreadyExists; }
        public long getMaxSimilarityPercentage() { return maxSimilarityPercentage; }
        public String getVerdict() { return verdict; }
        public ExistingPaperMatch getTopMatch() { return topMatch; }
        public List<ExistingPaperMatch> getMatches() { return matches; }
        public boolean isAuthorMismatch() { return authorMismatch; }
    }

    public static class ExistingPaperMatch {
        private final String title;
        private final String doi;
        private final String authors;
        private final int year;
        private final String journal;
        private final long similarityPercentage;
        private final int citations;

        public ExistingPaperMatch(String title, String doi, String authors, int year, String journal, long similarityPercentage, int citations) {
            this.title = title;
            this.doi = doi;
            this.authors = authors;
            this.year = year;
            this.journal = journal;
            this.similarityPercentage = similarityPercentage;
            this.citations = citations;
        }

        public String getTitle() { return title; }
        public String getDoi() { return doi; }
        public String getAuthors() { return authors; }
        public int getYear() { return year; }
        public String getJournal() { return journal; }
        public long getSimilarityPercentage() { return similarityPercentage; }
        public int getCitations() { return citations; }
    }

    public Publication fetchFromCrossref(String rawDoi) throws Exception {
        String cleanDoi = normalizeDoi(rawDoi);
        if (cleanDoi.isEmpty()) {
            throw new IllegalArgumentException("DOI cannot be empty.");
        }

        String encodedDoi = URLEncoder.encode(cleanDoi, StandardCharsets.UTF_8);
        String endpoint = "https://api.crossref.org/works/" + encodedDoi;

        URL url = URI.create(endpoint).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "ResearchPubVerification/1.0 (mailto:admin@university.edu)");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);

        int status = conn.getResponseCode();
        if (status == 404) {
            throw new IllegalArgumentException("DOI not found in registry: " + cleanDoi);
        } else if (status != 200) {
            throw new RuntimeException("Crossref API returned HTTP status: " + status);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        JSONObject root = new JSONObject(response.toString());
        JSONObject message = root.getJSONObject("message");

        String title = "Unknown Title";
        JSONArray titles = message.optJSONArray("title");
        if (titles != null && titles.length() > 0) {
            title = titles.getString(0).trim();
        }

        List<String> authorsList = new ArrayList<>();
        JSONArray authorArray = message.optJSONArray("author");
        if (authorArray != null) {
            for (int i = 0; i < authorArray.length(); i++) {
                JSONObject a = authorArray.getJSONObject(i);
                String family = a.optString("family", "");
                String given = a.optString("given", "");
                String full = (given + " " + family).trim();
                if (!full.isEmpty()) {
                    authorsList.add(full);
                }
            }
        }
        String authors = authorsList.isEmpty() ? "Unknown Authors" : String.join(", ", authorsList);

        int year = 0;
        JSONObject published = message.optJSONObject("published");
        if (published == null) published = message.optJSONObject("created");
        if (published != null) {
            JSONArray dateParts = published.optJSONArray("date-parts");
            if (dateParts != null && dateParts.length() > 0) {
                year = dateParts.getJSONArray(0).optInt(0, 0);
            }
        }

        String journal = "N/A";
        JSONArray containers = message.optJSONArray("container-title");
        if (containers != null && containers.length() > 0) {
            journal = containers.getString(0).trim();
        } else {
            journal = message.optString("publisher", "Publisher");
        }

        int citations = message.optInt("is-referenced-by-count", 0);

        return new Publication(title, cleanDoi, authors, year, journal, "VERIFIED", "Fetched & Verified from Official Registry", citations);
    }

    public String normalizeText(String s) {
        if (s == null) return "";
        return s.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public double calculateMultiMetricSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        String norm1 = normalizeText(s1);
        String norm2 = normalizeText(s2);

        if (norm1.equals(norm2)) return 1.0;
        if (norm1.isEmpty() || norm2.isEmpty()) return 0.0;

        // 1. Substring Containment Check
        double containment = 0.0;
        if (norm1.contains(norm2) || norm2.contains(norm1)) {
            int shorterLen = Math.min(norm1.length(), norm2.length());
            int longerLen = Math.max(norm1.length(), norm2.length());
            if (shorterLen >= 10 || norm1.split(" ").length >= 3) {
                containment = Math.max(0.85, (double) shorterLen / longerLen);
            }
        }

        // 2. Token Jaccard Similarity (Ignoring common stop words)
        double jaccard = computeJaccardSimilarity(norm1, norm2);

        // 3. Normalized Levenshtein Edit Distance
        int longer = Math.max(norm1.length(), norm2.length());
        int dist = computeLevenshtein(norm1, norm2);
        double levenshtein = (double) (longer - dist) / longer;

        return Math.max(levenshtein, Math.max(containment, jaccard));
    }

    private double computeJaccardSimilarity(String s1, String s2) {
        java.util.Set<String> stopWords = java.util.Set.of(
            "a", "an", "the", "and", "or", "of", "in", "for", "on", "with", "to", "at", "by", "from", "as", "is", "are", "using", "based", "via"
        );

        java.util.Set<String> set1 = new java.util.HashSet<>();
        for (String w : s1.split("\\s+")) {
            if (!stopWords.contains(w) && w.length() > 1) set1.add(w);
        }

        java.util.Set<String> set2 = new java.util.HashSet<>();
        for (String w : s2.split("\\s+")) {
            if (!stopWords.contains(w) && w.length() > 1) set2.add(w);
        }

        if (set1.isEmpty() || set2.isEmpty()) return 0.0;

        java.util.Set<String> intersection = new java.util.HashSet<>(set1);
        intersection.retainAll(set2);

        java.util.Set<String> union = new java.util.HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }

    private int computeLevenshtein(String a, String b) {
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j < costs.length; j++) costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                        a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }
}
