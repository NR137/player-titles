# Merchant and loot titles

The default set of titles the Title Merchant offers and name tags from loot chests give, shipped with the mod as
`merchant-and-loot.json`: at the first start it is written to `config/player_titles/titles/`, where a server can edit it,
replace it or remove it (a removed set stays removed), and add sets of its own. This document is where the set is
made: `scripts/titles/merchant_and_loot_titles.py` turns its tables into the file.

Unlike the advancement titles, these don't stand for one deed: they are characters, ways to play, places, roles,
internet, trends and slang, football, pop culture (music, film, fashion), anime and gaming. Descriptions say what the
title stands for. German titles can be English words, as people use them ("Mending Enjoyer" stays that in German too).
Names of works, games, brands and people stay out: only common slang and terms. Titles about one server only (its
insiders, its friends) belong into that server's own set.

## How they work

- **Sources:** every title can come from both, the merchant (`merchant`) and name tags (`loot`), except the three
  unreadable ones, which only name tags give: they are a find, not something to buy.
- **Rarity** decides how often the merchant offers a title and what it costs (`merchant.slotChances`, `prices`), and
  how often a name tag gives it (common 60, rare 25, epic 12, legendary 3 in 100).
- **German forms:** male, female, neutral as for the advancement titles; titles that are the same for everyone (an
  adjective, "Nachteule", "Eisenherz") have one form.

## How they look

- **Common:** one colour. **Rare:** a gradient (one with rune side letters). **Epic:** a gradient and an icon at both
  ends (one with magic side letters, one pulsing). **Legendary:** animated (wave, pulse, rainbow) with an icon.
- **The unreadable ones:** one each in the enchanting table's runes, the illagers' runes and magic letters; otherwise
  runes and magic letters are only decoration.
- No bold. Every icon is checked against the game's textures (26.3).

## Notes

- A player who buys every day has a set of this size in a few months; more sets keep the merchant interesting longer.
- Similar themes as advancement titles (Angler / Fisher) are on purpose: those stand for a deed, these for a way to
  play.

## Internet, trends and slang (34)

