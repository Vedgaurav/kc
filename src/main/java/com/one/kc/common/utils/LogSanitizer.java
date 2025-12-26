package com.one.kc.common.utils;

import java.util.*;
import java.util.regex.Pattern;

public final class LogSanitizer {

    private static final String MASK = "****";

    // Control characters & log forging protection
    private static final Pattern CONTROL_CHARS =
            Pattern.compile("[\n\r\t\b\f]");

    // Common sensitive keywords (extend as needed)
    private static final Pattern SENSITIVE_KEYS =
            Pattern.compile("(?i)(password|passwd|pwd|secret|token|apikey|api_key|authorization)");

    private LogSanitizer() {
        // Utility class
    }

    /* ===========================
       Public APIs
       =========================== */

    public static String sanitizeMessage(String message) {
        if (message == null) {
            return null;
        }
        return removeControlChars(message);
    }

    public static Object sanitizeParam(Object param) {
        if (param == null) {
            return null;
        }

        if (param instanceof String) {
            return sanitizeString((String) param);
        }

        if (param instanceof Map<?, ?>) {
            return sanitizeMap((Map<?, ?>) param);
        }

        if (param instanceof Collection<?>) {
            return sanitizeCollection((Collection<?>) param);
        }

        return param; // primitives / safe objects
    }

    /* ===========================
       Internal Helpers
       =========================== */

    private static String sanitizeString(String value) {
        String sanitized = removeControlChars(value);

        // Mask sensitive key=value patterns
        sanitized = sanitized.replaceAll(
                "(?i)(password|token|secret|apikey|authorization)\\s*=\\s*[^,\\s]+",
                "$1=" + MASK
        );

        return sanitized;
    }

    private static String removeControlChars(String value) {
        return CONTROL_CHARS.matcher(value).replaceAll("_");
    }

    private static Map<String, Object> sanitizeMap(Map<?, ?> map) {
        Map<String, Object> sanitized = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());

            Object value;
            if (SENSITIVE_KEYS.matcher(key).find()) {
                value = MASK;
            } else {
                value = sanitizeParam(entry.getValue());
            }

            sanitized.put(key, value);
        }
        return sanitized;
    }


    private static Collection<Object> sanitizeCollection(Collection<?> collection) {
        Collection<Object> sanitized =
                (collection instanceof List) ? new ArrayList<>() : new LinkedHashSet<>();

        for (Object item : collection) {
            sanitized.add(sanitizeParam(item));
        }
        return sanitized;
    }
}

