package com.francis.auction.menu;

import com.francis.auction.FrancisAuctionCore;
import com.francis.auction.model.Category;
import com.francis.auction.model.Listing;
import com.francis.auction.model.SortOrder;
import com.francis.auction.ui.ButtonFactory;
import com.francis.auction.util.ItemBuilder;
import com.francis.auction.util.Numbers;
import com.francis.auction.util.Pages;
import com.francis.auction.util.Placeholders;
import com.francis.auction.util.Text;
import java.util.List;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** The market itself: {@code /ah}. */
public final class AuctionMenu extends Menu {

    private Category category = Category.ALL;
    private SortOrder order = SortOrder.NEWEST;
    private String query = "";
    private int page;

    /** The listings drawn on the current page, in slot order. */
    private List<Listing> onPage = List.of();

    public AuctionMenu(FrancisAuctionCore plugin, Player viewer) {
        super(plugin, viewer);
    }

    /** Applies a search term and jumps back to the first page. */
    public void search(String term) {
        this.query = term == null ? "" : term.trim();
        this.page = 0;
    }

    @Override
    protected int size() {
        return plugin.settings().menuSize();
    }

    @Override
    protected String title() {
        return query.isEmpty()
                ? plugin.messages().title("auction")
                : plugin.messages().title("auction-search", Placeholders.of("query", query));
    }

    @Override
    protected void render(Inventory inventory) {
        int perPage = plugin.settings().itemsPerPage();
        List<Listing> matching = plugin.auctions().browse(category, order, query);
        page = Pages.clamp(page, matching.size(), perPage);
        onPage = Pages.slice(matching, page, perPage);

        long now = System.currentTimeMillis();
        for (int slot = 0; slot < onPage.size(); slot++) {
            inventory.setItem(slot, decorate(onPage.get(slot), now));
        }
        if (onPage.isEmpty()) {
            inventory.setItem(perPage / 2, emptyNotice());
        }
        renderNavigationBar(inventory, matching.size(), perPage);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        int perPage = plugin.settings().itemsPerPage();

        if (slot < perPage) {
            if (slot < onPage.size()) {
                openListing(onPage.get(slot));
            }
            return;
        }

        ButtonFactory buttons = plugin.buttons();
        if (slot == buttons.slot("previous-page")) {
            turnPage(-1);
        } else if (slot == buttons.slot("next-page")) {
            turnPage(1);
        } else if (slot == buttons.slot("refresh-page")) {
            playClick();
            refresh();
        } else if (slot == buttons.slot("category")) {
            playClick();
            category = event.getClick() == ClickType.RIGHT ? category.previous() : category.next();
            page = 0;
            refresh();
        } else if (slot == buttons.slot("sort")) {
            playClick();
            order = event.getClick() == ClickType.RIGHT ? order.previous() : order.next();
            page = 0;
            refresh();
        } else if (slot == buttons.slot("search")) {
            playClick();
            promptForSearch(event.getClick() == ClickType.RIGHT);
        } else if (slot == buttons.slot("your-items")) {
            playClick();
            new MyListingsMenu(plugin, viewer, this).open();
        } else if (slot == buttons.slot("expired-items")) {
            playClick();
            new ExpiredItemsMenu(plugin, viewer, this).open();
        }
    }

    // --------------------------------------------------------------- drawing

    private ItemStack decorate(Listing listing, long now) {
        Map<String, String> placeholders = Placeholders.of(
                "price", Numbers.format(listing.price()),
                "seller", listing.sellerName(),
                "time", plugin.duration(listing.remaining(now)));
        String path = listing.isOwnedBy(viewer.getUniqueId()) ? "listing.own-lore" : "listing.lore";
        return ItemBuilder.copyOf(listing.item())
                .appendLore(Text.fill(plugin.messages().rawLines(path), placeholders))
                .build();
    }

    private ItemStack emptyNotice() {
        return ItemBuilder.of(plugin.settings().filler())
                .name(plugin.messages().rawLine("empty.name"))
                .lore(plugin.messages().rawLines("empty.lore"))
                .clean()
                .build();
    }

    private void renderNavigationBar(Inventory inventory, int matches, int perPage) {
        ButtonFactory buttons = plugin.buttons();
        int barStart = size() - 9;
        for (int slot = barStart; slot < size(); slot++) {
            inventory.setItem(slot, buttons.filler());
        }

        int listed = plugin.auctions().listingCount(viewer.getUniqueId());
        int max = plugin.settings().maxListingsPerPlayer();
        Map<String, String> counters = Placeholders.of(
                "listings", listed,
                "max", max,
                "expired", plugin.auctions().expiredCount(viewer.getUniqueId()),
                "page", page + 1,
                "pages", Pages.count(matches, perPage),
                "query", query.isEmpty() ? plugin.messages().rawLine("buttons.search.no-query") : query);

        place(inventory, buttons.navigation("previous-page", counters), "previous-page");
        place(inventory, buttons.navigation("next-page", counters), "next-page");
        place(inventory, buttons.navigation("refresh-page", counters), "refresh-page");
        place(inventory, buttons.navigation("information", counters), "information");
        place(inventory, buttons.navigation("search", counters), "search");
        place(inventory, buttons.navigation("your-items", counters), "your-items");
        place(inventory, buttons.navigation("expired-items", counters), "expired-items");
        place(inventory, buttons.navigation("category", counters,
                Map.of("categories", buttons.categoryBlock(category))), "category");
        place(inventory, buttons.navigation("sort", counters,
                Map.of("orders", buttons.sortBlock(order))), "sort");
    }

    private void place(Inventory inventory, ItemStack button, String key) {
        int slot = plugin.buttons().slot(key);
        if (button != null && slot >= 0) {
            inventory.setItem(slot, button);
        }
    }

    // --------------------------------------------------------------- actions

    private void turnPage(int delta) {
        int perPage = plugin.settings().itemsPerPage();
        int total = plugin.auctions().browse(category, order, query).size();
        int target = Pages.clamp(page + delta, total, perPage);
        if (target != page) {
            playClick();
            page = target;
            refresh();
        }
    }

    private void openListing(Listing listing) {
        playClick();
        if (listing.isOwnedBy(viewer.getUniqueId())) {
            if (plugin.auctions().cancelListing(viewer, listing.id())) {
                plugin.messages().send(viewer, "listing-cancelled");
            }
            refresh();
            return;
        }
        new ConfirmPurchaseMenu(plugin, viewer, this, listing.id()).open();
    }

    /** Opens the chat search box; a right click simply clears the filter. */
    private void promptForSearch(boolean clear) {
        if (clear) {
            search("");
            refresh();
            return;
        }
        viewer.closeInventory();
        plugin.messages().send(viewer, "search-prompt");
        plugin.prompts().await(viewer, input -> {
            search(input);
            open();
        }, () -> {
            plugin.messages().send(viewer, "search-cancelled");
            open();
        });
    }
}
