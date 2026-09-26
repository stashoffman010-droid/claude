package com.francis.auction.menu;

import com.francis.auction.FrancisAuctionCore;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.InventoryHolder;

/** Routes inventory events to the {@link Menu} that owns them. */
public final class MenuListener implements Listener {

    private final FrancisAuctionCore plugin;

    public MenuListener(FrancisAuctionCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        Menu menu = menuOf(event.getInventory().getHolder());
        if (menu == null || !(event.getWhoClicked() instanceof Player)) {
            return;
        }
        // Cancel first: every path below either does nothing or acts explicitly,
        // so no click may ever move an item in or out of a menu.
        event.setCancelled(true);
        if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getInventory())) {
            menu.onClick(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (menuOf(event.getInventory().getHolder()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Menu menu = menuOf(event.getInventory().getHolder());
        if (menu != null) {
            menu.onClose(event);
        }
    }

    /** Delivers the sale notices that arrived while the seller was offline. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        List<String> notices = plugin.auctions().drainNotices(player.getUniqueId());
        if (notices.isEmpty()) {
            return;
        }
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> notices.forEach(player::sendMessage), 20L);
    }

    private static Menu menuOf(InventoryHolder holder) {
        return holder instanceof Menu menu ? menu : null;
    }
}
