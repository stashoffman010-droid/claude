package com.francis.auction.menu;

import com.francis.auction.FrancisAuctionCore;
import com.francis.auction.ui.ButtonFactory;
import com.francis.auction.util.Pages;
import com.francis.auction.util.Placeholders;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Shared layout for the paginated screens reached from the auction house:
 * a 45 slot grid with a fixed navigation bar underneath.
 */
abstract class SubMenu extends Menu {

    protected static final int GRID_SIZE = 45;
    protected static final int SLOT_PREVIOUS = 45;
    protected static final int SLOT_BACK = 49;
    protected static final int SLOT_NEXT = 53;

    private static final int SIZE = 54;

    protected final AuctionMenu parent;
    protected int page;

    protected SubMenu(FrancisAuctionCore plugin, Player viewer, AuctionMenu parent) {
        super(plugin, viewer);
        this.parent = parent;
    }

    @Override
    protected final int size() {
        return SIZE;
    }

    /** Draws the filler row plus the previous, back and next buttons. */
    protected final void renderBar(Inventory inventory, int totalEntries) {
        ButtonFactory buttons = plugin.buttons();
        for (int slot = GRID_SIZE; slot < SIZE; slot++) {
            inventory.setItem(slot, buttons.filler());
        }
        Map<String, String> counters = Placeholders.of(
                "page", page + 1,
                "pages", Pages.count(totalEntries, GRID_SIZE));
        inventory.setItem(SLOT_PREVIOUS, buttons.dialog("previous-page",
                buttons.navigationIcon("previous-page", Material.ARROW), counters));
        inventory.setItem(SLOT_NEXT, buttons.dialog("next-page",
                buttons.navigationIcon("next-page", Material.ARROW), counters));
        inventory.setItem(SLOT_BACK, buttons.dialog("back",
                buttons.dialogIcon("back", Material.OAK_DOOR), counters));
    }

    /**
     * Handles the three buttons every sub menu shares.
     *
     * @return {@code true} when the click was consumed.
     */
    protected final boolean handleBarClick(int slot, int totalEntries) {
        switch (slot) {
            case SLOT_PREVIOUS -> turnPage(-1, totalEntries);
            case SLOT_NEXT -> turnPage(1, totalEntries);
            case SLOT_BACK -> {
                playClick();
                parent.open();
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private void turnPage(int delta, int totalEntries) {
        int target = Pages.clamp(page + delta, totalEntries, GRID_SIZE);
        if (target != page) {
            playClick();
            page = target;
            refresh();
        }
    }

    /** Convenience for sub menus that add one more button to the bar. */
    protected final void placeExtra(Inventory inventory, int slot, ItemStack button) {
        if (button != null && slot >= GRID_SIZE && slot < SIZE) {
            inventory.setItem(slot, button);
        }
    }
}
