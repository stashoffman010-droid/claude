package com.francis.auction.menu;

import com.francis.auction.FrancisAuctionCore;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Base class for every screen in the plugin.
 *
 * <p>Using the menu itself as the {@link InventoryHolder} is what lets
 * {@link MenuListener} route a click back to the right screen without keeping
 * a map of open inventories.
 */
public abstract class Menu implements InventoryHolder {

    protected final FrancisAuctionCore plugin;
    protected final Player viewer;

    private Inventory inventory;

    protected Menu(FrancisAuctionCore plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
    }

    /** Slot count; always a multiple of nine. */
    protected abstract int size();

    /** Already coloured title. */
    protected abstract String title();

    /** Fills a freshly cleared inventory. */
    protected abstract void render(Inventory inventory);

    /** Called for clicks inside this menu; the event is cancelled beforehand. */
    public abstract void onClick(InventoryClickEvent event);

    /** Called when the viewer closes this menu. */
    public void onClose(InventoryCloseEvent event) {
        // Nothing to clean up by default.
    }

    public final void open() {
        inventory = Bukkit.createInventory(this, size(), title());
        refresh();
        viewer.openInventory(inventory);
    }

    /** Redraws in place, keeping the window open. */
    public final void refresh() {
        if (inventory == null) {
            return;
        }
        inventory.clear();
        render(inventory);
    }

    @Override
    public final Inventory getInventory() {
        return inventory;
    }

    /** Plays the configured button sound, if one is set. */
    protected final void playClick() {
        String sound = plugin.settings().clickSound();
        if (sound != null) {
            viewer.playSound(viewer.getLocation(), sound, SoundCategory.MASTER, 0.6F, 1.0F);
        }
    }
}
