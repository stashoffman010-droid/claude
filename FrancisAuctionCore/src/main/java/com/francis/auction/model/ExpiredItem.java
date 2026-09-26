package com.francis.auction.model;

import java.util.UUID;
import org.bukkit.inventory.ItemStack;

/**
 * A listing that left the market unsold and is waiting in its owner's
 * collection box.
 */
public record ExpiredItem(UUID id, UUID ownerId, ItemStack item, long expiredAt) {

    public ExpiredItem {
        item = item.clone();
    }

    @Override
    public ItemStack item() {
        return item.clone();
    }

    public long age(long now) {
        return Math.max(0L, now - expiredAt);
    }
}