| Id | Title (English; German male, female, neutral) | Description | Rarity, look |
| --- | --- | --- | --- |
| `main_character` | **Main Character**<br>**Main Character** | The story is clearly about them.<br>Die Geschichte dreht sich eindeutig um diese Person. | epic: gradient #FF9AA2→#FFD27F, icon `item/spyglass` |
| `npc` | **NPC**<br>**NPC** | Stands around and says the same thing every day.<br>Steht herum und sagt jeden Tag dasselbe. | common: colour #A0A0A0 |
| `aura_farmer` | **Aura Farmer**<br>**Aura Farmer** | Everything they do looks effortless. It isn't.<br>Alles sieht mühelos aus. Ist es nicht. | epic: gradient #FFF6A0→#C9A0FF, pulse, icon `item/amethyst_shard` |
| `touch_grass` | **Grass Toucher**<br>**Grass Toucher** | Has been outside. Once.<br>War draußen. Einmal. | common: colour #7FD47F |
| `no_life` | **No Life**<br>**No Life** | Online. Always.<br>Online. Immer. | rare: gradient #9A9A9A→#4A4A4A |
| `tryhard` | **Tryhard**<br>**Tryhard** | Takes every game far too seriously.<br>Nimmt jedes Spiel viel zu ernst. | rare: gradient #FF6B6B→#FFD200 |
| `noob` | **Noob**<br>**Noob** | Everybody starts somewhere.<br>Alle fangen mal klein an. | common: colour #9AD0FF |
| `pro_gamer` | **Pro Gamer**<br>**Pro Gamer** | Has the moves, and the excuses.<br>Hat die Moves, und die Ausreden. | rare: gradient #7DFFB0→#2E86DE |
| `gg` | **GG**<br>**GG** | Wins well, loses well.<br>Gewinnt fair, verliert fair. | common: colour #B6FF8A |
| `afk_legend` | **AFK Legend**<br>**AFK-Legende** | Present in body only.<br>Nur körperlich anwesend. | rare: gradient #A0A0C8→#5A5A7A |
| `mending_enjoyer` | **Mending Enjoyer**<br>**Mending Enjoyer** | Never lets a tool break.<br>Lässt kein Werkzeug kaputtgehen. | rare: gradient #7DFFB0→#1FAA59, icon `item/enchanted_book` |
| `dirt_house_enjoyer` | **Dirt House Enjoyer**<br>**Dirt House Enjoyer** | A roof is a roof.<br>Dach ist Dach. | common: colour #8B7355 |
| `side_quest_enjoyer` | **Side Quest Enjoyer**<br>**Side Quest Enjoyer** | Forgot the main goal three hours ago.<br>Hat das eigentliche Ziel vor drei Stunden vergessen. | rare: gradient #FFD27F→#E07A5F |
| `brainrot_enjoyer` | **Brainrot Enjoyer**<br>**Brainrot Enjoyer** | Has seen too many short videos.<br>Hat zu viele kurze Videos gesehen. | rare: gradient #FF9AA2→#7F00FF, sides magic |
| `copium` | **Copium Addict**<br>**Copium-Junkie** | Next time it will work. Surely.<br>Nächstes Mal klappt es. Ganz sicher. | common: colour #9AD0FF |
| `lowkey_legend` | **Lowkey Legend**<br>**Lowkey Legende** | Great at everything, tells nobody.<br>Gut in allem, erzählt es niemandem. | epic: gradient #E0E0E0→#7DFFB0, icon `item/totem_of_undying` |
| `final_boss` | **Final Boss**<br>**Final Boss** | The last thing you see before the credits.<br>Das Letzte, was man vor dem Abspann sieht. | legendary: gradient #FF4E00→#5A0E0E, wave, icon `item/nether_star` |
| `unc` | **Unc**<br>**Unc** | Has seen a few seasons come and go.<br>Hat schon ein paar Saisons kommen und gehen sehen. | rare: gradient #B5835A→#5A5A7A |
| `blind_box_enjoyer` | **Blind Box Enjoyer**<br>**Blind Box Enjoyer** | Has to open it to know what's inside. Again.<br>Muss es öffnen, um zu wissen, was drin ist. Schon wieder. | rare: gradient #FFB6E1→#FFD27F |
| `bro` | **Bro**<br>**Bro** | Always got your back.<br>Hält dir immer den Rücken frei. | common: colour #9AD0FF |
| `bestie` | **Bestie**<br>**Bestie** | Everyone's favourite.<br>Liebling von allen. | common: colour #FFB6E1 |
| `goat` | **GOAT**<br>**GOAT** | The greatest of all time. Allegedly.<br>Der Größte aller Zeiten. Angeblich. | legendary: gradient #FFE259→#FFA751, pulse, icon `item/goat_horn` |
| `w` | **W**<br>**W** | Only takes wins.<br>Nimmt nur Siege mit. | rare: gradient #7DFFB0→#17B45B |
| `l` | **L**<br>**L** | Took one. Took another.<br>Hat eins kassiert. Und noch eins. | common: colour #FF6B6B |
| `sus` | **Sus**<br>**Sus** | Acting a bit suspicious lately.<br>Benimmt sich in letzter Zeit etwas verdächtig. | common: colour #FF4E4E |
| `rizzler` | **Rizzler**<br>**Rizzler** | Charms every villager into a discount.<br>Bezirzt jeden Dorfbewohner zum Rabatt. | epic: gradient #FF9AA2→#FFD27F, icon `item/emerald` |
| `sigma` | **Sigma**<br>**Sigma** | Plays alone, wins alone.<br>Spielt allein, gewinnt allein. | rare: gradient #2B2B2B→#9A9A9A |
| `skill_issue` | **Skill Issue**<br>**Skill Issue** | It was not the lag.<br>Es lag nicht am Lag. | common: colour #C8C8C8 |
| `based` | **Based**<br>**Based** | Says what everyone thinks.<br>Sagt, was alle denken. | rare: gradient #FFD200→#FF6B6B |
| `cringe` | **Cringe**<br>**Cringe** | Owns it.<br>Steht dazu. | common: colour #C9A0FF |
| `slay` | **Slay**<br>**Slay** | Ate and left no crumbs.<br>Hat abgeliefert, ohne Reste. | epic: gradient #FF69B4→#FFD27F, wave, icon `item/diamond` |
| `no_cap` | **No Cap**<br>**No Cap** | Tells it like it is.<br>Sagt es, wie es ist. | common: colour #F7F08A |
| `delulu` | **Delulu**<br>**Delulu** | Delusional, and doing great.<br>Realitätsfern, und es läuft super. | rare: gradient #FFB6E1→#9AD0FF |
| `mid` | **Mid**<br>**Mid** | Not good, not bad. Just there.<br>Nicht gut, nicht schlecht. Einfach da. | common: colour #A0A0A0 |

## Football (13)

