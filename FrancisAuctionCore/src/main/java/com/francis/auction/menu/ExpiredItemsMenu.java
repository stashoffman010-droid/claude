package com.francis.auction.menu;

import com.francis.auction.FrancisAuctionCore;
import com.francis.auction.model.ExpiredItem;
import com.francis.auction.service.AuctionService;
import com.francis.auction.ui.ButtonFactory;
import com.francis.auction.util.ItemBuilder;
import com.francis.auction.util.Pages;
import com.francis.auction.util.Placeholders;
import com.francis.auction.util.Text;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/** The collection box for listings that left the market unsold. */
public final class ExpiredItemsMenu extends SubMenu {

    private static final int SLOT_COLLECT_ALL = 51;

    private List<ExpiredItem> onPage = List.of();
    private int total;

    public ExpiredItemsMenu(FrancisAuctionCore plugin, Player viewer, AuctionMenu parent) {
        super(plugin, viewer, parent);
    }

    @Override
    protected String title() {
        return plugin.messages().title("expired-items");
    }

    @Override
    protected void render(Inventory inventory) {
        List<ExpiredItem> owned = plugin.auctions().expiredOf(viewer.getUniqueId());
        total = owned.size();
        page = Pages.clamp(page, total, GRID_SIZE);
        onPage = Pages.slice(owned, page, GRID_SIZE);

        long now = System.currentTimeMillis();
        for (int slot = 0; slot < onPage.size(); slot++) {
            ExpiredItem item = onPage.get(slot);
            Map<String, String> placeholders = Placeholders.of("time", plugin.duration(item.age(now)));
            inventory.setItem(slot, ItemBuilder.copyOf(item.item())
                    .appendLore(Text.fill(plugin.messages().rawLines("listing.expired-lore"), placeholders))
                    .build());
        }

        renderBar(inventory, total);
        ButtonFactory buttons = plugin.buttons();
        placeExtra(inventory, SLOT_COLLECT_ALL, buttons.dialog("collect-all",
                buttons.dialogIcon("collect-all", Material.HOPPER),
                Placeholders.of("expired", total)));
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot < GRID_SIZE) {
            if (slot < onPage.size()) {
                playClick();
                collectOne(onPage.get(slot));
            }
            return;
        }
        if (handleBarClick(slot, total)) {
            return;
        }
        if (slot == SLOT_COLLECT_ALL) {
            playClick();
            collectEverything();
        }
    }

    private void collectOne(ExpiredItem item) {
        if (plugin.auctions().collectExpired(viewer, item.id())) {
            plugin.messages().send(viewer, "item-collected",
                    Placeholders.of("item", AuctionService.displayName(item.item())));
        } else {
            plugin.messages().send(viewer, "inventory-full");
        }
        refresh();
    }

    private void collectEverything() {
        if (total == 0) {
            plugin.messages().send(viewer, "no-expired-items");
            return;
        }
        int collected = plugin.auctions().collectAllExpired(viewer);
        if (collected == 0) {
            plugin.messages().send(viewer, "inventory-full");
        } else {
            plugin.messages().send(viewer, "collected-all", Placeholders.of("amount", collected));
        }
        refresh();
    }
}
