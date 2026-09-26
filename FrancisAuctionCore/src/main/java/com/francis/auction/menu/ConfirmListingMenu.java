package com.francis.auction.menu;

import com.francis.auction.FrancisAuctionCore;
import com.francis.auction.service.AuctionService;
import com.francis.auction.util.ItemBuilder;
import com.francis.auction.util.Numbers;
import com.francis.auction.util.Placeholders;
import com.francis.auction.util.Text;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** Are you sure you want to list this, at this price? */
public final class ConfirmListingMenu extends Menu {

    private static final int SIZE = 27;
    private static final int SLOT_CONFIRM = 11;
    private static final int SLOT_ITEM = 13;
    private static final int SLOT_CANCEL = 15;

    private final AuctionMenu parent;
    private final ItemStack item;
    private final double price;

    public ConfirmListingMenu(FrancisAuctionCore plugin, Player viewer, AuctionMenu parent,
                              ItemStack item, double price) {
        super(plugin, viewer);
        this.parent = parent;
        this.item = item.clone();
        this.price = price;
    }

    @Override
    protected int size() {
        return SIZE;
    }

    @Override
    protected String title() {
        return plugin.messages().title("confirm-listing");
    }

    @Override
    protected void render(Inventory inventory) {
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, plugin.buttons().filler());
        }
        Map<String, String> placeholders = placeholders();
        inventory.setItem(SLOT_ITEM, ItemBuilder.copyOf(item)
                .appendLore(Text.fill(plugin.messages().rawLines("confirm-listing.lore"), placeholders))
                .build());
        inventory.setItem(SLOT_CONFIRM, plugin.buttons().dialog("confirm",
                plugin.buttons().dialogIcon("confirm", Material.LIME_STAINED_GLASS_PANE), placeholders));
        inventory.setItem(SLOT_CANCEL, plugin.buttons().dialog("cancel",
                plugin.buttons().dialogIcon("cancel", Material.RED_STAINED_GLASS_PANE), placeholders));
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        switch (event.getSlot()) {
            case SLOT_CONFIRM -> {
                playClick();
                list();
            }
            case SLOT_CANCEL -> {
                playClick();
                parent.open();
            }
            default -> {
                // The item preview and the filler are not interactive.
            }
        }
    }

    private void list() {
        ItemStack held = viewer.getInventory().getItemInMainHand();
        if (held.getType().isAir() || !held.isSimilar(item) || held.getAmount() != item.getAmount()) {
            plugin.messages().send(viewer, "item-not-found-inventory");
            parent.open();
            return;
        }
        Map<String, String> placeholders = placeholders();
        switch (plugin.auctions().createListing(viewer, price)) {
            case SUCCESS -> plugin.messages().send(viewer, "listing-created", placeholders);
            case NO_ITEM -> plugin.messages().send(viewer, "not-holding-item");
            case LIMIT_REACHED -> plugin.messages().send(viewer, "listing-limit-reached",
                    Placeholders.of("max", plugin.settings().maxListingsPerPlayer()));
            case PRICE_TOO_LOW -> plugin.messages().send(viewer, "price-too-low",
                    Placeholders.of("price", Numbers.format(plugin.settings().minPrice())));
            case PRICE_TOO_HIGH -> plugin.messages().send(viewer, "price-too-high",
                    Placeholders.of("price", Numbers.format(plugin.settings().maxPrice())));
        }
        parent.open();
    }

    private Map<String, String> placeholders() {
        return Placeholders.of(
                "item", AuctionService.displayName(item),
                "price", Numbers.format(price),
                "time", plugin.duration(plugin.settings().listingDuration()));
    }
}
