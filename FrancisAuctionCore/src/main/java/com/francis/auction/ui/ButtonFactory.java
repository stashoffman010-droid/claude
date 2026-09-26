package com.francis.auction.ui;

import com.francis.auction.config.Messages;
import com.francis.auction.config.Settings;
import com.francis.auction.model.Category;
import com.francis.auction.model.SortOrder;
import com.francis.auction.util.ItemBuilder;
import com.francis.auction.util.Placeholders;
import com.francis.auction.util.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Builds every button from {@code messages.yml} and {@code config.yml}.
 *
 * <p>A lore line that is nothing but a block placeholder - {@code {categories}}
 * or {@code {orders}} - is replaced by however many lines that block needs,
 * which is what lets the category button list all six tabs.
 */
public final class ButtonFactory {

    private final Supplier<Settings> settings;
    private final Supplier<Messages> messages;

    public ButtonFactory(Supplier<Settings> settings, Supplier<Messages> messages) {
        this.settings = settings;
        this.messages = messages;
    }

    /** The slot {@code key} is configured for, or {@code -1} when it is disabled. */
    public int slot(String key) {
        Settings.Button button = settings.get().button(key);
        return button == null ? -1 : button.slot();
    }

    public boolean isEnabled(String key) {
        return settings.get().button(key) != null;
    }

    /** A navigation bar button using the icon configured for it. */
    public ItemStack navigation(String key, Map<String, String> placeholders) {
        return navigation(key, placeholders, Map.of());
    }

    public ItemStack navigation(String key, Map<String, String> placeholders,
                                Map<String, List<String>> blocks) {
        Settings.Button button = settings.get().button(key);
        return button == null ? null : render(button.icon(), key, placeholders, blocks);
    }

    /** The configured icon for a navigation button, or {@code fallback}. */
    public Material navigationIcon(String key, Material fallback) {
        Settings.Button button = settings.get().button(key);
        return button == null ? fallback : button.icon();
    }

    /** The configured icon for a dialog button, or {@code fallback}. */
    public Material dialogIcon(String key, Material fallback) {
        return settings.get().dialogIcon(key, fallback);
    }

    /** A button inside one of the smaller dialogs, with an explicit icon. */
    public ItemStack dialog(String key, Material icon, Map<String, String> placeholders) {
        return render(icon, key, placeholders, Map.of());
    }

    /** The pane used to pad the navigation bar. */
    public ItemStack filler() {
        return ItemBuilder.of(settings.get().filler())
                .name(orEmpty(messages.get().rawLine("buttons.filler.name")))
                .lore(List.of())
                .clean()
                .build();
    }

    /** Renders the six category lines for the {@code ITEMS CATEGORY} button. */
    public List<String> categoryBlock(Category selected) {
        Messages text = messages.get();
        String selectedPattern = orDefault(text.rawLine("buttons.category.entry-selected"), "&b▶ {category}");
        String otherPattern = orDefault(text.rawLine("buttons.category.entry-unselected"), "&7▷ {category}");
        List<String> lines = new ArrayList<>(Category.values().length);
        for (Category category : Category.values()) {
            String pattern = category == selected ? selectedPattern : otherPattern;
            lines.add(Text.fill(pattern, Placeholders.of("category", text.category(category))));
        }
        return lines;
    }

    /** Renders the sort lines for the {@code SORT ORDER} button. */
    public List<String> sortBlock(SortOrder selected) {
        Messages text = messages.get();
        String selectedPattern = orDefault(text.rawLine("buttons.sort.entry-selected"), "&6▶ {order}");
        String otherPattern = orDefault(text.rawLine("buttons.sort.entry-unselected"), "&7▷ {order}");
        List<String> lines = new ArrayList<>(SortOrder.values().length);
        for (SortOrder order : SortOrder.values()) {
            String pattern = order == selected ? selectedPattern : otherPattern;
            lines.add(Text.fill(pattern, Placeholders.of("order", text.sortOrder(order))));
        }
        return lines;
    }

    private ItemStack render(Material icon, String key, Map<String, String> placeholders,
                             Map<String, List<String>> blocks) {
        Messages text = messages.get();
        String name = orEmpty(text.rawLine("buttons." + key + ".name"));
        return ItemBuilder.of(icon)
                .name(Text.fill(name, placeholders))
                .lore(expand(text.rawLines("buttons." + key + ".lore"), placeholders, blocks))
                .clean()
                .build();
    }

    /** Applies placeholders line by line, growing block placeholders in place. */
    private List<String> expand(List<String> template, Map<String, String> placeholders,
                                Map<String, List<String>> blocks) {
        List<String> out = new ArrayList<>(template.size() + blocks.size() * 6);
        for (String line : template) {
            List<String> block = blocks.get(blockKey(line));
            if (block != null) {
                out.addAll(block);
            } else {
                out.add(Text.fill(line, placeholders));
            }
        }
        return out;
    }

    /** {@code "  {categories}  "} yields {@code "categories"}; anything else yields null. */
    private static String blockKey(String line) {
        String trimmed = line.trim();
        boolean wrapped = trimmed.length() > 2 && trimmed.startsWith("{") && trimmed.endsWith("}");
        return wrapped ? trimmed.substring(1, trimmed.length() - 1) : null;
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String orDefault(String value, String fallback) {
        return value == null ? fallback : value;
    }
}
