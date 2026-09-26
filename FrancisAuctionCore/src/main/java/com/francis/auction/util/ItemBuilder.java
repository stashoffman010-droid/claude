package com.francis.auction.util;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Small fluent helper so menu code stays free of {@link ItemMeta} plumbing. */
public final class ItemBuilder {

    private final ItemStack stack;

    private ItemBuilder(ItemStack stack) {
        this.stack = stack;
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(new ItemStack(material));
    }

    /** Starts from a copy, so the caller's stack is never mutated. */
    public static ItemBuilder copyOf(ItemStack source) {
        return new ItemBuilder(source.clone());
    }

    public ItemBuilder amount(int amount) {
        stack.setAmount(Math.max(1, Math.min(amount, stack.getMaxStackSize())));
        return this;
    }

    public ItemBuilder name(String name) {
        return edit(meta -> meta.setDisplayName(Text.color(name)));
    }

    public ItemBuilder lore(List<String> lore) {
        return edit(meta -> meta.setLore(Text.color(lore)));
    }

    /** Keeps the item's own lore and appends the auction house lines below it. */
    public ItemBuilder appendLore(List<String> lore) {
        return edit(meta -> {
            List<String> existing = meta.hasLore() ? meta.getLore() : List.of();
            List<String> merged = new java.util.ArrayList<>(existing);
            merged.addAll(Text.color(lore));
            meta.setLore(merged);
        });
    }

    /** Hides the vanilla extras so a button shows nothing but its own text. */
    public ItemBuilder clean() {
        return edit(meta -> meta.addItemFlags(ItemFlag.values()));
    }

    public ItemStack build() {
        return stack;
    }

    private ItemBuilder edit(java.util.function.Consumer<ItemMeta> action) {
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            action.accept(meta);
            stack.setItemMeta(meta);
        }
        return this;
    }
}
