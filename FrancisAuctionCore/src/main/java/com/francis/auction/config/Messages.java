package com.francis.auction.config;

import com.francis.auction.model.Category;
import com.francis.auction.model.SortOrder;
import com.francis.auction.util.Placeholders;
import com.francis.auction.util.Text;
import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Every player facing string, resolved from messages.yml.
 *
 * <p>Lookups never throw: a missing path yields the path itself, which makes a
 * typo obvious in game instead of breaking the menu that needed it.
 */
public final class Messages {

    private final FileConfiguration config;
    private final String prefix;

    public Messages(FileConfiguration config) {
        this.config = config;
        this.prefix = Text.color(config.getString("prefix", ""));
    }

    /** A single line, with placeholders applied and colours translated. */
    public String get(String path, Map<String, String> placeholders) {
        String raw = config.getString(path);
        return raw == null ? path : Text.color(Text.fill(raw, placeholders));
    }

    public String get(String path) {
        return get(path, Placeholders.none());
    }

    /** The same as {@link #get}, with the configured chat prefix in front. */
    public String prefixed(String path, Map<String, String> placeholders) {
        return prefix + get(path, placeholders);
    }

    /** The untranslated lines at {@code path}; the caller applies placeholders. */
    public List<String> rawLines(String path) {
        return config.isList(path) ? config.getStringList(path) : List.of();
    }

    /** The untranslated single line at {@code path}, or {@code null}. */
    public String rawLine(String path) {
        return config.getString(path);
    }

    /** Sends a prefixed chat message; blank messages are skipped. */
    public void send(CommandSender target, String path, Map<String, String> placeholders) {
        String message = get(path, placeholders);
        if (!message.isBlank()) {
            target.sendMessage(prefix + message);
        }
    }

    public void send(CommandSender target, String path) {
        send(target, path, Placeholders.none());
    }

    public String title(String key, Map<String, String> placeholders) {
        return get("titles." + key, placeholders);
    }

    public String title(String key) {
        return title(key, Placeholders.none());
    }

    public String category(Category category) {
        return config.getString("categories." + category.name(), category.name());
    }

    public String sortOrder(SortOrder order) {
        return config.getString("sort-orders." + order.name(), order.name());
    }

    /** The {@code time:} patterns handed to {@link com.francis.auction.util.Durations}. */
    public Map<String, String> timeUnits() {
        ConfigurationSection section = config.getConfigurationSection("time");
        if (section == null) {
            return Map.of("days", "{days}d", "hours", "{hours}h",
                    "minutes", "{minutes}m", "seconds", "{seconds}s");
        }
        return Map.of(
                "days", section.getString("days", "{days}d"),
                "hours", section.getString("hours", "{hours}h"),
                "minutes", section.getString("minutes", "{minutes}m"),
                "seconds", section.getString("seconds", "{seconds}s"));
    }

    public String expiredLabel() {
        return config.getString("time.expired", "expired");
    }
}
