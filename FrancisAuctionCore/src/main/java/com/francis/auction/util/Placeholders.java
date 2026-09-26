package com.francis.auction.util;

import java.util.LinkedHashMap;
import java.util.Map;

/** Compact construction of the placeholder maps the message layer expects. */
public final class Placeholders {

    private Placeholders() {
    }

    /** {@code of("price", 10, "seller", name)} builds {@code {price}}/{@code {seller}}. */
    public static Map<String, String> of(Object... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Placeholders.of needs an even number of arguments");
        }
        Map<String, String> map = new LinkedHashMap<>(keyValuePairs.length / 2);
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            map.put(String.valueOf(keyValuePairs[i]), String.valueOf(keyValuePairs[i + 1]));
        }
        return map;
    }

    public static Map<String, String> none() {
        return Map.of();
    }
}
