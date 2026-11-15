"""The shipped advancement titles (common/src/main/resources/defaults/titles/advancements.json), made from the tables in
docs/titles-advancements.md, which is where they are decided. Run after changing the document:

    python3 scripts/titles/advancement_titles.py
    python3 scripts/titles/advancement_titles.py --names   # also lists each advancement's own name next to its title

Each table row: the advancement (its id in backticks; the document has no names of the game's: they are Mojang's), the title (English; German male, female, neutral), the
description (English, German), the rarity and look, the "first on the server" title (English; German forms).
"""

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DOC = ROOT / 'docs' / 'titles-advancements.md'
OUT = ROOT / 'common' / 'src' / 'main' / 'resources' / 'defaults' / 'titles' / 'advancements.json'

ROW = re.compile(r'^\| .*?`(?P<id>[a-z_/]+)` \| \*\*(?P<en>.+?)\*\*<br>\*\*(?P<de>.+?)\*\* \| (?P<den>.+?)<br>(?P<dde>.+?) \| '
                 r'(?P<rarity>\w+): (?P<look>.+?) \| (?P<first_en>.+?)<br>(?P<first_de>.+?) \|$')
COLOR = re.compile(r'#[0-9A-Fa-f]{6}')
ICON = re.compile(r'icon `([^`]+)`')

# The "first on the server" titles are gold with the nether star at both ends and pulse, and keep what makes their
# title special: its icon next to the star, its side letters, its animation.
FIRST_COLORS = ['#FFE259', '#FFA751']
FIRST_ICON = 'item/nether_star'


def first_style(base: dict) -> dict:
    result = {'gradient': FIRST_COLORS, 'animation': base.get('animation', 'pulse')}
    icons = [FIRST_ICON] + ([base['icon']] if 'icon' in base else [])
    if len(icons) == 1:
        result['icon'] = FIRST_ICON
    else:
        result['icons'] = icons
    if 'sides' in base:
        result['sides'] = base['sides']
    return result
FIRST_SUFFIX = {'en_us': ' The first on this server.', 'de_de': ' Als Erstes auf diesem Server.'}


def texts(en: str, de: str) -> dict:
    forms = de.split(', ')
    if len(forms) == 1:
        forms *= 3
    if len(forms) != 3:
        sys.exit(f'Expected one or three German forms: {de}')
    result = {'en_us': en, 'de_de': forms[0]}
    if forms[1] != forms[0]:
        result['de_de/female'] = forms[1]
    if forms[2] != forms[0]:
        result['de_de/neutral'] = forms[2]
    return result


def style(rarity: str, look: str) -> dict:
    colors = COLOR.findall(look)
    result = {'color': colors[0]} if len(colors) == 1 else {'gradient': colors}
    icon = ICON.search(look)
    if icon:
        result['icon'] = icon.group(1)
    if 'wave' in look:
        result['animation'] = 'wave'
    return result


def game_names() -> dict:
    """The advancements' English and German names from a local game JAR (the smoke test's), for --names only."""
    import zipfile
    cache = ROOT / 'scripts' / '.cache' / 'smoke'
    jars = sorted(cache.glob('vanilla-client-*/versions/*/*.jar'))
    if not jars:
        sys.exit('No game JAR in scripts/.cache/smoke: run ./tasks.sh smoke once')
    root = jars[-1].parents[2]
    jar = zipfile.ZipFile(jars[-1])
    names = {'en_us': json.loads(jar.read('assets/minecraft/lang/en_us.json'))}
    index = next(root.glob('assets/indexes/*.json'), None)
    if index:
        entry = json.loads(index.read_text())['objects'].get('minecraft/lang/de_de.json')
        if entry:
            names['de_de'] = json.loads((root / 'assets' / 'objects' / entry['hash'][:2] / entry['hash']).read_text(encoding='utf-8'))
    return names


def main() -> None:
    show_names = '--names' in sys.argv[1:]
    names = game_names() if show_names else None
    titles = []
    for line in DOC.read_text(encoding='utf-8').splitlines():
        row = ROW.match(line)
        if not row:
            continue
        advancement = 'minecraft:' + row['id']
        key = row['id'].replace('/', '.')
        if names:
            lang_key = 'advancements.' + row['id'].replace('/', '.') + '.title'
            print(f"{row['id']}: {names['en_us'].get(lang_key, '?')} / {names.get('de_de', {}).get(lang_key, '?')} -> {row['en']} / {row['de']}")
        base = style(row['rarity'], row['look'])
        titles.append({
            'id': 'adv.' + key,
            'text': texts(row['en'], row['de']),
            'description': {'en_us': row['den'], 'de_de': row['dde']},
            'style': base,
            'rarity': row['rarity'],
            'sources': ['advancement:' + advancement],
            'category': 'advancements',
        })
        titles.append({
            'id': 'first.' + key,
            'text': texts(row['first_en'], row['first_de']),
            'description': {language: row['den' if language == 'en_us' else 'dde'] + suffix
                            for language, suffix in FIRST_SUFFIX.items()},
            'style': first_style(base),
            'rarity': 'legendary',
            'sources': ['first_advancement:' + advancement],
            'category': 'first',
        })
    if not titles:
        sys.exit(f'No title rows found in {DOC}')
    data = {'name': 'Advancements', 'enabled': True, 'titles': titles}
    OUT.write_text(json.dumps(data, indent='\t', ensure_ascii=False) + '\n', encoding='utf-8')
    print(f'{len(titles)} titles ({len(titles) // 2} advancements) written to {OUT.relative_to(ROOT)}')


if __name__ == '__main__':
    main()
