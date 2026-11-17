# Changelog

All notable changes to this project are documented here.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [0.2.0] - Unreleased

### Added

- The shipped set of merchant and loot titles (159): internet, trends and slang, football, pop culture, roles, anime,
  gaming and more, in categories of their own; the example titles are examples only again.
- Title name tags also from fishing; a chance per source (mineshaft and dungeon chests 85 %, fishing 50 %), the
  others stay vanilla name tags; 5 % are blank, and the first blank gives "Dud". A title name tag doesn't name mobs
  and can't go into an anvil. With every other title from name tags, a name tag gives "Completionist".
- "100%" for completing every advancement, and "100% Speedrunner" for the first on the server.
- Updates bring their changes to the shipped title sets, title by title, without overwriting the server's own;
  titles both changed are kept and listed in `/titles updates` (keep or take). `"enabled": false` on single titles.
  The game's `/reload` reloads the config and titles too. `/titles updates <title>` compares both versions in a dialog.
- `/titles give`, `take` and `list` work for offline players too (by name, as `/whitelist`).

### Fixed

- A Title Merchant lost its name tag when a player stood close to it: the game's "show trades" behaviour emptied the
  villager's hand. Merchants don't run it now (they have no vanilla trades to show).

## [0.1.0] - 2026-11-15

The first version, for trying it out on a server.

### Added

- Titles above the name, in the player list and in chat: colours, gradients, icons, side letters, animations; each
  player's own language (English, German) and German titles in male, female or neutral form; a chat format of its own.
- `/titles`: wearing titles, categories, pages, search and sorting, animated titles on or off, the German form.
- Title files in `config/player_titles/titles/`; the shipped sets: advancement titles (one per advancement and a
  "first on the server" title each, backfilled from the players' advancement files), examples, a secret title.
- Title Merchants: nitwits that pick up a name tag; daily offers per player for a daily currency, merchant levels,
  prices by reputation, the game's trading screen; through zombie villager and cure; nitwits from breeding; nitwits
  without bad gossip.
- Title name tags in loot chests.
- Options (`/titles config`), admin commands, permissions.
