package com.francis.auction.menu;

import com.francis.auction.FrancisAuctionCore;
import com.francis.auction.model.Listing;
import com.francis.auction.ui.ButtonFactory;
import com.francis.auction.util.ItemBuilder;
import com.francis.auction.util.Numbers;
import com.francis.auction.util.Pages;
import com.francis.auction.util.Placeholders;
import com.francis.auction.util.Text;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** Everything the viewer currently has on the market. */
public final class MyListingsMenu extends SubMenu {

    private static final int SLOT_SELL = 47;
    private static final int SLOT_TRANSACTIONS = 51;

    private List<Listing> onPage = List.of();
    private int total;

    public MyListingsMenu(FrancisAuctionCore plugin, Player viewer, AuctionMenu parent) {
        super(plugin, viewer, parent);
    }

    @Override
    protected String title() {
        return plugin.messages().title("your-items");
    }

    @Override
    protected void render(Inventory inventory) {
        List<Listing> owned = plugin.auctions().listingsOf(viewer.getUniqueId());
        total = owned.size();
        page = Pages.clamp(page, total, GRID_SIZE);
        onPage = Pages.slice(owned, page, GRID_SIZE);

        long now = System.currentTimeMillis();
        for (int slot = 0; slot < onPage.size(); slot++) {
            Listing listing = onPage.get(slot);
            Map<String, String> placeholders = Placeholders.of(
                    "price", Numbers.format(listing.price()),
                    "seller", listing.sellerName(),
                    "time", plugin.duration(listing.remaining(now)));
            inventory.setItem(slot, ItemBuilder.copyOf(listing.item())
                    .appendLore(Text.fill(plugin.messages().rawLines("listing.own-lore"), placeholders))
                    .build());
        }

        renderBar(inventory, total);
        ButtonFactory buttons = plugin.buttons();
        Map<String, String> counters = Placeholders.of(
                "listings", total,
                "max", plugin.settings().maxListingsPerPlayer());
        placeExtra(inventory, SLOT_SELL, buttons.dialog("sell-item",
                buttons.dialogIcon("sell-item", Material.GOLD_INGOT), counters));
        placeExtra(inventory, SLOT_TRANSACTIONS, buttons.dialog("transactions",
                buttons.dialogIcon("transactions", Material.WRITABLE_BOOK), counters));
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot < GRID_SIZE) {
            if (slot < onPage.size()) {
                playClick();
                if (plugin.auctions().cancelListing(viewer, onPage.get(slot).id())) {
                    plugin.messages().send(viewer, "listing-cancelled");
                }
                refresh();
            }
            return;
        }
        if (handleBarClick(slot, total)) {
            return;
        }
        switch (slot) {
            case SLOT_SELL -> {
                playClick();
                promptForPrice();
            }
            case SLOT_TRANSACTIONS -> {
                playClick();
                new TransactionsMenu(plugin, viewer, parent).open();
            }
            default -> {
                // Filler.
            }
        }
    }

    /** Asks for a price in chat, then shows the listing confirmation. */
    private void promptForPrice() {
        ItemStack held = viewer.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            plugin.messages().send(viewer, "not-holding-item");
            return;
        }
        if (total >= plugin.settings().maxListingsPerPlayer()) {
            plugin.messages().send(viewer, "listing-limit-reached",
                    Placeholders.of("max", plugin.settings().maxListingsPerPlayer()));
            return;
        }
        ItemStack snapshot = held.clone();
        viewer.closeInventory();
        plugin.messages().send(viewer, "price-prompt");
        plugin.prompts().await(viewer, input -> {
            OptionalDouble price = Numbers.parsePrice(input);
            if (price.isEmpty()) {
                plugin.messages().send(viewer, "invalid-price");
                open();
                return;
            }
            new ConfirmListingMenu(plugin, viewer, parent, snapshot, price.getAsDouble()).open();
        }, () -> {
            plugin.messages().send(viewer, "price-cancelled");
            open();
        });
    }
}