| Id | Title (English; German male, female, neutral) | Description | Rarity, look |
| --- | --- | --- | --- |
| `ball_wizard` | **Ball Wizard**<br>**Ballzauberer, Ballzauberin, Ballzaubernde** | The ball does what they say.<br>Der Ball macht, was hier gesagt wird. | epic: gradient #FFFFFF→#2E86DE, icon `item/slime_ball` |
| `football_god` | **Football God**<br>**Fußballgott, Fußballgöttin, Fußballgottheit** | Worshipped in the stands.<br>Auf den Rängen verehrt. | legendary: gradient #FFE259→#FFA751, pulse, icon `item/golden_helmet` |
| `bench_warmer` | **Bench Warmer**<br>**Bankdrücker, Bankdrückerin, Bankdrückende** | Ready when called. Rarely called.<br>Bereit, wenn gerufen. Selten gerufen. | common: colour #B5835A |
| `super_sub` | **Super Sub**<br>**Joker, Jokerin, Joker** | Comes on late, scores anyway.<br>Kommt spät rein, trifft trotzdem. | rare: gradient #B6FF8A→#FFD200 |
| `penalty_killer` | **Penalty Killer**<br>**Elfmeterkiller, Elfmeterkillerin, Elfmeterkiller** | Saves what others would concede.<br>Hält, was andere reinlassen. | rare: gradient #9AD0FF→#2E3A87 |
| `number_ten` | **The Number 10**<br>**Zehner, Zehnerin, Zehner** | Pulls the strings in midfield.<br>Zieht im Mittelfeld die Fäden. | rare: gradient #FFFFFF→#FF6B6B |
| `stoppage_time_hero` | **Stoppage Time Hero**<br>**Nachspielzeit-Held, Nachspielzeit-Heldin, Nachspielzeit-Legende** | Decides it in the 90+4th minute.<br>Entscheidet es in der 90.+4 Minute. | epic: gradient #FFD200→#FF4E00, pulse, icon `item/clock_00` |
| `var_victim` | **VAR Victim**<br>**VAR-Opfer** | Was offside by a toenail.<br>Stand einen Zehennagel im Abseits. | common: colour #FF9AA2 |
| `derby_winner` | **Derby Winner**<br>**Derbysieger, Derbysiegerin, Derbysiegende** | The only game that matters.<br>Das einzige Spiel, das zählt. | rare: gradient #FF6B6B→#8B0000 |
| `tiki_taka` | **Tiki-Taka**<br>**Tiki-Taka** | Pass, pass, pass, goal.<br>Pass, Pass, Pass, Tor. | rare: wave, gradient #7DFFB0→#2E86DE→#7DFFB0 |
| `kreisliga_legend` | **Kreisliga Legend**<br>**Kreisliga-Legende** | Famous in the village, unknown everywhere else.<br>Berühmt im Dorf, unbekannt im Rest der Welt. | common: colour #A8E07A |
| `captain` | **Captain**<br>**Kapitän, Kapitänin, Kapitänsbinde** | Wears the armband.<br>Trägt die Binde. | rare: gradient #FFFFFF→#C9A0FF |
| `ultra` | **Ultra**<br>**Ultra** | Loudest in the stands, rain or shine.<br>Am lautesten auf den Rängen, bei jedem Wetter. | common: colour #FF6B6B |

## Pop culture: music, film, fashion (24)

