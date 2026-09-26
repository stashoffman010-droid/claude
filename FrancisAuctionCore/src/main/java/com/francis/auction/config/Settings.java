package com.francis.auction.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/** Typed, validated view over config.yml. */
public final class Settings {

    /** An icon and the slot it occupies on the navigation bar. */
    public record Button(Material icon, int slot) {
    }

    private static final long DEFAULT_DURATION = 48L * 60L * 60L * 1000L;

    private final int maxListingsPerPlayer;
    private final long listingDuration;
    private final long expiredRetention;
    private final double minPrice;
    private final double maxPrice;
    private final double salesTaxPercent;
    private final int rows;
    private final String clickSound;
    private final Material filler;
    private final Map<String, Button> buttons;
    private final Map<String, Material> dialogIcons;
    private final long autosaveIntervalSeconds;

    public Settings(FileConfiguration config, Logger logger) {
        this.maxListingsPerPlayer = Math.max(1, config.getInt("auction.max-listings-per-player", 10));
        this.listingDuration = com.francis.auction.util.Durations
                .parse(config.getString("auction.listing-duration"), DEFAULT_DURATION);
        this.expiredRetention = com.francis.auction.util.Durations
                .parse(config.getString("auction.expired-retention"), 0L);
        this.minPrice = Math.max(0.01D, config.getDouble("auction.min-price", 1D));
        this.maxPrice = Math.max(minPrice, config.getDouble("auction.max-price", 1_000_000_000D));
        this.salesTaxPercent = Math.min(100D, Math.max(0D, config.getDouble("auction.sales-tax-percent", 0D)));
        this.rows = Math.min(6, Math.max(2, config.getInt("gui.rows", 6)));
        String sound = config.getString("gui.click-sound", "");
        this.clickSound = sound == null || sound.isBlank() ? null : sound;
        this.filler = material(config.getString("gui.filler"), Material.BLACK_STAINED_GLASS_PANE, logger, "gui.filler");
        this.buttons = readButtons(config.getConfigurationSection("gui.buttons"), logger);
        this.dialogIcons = readDialogIcons(config.getConfigurationSection("gui.dialog"), logger);
        this.autosaveIntervalSeconds = Math.max(0L, config.getLong("storage.autosave-interval", 300L));
    }

    public int maxListingsPerPlayer() {
        return maxListingsPerPlayer;
    }

    public long listingDuration() {
        return listingDuration;
    }

    /** {@code 0} means expired items are kept until the owner collects them. */
    public long expiredRetention() {
        return expiredRetention;
    }

    public double minPrice() {
        return minPrice;
    }

    public double maxPrice() {
        return maxPrice;
    }

    /** The share of a sale withheld from the seller, as a fraction of 1. */
    public double salesTaxFraction() {
        return salesTaxPercent / 100D;
    }

    public int rows() {
        return rows;
    }

    public int menuSize() {
        return rows * 9;
    }

    /** Slots available for listings; the final row is the navigation bar. */
    public int itemsPerPage() {
        return (rows - 1) * 9;
    }

    public String clickSound() {
        return clickSound;
    }

    public Material filler() {
        return filler;
    }

    /** @return the button configured under {@code gui.buttons.<key>}, or {@code null}. */
    public Button button(String key) {
        return buttons.get(key);
    }

    public Material dialogIcon(String key, Material fallback) {
        return dialogIcons.getOrDefault(key, fallback);
    }

    public long autosaveIntervalSeconds() {
        return autosaveIntervalSeconds;
    }

    private Map<String, Button> readButtons(ConfigurationSection section, Logger logger) {
        Map<String, Button> found = new LinkedHashMap<>();
        if (section == null) {
            logger.warning("config.yml has no gui.buttons section; the navigation bar will be empty.");
            return found;
        }
        int lastRowStart = menuSize() - 9;
        for (String key : section.getKeys(false)) {
            ConfigurationSection button = section.getConfigurationSection(key);
            if (button == null) {
                continue;
            }
            Material icon = material(button.getString("icon"), null, logger, "gui.buttons." + key + ".icon");
            int slot = button.getInt("slot", -1);
            if (icon == null) {
                continue;
            }
            if (slot < lastRowStart || slot >= menuSize()) {
                logger.warning("gui.buttons." + key + ".slot must be between " + lastRowStart
                        + " and " + (menuSize() - 1) + "; the button was skipped.");
                continue;
            }
            found.put(key, new Button(icon, slot));
        }
        return found;
    }

    private Map<String, Material> readDialogIcons(ConfigurationSection section, Logger logger) {
        Map<String, Material> found = new LinkedHashMap<>();
        if (section == null) {
            return found;
        }
        for (String key : section.getKeys(false)) {
            Material icon = material(section.getString(key), null, logger, "gui.dialog." + key);
            if (icon != null) {
                found.put(key, icon);
            }
        }
        return found;
    }

    private static Material material(String name, Material fallback, Logger logger, String path) {
        Material resolved = name == null ? null : Material.matchMaterial(name.trim().toUpperCase(java.util.Locale.ROOT));
        if (resolved == null) {
            logger.warning("Unknown material '" + name + "' at " + path
                    + (fallback == null ? "; the entry was skipped." : "; falling back to " + fallback + "."));
            return fallback;
        }
        return resolved;
    }
}
