"""What the smoke test (smoke_test.py) does with this mod's features. Each mod fills this file in.

The smoke test calls these in order:
  configure(folder)                 before the modded server starts: write config files into folder/config
  with_mod(server, check)           the modded server runs, without players: leave in the world what the mod saves
  with_client(server, name, check)  a vanilla client is online as `name` (an operator): what players without the mod
                                    see and do
  on_vanilla(server, check)         the same world on the vanilla server, the mod removed: what must have survived,
                                    and what must be plain vanilla again
`server.command(line)` types a console command; `server.run(line, expect, what)` types one and checks that an output
line matches the regex `expect`. `check(ok, what)` records a check.

expected_problems and expected_vanilla_warnings filter out log lines this scenario causes on purpose. TEST_MODS lists
other mods (Modrinth project ids) the modded servers run with, for testing only.

The example below stands for the rule every mod follows (../CLAUDE.md, "The world stays vanilla-valid"): the mod's own
data rides along in `custom_data`, which vanilla keeps but ignores.
"""

import time
from pathlib import Path

# Mods the servers run with besides ours (from Modrinth, newest for the version and loader, test only): Lithium, which
# many servers run, changes how block entities tick and hoppers move items.
TEST_MODS = ['lithium']

# A spot in the flat world above the grass (y = -61), kept loaded with /forceload.
CHEST = '0 -60 0'
# Where the Title Merchant stands: in the same chunk, next to where the client joins.
MERCHANT = '4.5 -60 4.5'


def configure(folder: Path) -> None:
	pass


def with_mod(server, check) -> None:
	server.run('forceload add 0 0', r'[Mm]arked chunk|already marked', 'the test chunk stays loaded')
	server.run(f'setblock {CHEST} minecraft:chest', r'Changed the block', 'a chest is placed')
	server.run(f'item replace block {CHEST} container.0 with minecraft:name_tag[minecraft:custom_data={{player_titles:{{test:1b}}}}]',
		r'Replaced \d+ slot', 'an item with the mod\'s custom_data goes into the chest')


def with_client(server, name: str, check) -> None:
	server.run(f'give {name} minecraft:name_tag[minecraft:custom_data={{player_titles:{{test:1b}}}}]',
		rf'Gave 1 .* to {name}', 'the vanilla client gets an item with the mod\'s custom_data')
	# Wearing an animated title: the client gets the player list name and passengers packets with the title (its own
	# display isn't sent to it, but the passengers packet names it) for a few seconds of animation.
	server.run(f'titles give {name} example_rainbow', r'Gave the title example_rainbow to 1 players',
		'an admin gives the vanilla client a title')
	server.command(f'execute as {name} run titles wear example_rainbow')
	server.run(f'titles list {name}', r'example_rainbow \* ', 'the vanilla client wears it (* in the list)')
	# Advancement titles follow the advancements: granting gives the title (and to the first one its "first" title),
	# revoking takes only the normal one.
	server.command(f'advancement grant {name} only minecraft:story/mine_stone')
	server.run(f'titles list {name}', r'first\.story\.mine_stone', 'an advancement gives its title and the first one')
	server.command(f'advancement revoke {name} only minecraft:story/mine_stone')
	server.run(f'titles list {name}', r'has 2 titles', 'revoking it takes its title, the first one stays')
	# A Title Merchant next to the client: a nitwit picks up a name tag. Its title and name above it, and the currency in
	# its hand, exist only in the packets the client gets.
	server.run(f'summon minecraft:villager {MERCHANT} {{VillagerData:{{profession:"minecraft:nitwit",type:"minecraft:plains",level:1}},'
		'CanPickUpLoot:1b,PersistenceRequired:1b,Tags:["smoke_merchant"]}', r'Summoned new', 'a nitwit is summoned')
	server.run(f'summon minecraft:item {MERCHANT} {{Item:{{id:"minecraft:name_tag",count:1}},PickupDelay:0s}}', r'Summoned new',
		'a name tag lies at its feet')
	ask_until(server, check, 'data get entity @e[type=minecraft:villager,tag=smoke_merchant,limit=1] CustomName',
		r'has the following entity data', 'the nitwit took it and is a Title Merchant (it has a name)')
	time.sleep(2)
	# A title name tag from a loot chest: its name, lore and glint exist only in the packets; the client must take them.
	server.run(f'give {name} minecraft:name_tag[minecraft:custom_data={{player_titles:{{title_tag:1b}}}}]',
		rf'Gave 1 .* to {name}', 'the vanilla client gets a title name tag')
	time.sleep(3)


def ask_until(server, check, command: str, expect: str, what: str, tries: int = 10) -> None:
	"""Runs a command again (every 2 seconds) until an output line matches: for what the game does a moment later."""
	for _ in range(tries):
		with server.lock:
			start = len(server.output)
		server.command(command)
		if server.wait_for(expect, 2, start) is not None:
			check(True, f'{what} (/{command})')
			return
	check(False, f'{what} (/{command})')


def on_vanilla(server, check) -> None:
	server.run(f'data get block {CHEST} Items[0]', r'player_titles', 'vanilla keeps the mod\'s custom_data on the item')
	# The game loads a chunk's entities a moment after the chunk: ask again until the villager is there.
	ask_until(server, check, 'data get entity @e[type=minecraft:villager,tag=smoke_merchant,limit=1] equipment.mainhand.id',
		r'minecraft:name_tag', 'the Title Merchant is an ordinary villager holding its name tag')


def expected_problems(lines: list[str]) -> list[str]:
	return lines


def expected_vanilla_warnings(lines: list[str]) -> list[str]:
	return lines