| Id | Title (English; German male, female, neutral) | Description | Rarity, look |
| --- | --- | --- | --- |
| `headliner` | **Headliner**<br>**Headliner** | Plays last, and everyone stays.<br>Spielt als Letztes, und alle bleiben. | epic: gradient #FF9AA2→#7F00FF, pulse, icon `item/music_disc_pigstep` |
| `one_hit_wonder` | **One-Hit Wonder**<br>**One-Hit-Wonder** | Had that one moment. Everyone still remembers it.<br>Hatte diesen einen Moment. Alle erinnern sich noch daran. | rare: gradient #FFD27F→#FF6B6B |
| `earworm` | **Earworm**<br>**Ohrwurm** | Impossible to get out of your head.<br>Geht nicht mehr aus dem Kopf. | common: colour #C9A0FF |
| `air_guitarist` | **Air Guitarist**<br>**Luftgitarrist, Luftgitarristin, Luftgitarre Spielende** | Plays every solo, holds no guitar.<br>Spielt jedes Solo, ohne Gitarre. | common: colour #FF6B6B |
| `mosh_pit_survivor` | **Mosh Pit Survivor**<br>**Moshpit-Überlebender, Moshpit-Überlebende, Moshpit-Überlebende** | Came out of the pit, mostly in one piece.<br>Kam aus dem Pit, größtenteils am Stück. | rare: gradient #9A9A9A→#FF4E00 |
| `karaoke_star` | **Karaoke Star**<br>**Karaoke-Star** | Never misses a high note. Or hits one.<br>Lässt keinen hohen Ton aus. Trifft aber auch keinen. | rare: gradient #FFF6A0→#FF9AA2 |
| `festival_veteran` | **Festival Veteran**<br>**Festival-Veteran, Festival-Veteranin, Festival-Erfahrene** | Knows mud, rain and the best spot at the stage.<br>Kennt Schlamm, Regen und den besten Platz vor der Bühne. | rare: gradient #A8E07A→#FFD27F |
| `bass_drop` | **Bass Drop**<br>**Bass Drop** | Arrives with a boom.<br>Kommt mit einem Wumms. | epic: gradient #7F00FF→#2E3A87, wave, icon `item/goat_horn` |
| `plot_twist` | **Plot Twist**<br>**Plot Twist** | Nobody saw them coming.<br>Hat niemand kommen sehen. | epic: gradient #D9B8FF→#3B1E6E, sides magic, icon `item/ender_eye` |
| `post_credits_scene` | **Post-Credits Scene**<br>**Post-Credits-Szene** | Shows up when everyone has left already.<br>Taucht auf, wenn alle schon gegangen sind. | rare: gradient #2E2A6B→#B9C7FF |
| `stunt_double` | **Stunt Double**<br>**Stuntdouble** | Takes the falls for everyone.<br>Fällt für alle anderen. | rare: gradient #FFB38A→#A0522D |
| `directors_cut` | **Director's Cut**<br>**Director's Cut** | Longer, and supposedly better.<br>Länger, und angeblich besser. | epic: gradient #F2E6C9→#B08D57, icon `item/spyglass` |
| `binge_watcher` | **Binge Watcher**<br>**Binge Watcher** | Just one more episode.<br>Nur noch eine Folge. | common: colour #6C7BD9 |
| `spoiler_alert` | **Spoiler Alert**<br>**Spoiler-Alarm** | Knows how it ends, and tells you.<br>Weiß, wie es ausgeht, und erzählt es. | common: colour #FF9AA2 |
| `popcorn_enjoyer` | **Popcorn Enjoyer**<br>**Popcorn Enjoyer** | Here for the drama, not to take part.<br>Für das Drama hier, nicht zum Mitmachen. | common: colour #FFE259 |
| `red_carpet` | **Red Carpet**<br>**Roter Teppich** | Every entrance is a premiere.<br>Jeder Auftritt ist eine Premiere. | legendary: gradient #FF4E4E→#8B0000, pulse, icon `item/golden_helmet` |
| `drip` | **Drip**<br>**Drip** | The outfit speaks first.<br>Das Outfit spricht zuerst. | epic: gradient #48DBFB→#C9A0FF, wave, icon `item/diamond_chestplate` |
| `fit_check` | **Fit Check**<br>**Fit Check** | Always ready for one.<br>Immer bereit dafür. | rare: gradient #FFD27F→#C9A0FF |
| `sneakerhead` | **Sneakerhead**<br>**Sneakerhead** | Owns more shoes than feet can wear.<br>Besitzt mehr Schuhe, als Füße tragen können. | rare: gradient #FFFFFF→#FF6B6B, icon `item/leather_boots` |
| `fashion_icon` | **Fashion Icon**<br>**Fashion-Ikone** | Wears it first, everyone else next season.<br>Trägt es zuerst, alle anderen nächste Saison. | legendary: rainbow, icon `item/amethyst_shard` |
| `vintage_lover` | **Vintage Lover**<br>**Vintage Lover** | Everything old is new again.<br>Alles Alte ist wieder neu. | common: colour #B08D57 |
| `old_money` | **Old Money**<br>**Old Money** | Quietly rich, loudly tasteful.<br>Leise reich, laut geschmackvoll. | epic: gradient #F2E6C9→#2D6A4F, icon `item/emerald` |
| `runway_ready` | **Runway Ready**<br>**Laufsteg-ready** | Walks every path like a runway.<br>Läuft jeden Weg wie einen Laufsteg. | rare: gradient #FF9AA2→#E0E0E0 |
| `y2k` | **Y2K**<br>**Y2K** | Dressed like the year 2000 never ended.<br>Angezogen, als wäre das Jahr 2000 nie vorbei. | common: colour #48DBFB |

## Roles (18)

