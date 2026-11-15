# Player Titles

**Titles for players: a short styled text above the name, in the player list and in chat, collected from
advancements, Title Merchants and loot chests.**

A **server-side mod for Fabric and NeoForge** (Minecraft 26.3). Players join with an unmodded client, and the world
stays a valid vanilla world: remove the mod, and at worst the vanilla behaviour comes back.

**NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.**

> [!NOTE]
> **Written with AI.** Most of this project's code, texts and docs were written by an AI coding assistant (Claude by
> Anthropic), directed and partly tested by NR137. Not every feature has been tested in depth, so some things may not
> work as expected.

## ✨ Features

- **Titles above the name, in the player list and in chat:** colours, gradients, icons (any block or item texture of
  the game), side letters (the enchanting table's or the illagers' runes, "magic" letters), and animations (rainbow,
  pulse, wave) that the server redraws. Each player sees titles in their own language (English, German), German
  titles in the wearer's chosen form (male, female, neutral). Chat gets the title before the name and its description
  on hover, and a format of its own (`[name] message` by default).
- **`/titles`:** a chest screen with your titles: wear one with a click, categories, pages, search and sorting (a
  dialog), animated titles on or off for yourself, the German form of your titles.
- **Advancement titles:** every advancement gives its own title (and loses it when the advancement is lost, e.g. after
  a game update). The first player on the server to get one also gets its "first" title, which exists only once;
  backfilled from the players' advancement files when the mod is added to a running server.
- **Title Merchants:** a nitwit that picks up a dropped name tag becomes one ("Title Merchant" above its name). It
  offers each player their own titles, new every day (real-life date), the same at every merchant, for a daily
  currency of otherwise useless items (rotten flesh, poisonous potatoes, …); more offers with the player's merchant
  level, prices that follow the villager's gossip about the player, the game's own trading screen. It stays a merchant
  as a zombie villager and after curing; killing one may give a secret title. Bred villagers can become nitwits (1/12,
  vanilla: never), and nitwits can be removed without bad gossip.
- **Title name tags:** name tags in loot chests collect a random title when used.

## 🏷️ Title files

Titles are defined in `config/player_titles/titles/*.json`, one set per file. The mod's own sets (`advancements.json`,
`examples.json`, `special.json`) are written there once, so they can be changed or removed; a removed one stays
removed (`.shipped` lists what was written). A server adds its own set by dropping in a file, then `/titles reload`.

```json
{
  "name": "My titles",
  "enabled": true,
  "titles": [
    {
      "id": "my.wanderer",
      "text": {"en_us": "Wanderer", "de_de": "Wanderer", "de_de/female": "Wanderin", "de_de/neutral": "Wandernde"},
      "description": {"en_us": "Never stays in one place for long.", "de_de": "Bleibt nie lange an einem Ort."},
      "style": {"color": "#A0A0A0"},
      "rarity": "common",
      "sources": ["merchant", "loot"],
      "category": "travel"
    }
  ]
}
```

| Field | What |
| --- | --- |
| `id` | Unique; what players own (keep it when changing the text) |
| `text` | Per language; `de_de` is the male form, `de_de/female` and `de_de/neutral` the others (optional) |
| `description` | What the title stands for, per language (shown in `/titles`, on hover in chat) |
| `style` | How it looks (below) |
| `rarity` | `common`, `rare`, `epic` or `legendary`: how often it is offered and what it costs |
| `sources` | Where it comes from: `admin`, `merchant`, `loot`, `merchant_kill`, `advancement:<id>`, `first_advancement:<id>` |
| `category` | Its category in `/titles` (default: the file name); names from the language files or the set's `name` |

A text missing in a language falls back to `fallbackLanguage`, then to the first language the title has.

**Styles** (`"style"`), combined as wanted:

| Style | Example |
| --- | --- |
| One colour | `{"color": "#A0A0A0"}` |
| Gradient | `{"gradient": ["#2E86DE", "#48DBFB"]}` |
| Animation | `{"color": "#FF6B35", "animation": "pulse"}`, `{"gradient": [...], "animation": "wave"}`, `{"animation": "rainbow"}` |
| Icon at both ends | `{"icon": "block/fire_0"}` (moving textures move), `{"icon": "item/nether_star"}` |
| Several icons | `{"icons": ["item/nether_star", "item/map"]}` (in this order at the start, the other way round at the end) |
| Side letters | `{"sides": "magic"}`, `"runes"` (enchanting table), `"illager_runes"` |
| Whole title in runes | `{"font": "alt"}` or `"illageralt"` (unreadable: for rare titles) |
| Whole title as magic letters | `{"obfuscated": true}` (unreadable too) |
| Bold, italic | `{"bold": true}`, `{"italic": true}` (bold makes some letters hard to read) |

The advancement titles are decided in [`docs/titles-advancements.md`](docs/titles-advancements.md) and made from it by
`scripts/titles/advancement_titles.py`.

## ⚙️ Options

`config/player_titles/config.json`, read at every start. Most options can be changed in the game with
`/titles config <option> <value>` (right away, and saved); `/titles config` shows them all.

| Option | Default | What |
| --- | --- | --- |
| `aboveHead`, `playerList`, `chat` | `true` | Where worn titles show |
| `chatFormat` | `[%s] %s` | Chat messages: the name, then the message (vanilla: `<%s> %s`) |
| `language` | `player` | Titles in each player's language, or one for everyone (`en_us`, `de_de`); the screen and messages are always in the player's |
| `fallbackLanguage` | `en_us` | For languages the mod doesn't have, and texts missing in one |
| `defaultForm` | `male` | The German form for players who haven't chosen one |
| `animationTicks` | `2` | How often animated titles move (ten times a second), only for players near enough to read them |
| `playerListAnimations`, `playerListAnimationTicks` | `false`, `2` | Animated titles in the player list too |
| `merchant.villagers` | `nitwits` | Who becomes a Title Merchant: `nitwits`, `unemployed` (they then never take a job) or `both` |
| `merchant.screen` | `trading` | The game's trading screen, or `chest` |
| `merchant.offers`, `maxOffers`, `levels` | `3`, `7`, `5, 15, 30, 50` | Offers a day, one more per level (Apprentice to Master after that many titles bought) |
| `merchant.currency` | rotten flesh 1.5, poisonous potato 0.5, spider eye 1, pufferfish 0.5, glow lichen 1, leaf litter 2 | The daily currency, in turn, with a factor on the prices |
| `merchant.prices` | `8, 16, 32, 48` | Per rarity (common to legendary) |
| `merchant.slotChances` | | How likely each rarity is, per offer |
| `merchant.reputationPrices` | `true` | Prices follow the villager's gossip about the player, as in vanilla (from half to three times) |
| `merchant.breedNitwitChance` | `1/12` | Bred villagers become nitwits with this chance (vanilla: 0) |
| `merchant.nitwitKillsAffectReputation` | `false` | Hurting or killing a nitwit gives bad gossip (vanilla: yes); merchants always do |
| `merchant.killTitleChance` | `0.02` | The chance of the secret title for killing a merchant, plus 1 % per Looting level |
| `permissions` | | Who has each permission without a permission mod: `everyone` or `operators` |

## ⌨️ Commands

| Command | Who | What |
| --- | --- | --- |
| `/titles` | everyone | The title screen |
| `/titles wear <title>`, `/titles off` | everyone | Wear a title, take it off |
| `/titles search [text]` | everyone | The screen with matching titles |
| `/titles animations on\|off` | everyone | Animated titles move for you, or stand still (fewer packets) |
| `/titles form male\|female\|neutral` | everyone | The form of your titles (in German) |
| `/titles give\|take <players> <title>`, `/titles list <player>` | admins | Titles of players |
| `/titles first reset [advancement]` | admins | "First" titles free again |
| `/titles config [...]`, `/titles reload` | admins | The options; the config and title files again |

