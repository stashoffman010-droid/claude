package com.francis.auction.command;

import com.francis.auction.FrancisAuctionCore;
import com.francis.auction.menu.AuctionMenu;
import com.francis.auction.menu.ConfirmListingMenu;
import com.francis.auction.menu.ExpiredItemsMenu;
import com.francis.auction.menu.MyListingsMenu;
import com.francis.auction.menu.TransactionsMenu;
import com.francis.auction.util.Numbers;
import com.francis.auction.util.Placeholders;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** {@code /ah} and its sub commands. */
public final class AuctionCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUB_COMMANDS =
            List.of("sell", "search", "listings", "expired", "transactions");

    private final FrancisAuctionCore plugin;

    public AuctionCommand(FrancisAuctionCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "not-player");
            return true;
        }
        AuctionMenu menu = new AuctionMenu(plugin, player);
        if (args.length == 0) {
            menu.open();
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "sell" -> sell(player, menu, args);
            case "search" -> search(player, menu, args);
            case "listings", "mine" -> new MyListingsMenu(plugin, player, menu).open();
            case "expired" -> new ExpiredItemsMenu(plugin, player, menu).open();
            case "transactions" -> new TransactionsMenu(plugin, player, menu).open();
            default -> menu.open();
        }
        return true;
    }

    private void sell(Player player, AuctionMenu menu, String[] args) {
        if (!player.hasPermission("francisauction.sell")) {
            plugin.messages().send(player, "no-permission");
            return;
        }
        if (args.length < 2) {
            player.sendMessage(plugin.messages().get("price-prompt"));
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            plugin.messages().send(player, "not-holding-item");
            return;
        }
        if (plugin.auctions().listingCount(player.getUniqueId()) >= plugin.settings().maxListingsPerPlayer()) {
            plugin.messages().send(player, "listing-limit-reached",
                    Placeholders.of("max", plugin.settings().maxListingsPerPlayer()));
            return;
        }
        OptionalDouble price = Numbers.parsePrice(args[1]);
        if (price.isEmpty()) {
            plugin.messages().send(player, "invalid-price");
            return;
        }
        new ConfirmListingMenu(plugin, player, menu, held, price.getAsDouble()).open();
    }

    private void search(Player player, AuctionMenu menu, String[] args) {
        menu.search(String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)));
        menu.open();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String partial = args[0].toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : SUB_COMMANDS) {
            if (option.startsWith(partial)) {
                matches.add(option);
            }
        }
        return matches;
    }
}
