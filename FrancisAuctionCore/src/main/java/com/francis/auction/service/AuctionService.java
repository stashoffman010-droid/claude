package com.francis.auction.service;

import com.francis.auction.config.Messages;
import com.francis.auction.config.Settings;
import com.francis.auction.economy.EconomyHook;
import com.francis.auction.model.Category;
import com.francis.auction.model.ExpiredItem;
import com.francis.auction.model.Listing;
import com.francis.auction.model.SortOrder;
import com.francis.auction.model.TransactionRecord;
import com.francis.auction.storage.AuctionStorage;
import com.francis.auction.util.Numbers;
import com.francis.auction.util.Placeholders;
import com.francis.auction.util.Text;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * The market itself: every rule about listing, buying, cancelling and
 * expiring lives here, and nothing in this class touches a menu.
 */
public final class AuctionService {

    /** Why {@link #createListing} did or did not put an item on the market. */
    public enum ListOutcome {
        SUCCESS, NO_ITEM, LIMIT_REACHED, PRICE_TOO_LOW, PRICE_TOO_HIGH
    }

    /** Why {@link #purchase} did or did not complete a sale. */
    public enum PurchaseOutcome {
        SUCCESS, NOT_FOUND, OWN_LISTING, NO_ECONOMY, NOT_ENOUGH_MONEY, INVENTORY_FULL
    }

    /** Total trades kept on disk before the oldest are dropped. */
    private static final int TRANSACTION_CAPACITY = 5_000;

    private final Map<UUID, Listing> listings = new ConcurrentHashMap<>();
    private final Map<UUID, ExpiredItem> expired = new ConcurrentHashMap<>();
    private final Map<UUID, TransactionRecord> transactions = new ConcurrentHashMap<>();
    /** Sale notices for sellers who were offline, delivered on their next join. */
    private final Map<UUID, List<String>> pendingNotices = new ConcurrentHashMap<>();

    private final Supplier<Settings> settings;
    private final Supplier<Messages> messages;
    private final EconomyHook economy;
    private final AuctionStorage storage;

    private volatile boolean dirty;

    public AuctionService(Supplier<Settings> settings,
                          Supplier<Messages> messages,
                          EconomyHook economy,
                          AuctionStorage storage) {
        this.settings = settings;
        this.messages = messages;
        this.economy = economy;
        this.storage = storage;
    }

    // ---------------------------------------------------------------- loading

    public void load() {
        AuctionStorage.Snapshot snapshot = storage.load();
        listings.clear();
        expired.clear();
        transactions.clear();
        pendingNotices.clear();
        snapshot.listings().forEach(listing -> listings.put(listing.id(), listing));
        snapshot.expired().forEach(item -> expired.put(item.id(), item));
        snapshot.transactions().forEach(record -> transactions.put(record.id(), record));
        pendingNotices.putAll(snapshot.notices());
        dirty = false;
    }

    /** Writes only when something changed since the last save. */
    public void saveIfDirty() {
        if (dirty) {
            dirty = false;
            storage.saveAsync(snapshot());
        }
    }

    public void saveNow() {
        dirty = false;
        storage.saveNow(snapshot());
    }

    private AuctionStorage.Snapshot snapshot() {
        return new AuctionStorage.Snapshot(
                List.copyOf(listings.values()),
                List.copyOf(expired.values()),
                List.copyOf(transactions.values()),
                Map.copyOf(pendingNotices));
    }

    // ---------------------------------------------------------------- queries

    /** The market page, filtered and ordered the way the viewer asked for. */
    public List<Listing> browse(Category category, SortOrder order, String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Listing> results = new ArrayList<>();
        for (Listing listing : listings.values()) {
            if (!category.matches(listing.item())) {
                continue;
            }
            if (!needle.isEmpty() && !displayName(listing.item()).toLowerCase(Locale.ROOT).contains(needle)) {
                continue;
            }
            results.add(listing);
        }
        results.sort(order.comparator());
        return results;
    }

    public List<Listing> listingsOf(UUID playerId) {
        List<Listing> owned = new ArrayList<>();
        for (Listing listing : listings.values()) {
            if (listing.isOwnedBy(playerId)) {
                owned.add(listing);
            }
        }
        owned.sort(SortOrder.NEWEST.comparator());
        return owned;
    }

    public int listingCount(UUID playerId) {
        int count = 0;
        for (Listing listing : listings.values()) {
            if (listing.isOwnedBy(playerId)) {
                count++;
            }
        }
        return count;
    }

