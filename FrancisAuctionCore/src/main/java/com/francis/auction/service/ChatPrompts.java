package com.francis.auction.service;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

/**
 * One-shot chat input, used by the search box and by the sell price prompt.
 *
 * <p>The chat event arrives off the server thread, so the callback is always
 * handed back on the main thread before it touches anything Bukkit owns.
 */
public final class ChatPrompts implements Listener {

    private record Prompt(Consumer<String> onInput, Runnable onCancel) {
    }

    private static final String CANCEL_KEYWORD = "cancel";

    private final Plugin plugin;
    private final Map<UUID, Prompt> waiting = new ConcurrentHashMap<>();

    public ChatPrompts(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Captures the player's next chat message.
     *
     * @param onInput  receives the typed text, on the main thread
     * @param onCancel runs when the player types {@code cancel} or quits
     */
    public void await(Player player, Consumer<String> onInput, Runnable onCancel) {
        waiting.put(player.getUniqueId(), new Prompt(onInput, onCancel));
    }

    /** Drops any pending prompt without running either callback. */
    public void forget(Player player) {
        waiting.remove(player.getUniqueId());
    }

    public boolean isWaiting(Player player) {
        return waiting.containsKey(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Prompt prompt = waiting.remove(player.getUniqueId());
        if (prompt == null) {
            return;
        }
        event.setCancelled(true);
        String input = event.getMessage().trim();
        boolean cancelled = input.isEmpty() || input.equalsIgnoreCase(CANCEL_KEYWORD)
                || input.toLowerCase(Locale.ROOT).startsWith("/");
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (cancelled) {
                prompt.onCancel().run();
            } else {
                prompt.onInput().accept(input);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        waiting.remove(event.getPlayer().getUniqueId());
    }
}
