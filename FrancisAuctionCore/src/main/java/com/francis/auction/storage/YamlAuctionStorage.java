package com.francis.auction.storage;

import com.francis.auction.model.ExpiredItem;
import com.francis.auction.model.Listing;
import com.francis.auction.model.TransactionRecord;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Stores the market in a single YAML file.
 *
 * <p>Item stacks go through Bukkit's own serialisation, so enchantments,
 * custom names, NBT backed components and durability all survive a restart
 * without any hand written codec.
 */
public final class YamlAuctionStorage implements AuctionStorage {

    private final Plugin plugin;
    private final Logger logger;
    private final File file;

    public YamlAuctionStorage(Plugin plugin, File file) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.file = file;
    }

    @Override
    public Snapshot load() {
        if (!file.isFile()) {
            return Snapshot.empty();
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        return new Snapshot(
                readListings(yaml.getConfigurationSection("listings")),
                readExpired(yaml.getConfigurationSection("expired")),
                readTransactions(yaml.getConfigurationSection("transactions")),
                readNotices(yaml.getConfigurationSection("notices")));
    }

    @Override
    public void saveAsync(Snapshot snapshot) {
        String contents = serialise(snapshot);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> write(contents));
    }

    @Override
    public void saveNow(Snapshot snapshot) {
        write(serialise(snapshot));
    }

    /** Runs on the server thread: item stacks are only touched here. */
    private String serialise(Snapshot snapshot) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Listing listing : snapshot.listings()) {
            ConfigurationSection section = yaml.createSection("listings." + listing.id());
            section.set("seller-id", listing.sellerId().toString());
            section.set("seller-name", listing.sellerName());
            section.set("item", listing.item());
            section.set("price", listing.price());
            section.set("created-at", listing.createdAt());
            section.set("expires-at", listing.expiresAt());
        }
        for (ExpiredItem item : snapshot.expired()) {
            ConfigurationSection section = yaml.createSection("expired." + item.id());
            section.set("owner-id", item.ownerId().toString());
            section.set("item", item.item());
            section.set("expired-at", item.expiredAt());
        }
        for (TransactionRecord record : snapshot.transactions()) {
            ConfigurationSection section = yaml.createSection("transactions." + record.id());
            section.set("buyer-id", record.buyerId().toString());
            section.set("buyer-name", record.buyerName());
            section.set("seller-id", record.sellerId().toString());
            section.set("seller-name", record.sellerName());
            section.set("item", record.item());
            section.set("price", record.price());
            section.set("timestamp", record.timestamp());
        }
        for (Map.Entry<UUID, List<String>> entry : snapshot.notices().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                yaml.set("notices." + entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }
        return yaml.saveToString();
    }

    /** Writes through a temporary file so a crash cannot truncate the market. */
    private void write(String contents) {
        try {
            Path target = file.toPath();
            Files.createDirectories(target.getParent());
            Path temporary = target.resolveSibling(file.getName() + ".tmp");
            Files.writeString(temporary, contents, StandardCharsets.UTF_8);
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException failure) {
            logger.log(Level.SEVERE, "Could not write " + file.getName(), failure);
        }
    }

    private List<Listing> readListings(ConfigurationSection root) {
        List<Listing> found = new ArrayList<>();
        if (root == null) {
            return found;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            UUID id = parseId(key);
            if (section == null || id == null) {
                continue;
            }
            UUID sellerId = parseId(section.getString("seller-id"));
            ItemStack item = section.getItemStack("item");
            if (sellerId == null || item == null) {
                logger.warning("Skipping malformed listing " + key + " in " + file.getName() + ".");
                continue;
            }
            found.add(new Listing(id, sellerId, section.getString("seller-name", "Unknown"), item,
                    section.getDouble("price"), section.getLong("created-at"), section.getLong("expires-at")));
        }
        return found;
    }

    private List<ExpiredItem> readExpired(ConfigurationSection root) {
        List<ExpiredItem> found = new ArrayList<>();
        if (root == null) {
            return found;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            UUID id = parseId(key);
            if (section == null || id == null) {
                continue;
            }
            UUID ownerId = parseId(section.getString("owner-id"));
            ItemStack item = section.getItemStack("item");
            if (ownerId == null || item == null) {
                logger.warning("Skipping malformed expired item " + key + " in " + file.getName() + ".");
                continue;
            }
            found.add(new ExpiredItem(id, ownerId, item, section.getLong("expired-at")));
        }
        return found;
    }

    private List<TransactionRecord> readTransactions(ConfigurationSection root) {
        List<TransactionRecord> found = new ArrayList<>();
        if (root == null) {
            return found;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            UUID id = parseId(key);
            if (section == null || id == null) {
                continue;
            }
            UUID buyerId = parseId(section.getString("buyer-id"));
            UUID sellerId = parseId(section.getString("seller-id"));
            ItemStack item = section.getItemStack("item");
            if (buyerId == null || sellerId == null || item == null) {
                logger.warning("Skipping malformed transaction " + key + " in " + file.getName() + ".");
                continue;
            }
            found.add(new TransactionRecord(id, buyerId, section.getString("buyer-name", "Unknown"),
                    sellerId, section.getString("seller-name", "Unknown"), item,
                    section.getDouble("price"), section.getLong("timestamp")));
        }
        return found;
    }

    private Map<UUID, List<String>> readNotices(ConfigurationSection root) {
        Map<UUID, List<String>> found = new LinkedHashMap<>();
        if (root == null) {
            return found;
        }
        for (String key : root.getKeys(false)) {
            UUID playerId = parseId(key);
            List<String> queued = root.getStringList(key);
            if (playerId != null && !queued.isEmpty()) {
                found.put(playerId, new java.util.concurrent.CopyOnWriteArrayList<>(queued));
            }
        }
        return found;
    }

    private static UUID parseId(String raw) {
        try {
            return raw == null ? null : UUID.fromString(raw);
        } catch (IllegalArgumentException malformed) {
            return null;
        }
    }
}