    public List<ExpiredItem> expiredOf(UUID playerId) {
        List<ExpiredItem> owned = new ArrayList<>();
        for (ExpiredItem item : expired.values()) {
            if (item.ownerId().equals(playerId)) {
                owned.add(item);
            }
        }
        owned.sort(Comparator.comparingLong(ExpiredItem::expiredAt).reversed());
        return owned;
    }

    public int expiredCount(UUID playerId) {
        return expiredOf(playerId).size();
    }

    public List<TransactionRecord> transactionsOf(UUID playerId) {
        List<TransactionRecord> involved = new ArrayList<>();
        for (TransactionRecord record : transactions.values()) {
            if (record.buyerId().equals(playerId) || record.sellerId().equals(playerId)) {
                involved.add(record);
            }
        }
        involved.sort(Comparator.comparingLong(TransactionRecord::timestamp).reversed());
        return involved;
    }

    public Optional<Listing> listing(UUID id) {
        return Optional.ofNullable(listings.get(id));
    }

    // --------------------------------------------------------------- mutation

    /**
     * Moves the stack in the seller's main hand onto the market.
     *
     * <p>The item only leaves the hand once every rule has passed, and it is
     * taken from that one slot rather than matched across the inventory.
     */
    public ListOutcome createListing(Player seller, double price) {
        ItemStack held = seller.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) {
            return ListOutcome.NO_ITEM;
        }
        Settings current = settings.get();
        if (listingCount(seller.getUniqueId()) >= current.maxListingsPerPlayer()) {
            return ListOutcome.LIMIT_REACHED;
        }
        if (price < current.minPrice()) {
            return ListOutcome.PRICE_TOO_LOW;
        }
        if (price > current.maxPrice()) {
            return ListOutcome.PRICE_TOO_HIGH;
        }

        ItemStack listed = held.clone();
        seller.getInventory().setItemInMainHand(null);

