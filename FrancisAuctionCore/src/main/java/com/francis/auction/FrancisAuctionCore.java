package com.francis.auction;

import com.francis.auction.command.AdminCommand;
import com.francis.auction.command.AuctionCommand;
import com.francis.auction.config.Messages;
import com.francis.auction.config.Settings;
import com.francis.auction.economy.EconomyHook;
import com.francis.auction.menu.MenuListener;
import com.francis.auction.service.AuctionService;
import com.francis.auction.service.ChatPrompts;
import com.francis.auction.storage.AuctionStorage;
import com.francis.auction.storage.YamlAuctionStorage;
import com.francis.auction.ui.ButtonFactory;
import com.francis.auction.util.Durations;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Francis Auction Core - a community auction house.
 *
 * <p>The plugin is assembled here and nowhere else: this class owns the
 * configuration, the market service and the menu factory, and every other
 * class reaches them through the accessors below.
 */
public final class FrancisAuctionCore extends JavaPlugin {

    /** How often expired listings are swept into their owners' collection boxes. */
    private static final long SWEEP_INTERVAL_TICKS = 20L * 30L;

    private Settings settings;
    private Messages messages;
    private ButtonFactory buttons;
    private AuctionService auctions;
    private ChatPrompts prompts;
    private final EconomyHook economy = new EconomyHook();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
        reloadSettings();

        if (!economy.hook(this)) {
            getLogger().warning("Vault was not found, so buying and selling are disabled "
                    + "until an economy plugin is installed.");
        }

        AuctionStorage storage = new YamlAuctionStorage(this, new File(getDataFolder(), "market.yml"));
        auctions = new AuctionService(this::settings, this::messages, economy, storage);
        auctions.load();

        prompts = new ChatPrompts(this);
        getServer().getPluginManager().registerEvents(prompts, this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);

        register("ah", new AuctionCommand(this));
        register("auctioncore", new AdminCommand(this));

        scheduleMaintenance();
        getLogger().info("Francis Auction Core is open for business.");
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
        if (auctions != null) {
            auctions.saveNow();
        }
    }

    /** Re-reads config.yml and messages.yml without restarting the server. */
    public void reloadSettings() {
        reloadConfig();
        settings = new Settings(getConfig(), getLogger());
        messages = new Messages(loadMessages());
        buttons = new ButtonFactory(this::settings, this::messages);
    }

    public Settings settings() {
        return settings;
    }

    public Messages messages() {
        return messages;
    }

    public ButtonFactory buttons() {
        return buttons;
    }

    public AuctionService auctions() {
        return auctions;
    }

    public ChatPrompts prompts() {
        return prompts;
    }

    public EconomyHook economy() {
        return economy;
    }

    /** Renders a duration with the patterns configured in messages.yml. */
    public String duration(long millis) {
        return Durations.format(millis, messages.timeUnits(), messages.expiredLabel());
    }

    // ---------------------------------------------------------------- wiring

    /**
     * Loads messages.yml from the data folder and backs it with the copy inside
     * the jar, so an outdated file never leaves a menu with missing lines.
     */
    private YamlConfiguration loadMessages() {
        YamlConfiguration onDisk =
                YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml"));
        try (InputStream bundled = getResource("messages.yml")) {
            if (bundled != null) {
                onDisk.setDefaults(YamlConfiguration.loadConfiguration(
                        new InputStreamReader(bundled, StandardCharsets.UTF_8)));
            }
        } catch (Exception failure) {
            getLogger().warning("Could not read the bundled messages.yml: " + failure.getMessage());
        }
        return onDisk;
    }

    private <T extends CommandExecutor & TabCompleter> void register(String name, T executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().severe("Command /" + name + " is missing from plugin.yml.");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void scheduleMaintenance() {
        getServer().getScheduler().runTaskTimer(this,
                () -> auctions.sweepExpired(), SWEEP_INTERVAL_TICKS, SWEEP_INTERVAL_TICKS);

        long autosave = settings.autosaveIntervalSeconds();
        if (autosave > 0L) {
            long ticks = autosave * 20L;
            getServer().getScheduler().runTaskTimer(this, () -> auctions.saveIfDirty(), ticks, ticks);
        }
    }
}
