package com.francis.auction.menu;

import com.francis.auction.FrancisAuctionCore;
import com.francis.auction.model.TransactionRecord;
import com.francis.auction.util.ItemBuilder;
import com.francis.auction.util.Numbers;
import com.francis.auction.util.Pages;
import com.francis.auction.util.Placeholders;
import com.francis.auction.util.Text;
import java.util.List;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/** A read-only log of the viewer's completed trades. */
public final class TransactionsMenu extends SubMenu {

    private int total;

    public TransactionsMenu(FrancisAuctionCore plugin, Player viewer, AuctionMenu parent) {
        super(plugin, viewer, parent);
    }

    @Override
    protected String title() {
        return plugin.messages().title("transactions");
    }

    @Override
    protected void render(Inventory inventory) {
        List<TransactionRecord> history = plugin.auctions().transactionsOf(viewer.getUniqueId());
        total = history.size();
        page = Pages.clamp(page, total, GRID_SIZE);
        List<TransactionRecord> onPage = Pages.slice(history, page, GRID_SIZE);

        long now = System.currentTimeMillis();
        for (int slot = 0; slot < onPage.size(); slot++) {
            TransactionRecord record = onPage.get(slot);
            boolean bought = record.isBuyer(viewer.getUniqueId());
            Map<String, String> placeholders = Placeholders.of(
                    "buyer", record.buyerName(),
                    "seller", record.sellerName(),
                    "price", Numbers.format(record.price()),
                    "time", plugin.duration(record.age(now)));
            String path = bought ? "transaction.bought-lore" : "transaction.sold-lore";
            inventory.setItem(slot, ItemBuilder.copyOf(record.item())
                    .appendLore(Text.fill(plugin.messages().rawLines(path), placeholders))
                    .build());
        }
        renderBar(inventory, total);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        handleBarClick(event.getSlot(), total);
    }
}