| Id | Title (English; German male, female, neutral) | Description | Rarity, look |
| --- | --- | --- | --- |
| `boss` | **Boss**<br>**Boss** | Gives the orders around here.<br>Gibt hier die Befehle. | epic: gradient #FFD200→#C98A1B, icon `item/golden_helmet` |
| `servant` | **Servant**<br>**Diener, Dienerin, Dienende** | Always at your service.<br>Stets zu Diensten. | common: colour #B0B0C8 |
| `minion` | **Minion**<br>**Untertan, Untertanin, Untertan** | Does the work, gets none of the credit.<br>Macht die Arbeit, bekommt nichts vom Ruhm. | common: colour #F7F08A |
| `overlord` | **Overlord**<br>**Oberherr, Oberherrin, Oberhaupt** | Everything as far as the eye can see is theirs.<br>Alles, so weit das Auge reicht, gehört hierher. | legendary: gradient #8B0000→#2E2A6B, wave, icon `item/netherite_chestplate` |
| `rookie` | **Rookie**<br>**Rookie** | New here, and eager.<br>Neu hier, und voller Eifer. | common: colour #9AD0FF |
| `veteran` | **Veteran**<br>**Veteran, Veteranin, Erfahrene** | Has seen it all.<br>Hat schon alles gesehen. | rare: gradient #B5835A→#5A3A22 |
| `champion` | **Champion**<br>**Champion** | Beat everyone at least once.<br>Hat jeden mindestens einmal geschlagen. | epic: gradient #FFE259→#FFA751, icon `item/diamond_sword` |
| `hero` | **Hero**<br>**Held, Heldin, Heldenhaft** | Shows up when it matters.<br>Ist da, wenn es darauf ankommt. | rare: gradient #FFFFFF→#2E86DE |
| `villain` | **Villain**<br>**Bösewicht** | Every story needs one.<br>Jede Geschichte braucht einen. | rare: gradient #5A0E0E→#7F00FF |
| `sidekick` | **Sidekick**<br>**Sidekick** | Always one step behind the hero.<br>Immer einen Schritt hinter dem Helden. | common: colour #A8E07A |
| `knight` | **Knight**<br>**Ritter, Ritterin, Ritterlich** | Sworn to protect.<br>Zum Schutz geschworen. | rare: gradient #D8D8D8→#2E3A87, icon `item/iron_sword` |
| `squire` | **Squire**<br>**Knappe, Knappin, Knappe** | Carries the gear, dreams of the armour.<br>Trägt die Ausrüstung, träumt von der Rüstung. | common: colour #C8B08A |
| `jester` | **Jester**<br>**Hofnarr, Hofnärrin, Spaßmacher** | Makes the whole server laugh.<br>Bringt den ganzen Server zum Lachen. | rare: rainbow |
| `wizard` | **Wizard**<br>**Zauberer, Zauberin, Zaubernde** | Knows a spell for everything.<br>Kennt für alles einen Zauber. | epic: gradient #C9A0FF→#2E2A6B, sides runes, icon `item/blaze_rod` |
| `rebel` | **Rebel**<br>**Rebell, Rebellin, Rebellisch** | Doesn't follow the rules. Any of them.<br>Hält sich an keine Regeln. An keine einzige. | rare: gradient #FF6B6B→#2B2B2B |
| `outlaw` | **Outlaw**<br>**Gesetzloser, Gesetzlose, Gesetzlos** | Wanted in three villages.<br>In drei Dörfern gesucht. | rare: gradient #B5835A→#8B0000 |
| `peasant` | **Peasant**<br>**Bauer, Bäuerin, Landvolk** | Humble, hungry, hard-working.<br>Bescheiden, hungrig, fleißig. | common: colour #A0A070 |
| `chief` | **Chief**<br>**Häuptling** | The village listens when they speak.<br>Das Dorf hört zu, wenn hier gesprochen wird. | epic: gradient #FFB38A→#A0522D, icon `item/goat_horn` |

## Anime (11)

| Id | Title (English; German male, female, neutral) | Description | Rarity, look |
| --- | --- | --- | --- |
| `uwu` | **UwU**<br>**UwU** | Too cute to handle.<br>Zu süß, um damit klarzukommen. | epic: gradient #FFB6E1→#FF69B4, pulse, icon `item/pink_petals` |
| `owo` | **OwO**<br>**OwO** | What's this?<br>Was ist das denn? | rare: gradient #FFB6E1→#C9A0FF |
| `senpai` | **Senpai**<br>**Senpai** | Finally noticed you.<br>Hat dich endlich bemerkt. | epic: gradient #FFB6E1→#FF6B6B, icon `block/cherry_sapling` |
| `kouhai` | **Kouhai**<br>**Kouhai** | Still waiting to be noticed.<br>Wartet noch darauf, bemerkt zu werden. | common: colour #FFB6E1 |
| `weeb` | **Weeb**<br>**Weeb** | Has watched more anime than is healthy.<br>Hat mehr Anime geschaut, als gesund ist. | rare: gradient #FF69B4→#7F00FF |
| `otaku` | **Otaku**<br>**Otaku** | Knows every episode by heart.<br>Kennt jede Folge auswendig. | rare: gradient #C9A0FF→#48DBFB |
| `kawaii` | **Kawaii**<br>**Kawaii** | Simply adorable.<br>Einfach entzückend. | common: colour #FFB6E1 |
| `baka` | **Baka**<br>**Baka** | It's not like they wanted this title or anything.<br>Nicht, dass dieser Titel gewollt war oder so. | common: colour #FF6B6B |
| `tsundere` | **Tsundere**<br>**Tsundere** | Cold outside, warm inside. Don't tell anyone.<br>Außen kalt, innen warm. Nichts verraten. | epic: gradient #9AD0FF→#FF69B4, wave, icon `item/heart_of_the_sea` |
| `chibi` | **Chibi**<br>**Chibi** | Small, round, unstoppable.<br>Klein, rund, unaufhaltsam. | common: colour #FFD27F |
| `nyan` | **Nyan**<br>**Nyan** | Part cat, part rainbow.<br>Teils Katze, teils Regenbogen. | legendary: rainbow, icon `item/cod` |

