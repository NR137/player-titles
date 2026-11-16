"""The shipped merchant and loot titles (common/src/main/resources/defaults/titles/merchant-and-loot.json), made from the tables
in docs/titles-merchant-and-loot.md, which is where they are decided. Run after changing the document:

    python3 scripts/titles/merchant_and_loot_titles.py

Each table row: the id in backticks, the title (English; German male, female, neutral, or one form), the description
(English, German), "rarity: look". Each section is a category of the title screen; the unreadable ones come only from
name tags, all others from the merchant and name tags too.
"""

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DOC = ROOT / 'docs' / 'titles-merchant-and-loot.md'
OUT = ROOT / 'common' / 'src' / 'main' / 'resources' / 'defaults' / 'titles' / 'merchant-and-loot.json'

ROW = re.compile(r'^\| `(?P<id>[a-z0-9_]+)` \| \*\*(?P<en>.+?)\*\*<br>\*\*(?P<de>.+?)\*\* \| (?P<den>.+?)<br>(?P<dde>.+?) \| '
                 r'(?P<rarity>common|rare|epic|legendary): (?P<look>.+?) \|$')
SECTION = re.compile(r'^## (?P<name>.+?)(?: \(\d+.*\))?$')
# The document's sections as categories of the title screen (their names are in the language files).
CATEGORIES = {
    'Internet, trends and slang': 'trends', 'Football': 'football', 'Pop culture: music, film, fashion': 'pop_culture',
    'Roles': 'roles', 'Anime': 'anime', 'Gaming': 'gaming', 'General: common': 'general',
    'General: rare': 'general', 'General: epic': 'general', 'General: legendary': 'general', 'The unreadable ones': 'unreadable',
}
COLOR = re.compile(r'#[0-9A-Fa-f]{6}')
ICON = re.compile(r'icon `([^`]+)`')


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


def style(look: str) -> dict:
    result = {}
    colors = COLOR.findall(look)
    if len(colors) == 1:
        result['color'] = colors[0]
    elif colors:
        result['gradient'] = colors
    icon = ICON.search(look)
    if icon:
        result['icon'] = icon.group(1)
    for animation in ('rainbow', 'pulse', 'wave'):
        if re.search(rf'\b{animation}\b', look):
            result['animation'] = animation
    sides = re.search(r'sides (magic|runes|illager_runes)', look)
    if sides:
        result['sides'] = sides.group(1)
    if 'whole title in runes' in look:
        result['font'] = 'alt'
    if 'whole title in illager runes' in look:
        result['font'] = 'illageralt'
    if 'whole title as magic letters' in look:
        result['obfuscated'] = True
    if not result:
        sys.exit(f'No look in: {look}')
    return result


def main() -> None:
    titles = []
    category = None
    for line in DOC.read_text(encoding='utf-8').splitlines():
        section = SECTION.match(line)
        if section:
            category = CATEGORIES.get(section['name'])
            continue
        row = ROW.match(line)
        if not row:
            continue
        if category is None:
            sys.exit(f'A title outside the known sections: {row["id"]}')
        titles.append({
            'id': 'merchant_loot.' + row['id'],
            'text': texts(row['en'], row['de']),
            'description': {'en_us': row['den'], 'de_de': row['dde']},
            'style': style(row['look']),
            'rarity': row['rarity'],
            'sources': ['loot'] if category == 'unreadable' else ['merchant', 'loot'],
            'category': category,
        })
    ids = [title['id'] for title in titles]
    if len(ids) != len(set(ids)):
        sys.exit('An id is used twice: ' + ', '.join(sorted({i for i in ids if ids.count(i) > 1})))
    data = {'name': 'Merchant and loot', 'enabled': True, 'titles': titles}
    OUT.write_text(json.dumps(data, indent='\t', ensure_ascii=False) + '\n', encoding='utf-8')
    print(f'{len(titles)} titles written to {OUT.relative_to(ROOT)}')


if __name__ == '__main__':
    main()
