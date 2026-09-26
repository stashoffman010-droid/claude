package com.francis.auction.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Colour code translation for every string that comes out of a configuration
 * file.
 *
 * <p>Both the legacy {@code &a} codes and the hex codes {@code &#RRGGBB} /
 * {@code <#RRGGBB>} are understood. A hex colour resets formatting on the
 * client, so {@code &l} has to be written <em>after</em> the hex code.
 */
public final class Text {

    private static final char SECTION = '§';
    private static final Pattern HEX = Pattern.compile("(?i)(?:&#|<#)([0-9a-f]{6})>?");
    private static final String LEGACY_CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx";
    private static final Pattern STRIP = Pattern.compile("(?i)" + SECTION + "[0-9A-FK-ORX]");

    private Text() {
    }

    /** Translates every supported colour code in {@code input}. */
    public static String color(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(input.length() + 16);
        Matcher matcher = HEX.matcher(input);
        int cursor = 0;
        while (matcher.find()) {
            out.append(legacy(input.substring(cursor, matcher.start())));
            out.append(hex(matcher.group(1)));
            cursor = matcher.end();
        }
        return out.append(legacy(input.substring(cursor))).toString();
    }

    /** Translates every line of {@code input}, preserving order. */
    public static List<String> color(List<String> input) {
        List<String> out = new ArrayList<>(input.size());
        for (String line : input) {
            out.add(color(line));
        }
        return out;
    }

    /** Removes every colour code, leaving readable plain text. */
    public static String strip(String input) {
        return input == null ? "" : STRIP.matcher(color(input)).replaceAll("");
    }

    /** Replaces every {@code {key}} in {@code template} with its value. */
    public static String fill(String template, Map<String, String> placeholders) {
        if (template == null || template.isEmpty() || placeholders.isEmpty()) {
            return template == null ? "" : template;
        }
        String out = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            out = out.replace('{' + entry.getKey() + '}', entry.getValue());
        }
        return out;
    }

    /** Replaces every {@code {key}} in each line of {@code template}. */
    public static List<String> fill(List<String> template, Map<String, String> placeholders) {
        List<String> out = new ArrayList<>(template.size());
        for (String line : template) {
            out.add(fill(line, placeholders));
        }
        return out;
    }

    private static String legacy(String input) {
        if (input.isEmpty()) {
            return input;
        }
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&' && LEGACY_CODES.indexOf(chars[i + 1]) > -1) {
                chars[i] = SECTION;
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }

    /** Builds the {@code §x§R§R§G§G§B§B} sequence the client expects. */
    private static String hex(String rrggbb) {
        StringBuilder out = new StringBuilder(14).append(SECTION).append('x');
        for (char c : rrggbb.toCharArray()) {
            out.append(SECTION).append(Character.toLowerCase(c));
        }
        return out.toString();
    }
}
