package com.francis.auction.model;

import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * The tabs offered by the {@code ITEMS CATEGORY} button.
 *
 * <p>Membership is derived from the material name rather than a hard coded
 * list, so items added by future Minecraft versions land in the right tab
 * without a code change.
 */
public enum Category {

    ALL {
        @Override
        public boolean matches(Material material) {
            return true;
        }
    },
    TOOLS {
        @Override
        public boolean matches(Material material) {
            String name = material.name();
            return endsWithAny(name, "_PICKAXE", "_AXE", "_SHOVEL", "_HOE")
                    || equalsAny(name, "SHEARS", "FLINT_AND_STEEL", "FISHING_ROD", "BRUSH",
                            "SPYGLASS", "COMPASS", "CLOCK", "LEAD", "NAME_TAG", "BUCKET");
        }
    },
    WEAPONS {
        @Override
        public boolean matches(Material material) {
            String name = material.name();
            return endsWithAny(name, "_SWORD", "_ARROW")
                    || equalsAny(name, "BOW", "CROSSBOW", "TRIDENT", "ARROW", "SHIELD", "MACE",
                            "FIREWORK_ROCKET", "TNT");
        }
    },
    ARMORS {
        @Override
        public boolean matches(Material material) {
            String name = material.name();
            return endsWithAny(name, "_HELMET", "_CHESTPLATE", "_LEGGINGS", "_BOOTS", "_HORSE_ARMOR")
                    || equalsAny(name, "ELYTRA", "TURTLE_HELMET", "SHIELD", "TOTEM_OF_UNDYING");
        }
    },
    FOOD {
        @Override
        public boolean matches(Material material) {
            return material.isEdible() || equalsAny(material.name(), "CAKE", "MILK_BUCKET", "HONEY_BOTTLE");
        }
    },
    MINERALS {
        @Override
        public boolean matches(Material material) {
            String name = material.name();
            return endsWithAny(name, "_INGOT", "_NUGGET", "_ORE", "_SCRAP", "_SHARD")
                    || startsWithAny(name, "RAW_")
                    || equalsAny(name, "DIAMOND", "EMERALD", "COAL", "CHARCOAL", "LAPIS_LAZULI",
                            "QUARTZ", "REDSTONE", "DIAMOND_BLOCK", "EMERALD_BLOCK", "GOLD_BLOCK",
                            "IRON_BLOCK", "COPPER_BLOCK", "NETHERITE_BLOCK", "COAL_BLOCK",
                            "LAPIS_BLOCK", "REDSTONE_BLOCK", "RAW_IRON_BLOCK", "RAW_GOLD_BLOCK",
                            "RAW_COPPER_BLOCK", "AMETHYST_SHARD");
        }
    };

    private static final Category[] VALUES = values();

    /** @return whether an item of this material belongs in the tab. */
    public abstract boolean matches(Material material);

    public boolean matches(ItemStack item) {
        return item != null && matches(item.getType());
    }

    /** The next tab in the order shown on the button, wrapping around. */
    public Category next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    /** The previous tab in the order shown on the button, wrapping around. */
    public Category previous() {
        return VALUES[(ordinal() + VALUES.length - 1) % VALUES.length];
    }

    /** Resolves a stored name, falling back to {@link #ALL}. */
    public static Category from(String name) {
        if (name != null) {
            for (Category category : VALUES) {
                if (category.name().equalsIgnoreCase(name.trim())) {
                    return category;
                }
            }
        }
        return ALL;
    }

    private static boolean endsWithAny(String name, String... suffixes) {
        for (String suffix : suffixes) {
            if (name.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean startsWithAny(String name, String... prefixes) {
        for (String prefix : prefixes) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean equalsAny(String name, String... exact) {
        String upper = name.toUpperCase(Locale.ROOT);
        for (String candidate : exact) {
            if (upper.equals(candidate)) {
                return true;
            }
        }
        return false;
    }
}