## Gaming (8)

| Id | Title (English; German male, female, neutral) | Description | Rarity, look |
| --- | --- | --- | --- |
| `plot_armor` | **Plot Armor**<br>**Plot Armor** | Should have died ten times. Didn't.<br>Hätte zehnmal sterben müssen. Ist es nicht. | epic: gradient #D8D8D8→#7A7A7A, icon `item/totem_of_undying` |
| `lore_master` | **Lore Master**<br>**Lore Master** | Knows the story behind every block.<br>Kennt die Geschichte hinter jedem Block. | rare: gradient #F2E6C9→#B08D57, icon `item/book` |
| `speedrunner` | **Speedrunner**<br>**Speedrunner, Speedrunnerin, Speedrunner** | Any% or nothing.<br>Any% oder gar nicht. | epic: gradient #FFD200→#FF4E00, wave, icon `item/compass_00` |
| `easter_egg` | **Easter Egg**<br>**Easter Egg** | Hidden where nobody looks.<br>Versteckt, wo niemand hinsieht. | rare: rainbow |
| `glitch_hunter` | **Glitch Hunter**<br>**Glitch Hunter** | Finds the bug before the developers do.<br>Findet den Bug vor den Entwicklern. | epic: gradient #7DFFE0→#7F00FF, sides magic, icon `item/echo_shard` |
| `one_more_turn` | **One More Night**<br>**Nur noch eine Nacht** | Says that every night.<br>Sagt das jede Nacht. | common: colour #6C7BD9 |
| `certified_classic` | **Certified Classic**<br>**Certified Classic** | Never goes out of style.<br>Kommt nie aus der Mode. | legendary: gradient #FFE259→#C98A1B, wave, icon `item/music_disc_pigstep` |
| `respawn_enjoyer` | **Respawn Enjoyer**<br>**Respawn Enjoyer** | Death is just a shortcut home.<br>Der Tod ist nur eine Abkürzung nach Hause. | common: colour #FF9AA2 |

## General: common (18)

| Id | Title (English; German male, female, neutral) | Description | Rarity, look |
| --- | --- | --- | --- |
| `homebody` | **Homebody**<br>**Stubenhocker, Stubenhockerin, Stubenhockende** | Rarely leaves the base.<br>Verlässt selten die Basis. | common: colour #C8B08A |
| `night_owl` | **Night Owl**<br>**Nachteule** | Plays best after dark.<br>Spielt am besten nach Einbruch der Dunkelheit. | common: colour #6C7BD9 |
| `early_bird` | **Early Bird**<br>**Frühaufsteher, Frühaufsteherin, Frühaufstehende** | Up with the sun.<br>Steht mit der Sonne auf. | common: colour #FFD27F |
| `builder` | **Builder**<br>**Erbauer, Erbauerin, Erbauende** | Always has a project going.<br>Hat immer ein Projekt am Laufen. | common: colour #D9A066 |
| `digger` | **Digger**<br>**Buddler, Buddlerin, Buddelnde** | Happiest underground.<br>Am glücklichsten unter Tage. | common: colour #8B7355 |
| `farmhand` | **Farmhand**<br>**Feldarbeiter, Feldarbeiterin, Feldarbeitende** | Knows every crop by name.<br>Kennt jede Feldfrucht beim Namen. | common: colour #A8E07A |
| `stargazer` | **Stargazer**<br>**Sterngucker, Sternguckerin, Sternguckende** | Spends the nights looking up.<br>Verbringt die Nächte mit Blick nach oben. | common: colour #B9C7FF |
| `hoarder` | **Hoarder**<br>**Hamsterer, Hamsterin, Hamsternde** | Never throws anything away.<br>Wirft nie etwas weg. | common: colour #C9A0FF |
| `tinkerer` | **Tinkerer**<br>**Tüftler, Tüftlerin, Tüftelnde** | Redstone is a hobby.<br>Redstone ist ein Hobby. | common: colour #FF6B6B |
| `wayfarer` | **Wayfarer**<br>**Reisender, Reisende, Reisende** | Always on the road.<br>Immer unterwegs. | common: colour #9AD0FF |
| `fisher` | **Fisher**<br>**Fischer, Fischerin, Fischende** | Patient by the water.<br>Geduldig am Wasser. | common: colour #7FE3E0 |
| `lumberjack` | **Lumberjack**<br>**Holzfäller, Holzfällerin, Holzfällende** | Has cleared a forest or two.<br>Hat schon den einen oder anderen Wald gerodet. | common: colour #B5835A |
| `cook` | **Cook**<br>**Koch, Köchin, Kochende** | Never goes hungry.<br>Hungert nie. | common: colour #FFB38A |
| `florist` | **Florist**<br>**Florist, Floristin, Blumenkundige** | Grows flowers for fun.<br>Zieht Blumen zum Vergnügen. | common: colour #FF9AA2 |
| `sleepyhead` | **Sleepyhead**<br>**Schlafmütze** | Goes to bed at dusk.<br>Geht bei Dämmerung ins Bett. | common: colour #A0A0C8 |
| `friendly_face` | **Friendly Face**<br>**Freundliches Gesicht** | Always says hello.<br>Grüßt immer. | common: colour #F7F08A |
| `scout` | **Scout**<br>**Kundschafter, Kundschafterin, Kundschaftende** | First to see what lies behind the next hill.<br>Sieht als Erstes, was hinter dem nächsten Hügel liegt. | common: colour #7FDBDA |
| `cave_dweller` | **Cave Dweller**<br>**Höhlenbewohner, Höhlenbewohnerin, Höhlenbewohnende** | Feels at home in the dark.<br>Fühlt sich im Dunkeln zu Hause. | common: colour #9A9A9A |

