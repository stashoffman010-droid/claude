package com.francis.auction.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parsing and rendering of the human readable durations used in the configs. */
public final class Durations {

    private static final Pattern TOKEN = Pattern.compile("(?i)(\\d+)\\s*([dhms])");

    private Durations() {
    }

    /**
     * Parses a duration such as {@code 48h}, {@code 7d} or {@code 1h30m}.
     *
     * @return the duration in milliseconds, or {@code fallback} when nothing
     *         could be parsed.
     */
    public static long parse(String input, long fallback) {
        if (input == null || input.isBlank()) {
            return fallback;
        }
        Matcher matcher = TOKEN.matcher(input.trim());
        long total = 0L;
        boolean found = false;
        while (matcher.find()) {
            long amount = Long.parseLong(matcher.group(1));
            total += switch (Character.toLowerCase(matcher.group(2).charAt(0))) {
                case 'd' -> TimeUnit.DAYS.toMillis(amount);
                case 'h' -> TimeUnit.HOURS.toMillis(amount);
                case 'm' -> TimeUnit.MINUTES.toMillis(amount);
                default -> TimeUnit.SECONDS.toMillis(amount);
            };
            found = true;
        }
        return found ? total : fallback;
    }

    /**
     * Renders {@code millis} as at most two units, largest first, using the
     * patterns configured under {@code time:} in messages.yml.
     *
     * @param units the {@code days}/{@code hours}/{@code minutes}/{@code seconds}
     *              patterns, each containing the matching placeholder.
     */
    public static String format(long millis, Map<String, String> units, String expiredLabel) {
        if (millis <= 0L) {
            return expiredLabel;
        }
        Map<String, Long> parts = new LinkedHashMap<>();
        parts.put("days", TimeUnit.MILLISECONDS.toDays(millis));
        parts.put("hours", TimeUnit.MILLISECONDS.toHours(millis) % 24);
        parts.put("minutes", TimeUnit.MILLISECONDS.toMinutes(millis) % 60);
        parts.put("seconds", TimeUnit.MILLISECONDS.toSeconds(millis) % 60);

        StringBuilder out = new StringBuilder();
        int rendered = 0;
        for (Map.Entry<String, Long> part : parts.entrySet()) {
            if (part.getValue() <= 0L || rendered == 2) {
                continue;
            }
            String pattern = units.get(part.getKey());
            if (pattern == null) {
                continue;
            }
            if (rendered > 0) {
                out.append(' ');
            }
            out.append(pattern.replace('{' + part.getKey() + '}', String.valueOf(part.getValue())));
            rendered++;
        }
        return rendered == 0 ? expiredLabel : out.toString();
    }
}
