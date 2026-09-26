package com.francis.auction.model;

import java.util.UUID;
import org.bukkit.inventory.ItemStack;

/**
 * One item on the market.
 *
 * @param id         identity used by the menus and the storage file
 * @param sellerId   owner of the listing
 * @param sellerName last known name of the owner, for display only
 * @param item       the stack handed over on purchase
 * @param price      asking price
 * @param createdAt  epoch millis the listing went live
 * @param expiresAt  epoch millis the listing leaves the market
 */
public record Listing(UUID id,
                      UUID sellerId,
                      String sellerName,
                      ItemStack item,
                      double price,
                      long createdAt,
                      long expiresAt) {

    public Listing {
        item = item.clone();
    }

    @Override
    public ItemStack item() {
        return item.clone();
    }

    public boolean isExpired(long now) {
        return now >= expiresAt;
    }

    public long remaining(long now) {
        return Math.max(0L, expiresAt - now);
    }

    public boolean isOwnedBy(UUID playerId) {
        return sellerId.equals(playerId);
    }
}