## General: rare (14)

| Id | Title (English; German male, female, neutral) | Description | Rarity, look |
| --- | --- | --- | --- |
| `trailblazer` | **Trailblazer**<br>**Pfadfinder, Pfadfinderin, Pfadfindende** | Goes where nobody went before.<br>Geht, wo noch niemand war. | rare: gradient #7DFFB0→#17B45B |
| `night_walker` | **Night Walker**<br>**Nachtwandler, Nachtwandlerin, Nachtwandelnde** | Not afraid of the dark.<br>Hat keine Angst vor der Dunkelheit. | rare: gradient #8E7CFF→#2E2A6B |
| `treasure_hunter` | **Treasure Hunter**<br>**Schatzsucher, Schatzsucherin, Schatzsuchende** | Opens every chest they find.<br>Öffnet jede Truhe, die sich findet. | rare: gradient #FFE259→#C98A1B |
| `architect` | **Architect**<br>**Architekt, Architektin, Architekturkundige** | Plans before building.<br>Plant, bevor gebaut wird. | rare: gradient #E0E0E0→#9AD0FF |
| `ironheart` | **Ironheart**<br>**Eisenherz** | Hard to scare, harder to beat.<br>Schwer zu erschrecken, schwerer zu besiegen. | rare: gradient #D8D8D8→#7A7A7A |
| `sea_wolf` | **Sea Wolf**<br>**Seebär, Seebärin, Seefahrende** | Most at home on the waves.<br>Auf den Wellen am meisten zu Hause. | rare: gradient #48DBFB→#0A3D62 |
| `mountaineer` | **Mountaineer**<br>**Bergsteiger, Bergsteigerin, Bergsteigende** | Has climbed the highest peaks.<br>Hat die höchsten Gipfel bestiegen. | rare: gradient #FFFFFF→#8EA6B4 |
| `desert_rose` | **Desert Rose**<br>**Wüstenrose** | Blooms where nothing grows.<br>Blüht, wo nichts wächst. | rare: gradient #F6D6AD→#E07A5F |
| `frostbite` | **Frostbite**<br>**Frostbeule** | Loves the cold.<br>Liebt die Kälte. | rare: gradient #E0FFFF→#7FB7FF |
| `wildling` | **Wildling**<br>**Wildfang** | Raised by the forest.<br>Vom Wald großgezogen. | rare: gradient #A8E07A→#2D6A4F |
| `lucky_charm` | **Lucky Charm**<br>**Glücksbringer, Glücksbringerin, Glückbringende** | Things just tend to work out.<br>Irgendwie klappt immer alles. | rare: gradient #B6FF8A→#FFD200 |
| `scholar` | **Scholar**<br>**Gelehrter, Gelehrte, Gelehrte** | Has read every book in the library.<br>Hat jedes Buch der Bibliothek gelesen. | rare: gradient #F2E6C9→#B08D57, sides runes |
| `redstone_wizard` | **Redstone Wizard**<br>**Redstone-Zauberer, Redstone-Zauberin, Redstone-Zaubernde** | Builds machines nobody understands.<br>Baut Maschinen, die niemand versteht. | rare: gradient #FF6B6B→#8B0000 |
| `beastmaster` | **Beastmaster**<br>**Tierbändiger, Tierbändigerin, Tierbändigende** | Animals follow them anywhere.<br>Tiere folgen überallhin. | rare: gradient #FFB38A→#A0522D |

## General: epic (10)

