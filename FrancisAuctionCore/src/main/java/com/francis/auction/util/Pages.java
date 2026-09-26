package com.francis.auction.util;

import java.util.List;

/** Slicing helpers shared by every paginated menu. */
public final class Pages {

    private Pages() {
    }

    /** Number of pages needed for {@code total} entries; never below one. */
    public static int count(int total, int perPage) {
        return Math.max(1, (total + perPage - 1) / perPage);
    }

    /** Clamps {@code page} into {@code [0, count - 1]}. */
    public static int clamp(int page, int total, int perPage) {
        return Math.min(Math.max(0, page), count(total, perPage) - 1);
    }

    /** The entries shown on {@code page}; an out of range page yields nothing. */
    public static <T> List<T> slice(List<T> all, int page, int perPage) {
        int from = page * perPage;
        if (from >= all.size() || from < 0) {
            return List.of();
        }
        return all.subList(from, Math.min(all.size(), from + perPage));
    }
}
