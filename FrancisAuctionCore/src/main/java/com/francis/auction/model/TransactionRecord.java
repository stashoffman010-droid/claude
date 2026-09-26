package com.francis.auction.model;

import java.util.UUID;
import org.bukkit.inventory.ItemStack;

/** A completed sale, kept for the transactions menu. */
public record TransactionRecord(UUID id,
                                UUID buyerId,
                                String buyerName,
                                UUID sellerId,
                                String sellerName,
                                ItemStack item,
                                double price,
                                long timestamp) {

    public TransactionRecord {
        item = item.clone();
    }

    @Override
    public ItemStack item() {
        return item.clone();
    }

    /** @return whether {@code playerId} was on the buying side of this trade. */
    public boolean isBuyer(UUID playerId) {
        return buyerId.equals(playerId);
    }

    public long age(long now) {
        return Math.max(0L, now - timestamp);
    }
}
