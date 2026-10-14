# Middle-earth Landscape

A NeoForge 1.21.1 mod that adds one thing: a Middle-earth dimension.

Landmass comes from a landmask PNG. Biomes come from a region map PNG, **one biome
per region**. There are no blocks, items, entities, structures or recipes — this is
landscape generation and nothing else.

## Getting there

The mod registers no command. Use vanilla:

```
/execute in lotrmod:middleearth run tp @s ~ ~ ~
```

The F3 debug screen reports the current region, its terrain shape, the landmask
brightness and the terrain height — which is usually enough to tell whether the
maps loaded correctly.

## How it works

Two PNGs under `src/main/resources/data/lotrmod/maps/`, each pixel covering
16×16 blocks and centred on world origin:

| Map | Role |
| --- | --- |
| `middleearth_landmask.png` | Dark = land, light = ocean. Decides the coastline and the seabed. |
| `middleearth_regions.png` | One flat colour per region. Decides the biome. |

They live under `data/` rather than `assets/` on purpose. Worldgen is server-side,
and a dedicated server's `ResourceManager` only loads `data/` — maps under `assets/`
are invisible to it, so a packaged build would silently fall back to a blank map and
generate flat terrain everywhere.

`Region.java` is the single source of truth. Each constant carries its map colour,
its display name, a **shape** (how the terrain noise is weighted) and a **surface**
(what the ground is finished with). The biome id is the enum name lowercased, so
`THE_SHIRE` is always `lotrmod:the_shire` — the enum and the JSON file name cannot
drift apart.

Terrain height is a vanilla-style noise stack — continental baseline, erosion,
peaks-and-valleys, ridges, fine detail — with per-region weights bilinearly
interpolated over a 96-block grid so region borders blend rather than step.

| Shape | Regions | Feel |
| --- | --- | --- |
| `MOUNTAIN` | Misty, Blue, Grey, White, Shadow, Erebor, Iron Hills | Ridged spines, 60–110 blocks of relief |
| `HILLY` | Eriador, Arnor, Dale, The Shire | Rolling |
| `FLAT` | Gondor, Rohan, Rhûn, Harad, Dead Lands, Vale of Anduin, E. Rhovanion, Sea of Rhûn | Gentle plains |
| `FOREST` | Lindon, Lothlórien, Mirkwood, Fangorn | Mid-range, shape defined by trees |
| `RIVER` | Anduin, Celduin | Low, heavily eroded beds |
| `MIXED` | Mordor, Forodwaith, Rivendell | Default variation |

## Editing the maps

Both maps reload with `/reload` — no restart needed. Already-generated chunks keep
their old terrain, so test map changes in a fresh world.

Adding a region takes two steps: add the enum constant with its map colour, and add
`data/lotrmod/worldgen/biome/<name>.json`. Then add it to `possible_biomes` in
`data/lotrmod/dimension/middleearth.json`.

## A warning about biome features

Every biome JSON lists its vegetation as an ascending subsequence of one canonical
master order, documented at the top of `gen_biomes.sh`. This is not cosmetic: if two
biomes disagree about which of two shared features comes first, NeoForge's
feature-order sort hits a cycle and the game **crashes on world load**. If you
hand-edit a biome's `features`, keep the master order.

`gen_biomes.sh` regenerates all 28 biome JSONs from that single ordering. Prefer
editing the script over editing the JSONs.
