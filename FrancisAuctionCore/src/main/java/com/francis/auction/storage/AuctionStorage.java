package com.francis.auction.storage;

import com.francis.auction.model.ExpiredItem;
import com.francis.auction.model.Listing;
import com.francis.auction.model.TransactionRecord;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Persistence boundary for the auction house. */
public interface AuctionStorage {

    /** Everything the plugin keeps between restarts. */
    record Snapshot(Collection<Listing> listings,
                    Collection<ExpiredItem> expired,
                    Collection<TransactionRecord> transactions,
                    Map<UUID, List<String>> notices) {

        public static Snapshot empty() {
            return new Snapshot(List.of(), List.of(), List.of(), Map.of());
        }
    }

    /** Reads the data file; an unreadable or missing file yields an empty snapshot. */
    Snapshot load();

    /**
     * Serialises on the calling thread and writes to disk off the main thread.
     * Safe to call from the server thread after any mutation.
     */
    void saveAsync(Snapshot snapshot);

    /** Serialises and writes immediately; used on shutdown. */
    void saveNow(Snapshot snapshot);
}
