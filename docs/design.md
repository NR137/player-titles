# Player Titles: design

Draft, agreed with NR137 on 2026-11-15. Server-side only, the world stays vanilla-valid (see `../CLAUDE.md`). Points
marked **(to check)** depend on game behaviour not yet verified in the 26.3 code.

## The idea

Titles are a cosmetic for players: a short text with a style, shown above the player's name, in the player list and
in chat. Players collect titles (advancements, the Title Merchant, name tags from chests, later gifts and statistics)
and pick one to wear. No currency, no crates (`../CLAUDE.md`).

## Phases

1. **Core** (with the shared library): title definitions, owning and wearing titles, showing them, the title screen,
   advancement titles (and "first on the server" titles, backfilled).
2. **Title Merchant and chest loot.**
3. **Gift titles** (players create a title for someone else; admins can block them).
4. **Statistics titles** (best players, with Stats WebUI Plus's ranking) and the Titles pages of Stats WebUI Plus.

No releases needed in between.

## Title definitions

- Files in `config/player_titles/titles/*.json`, each one set of titles; the mod ships its sets (English and German)
  and writes them there, so they can be changed or turned off (`"enabled": false`, per title or set); updates bring
  their changes title by title without overwriting the server's (README: "Title files"). A server adds its own sets by
  dropping in a file (titles about that server only, its insiders and friends, belong there, never in the
  mod's own sets).
- A title: `id` (unique), `text` and `description` per language (`en_us`, `de_de`; English or a configured language
  as the fallback), `style`, `rarity` (common, rare, epic, legendary: decides how often it is offered and what it
  costs), and its sources.
- Styles: a colour, a gradient, bold/italic, built-in icons from vanilla glyphs or sprites **(to check: what 26.3
  text can show without a resource pack)**, and animations (rainbow, pulse, wave) that the server redraws a few times
  a second.
- Format documented in the README, with an example per style.

## Owning and wearing

- What each player owns (title, where from, when) and wears is saved in the world, in the mod's own file
  (`data/player_titles/...`), which vanilla ignores.
- `/titles` opens a chest screen with the player's titles (styled name, description, where and when it was obtained);
  clicking one wears it, one button takes it off. Pages for many titles.
- Every player sees titles in their own language (the server knows each client's language).

## Showing titles

- **Above the name:** a text display entity riding the player, which the server only sends to other players and
  never saves; hidden while the player sneaks, is invisible or a spectator, like the name. **(to check: position over
  the name tag, also when the player sits or rides.)**
- **Player list:** the player's display name in the list with the title, sent per viewer (their language).
- **Chat:** the title before the name. The name isn't part of the message signature, so this doesn't touch it.
- Each of the three can be switched off in the server's config.

## Advancement titles

- Every advancement gives its own title (not the advancement's name, a fitting one; harder advancements, finer
  styles). These titles can't be got any other way.
- The first player on the server to get an advancement also gets its "first" title, more precious. Backfilled at the
  first start from the dates the game stores for every advancement, and kept up from then on.
- **Advancements can change with game versions** (Adventuring Time gets new biomes, A Complete Catalogue new cat
  variants), so a player can lose one they had: their advancement title goes with it (taken off if worn) and comes
  back when they complete it again. The same when an admin revokes it (`/advancement revoke`).
- **A "first" title stays** once given, even if its holder loses the advancement, and each exists only once: a player
  who completes a changed advancement later doesn't get it, because it already has its holder. Which player holds
  which "first" title is kept in the mod's own file.
- **All advancements:** a player who has completed every advancement with a title gets "100%" (taken again if they lose
  one), the first on the server "100% Speedrunner" (kept, backfilled like the others).
- Getting a title one already has changes nothing (any title, from any source).

## Title Merchant (phase 2)

- A villager becomes a Title Merchant by picking up a dropped name tag: it holds the name tag, gets a name and offers
  titles. Which villagers can: option `merchant.villagers` (also `/titles config merchant …`), `nitwits` (default), `unemployed` or `both`. A villager
  without a name gets one: the name tag's own (renamed in an anvil), or one from our list (Ambrose, Ottilie, …). It is
  the villager's vanilla name (shown as vanilla shows names: when looked at, close by) and a name tag can change it.
  Above it the merchant shows the title "Title Merchant" (animated, name tags at both ends), like a player's title.
  When it dies it drops nothing it holds; its killer gets the secret title "Bad for Business" / "Geschäftsschädigend"
  with a small chance (`merchant.killTitleChance`, 2 %, and 1 % more per Looting level, as the game's rare drops;
  titles with the source `merchant_kill`). Players see it hold today's currency (and the name tag in its other hand, which the
  game doesn't draw); that is only in the packets, saved it holds the name tag. The player who drops the name tag needs `player_titles.merchant`.
- After removing the mod it is an ordinary villager with a name holding a name tag; which villagers are merchants is
  kept in the mod's own file, not on the entity.
- **Offers** are per player and the same at every merchant (building more merchants changes nothing), new every day
  (the server's date, at midnight): titles with the source `merchant` the player doesn't have, the rarer ones less
  often (see the chances per offer below). Players start with 3 offers a day; one more with each level (after
  5, 15, 30 and 50 titles bought: `merchant.offers`, `maxOffers`, `levels`), up to 7 at Master. A title bought today stays shown as
  bought until tomorrow.
- **Payment:** a "currency" item that changes daily, in turn from a configurable list of items players get anyway
  and can't do much with, each with a factor on the prices: the easier to get many of, the more it takes
  (`merchant.currency`: rotten flesh 1.5, poisonous potato 0.5, spider eye 1, pufferfish 0.5, glow lichen 1 (common,
  but rarely collected), leaf litter 2); more for rarer titles
  (`merchant.prices`: 8, 16, 32, 48).
- **Chances per offer:** each offer, in order, has its own chances for the rarities (`merchant.slotChances`): the
  first mostly common (80 % common, 20 % rare), the seventh only epic and legendary (40/60). First the rarity is drawn
  (among those the player has titles of left), then one of its titles. With `merchant.reputationPrices` (default on) the price follows the merchant's
  gossip about the player, as vanilla villagers' prices do: one hit 25 % dearer, killing a villager nearby about 125 %,
  trading (each purchase counts as one) and curing cheaper, and the Hero of the Village discount; from half to three
  times the price.
- **The screen** (`merchant.screen`): `trading` (default), the game's own trading screen: each offer is "currency →
  a name tag named after the title" (description and rarity in its tooltip); a change by reputation shows as the
  game shows it, the base price crossed out; prices over 64 take the second slot (128 at most); bought ones are sold
  out. Taking the result gives the title, the name tag never stays (and can't be dropped from the slot), and a
  shift-click buys one title, not the next ones too (as the game would). The merchant's level is the player's:
  Novice, Apprentice after 5 titles, Journeyman after 15, Expert after 30, Master after 50 (`merchant.levels`), with
  the game's bar. `chest`: a chest screen with the offers in the middle row and today's currency at the top.
- **Unemployed merchants** never take a job: when the game would give them one, they stay unemployed and the
  workstation is free again for others. Villagers with a job can't become merchants.
- **Nitwits from breeding:** in vanilla, bred villagers are always born unemployed and can never become nitwits
  (Mojira MC-156556, reported as a bug, intent unknown; the code sets it on purpose). Option `breedNitwitChance`:
  bred villagers become nitwits with this chance, default 1/12 (the share of nitwits among a village's villagers in
  vanilla's village structures), 0 for vanilla.
- **Killing nitwits:** option `nitwitKillsAffectReputation`, default off (Title Merchants don't count as nitwits here): hurting or killing a nitwit doesn't give
  the witnesses negative gossip about the player (vanilla: minor negative for hurting, major negative for killing,
  which raises prices and can turn iron golems against the player), so unwanted nitwits from breeding can be removed.

## Name tags from chests (phase 2)

- Name tags in loot chests (and chest minecarts) get our mark (`custom_data`) when the loot is made: a title name
  tag. Using it into the air collects a random title the player doesn't have, from those with the source `loot`
  (weighted like the merchant's), and uses it up; on a mob it names the mob as usual. Its name ("Title Name Tag"),
  lore and glint exist only in the packets, so in vanilla it is an ordinary name tag again.

- **Chances:** a name tag from loot becomes a title name tag with a chance per source (`loot.markChances`: mineshaft
  and dungeon chests 85 %, fishing 50 %, others 50 %); the others stay vanilla name tags. A title name tag is blank
  with 5 % (`loot.blankChance`): used up, no title, a message; the first blank gives the title "Dud" / "Niete". A
  title name tag doesn't name mobs (a crafted name tag does) and can't be used in an anvil.
- **Having them all:** once a player has every title with the source `loot`, their next title name tag is blank for
  sure (and so gives "Dud" if they don't have it yet); after that one is blank with 80 % (`loot.lastBlankChance`), or
  gives "Completionist" / "Titelsammler" (source `loot_complete`, only from name tags, never from the merchant). Then
  title name tags can't be used any more; they can be kept or given away.

## Gift titles (phase 3)

- A rare offer or rare loot (e.g. 2 %): a gift title. Using it opens a dialog to write a new title (its text must not
  be one of the existing titles) with a choice of styles. The gift can only be given to other players, not used by
  the one who made it, and is hidden until redeemed. A gift title is never blank. Admins can block a gift title; it stays in players' lists, marked
  as blocked. A word filter for the text.

## Statistics titles (phase 4)

- Titles for the best players (e.g. most mobs killed, highest rank by Stats WebUI Plus's ranking), checked regularly;
  a player who loses the lead loses the title. Stats WebUI Plus gets Titles pages (only titles someone owns).

## Permissions (defaults without a permission mod)

| Node | Default | What |
| --- | --- | --- |
| `player_titles.use` | everyone | `/titles`, wearing titles |
| `player_titles.merchant` | everyone | making and using Title Merchants |
| `player_titles.admin` | operators | giving and taking titles, blocking gift titles, the config |

## Languages

English and German only (`../CLAUDE.md`), for title texts, the screen and all messages.
