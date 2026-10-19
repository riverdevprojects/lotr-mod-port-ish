# Middle-earth Landscape

A NeoForge 1.21.1 mod that adds one thing: a Middle-earth dimension.

Landmass comes from a landmask PNG. Biomes come from a region map PNG, **one biome
per region**. There are no blocks, items, entities, structures or recipes — this is
landscape generation and nothing else.

## Getting there

`/middleearth` (op level 2) takes you to the same x/z in Middle-earth, standing on
the surface. Vanilla works too:

```
/execute in lotrmod:middleearth run tp @s ~ ~ ~
```

The F3 debug screen reports whether the maps loaded, the current region and its
shape, the distance to the coast and the climate values the terrain is built from.

## How it works

Two PNGs under `src/main/resources/data/lotrmod/maps/`, each pixel covering
16×16 blocks and centred on world origin:

| Map | Role |
| --- | --- |
| `middleearth_landmask.png` | Dark = land, light = ocean. Decides the coastline. |
| `middleearth_regions.png` | One flat colour per region. Decides the biome and how rugged the land is. |

They live under `data/` rather than `assets/` on purpose. Worldgen is server-side,
and a dedicated server's `ResourceManager` only loads `data/` — maps under `assets/`
are invisible to it, so a packaged build would silently fall back to a blank map and
generate flat terrain everywhere.

`Region.java` holds each region's map colour, display name and **shape**. The biome
id is the enum name lowercased, so `THE_SHIRE` is always `lotrmod:the_shire` — the
enum and the JSON file name cannot drift apart.

### Terrain

The terrain *is* vanilla's overworld terrain: the same height splines, 3D noise,
caves, aquifers and ore veins, so deserts get vanilla's dunes and terraces, hills
roll the way vanilla hills do, and mountains are vanilla's peaks. What changes is
where two of the climate values vanilla shapes the ground from come from:

- **Continentalness** comes from the landmask. It runs from deep ocean far
  offshore, through vanilla's coast band at the shoreline, up to the region's own
  value a couple of hundred blocks inland, so the coast is where the landmask
  draws it.
- **Erosion** comes from the region map. Each region's shape sets a centre (low is
  rugged, high is flat) and a spread, and vanilla's erosion noise wanders between
  them so no region is uniform.

Vanilla's ridge noise still adds peaks, valleys and rivers everywhere. Region
values are blurred over about a hundred blocks, so mountains rise out of the
plains instead of stepping up at the map's pixel edges.

| Shape | Regions | Continentalness | Erosion |
| --- | --- | --- | --- |
| `MOUNTAIN` | Misty, Blue, Grey, White, Shadow, Erebor, Iron Hills | 0.55 | −0.80 to −0.45 by range, ±0.10 |
| `HILLY` | Eriador, Arnor, Dale, The Shire | 0.25 | 0.00 ± 0.25 |
| `FLAT` | Gondor, Rohan, Rhûn, Dead Lands, Vale of Anduin, E. Rhovanion | 0.15 | 0.30 ± 0.15 |
| `FOREST` | Lindon, Lothlórien, Mirkwood, Fangorn | 0.20 | 0.10 ± 0.25 |
| `DESERT` | Harad | 0.20 | 0.05 ± 0.30 |
| `MIXED` | Mordor, Forodwaith, Rivendell | 0.25 | −0.10 ± 0.35 |
| `RIVER` | Anduin, Celduin | −0.12 | 0.60 ± 0.10 (a wet floodplain at sea level) |
| `LAKE` | Sea of Rhûn | −0.30 | 0.30 ± 0.10 (sinks below sea level and fills) |

`MiddleEarthClimate` works the values out from the maps and the `lotrmod:climate`
density function hands them to the JSON in `data/lotrmod/worldgen/`.
`density_function/middleearth/continents.json` and `erosion.json` are written by
hand. The rest of that folder, and everything in `noise_settings/middleearth.json`
above the surface rule, are vanilla's own files renamed to read the Middle-earth
values. `gen_terrain.sh` regenerates them from the Minecraft jar.

### Ground

What each region is finished with — sand over sandstone in Harad, snow and bare
rock on the high peaks, podzol in Mirkwood, mud in the Vale of Anduin — is the
surface rule at the end of `noise_settings/middleearth.json`, keyed by biome and
built from vanilla's own rules. A region that isn't named there gets vanilla's
grass and dirt. Beaches are the one thing added in Java: low ground near the
landmask's coast is turned to sand by `MiddleEarthChunkGenerator`.

## Editing the maps

Both maps reload with `/reload` — no restart needed. Already-generated chunks keep
their old terrain, so test map changes in a fresh world.

Adding a region takes two steps: add the enum constant with its map colour and
shape, and add `data/lotrmod/worldgen/biome/<name>.json`. Then add it to
`possible_biomes` in `data/lotrmod/dimension/middleearth.json`, and to the surface
rule if its ground should be anything but grass.

## A warning about biome features

Every biome JSON lists its vegetation as an ascending subsequence of one canonical
master order, documented at the top of `gen_biomes.sh`. This is not cosmetic: if two
biomes disagree about which of two shared features comes first, NeoForge's
feature-order sort hits a cycle and the game **crashes on world load**. If you
hand-edit a biome's `features`, keep the master order.

`gen_biomes.sh` regenerates all 28 biome JSONs from that single ordering. Prefer
editing the script over editing the JSONs.
