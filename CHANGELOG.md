# Changelog

All notable changes to this project are documented here.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [0.2.0] - Unreleased

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
