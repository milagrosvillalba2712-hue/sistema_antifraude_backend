package com.antifraude.drools.similarity;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Medidas de similitud entre nombres para screening difuso (fuzzy matching).
 * <p>
 * Algoritmo híbrido: Sørensen–Dice (bigramas) ponderado 0.6 + ratio de
 * Levenshtein ponderado 0.4, sobre valores normalizados. Sin dependencias
 * externas (implementación propia).
 */
public final class NameSimilarity {

    private static final double DICE_WEIGHT = 0.6;
    private static final double LEVENSHTEIN_WEIGHT = 0.4;

    private NameSimilarity() {
    }

    /**
     * Similitud 0–100 entre dos nombres. Normaliza y compara; si las
     * formas normalizadas son idénticas devuelve 100.
     */
    public static double similarity(String a, String b) {
        String na = normalize(a);
        String nb = normalize(b);
        if (na == null || nb == null) {
            return 0.0;
        }
        if (na.equals(nb)) {
            return 100.0;
        }
        if (na.isEmpty() || nb.isEmpty()) {
            return 0.0;
        }
        double dice = diceCoefficient(na, nb);
        double levRatio = levenshteinRatio(na, nb);
        return DICE_WEIGHT * dice + LEVENSHTEIN_WEIGHT * levRatio;
    }

    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^\\p{Alnum}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    /** Sørensen–Dice sobre bigramas: 2*|A∩B| / (|A|+|B|), en 0–100. */
    static double diceCoefficient(String a, String b) {
        Set<String> bigramsA = bigrams(a);
        Set<String> bigramsB = bigrams(b);
        if (bigramsA.isEmpty() || bigramsB.isEmpty()) {
            return 0.0;
        }
        int intersection = 0;
        for (String bg : bigramsA) {
            if (bigramsB.contains(bg)) {
                intersection++;
            }
        }
        return (2.0 * intersection) / (bigramsA.size() + bigramsB.size()) * 100.0;
    }

    /** Ratio de similitud basado en distancia de Levenshtein, en 0–100. */
    static double levenshteinRatio(String a, String b) {
        int dist = levenshtein(a, b);
        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) {
            return 100.0;
        }
        return (1.0 - (double) dist / maxLen) * 100.0;
    }

    static int levenshtein(String a, String b) {
        int la = a.length();
        int lb = b.length();
        int[] prev = new int[lb + 1];
        int[] curr = new int[lb + 1];
        for (int j = 0; j <= lb; j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= la; i++) {
            curr[0] = i;
            for (int j = 1; j <= lb; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[lb];
    }

    static Set<String> bigrams(String s) {
        Set<String> result = new HashSet<>();
        for (int i = 0; i < s.length() - 1; i++) {
            result.add(s.substring(i, i + 2));
        }
        return result;
    }
}
