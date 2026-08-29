package com.antifraude.licensing.catalog;

import java.util.Map;

/** Utilidad menor para extrer valores de los items JSON (esquema convencional/free-form del CP). */
final class Str {

    private Str() {
    }

    static String code(Map<String, Object> item, String... keys) {
        for (String key : keys) {
            Object value = item.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value).trim();
            }
        }
        return null;
    }

    static String string(Map<String, Object> item, String... keys) {
        String value = code(item, keys);
        return value == null ? null : value;
    }
}
