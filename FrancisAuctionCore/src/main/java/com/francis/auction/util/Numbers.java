package com.francis.auction.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.OptionalDouble;

/** Price parsing and display formatting. */
public final class Numbers {

    private static final DecimalFormat PLAIN =
            new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));
    private static final String[] SUFFIXES = {"", "K", "M", "B", "T"};

    private Numbers() {
    }

    /**
     * Parses a price. Accepts plain numbers, thousands separators and the
     * shorthand suffixes {@code k}, {@code m}, {@code b} and {@code t}.
     */
    public static OptionalDouble parsePrice(String input) {
        if (input == null || input.isBlank()) {
            return OptionalDouble.empty();
        }
        String cleaned = input.trim().replace(",", "").replace("$", "");
        double multiplier = 1D;
        char last = Character.toLowerCase(cleaned.charAt(cleaned.length() - 1));
        int suffix = "kmbt".indexOf(last);
        if (suffix > -1) {
            multiplier = Math.pow(1000D, suffix + 1D);
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        try {
            double value = Double.parseDouble(cleaned) * multiplier;
            if (!Double.isFinite(value) || value <= 0D) {
                return OptionalDouble.empty();
            }
            return OptionalDouble.of(round(value));
        } catch (NumberFormatException expected) {
            return OptionalDouble.empty();
        }
    }

    /** Rounds to two decimal places, the precision economies work with. */
    public static double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /** {@code 1234.5} becomes {@code 1,234.5}. */
    public static String format(double value) {
        synchronized (PLAIN) {
            return PLAIN.format(value);
        }
    }

    /** {@code 1234567} becomes {@code 1.23M}; used where space is tight. */
    public static String compact(double value) {
        double remaining = value;
        int tier = 0;
        while (remaining >= 1000D && tier < SUFFIXES.length - 1) {
            remaining /= 1000D;
            tier++;
        }
        return tier == 0 ? format(remaining) : format(round(remaining)) + SUFFIXES[tier];
    }
}
