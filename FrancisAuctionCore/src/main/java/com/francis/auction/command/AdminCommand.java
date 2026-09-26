package com.francis.auction.command;

import com.francis.auction.FrancisAuctionCore;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

/** {@code /auctioncore reload}. */
public final class AdminCommand implements CommandExecutor, TabCompleter {

    private final FrancisAuctionCore plugin;

    public AdminCommand(FrancisAuctionCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage("/" + label + " reload");
            return true;
        }
        plugin.reloadSettings();
        plugin.messages().send(sender, "reloaded");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return args.length == 1 && "reload".startsWith(args[0].toLowerCase(Locale.ROOT))
                ? List.of("reload")
                : List.of();
    }
}
