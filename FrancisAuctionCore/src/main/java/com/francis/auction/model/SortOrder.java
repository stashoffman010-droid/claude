package com.francis.auction.model;

import java.util.Comparator;

/** The orders offered by the {@code SORT ORDER} button. */
public enum SortOrder {

    NEWEST(Comparator.comparingLong(Listing::createdAt).reversed()),
    OLDEST(Comparator.comparingLong(Listing::createdAt)),
    PRICE_LOW(Comparator.comparingDouble(Listing::price)),
    PRICE_HIGH(Comparator.comparingDouble(Listing::price).reversed());

    private static final SortOrder[] VALUES = values();

    private final Comparator<Listing> comparator;

    SortOrder(Comparator<Listing> comparator) {
        this.comparator = comparator;
    }

    public Comparator<Listing> comparator() {
        return comparator;
    }

    public SortOrder next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public SortOrder previous() {
        return VALUES[(ordinal() + VALUES.length - 1) % VALUES.length];
    }

    public static SortOrder from(String name) {
        if (name != null) {
            for (SortOrder order : VALUES) {
                if (order.name().equalsIgnoreCase(name.trim())) {
                    return order;
                }
            }
        }
        return NEWEST;
    }
}