## 🔒 Permissions

Every command and gated action has a permission node, which a permission mod such as LuckPerms can manage. Without
one, player features are allowed for everyone and admin features for operators (both can be changed in
`permissions`).

| Node | Default | What |
| --- | --- | --- |
| `player_titles.use` | everyone | `/titles`, wearing titles |
| `player_titles.merchant` | everyone | Making Title Merchants (dropping the name tag) and trading with them |
| `player_titles.admin` | operators | Giving and taking titles, the config |

## 🧹 Removing the mod

- What players own and wear, the merchants and the "first" holders are in the mod's own file
  (`<world>/data/player_titles/players.dat`), which vanilla ignores.
- Titles above the name, the merchants' titles and what they hold, title name tags' names: only in the packets, gone
  with the mod.
- A Title Merchant is an ordinary villager with a name, holding a name tag (that it doesn't drop). A title name tag is
  an ordinary name tag (our mark stays in its `custom_data`, which vanilla keeps and ignores).
- Chat and the player list are vanilla again.

## 🛠️ Development

**Needs:** [Docker](https://docs.docker.com/get-docker/) (with Compose) · Minecraft 26.3 to play on the dev server

Everything else runs in one dev image (`docker/Dockerfile`): Java and Python in the versions of
[`.tool-versions`](.tool-versions) (the only place they are set), Gradle from its wrapper, a virtual screen for the
client tests. [`compose.yaml`](compose.yaml) runs it with your checkout mounted and the Gradle cache in a volume.
Compose rebuilds the image by itself when `docker/Dockerfile` or `.tool-versions` change.

**VS Code:** open the folder and run **Dev Containers: Reopen in Container** (extension *Dev Containers*). Terminal,
Gradle and Java tooling then run in the container, the commands below work without `docker compose run --rm dev`, and
the pre-commit hook is turned on.

The build and test commands live in [`tasks.sh`](tasks.sh):

```bash
docker compose run --rm dev ./tasks.sh build      # JARs in fabric/build/libs/ and neoforge/build/libs/
docker compose run --rm dev ./tasks.sh test       # the quick tests
docker compose run --rm dev ./tasks.sh all        # everything CI runs
docker compose run --rm dev                       # a shell in the dev image
docker compose up server                          # Fabric dev server (fabric/runs/server/); LOADER=neoforge for NeoForge
```

Connect with Minecraft to `localhost:25565`. The first start of a dev server stops until you accept Mojang's EULA in
`<loader>/runs/server/eula.txt`.

### Tests

| Task | What |
| --- | --- |
| `test` | The quick tests (`test-java`) |
| `test-java` | JUnit tests of `common/` (`common/src/test`) |
| `gametest` | Game tests on each loader's game test server, with Lithium (`common/src/gametest`, registered in `fabric/src/gametest` and `neoforge/src/gametest`) |
| `smoke` | The built JARs on the official Fabric and NeoForge servers; a **vanilla client** (the official client, no mods) joins and plays; then each world opens on the **official vanilla server without the mod** and must load, keep what it should and save without errors (needs `build` first; see [`scripts/README.md`](scripts/README.md)) |
| `client` | Singleplayer: each loader's dev client opens a copy of the smoke test's world (needs `smoke` first) |
| `changelog` | `CHANGELOG.md` is ready for a release: its top section is `mod_version` with a date, and there is no Unreleased section. With a base ref (`./tasks.sh changelog origin/main`), a change to the mod JAR must also raise `mod_version` |
| `changes` | What the changes since a base ref touch: `jar`, `tools` or `docs` (see Releasing) |
| `versions` | Every version the project uses against the newest one (Minecraft, loaders, Mixin, Java, Gradle and its plugins, JUnit, the dev image's tools, the workflows' actions and runners); fails when something can be updated |

The `tests` workflow (`.github/workflows/tests.yml`) runs them in the same image: on draft pull requests only the quick
tests, on other pull requests, pushes to `main` and before every release all of them. A pull request that only changes
texts skips them. The `versions` workflow runs `versions` on every pull request into `main` and by hand; it is not
required to merge.

A pre-commit hook runs the Java tests when a commit touches the code, in the dev container or through Docker. The dev
container turns it on; in another clone, run `git config core.hooksPath .githooks` once. `git commit --no-verify`
skips it.

### Releasing

The `release` workflow (`.github/workflows/release.yml`) only runs when started by hand: **Actions → release → Run
workflow**, or `gh workflow run release.yml`. It runs the tests first; if one fails, nothing is built or released.

Every change that changes the mod JAR is a release; the rest goes into `main` without one. `./tasks.sh changes` sorts
a pull request by its files:

| Kind | Files | Tests | Release |
| --- | --- | --- | --- |
| `jar` | `common/`, `fabric/`, `neoforge/`, `build-logic/`, the Gradle files, `LICENSE`, `.tool-versions` (but not the tests in them) | all | yes: `mod_version` must be raised |
| `tools` | the tests in those folders, `scripts/`, `docker/`, `.github/`, `compose.yaml`, `tasks.sh`, … | all | no |
| `docs` | `*.md` outside those folders, `docs/` | skipped (the checks pass) | no |

1. For a release, set `mod_version` in `gradle.properties` and give its `CHANGELOG.md` section a date
   (`## [x.y.z] - YYYY-MM-DD`), on the pull request's branch before merging.
2. Run the workflow without "draft release" to only build: the two JARs and the release notes are attached to the run
   for a day.
3. Run it with "draft release" to create a draft GitHub release `v<mod_version>+<minecraft_version>` with both JARs and
   that version's `CHANGELOG.md` section as notes; check it, then publish it on GitHub.

### Project layout

One codebase builds both loaders (based on [MultiLoader-Template](https://github.com/jaredlll08/MultiLoader-Template)):

| Folder | What |
| --- | --- |
| `common/` | Everything shared, including the mixins. Compiled against plain Minecraft, so it cannot use Fabric or NeoForge APIs. |
| `fabric/` | Fabric entry point, `fabric.mod.json` |
| `neoforge/` | NeoForge entry point, `neoforge.mods.toml` |
| `libs/nr137-lib/` | The shared library of NR137's mods (its own git repo) |
| `build-logic/` | Gradle scripts shared by the three projects |
| `docker/`, `compose.yaml`, `.devcontainer/` | The dev image, its tasks and the VS Code dev container |
| `scripts/` | Smoke, vanilla and singleplayer tests, version check ([README](scripts/README.md)), the advancement titles (`titles/`) |
| `docs/` | The design (`design.md`), the advancement titles (`titles-advancements.md`) |

Loader-specific needs (permissions, the config folder, versions) go through the shared library
[`libs/nr137-lib`](libs/nr137-lib) (`de.nr137.lib`, mod id `nr137_lib`): built with the mod (a Gradle composite build)
and packed into its JAR (Fabric `include`, NeoForge `jarJar`). It also has the config files, server-side translations,
chest screens, dialogs, items changed only in packets and text widths.

## 🧰 Built with

- **Mod:** Java with [Gradle](https://gradle.org), [Fabric Loom](https://github.com/FabricMC/fabric-loom) and
  [ModDevGradle](https://github.com/neoforged/ModDevGradle), in the project layout of
  [MultiLoader-Template](https://github.com/jaredlll08/MultiLoader-Template) (CC0)
- **Tests:** [JUnit](https://junit.org), Minecraft's game tests, [Python](https://www.python.org); the tests run with
  [Lithium](https://modrinth.com/mod/lithium) (LGPL-3.0, downloaded for testing, not part of the mod)
- **Dev environment:** [Docker](https://www.docker.com)

## 📄 License

MIT License, see [LICENSE](LICENSE). © NR137. The JAR contains no files of the game. Minecraft and the game's names
belong to Mojang and Microsoft.