| Id | Title (English; German male, female, neutral) | Description | Rarity, look |
| --- | --- | --- | --- |
| `dragonheart` | **Dragonheart**<br>**Drachenherz** | Fearless as a dragon.<br>Furchtlos wie ein Drache. | epic: gradient #D9B8FF→#7B4FD1, icon `item/dragon_breath` |
| `fortunes_favorite` | **Fortune's Favorite**<br>**Glückskind** | Fortune smiles on them.<br>Das Glück lächelt hier. | epic: gradient #7DFFB0→#1FAA59, icon `item/emerald` |
| `stormcaller` | **Stormcaller**<br>**Sturmrufer, Sturmruferin, Sturmrufende** | Thunder answers.<br>Der Donner antwortet. | epic: gradient #9AD0FF→#2E3A87, icon `item/trident` |
| `ghost` | **Ghost**<br>**Geist** | Seen by few, heard by none.<br>Von wenigen gesehen, von niemandem gehört. | epic: gradient #FFFFFF→#B0B0C8, icon `item/phantom_membrane`, sides magic |
| `netherborn` | **Netherborn**<br>**Netherkind** | Seems to like the heat.<br>Scheint die Hitze zu mögen. | epic: gradient #FF7A59→#5A0E0E, icon `item/blaze_powder` |
| `abyss_walker` | **Abyss Walker**<br>**Abgrundwanderer, Abgrundwanderin, Abgrundwandernde** | Has walked where the Warden sleeps.<br>War dort, wo der Wärter schläft. | epic: gradient #2FB8A6→#052A2B, icon `item/echo_shard` |
| `star_seeker` | **Star Seeker**<br>**Sternsucher, Sternsucherin, Sternsuchende** | Always reaching for more.<br>Strebt immer nach mehr. | epic: gradient #FFF6A0→#C9A0FF, icon `item/amethyst_shard` |
| `thunderfist` | **Thunderfist**<br>**Donnerfaust** | Hits hard, asks later.<br>Schlägt hart zu, fragt später. | epic: gradient #FFD200→#FF4E00, icon `item/mace` |
| `phoenix` | **Phoenix**<br>**Phönix** | Always comes back.<br>Kommt immer zurück. | epic: gradient #FFD200→#FF4E00, icon `block/fire_0`, pulse |
| `wind_whisperer` | **Wind Whisperer**<br>**Windflüsterer, Windflüsterin, Windflüsternde** | The breeze carries their voice.<br>Der Wind trägt die Stimme. | epic: gradient #E0FFFF→#9AD0FF, icon `item/wind_charge` |

## General: legendary (6)

| Id | Title (English; German male, female, neutral) | Description | Rarity, look |
| --- | --- | --- | --- |
| `living_legend` | **Living Legend**<br>**Lebende Legende** | Stories are told about them.<br>Über diese Person erzählt man Geschichten. | legendary: rainbow, icon `item/nether_star` |
| `worldshaper` | **Worldshaper**<br>**Weltenformer, Weltenformerin, Weltenformende** | Leaves a mark wherever they go.<br>Hinterlässt überall Spuren. | legendary: gradient #7DFFB0→#2E86DE, wave, icon `item/diamond_pickaxe` |
| `voidwalker` | **Voidwalker**<br>**Leerenwanderer, Leerenwanderin, Leerenwandernde** | Has looked into the void, and came back.<br>Hat in die Leere geblickt und ist zurückgekehrt. | legendary: gradient #D9B8FF→#3B1E6E, wave, icon `item/ender_eye` |
| `timeless` | **Timeless**<br>**Zeitlos** | Has been here since the first day.<br>Ist seit dem ersten Tag hier. | legendary: gradient #FFE259→#FFA751, pulse, icon `item/clock_00` |
| `sovereign` | **Sovereign**<br>**Souverän** | Rules without a crown.<br>Herrscht ohne Krone. | legendary: rainbow, icon `item/golden_helmet` |
| `mythic` | **Mythic**<br>**Mythisch** | Half real, half rumour.<br>Halb Wirklichkeit, halb Gerücht. | legendary: gradient #FF9AA2→#C9A0FF, wave, sides magic, icon `item/heart_of_the_sea` |

## The unreadable ones (3, legendary, only from name tags)

| Id | Title (English; German male, female, neutral) | Description | Rarity, look |
| --- | --- | --- | --- |
| `arcane_word` | **Arcane Word**<br>**Arkanes Wort** | A word only the enchanting table knows.<br>Ein Wort, das nur der Zaubertisch kennt. | legendary: colour #B983FF, whole title in runes (`alt`) |
| `raiders_oath` | **Raider's Oath**<br>**Plündererschwur** | Sworn to the illagers, in their own tongue.<br>Den Illagern geschworen, in ihrer eigenen Sprache. | legendary: gradient #9AA89A→#5E6E63, whole title in illager runes |
| `forgotten` | **Forgotten**<br>**Vergessen** | A name that time has erased.<br>Ein Name, den die Zeit gelöscht hat. | legendary: colour #C8C8C8, whole title as magic letters |