        long now = System.currentTimeMillis();
        Listing listing = new Listing(UUID.randomUUID(), seller.getUniqueId(), seller.getName(),
                listed, Numbers.round(price), now, now + current.listingDuration());
        listings.put(listing.id(), listing);
        markDirty();
        return ListOutcome.SUCCESS;
    }

    /** Buys {@code listingId} for {@code buyer}, paying the seller after tax. */
    public PurchaseOutcome purchase(Player buyer, UUID listingId) {
        Listing listing = listings.get(listingId);
        if (listing == null) {
            return PurchaseOutcome.NOT_FOUND;
        }
        if (listing.isOwnedBy(buyer.getUniqueId())) {
            return PurchaseOutcome.OWN_LISTING;
        }
        if (!economy.isAvailable()) {
            return PurchaseOutcome.NO_ECONOMY;
        }
        if (!economy.has(buyer, listing.price())) {
            return PurchaseOutcome.NOT_ENOUGH_MONEY;
        }
        if (buyer.getInventory().firstEmpty() == -1) {
            return PurchaseOutcome.INVENTORY_FULL;
        }
        // Re-check under the same tick before money moves: two players can click
        // the same slot in the same tick and only one of them may win.
        if (listings.remove(listingId) == null) {
            return PurchaseOutcome.NOT_FOUND;
        }
        if (!economy.withdraw(buyer, listing.price())) {
            listings.put(listing.id(), listing);
            return PurchaseOutcome.NOT_ENOUGH_MONEY;
        }

        double payout = Numbers.round(listing.price() * (1D - settings.get().salesTaxFraction()));
        OfflinePlayer seller = Bukkit.getOfflinePlayer(listing.sellerId());
        economy.deposit(seller, payout);
        buyer.getInventory().addItem(listing.item());

        recordTransaction(buyer, listing);
        notifySeller(listing, buyer, payout);
        markDirty();
        return PurchaseOutcome.SUCCESS;
    }

    /**
     * Takes a listing off the market and hands the item back.
     *
     * @return {@code true} when the listing existed and belonged to the player.
     */
    public boolean cancelListing(Player seller, UUID listingId) {
        Listing listing = listings.get(listingId);
        if (listing == null || !listing.isOwnedBy(seller.getUniqueId())) {
            return false;
        }
        listings.remove(listingId);
        giveOrStore(seller, listing.item());
        markDirty();
        return true;
    }

    /** @return {@code true} when the expired item existed and was handed over. */
    public boolean collectExpired(Player owner, UUID expiredId) {
        ExpiredItem item = expired.get(expiredId);
        if (item == null || !item.ownerId().equals(owner.getUniqueId())) {
            return false;
        }
        if (owner.getInventory().firstEmpty() == -1) {
            return false;
        }
        expired.remove(expiredId);
        owner.getInventory().addItem(item.item());
        markDirty();
        return true;
    }

    /** @return how many items were collected before the inventory filled up. */
    public int collectAllExpired(Player owner) {
        int collected = 0;
        for (ExpiredItem item : expiredOf(owner.getUniqueId())) {
            if (owner.getInventory().firstEmpty() == -1) {
                break;
            }
            expired.remove(item.id());
            owner.getInventory().addItem(item.item());
            collected++;
        }
        if (collected > 0) {
            markDirty();
        }
        return collected;
    }

    /**
     * Moves listings past their deadline into their owner's collection box and
     * drops expired entries that outlived the configured retention.
     */
    public void sweepExpired() {
        long now = System.currentTimeMillis();
        boolean changed = false;

        for (Listing listing : List.copyOf(listings.values())) {
            if (!listing.isExpired(now) || listings.remove(listing.id()) == null) {
                continue;
            }
            ExpiredItem item = new ExpiredItem(listing.id(), listing.sellerId(), listing.item(), now);
            expired.put(item.id(), item);
            changed = true;

            Player seller = Bukkit.getPlayer(listing.sellerId());
            if (seller != null) {
                messages.get().send(seller, "listing-expired",
                        Placeholders.of("item", displayName(listing.item())));
            }
        }

        long retention = settings.get().expiredRetention();
        if (retention > 0L) {
            for (ExpiredItem item : List.copyOf(expired.values())) {
                if (item.age(now) > retention) {
                    expired.remove(item.id());
                    changed = true;
                }
            }
        }

        if (changed) {
            markDirty();
        }
    }

    // ---------------------------------------------------------------- helpers

    /** The name shown to players: the custom name when set, else the material. */
    public static String displayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
            return Text.strip(item.getItemMeta().getDisplayName());
        }
        String raw = item.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder pretty = new StringBuilder(raw.length());
        boolean capitalise = true;
        for (char letter : raw.toCharArray()) {
            pretty.append(capitalise ? Character.toUpperCase(letter) : letter);
            capitalise = letter == ' ';
        }
        return pretty.toString();
    }

    /** Hands over and clears the notices queued while {@code player} was away. */
    public List<String> drainNotices(UUID playerId) {
        List<String> queued = pendingNotices.remove(playerId);
        if (queued == null || queued.isEmpty()) {
            return List.of();
        }
        markDirty();
        return List.copyOf(queued);
    }

    private void recordTransaction(Player buyer, Listing listing) {
        TransactionRecord record = new TransactionRecord(UUID.randomUUID(),
                buyer.getUniqueId(), buyer.getName(),
                listing.sellerId(), listing.sellerName(),
                listing.item(), listing.price(), System.currentTimeMillis());
        transactions.put(record.id(), record);
        trimHistory();
    }

    /** Keeps the history bounded so the data file cannot grow forever. */
    private void trimHistory() {
        int excess = transactions.size() - TRANSACTION_CAPACITY;
        if (excess <= 0) {
            return;
        }
        List<TransactionRecord> oldestFirst = new ArrayList<>(transactions.values());
        oldestFirst.sort(Comparator.comparingLong(TransactionRecord::timestamp));
        for (int i = 0; i < excess; i++) {
            transactions.remove(oldestFirst.get(i).id());
        }
    }

    private void notifySeller(Listing listing, Player buyer, double payout) {
        Map<String, String> placeholders = new HashMap<>(Placeholders.of(
                "buyer", buyer.getName(),
                "item", displayName(listing.item()),
                "price", Numbers.format(listing.price()),
                "payout", Numbers.format(payout)));
        Player seller = Bukkit.getPlayer(listing.sellerId());
        if (seller != null && seller.isOnline()) {
            messages.get().send(seller, "item-sold", placeholders);
            return;
        }
        pendingNotices.computeIfAbsent(listing.sellerId(), key -> new java.util.concurrent.CopyOnWriteArrayList<>())
                .add(messages.get().prefixed("item-sold-offline", placeholders));
    }

    /** Hands an item back, or parks it in the collection box when there is no room. */
    private void giveOrStore(Player player, ItemStack item) {
        if (player.getInventory().firstEmpty() == -1) {
            ExpiredItem stored = new ExpiredItem(UUID.randomUUID(), player.getUniqueId(),
                    item, System.currentTimeMillis());
            expired.put(stored.id(), stored);
            return;
        }
        player.getInventory().addItem(item);
    }

    /**
     * Flags the market as changed. With {@code storage.autosave-interval: 0}
     * the write happens right away instead of waiting for the save timer.
     */
    private void markDirty() {
        dirty = true;
        if (settings.get().autosaveIntervalSeconds() == 0L) {
            saveIfDirty();
        }
    }
}
