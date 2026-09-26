# Francis Auction Core

A community auction house for Spigot / Paper 1.21, rebuilt from the old
`DonutAuction` plugin as a clean, readable source project.

* **Plugin name:** `FrancisAuctionCore`
* **Author:** Francis
* **Commands:** `/ah` (aliases `/auction`, `/auctionhouse`, `/francisauction`),
  `/auctioncore reload` (alias `/fac`)

## Building

The jar is built with Maven and needs the Spigot API, which lives on Spigot's
own repository rather than Maven Central:

```
mvn -f FrancisAuctionCore/pom.xml clean package
```

The jar lands in `FrancisAuctionCore/target/FrancisAuctionCore-2.0.0.jar`.
Drop it in `plugins/`, restart, and the plugin writes `config.yml`,
`messages.yml` and `market.yml` into `plugins/FrancisAuctionCore/`.

Vault plus an economy plugin is required for buying and selling; without it
the plugin still loads and tells players the auction house is closed.

## Layout

```
com.francis.auction
├── FrancisAuctionCore      plugin entry point, owns every service
├── command/                /ah and /auctioncore
├── config/                 typed views over config.yml and messages.yml
├── economy/                Vault hook
├── menu/                   every screen, one class each
├── model/                  Listing, ExpiredItem, TransactionRecord, Category, SortOrder
├── service/                the market rules and the chat prompt helper
├── storage/                YAML persistence
├── ui/                     builds buttons from the config files
└── util/                   colour codes, durations, prices, pagination
```

Nothing in `menu/` knows how the market works, and nothing in `service/`
knows a menu exists.

## Customising the menu

Every button is described in two places and needs no code change:

* `config.yml` → `gui.buttons.<name>.icon` and `.slot` — which item the button
  is and where it sits on the navigation bar.
* `messages.yml` → `buttons.<name>.name` and `.lore` — the tooltip.

Colours accept both `&a` legacy codes and `&#RRGGBB` hex codes. Because a
colour resets formatting in Minecraft, write `&#A6FF27&lTEXT`, not
`&l&#A6FF27TEXT`.

A lore line that contains nothing but `{categories}` or `{orders}` is replaced
by one line per entry, which is how the category button lists all six tabs.

### Placeholders

| Button          | Available placeholders                        |
| --------------- | --------------------------------------------- |
| `information`   | `{listings}` `{max}` `{expired}` `{page}` `{pages}` |
| `category`      | `{categories}` block                          |
| `sort`          | `{orders}` block                              |
| `search`        | `{query}`                                     |
| `your-items`    | `{listings}` `{max}`                          |
| `expired-items` | `{expired}`                                   |
| page buttons    | `{page}` `{pages}`                            |

## Notes on the rebuild

The supplied `DonutAuction-1.2.jar` was obfuscated with yGuard, so there was
no source to edit — this is a fresh implementation that keeps the same feature
set (browse, search, category filter, sort, buy confirmation, your listings,
expired collection box, transaction history, Vault economy, per-player listing
cap and listing expiry).

Data does **not** migrate from the old plugin: listings live in
`plugins/FrancisAuctionCore/market.yml` in a new format.
