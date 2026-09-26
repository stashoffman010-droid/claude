package com.francis.auction.menu;

import com.francis.auction.FrancisAuctionCore;
import com.francis.auction.model.Listing;
import com.francis.auction.service.AuctionService;
import com.francis.auction.util.ItemBuilder;
import com.francis.auction.util.Numbers;
import com.francis.auction.util.Placeholders;
import com.francis.auction.util.Text;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/** Are you sure you want to buy this? */
public final class ConfirmPurchaseMenu extends Menu {

    private static final int SIZE = 27;
    private static final int SLOT_CONFIRM = 11;
    private static final int SLOT_ITEM = 13;
    private static final int SLOT_CANCEL = 15;

    private final AuctionMenu parent;
    private final UUID listingId;

    public ConfirmPurchaseMenu(FrancisAuctionCore plugin, Player viewer, AuctionMenu parent, UUID listingId) {
        super(plugin, viewer);
        this.parent = parent;
        this.listingId = listingId;
    }

    @Override
    protected int size() {
        return SIZE;
    }

    @Override
    protected String title() {
        return plugin.messages().title("confirm-purchase");
    }

    @Override
    protected void render(Inventory inventory) {
        Optional<Listing> found = plugin.auctions().listing(listingId);
        if (found.isEmpty()) {
            return;
        }
        Listing listing = found.get();
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, plugin.buttons().filler());
        }

        Map<String, String> placeholders = Placeholders.of(
                "price", Numbers.format(listing.price()),
                "seller", listing.sellerName(),
                "item", AuctionService.displayName(listing.item()),
                "time", plugin.duration(listing.remaining(System.currentTimeMillis())));

        inventory.setItem(SLOT_ITEM, ItemBuilder.copyOf(listing.item())
                .appendLore(Text.fill(plugin.messages().rawLines("confirm-purchase.lore"), placeholders))
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
                buy();
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

    private void buy() {
        Optional<Listing> found = plugin.auctions().listing(listingId);
        if (found.isEmpty()) {
            plugin.messages().send(viewer, "auction-not-found");
            parent.open();
            return;
        }
        Listing listing = found.get();
        Map<String, String> placeholders = Placeholders.of(
                "item", AuctionService.displayName(listing.item()),
                "price", Numbers.format(listing.price()),
                "seller", listing.sellerName());

        switch (plugin.auctions().purchase(viewer, listingId)) {
            case SUCCESS -> plugin.messages().send(viewer, "item-purchased", placeholders);
            case NOT_FOUND -> plugin.messages().send(viewer, "auction-not-found");
            case OWN_LISTING -> plugin.messages().send(viewer, "own-listing");
            case NO_ECONOMY -> plugin.messages().send(viewer, "no-economy");
            case NOT_ENOUGH_MONEY -> plugin.messages().send(viewer, "not-enough-money");
            case INVENTORY_FULL -> plugin.messages().send(viewer, "inventory-full");
        }
        parent.open();
    }
}
