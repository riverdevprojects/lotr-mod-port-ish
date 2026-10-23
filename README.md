# Middle-earth Landscape

A NeoForge 1.21.1 mod that adds one thing: a Middle-earth dimension.

Landmass comes from a landmask PNG. Biomes come from a region map PNG, **one biome
per region** (the Shire and Lindon are the exceptions, split up; see below). Apart
from the plants the landscape needs that vanilla lacks — the cattail, and the
aspen, shire pine, beech and chestnut from the original mod, whose logs craft
into birch, spruce, oak and dark oak planks, and the red poplar from vanilla's
newer dappled forest, whose logs craft into birch planks, Mirkwood's mirk oak,
pine and vines, whose logs craft into dark oak and spruce planks, Rohan's dry grasses, the Dead Lands' short grass and Gondor's white rock — the one addition is the Shire's **hobbit holes**, with the blocks they
are built from and the hobbits who live in them (see below). Otherwise this is
landscape generation and nothing else.

## Getting there

`/middleearth` (op level 2) takes you to the middle of the Shire (-9536, -8384), standing on
the surface. Vanilla works too:

```
/execute in lotrmod:middleearth run tp @s ~ ~ ~
```

The F3 debug screen reports whether the maps loaded, the current region and its
shape, the distance to the coast and the climate values the terrain is built from.

## How it works

Three PNGs under `src/main/resources/data/lotrmod/maps/`, each pixel covering
16×16 blocks and centred on world origin:

| Map | Role |
| --- | --- |
| `middleearth_landmask.png` | Dark = land, light = ocean. Decides the coastline. |
| `middleearth_regions.png` | One flat colour per region. Decides the biome and how rugged the land is. |
| `middleearth_rivers.png` | One-pixel river centrelines in the river's region colour; alpha = width in blocks. |

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

Vanilla's ridge noise still adds peaks and valleys, but is kept out of its
deepest valleys (`density_function/middleearth/ridges_folded.json`), so vanilla
carves no rivers of its own and leaves only the odd small lake. Region values are
blurred over about a hundred blocks, so mountains rise out of the
plains instead of stepping up at the map's pixel edges.

| Shape | Regions | Continentalness | Erosion |
| --- | --- | --- | --- |
| `MOUNTAIN` | Misty, Blue, Grey, White, Shadow, Erebor, Iron Hills | 0.55 | −0.80 to −0.45 by range, ±0.10 |
| `HILLY` | Lindon, Eriador, Arnor, Dale, The Shire | 0.25 | 0.00 ± 0.25 (the Shire's ground itself comes from `ShireTerrain`, below) |
| `FLAT` | Gondor, Rohan, Rhûn, Dead Lands, E. Rhovanion | 0.15 | 0.30 ± 0.15 (Gondor's, Rohan's and the Dead Lands' ground itself comes from `GondorTerrain`, `RohanTerrain` and `DeadLandsTerrain`, below) |
| `MARSH` | Vale of Anduin | 0.15 | 0.30 ± 0.15 (the ground itself comes from `MarshTerrain`, below) |
| `FOREST` | Lothlórien, Mirkwood, Fangorn | 0.20 | 0.10 ± 0.25 |
| `DESERT` | Harad | 0.20 | 0.05 ± 0.30 |
| `MIXED` | Mordor, Forodwaith, Rivendell | 0.25 | −0.10 ± 0.35 |
| `RIVER` | Anduin, Celduin | unused: rivers come from the river map (below) | |
| `LAKE` | Sea of Rhûn | −0.30 | 0.30 ± 0.10 (sinks below sea level and fills) |

`MiddleEarthClimate` works the values out from the maps and the `lotrmod:climate`
density function hands them to the JSON in `data/lotrmod/worldgen/`.
`density_function/middleearth/continents.json`, `erosion.json` and
`ridges_folded.json` are written by hand. The rest of that folder, and everything in `noise_settings/middleearth.json`
above the surface rule, are vanilla's own files renamed to read the Middle-earth
values. `gen_terrain.sh` regenerates them from the Minecraft jar.

The mountain ranges are the exception. Inside them the terrain fades over, across
about a hundred blocks, to the mod's original mountain heightfield
(`MountainTerrain`): stacked 2D noise plus a ridge layer, 110 blocks high in the
Misty, Blue and Shadow mountains, 85 in the White and Grey, and 60 in Erebor and
the Iron Hills. Vanilla's top slide still caps peaks at about y 250.

Rivers are the other exception. `RiverMap` traces each line on the river map,
smooths it and lets it meander a little, and `RiverTerrain` cuts a channel along
it: a bed a few blocks below sea level, a low bank, and a valley either side
easing back into the land over a hundred blocks or so. `MiddleEarthChunkGenerator`
fills the channel with water up to the waterline itself; vanilla's aquifer only
sometimes floods ground cut below sea level, and left to it rivers came out dry in
stretches. Rivers only ever lower the ground, so where one runs into the sea or the Sea
of Rhûn the water stays as deep as it was. The channel takes the river's biome
(`lotrmod:anduin_river`, `lotrmod:celduin`). The Anduin widens from 69 blocks at
its source to 103 at its mouth, and the Celduin from 41 to 64.

Marshes are the third. The Vale of Anduin (shape `MARSH`), painted on the region
map as a band about 290 blocks wide either side of the upper Anduin, is built from
`MarshTerrain`: ground held within a block or two of the waterline, so the dips
between low hummocks become shallow pools, which the chunk generator fills with
water the same way it fills rivers.

The Shire is the fourth. Vanilla's terrain is lumpier at any erosion than the
original mod's gentlest land biome, so inside the Shire the ground fades over to
`ShireTerrain`: soft downs, swells a couple of hundred blocks across with a smaller
roll on top, around y 74 and kept above the waterline.

Rohan is the fifth: inside it the ground fades over to `RohanTerrain`, wide
rolling grassland (see below).

The Dead Lands are the sixth: inside them the ground fades over to
`DeadLandsTerrain`, low worn ground broken into shallow ledges (see below).

Gondor is the seventh: inside it the ground fades over to `GondorTerrain`,
low plains with hills of white marble and reedy wetlands (see below). Its
ground fades in over the first 160 blocks from the coast, so its shore keeps
vanilla's beaches.

All seven are done by the `lotrmod:terrain_blend` density function. It wraps
vanilla's density before caves are carved, so mountains, marshes, the Shire,
Rohan, the Dead Lands, Gondor and river valleys still get vanilla's caves,
aquifers and ore veins.

### Ground

What each region is finished with — sand over sandstone in Harad, snow and bare
rock on the high peaks, podzol in Mirkwood, grass and mud in the Vale of Anduin — is the
surface rule at the end of `noise_settings/middleearth.json`, keyed by biome and
built from vanilla's own rules. A region that isn't named there gets vanilla's
grass and dirt. Beaches are added in Java: low ground near the landmask's coast is
turned to sand by `MiddleEarthChunkGenerator`.

The Vale of Anduin is planted as a marsh: cattails (`lotrmod:cattail`, a two-tall
plant that grows on the bank or in one block of water) thick around its pools, lily
pads, blue orchids and a scattering of vanilla's swamp oaks. Cattails also line
the rivers and the Sea of Rhûn, more thinly. The features are
`worldgen/configured_feature/patch_cattail.json` and the placed features beside it.

### The Shire

The Shire is laid out in rings, as in the original mod, by distance from its
border on the region map (`ShireZones`), with noise pushing each ring's edge in
and out by up to 220 blocks:

| Ring | Biome | Where | Trees |
| --- | --- | --- | --- |
| Heart | `lotrmod:the_shire` | more than ~300 blocks inside the border | Mostly open grass: a lone oak or large oak in about one chunk in seven, and the odd small, thin woody patch of oak, aspen and birch |
| Outskirts | `lotrmod:shire_outskirts` | the ~300 blocks inside the border | Woodland throughout: thick mixed woods on one side of a noise, aspen stands on the other |
| Shire Woodlands | `lotrmod:shire_woodlands` | Eriador or Arnor within ~480 blocks of the border | Thick shire pine forest with aspens, birches and oaks, on Eriador's hills |

The tree mixes follow the original biomes' weights. Aspen and shire pine are
literal ports of the original generators (`AspenFeature`, `ShirePineFeature`,
under `worldgen/feature`), with the original textures; large aspens have a 2×2
trunk. The mixes are the `random_selector` configured features `trees_shire`,
`trees_shire_woods`, `trees_aspen_stand` and `trees_shire_woodlands`. The Shire's
bright grass colour is the original's.

### Eriador

Eriador (`lotrmod:eriador`) follows the original's `LOTRBiomeGenEriador`: open
green grassland (vanilla's colour for the original's climate, 0.9 and 0.8) with
few trees of its own.

- **Trees** (`trees_eriador`): mostly oak, then spruce, chestnut, birch, large oak
  and aspen, with the odd yellow beech, by the original's weights. One chunk in
  ten gets a tree, and one chunk in twelve a clump of six to ten
  (`trees_eriador_grove`), as the original's tree clusters did.
- **Copses** (`trees_beech_copse`, `trees_aspen_copse`): where a noise peaks,
  small woods of beech or aspen stand in for the original's forest variants.
- **Boulders** (`eriador_boulders`): a port of the original's `LOTRWorldGenBoulder`
  (`BoulderFeature`): balls of stone, andesite and mossy cobblestone, settled
  onto level ground, in about one chunk in three.
- **Rocky ground**: as with the original's `enableRocky`, high ground is
  patched with bare stone and gravel, more of it the higher it gets, from
  y 76 up. These are the `lotrmod:eriador` rules in the surface rule, driven
  by the `lotrmod:eriador_rocks` noise.

Beech and chestnut are vanilla tree features with the original's textures:
straight trunks and blob leaves for the small trees, fancy trunks and leaves
for the large ones.

### Lindon

Lindon follows the original's `LOTRBiomeGenLindon` and
`LOTRBiomeGenLindonWoodlands`: open rolling fields broken by broad woods. Where
a noise in `MiddleEarthBiomeSource` rises high enough (about a third of Lindon,
in woods a few hundred blocks across) the biome is `lotrmod:lindon_woodlands`;
elsewhere it is `lotrmod:lindon`. Grass is vanilla's colour for the original
climates (0.9 and 0.9; 0.9 and 1.0 in the woods, there taken a third of the
way to the dappled forest's orange).

- **Trees** (`trees_lindon`): mostly birch and tall birch, then aspen, large
  birch, oak and beech, with the odd chestnut, large oak and great "party" birch,
  by the original's weights (its few apple and pear trees are left out). In the
  fields one chunk in ten gets a tree and one in twenty a clump of six to ten
  (`trees_lindon_grove`); the woods get six or seven a chunk
  (`trees_lindon_woodlands`) as well as the clumps.
- **Copses** (`trees_lindon_birch_copse`, `_aspen_copse`, `_beech_copse`): where
  a noise peaks, small stands of one tree, as the original's forest variants.
- **Party birch** (`birch_party`): a port of the original's
  `LOTRWorldGenPartyTrees` (`PartyTreeFeature`), a trunk three blocks square
  with thick boughs spreading near the top. The large birch is vanilla's fancy
  tree in birch.
- **Red poplars** (`trees_lindon_poplar`, `trees_lindon_poplar_stand`): in the
  woods only, one in about three chunks and denser stands where a noise peaks.
  The poplar is from vanilla's dappled forest (Java 26.3), which 1.21.1 doesn't
  have: `PoplarFeature` reproduces its trunk and foliage placers, with the
  numbers from vanilla's `red_poplar` and vanilla's textures, the leaves
  darkened and muted. Its shelf
  mushrooms, leaf litter and falling-leaf particles are left out, as 1.21.1
  has none of them.
- **Forest floor**: in the woods, patches of podzol and coarse dirt, as the
  original's `enablePodzol` gave dense woods. These are the
  `lotrmod:lindon_woodlands` rules in the surface rule, driven by the
  `lotrmod:lindon_podzol` noises.
- **Rocky ground**: as Eriador's, from the same rules.

Birch leaves use the original mod's olive texture, untinted as the original drew
them (`assets/minecraft/models/block/birch_leaves.json`). That changes birch
leaves everywhere, as the original did.

### Fangorn

Fangorn (`lotrmod:fangorn_forest`) follows the original's `LOTRBiomeGenFangorn`:
a dark, close forest of tall trees under the crowns of giant ones, in a green
haze. Its colours (vanilla's grass and leaves for the original's climate, 0.7
and 0.8, and the original's sky and fog) are taken half way to grey for a
muted, misty look.

- **Giant trees** (`lotrmod:fangorn_tree`): a port of the original's
  `LOTRWorldGenFangornTrees` (`FangornTree`): a leaning trunk up to 40 blocks
  tall and 17 across at the foot, with boughs spreading all round its top whose
  side branches end in balls of leaves hung with vines. Oak five times in
  seven, golden beech otherwise, as the original's weights. The crown reaches
  some 45 blocks from the trunk, further than a feature may write, so the
  giants are a structure (`FangornTreeStructure`, `FangornTreePiece`), laid out
  from one seed and written a chunk at a time. The `fangorn_trees` structure set
  puts about one in every sixteen chunks: the original rolled them far more
  often, but they failed wherever another tree's crown already stood.
- **Trees** (`trees_fangorn`): eleven tries a chunk at vanilla's oak, dark oak,
  large oak and birch, each a little taller than vanilla's own (`fangorn_oak`,
  `fangorn_dark_oak`, `fangorn_fancy_oak`, `fangorn_birch`), by the original's
  weights with its aspens and beeches folded into the nearest vanilla tree.
  A tree only takes where its trunk and the blocks round it are clear of other
  trees, so none grow up through another's leaves.
- **Fallen logs** (`fangorn_logs`): a port of the original's `LOTRWorldGenLogs`
  (`FallenLogFeature`), five a chunk, in the original's rotten log
  (`lotrmod:rotten_log`): two in five lie on the ground, two to seven blocks
  long; the rest stand as stumps.
- **Bushes** (`fangorn_bushes`): a leaf or a ragged three-by-three of them,
  as the original's `LOTRWorldGenBushes`.
- **Fangorn plants** (`fangorn_plants`): the original's six colours, green,
  brown, gold, yellow, red and silver, a patch of one colour in every other
  chunk, among vanilla's forest flowers.
- **Ent jars** (`ent_jars`): the original's `LOTRWorldGenEntJars`, a few
  earthen jars (`lotrmod:ent_jar`) on the grass, one chunk in ten. Only the jar
  is ported, not the draughts kept in it.
- **Forest floor**: about half the ground is patched with podzol, coarse dirt
  and the odd bit of gravel, in the original's proportions for its
  `enablePodzol`. These are the `lotrmod:fangorn_forest` rules in the surface
  rule, on the `lotrmod:lindon_podzol` noises.
- **Fog** (`client/FangornFog`): the original marked Fangorn foggy and drew it
  with vanilla's Nether fog, from a twentieth of the view distance out to half
  of it. Biome JSON can't, so the fog distance is set from the client, fading
  over two seconds on the way in and out.

The original's willows (which grow only near water), fallen leaves, Ents and
Huorns are left out.

### Mirkwood

Mirkwood (`lotrmod:mirkwood`) follows the original's `LOTRBiomeGenMirkwoodCorrupted`,
made thicker and gloomier: more trees, a deeper canopy, and far less light.
Under the trees almost every column has leaves overhead, most of them three or
more deep. Its grass, leaves and black water are the original's; its sky is a
dull grey-green and its fog near black.

- **Mirk oaks** (`trees_mirkwood`): a port of the original's
  `LOTRWorldGenMirkOak` (`MirkOakFeature`, `lotrmod:mirk_oak`). A three-wide
  trunk 14 to 35 tall throws boughs out all round its top, each ending in a
  flat-topped crown that widens as it goes down, propped on a star of wood,
  with roots spreading from its foot. The crowns are five layers deep (the
  original's were four), leaves droop under them half the time rather than a
  third, and vines hang twice as often. Twelve tries a chunk (the original's
  eight), a pine one time in three and vanilla's large oak now and then.
- **Understory** (`trees_mirkwood_understory`): the original's small mirk oak,
  a one-wide trunk under a single crown, six tries a chunk.
  Every Mirkwood tree is placed on the ground under any leaves already
  there, rather than on top of them, so they fill in beneath each other's crowns.
- **Pines** (`lotrmod:pine`): a port of the original's `LOTRWorldGenPine`
  (`PineFeature`): a bare trunk 12 to 24 tall under a ragged spire of leaf
  rings, rising out of the oak canopy.
- **Mirk vines** (`lotrmod:mirk_vines`): the original's dark vines, untinted,
  climbable, hanging from the oaks' crowns.
- **Fallen logs** (`mirkwood_logs`): `FallenLogFeature` in mirk oak, three a
  chunk, as the original's.
- **Webs** (`mirkwood_webs`): cobwebs for the original's webs of Ungoliant,
  only where there is a leaf just overhead or a trunk alongside.
- **Forest floor**: Fangorn's podzol, coarse dirt and gravel rule, ferns and
  mushrooms.
- **Spawns**: mostly spiders and cave spiders; few animals.
- **Gloom** (`client/MirkwoodGloom`): fading in over three seconds, the
  lightmap is dimmed by up to 60% (torchlight least, so a lit camp stands
  out), a fog closes in nearer than Fangorn's, and its colour is darkened.
  Biome JSON can't dim the light, so the dimension's sky effects are ours
  (`lotrmod:middleearth`, `client/MiddleEarthEffects`): the overworld's in
  every way except for that one call. This only changes what the client draws:
  the game's light level, which mob spawning reads, comes from the canopy itself.

The original's Wood-elf ruins, Mirkwood spiders, Dol Guldur orcs and
mirk trolls are left out.

### Rohan

Rohan (`lotrmod:rohan`) is not a port of the original's: it is built from
scratch as dry, open, very grassy horse country, gold grass under a wide pale
sky, with dark scrub only along the low ground.

- **Ground** (`RohanTerrain`): long swells about 500 blocks across around
  y 72, gentler rolls on them, now and then a broad, low down up to 20 blocks
  higher, and shallow swales a few blocks deep winding across the plain like
  dry streambeds. `RohanTerrain.moisture` says how wet the ground is: highest
  in the swales, then the hollows, lowest on the swells and downs. The grass
  colour and the scrub both follow it.
- **Grass colour** (`client/RohanGrassColors`): a biome has one grass tint,
  so in Rohan it is worked out from the ground instead, through a colour
  resolver registered on the client: drifts of straw and tawny over the dry
  ground, turning sage and then green down in the swales. Grass blocks, grass
  and ferns take it; outside Rohan they keep the biome's colour as before.
  The biome JSON's `grass_color` is only a fallback.
- **Grasses**: three new plants, coloured in their textures so they stay gold
  everywhere: `lotrmod:dry_grass` (short straw tufts), `lotrmod:tall_dry_grass`
  (two tall, gone to seed in russet plumes) and `lotrmod:wild_oats`. They drop
  seeds like vanilla grass, or themselves with shears.
  `patch_rohan_grass` covers the dry ground in mostly dry grass,
  `patch_rohan_lush_grass` the wet ground in mostly vanilla grass, and
  `patch_rohan_tall_grass` puts waist-high stands of seeded grass where a
  noise rises. About 40% of the ground carries a plant.
- **Scrub and trees** (`rohan_scrub`, `trees_rohan`): bushes of dark oak,
  oak and spruce, and small oaks and birches, only in the swales. They are
  kept there by `lotrmod:rohan_moisture`, a placement filter that passes
  where the moisture lies between a `min` and a `max`. Out on the plain stands
  the odd lone tree (`trees_rohan_lone`), one in about 32 chunks.
- **Copses** (`trees_rohan_copse`, `rohan_copse_scrub`): small woods of oak
  and birch, with a little large oak and scrub among them, five to a dozen
  trees thick, where vanilla's biome noise peaks; one every couple of hundred
  blocks, with open grass between.
- **Fewer holes**: ravines are a seventh as common as elsewhere (the biome
  uses `lotrmod:canyon_rare`, vanilla's canyon at probability 0.0015). The
  gashes vanilla's noise caves open through the ground are mostly closed
  over too: near the surface the terrain is the lesser of the ground and the
  cave entrance noise, and `lotrmod:cave_entrances` raises that noise inside
  Rohan (`CaveEntranceDensityFunction`, wrapped in by `gen_terrain.sh`).
  Caves further down are left as they are.
- **Stone**: small boulders of stone, andesite and cobblestone
  (`rohan_boulders`, the same `lotrmod:boulder` feature as Eriador's) in about
  one chunk in five. On the downs above y 86, bare rock and gravel break
  through the grass.
- **Bare earth**: patches of coarse dirt with a little gravel where the
  `lotrmod:rohan_bare` noise is high, and the odd bare spot elsewhere. These
  are the `lotrmod:rohan` rules in the surface rule.
- **Animals**: herds of horses, a few donkeys, sheep, cattle, rabbits and
  chickens. No pigs.

### The Dead Lands

The Dead Lands (`lotrmod:dead_lands`), the band of waste south and east of
Mirkwood down to Mordor, are a port of the original's Brown Lands
(`LOTRBiomeGenBrownLands`): flat, dead, brown country of withered grass and
bare earth, with nothing growing taller than a dead tree.

- **Ground** (`DeadLandsTerrain`): for the original's heights (0.2, 0.2), low
  ground around y 70, undulating gently over a couple of hundred blocks and
  broken into ledges a block or two high that run across the slopes like
  contour lines, as the original's terrain noise left gentle ground.
- **Surface**: grass, in the original's dead brown (`#AD8B69`), patched about
  half and half with coarse dirt, which runs down through the dirt below, as
  the original's two noises (a broad one and a speckle) laid it out. These are
  the `lotrmod:dead_lands` rules in the surface rule, on the
  `lotrmod:dead_lands_dirt` noise.
- **Dead oaks** (`trees_dead_lands`): a port of the original's
  `LOTRWorldGenDeadTrees` (`DeadTreeFeature`, `lotrmod:dead_tree`): a bare
  oak trunk three to six tall with four leafless branches of oak wood
  climbing out from its top. The original's only tree, in about one chunk
  in ten.
- **Boulders** (`dead_lands_boulders`): the original's stone boulders, one or
  two wide, one to six of them in one chunk in eight (`lotrmod:boulder`).
- **Grass** (`patch_dead_lands_grass`): two patches a chunk of the original's
  short grass (`lotrmod:short_grass`, its texture, tinted by the biome like
  vanilla's), with vanilla's short grass one time in three.
- **Stone ruins** (`dead_lands_stone_ruin`): a port of the original's
  `LOTRWorldGenStoneRuin` (`StoneRuinFeature`, `lotrmod:stone_ruin`) in stone
  bricks: a broken column with fallen slabs and stubs of wall around it, in
  one chunk in 2000, as the original's.
- **Weather and sky**: no rain, the original's brown-grey sky; no animals.

The original's small stone ruins (huts with rotten timber and loot chests)
and its Mordor invasions are left out.

### Hobbit holes

`lotrmod:hobbit_hole` is a port of the original's `LOTRWorldGenHobbitHole`. It
generates only in the heart of the Shire (`lotrmod:the_shire`, through the
`#lotrmod:has_structure/hobbit_hole` biome tag), since the original built hobbit
holes in the Shire biome itself and not its woodlands, moors or marshes. The
`hobbit_holes` structure set tries about once per 81 chunks (the original tried
once per 90). A hole needs dry ground within 16 blocks of its centre that varies
in height by no more than 8 blocks.

`HobbitHolePiece` follows the original line for line: a grassy mound with a
chimney, a fenced front garden, a round two-by-two hobbit-gate door under a
porch of green clay tiles, a hall, a study, a bedroom, a sitting room, a dining
room, a kitchen and a larder. It is built a chunk at a time from one seed, so
every chunk rolls the same materials. A married couple of hobbits, named from the
original's name lists (`data/lotrmod/names/`), live in each hole and stay
within 24 blocks of it.

Blocks ported for it: the four hobbit gates, oak wood beam, green clay tiling
slab, bronze, iron, silver and gold chandeliers, hobbit oven and hobbit crafting
table. The original's own woods are swapped for the nearest vanilla planks.
Plates and mugs of food, the wall banner and the hobbits' rings are left out.

## Editing the maps

All three maps reload with `/reload` — no restart needed. Already-generated chunks keep
their old terrain, so test map changes in a fresh world.

Adding a region takes two steps: add the enum constant with its map colour and
shape, and add `data/lotrmod/worldgen/biome/<name>.json`. Then add it to
`possible_biomes` in `data/lotrmod/dimension/middleearth.json`, and to the surface
rule if its ground should be anything but grass.

To add or move a river, draw it on `middleearth_rivers.png` as a line one pixel
wide (8-connected), in the colour of its region (`ANDUIN_RIVER`, `CELDUIN`, or a
new `Shape.RIVER` region), with each pixel's alpha set to the width in blocks
there. Run the line a pixel or two into the water it flows into, and join
tributaries to the river they feed. Don't paint rivers on the region map: the land
either side of a river belongs to the regions it runs between.

## A warning about biome features

Every biome JSON lists its vegetation as an ascending subsequence of one canonical
master order, documented at the top of `gen_biomes.sh`. This is not cosmetic: if two
biomes disagree about which of two shared features comes first, NeoForge's
feature-order sort hits a cycle and the game **crashes on world load**. If you
hand-edit a biome's `features`, keep the master order.

`gen_biomes.sh` regenerates all 31 biome JSONs from that single ordering. Prefer
editing the script over editing the JSONs.

### Gondor

Gondor is not a port of the original's many Gondor biomes: it is built from
scratch as five landscapes, each its own biome, all scattered with boulders of
white marble. `GondorTerrain.zone` lays them out from two broad noises, one
running from rocky ground to wet ground (leaning wet towards the coast) and
one saying how wooded the land is, in patches several hundred blocks across:

| Landscape | Biome | Where | |
| --- | --- | --- | --- |
| Plains | `lotrmod:gondor` | open ground, few trees | Wide, low, gently rolling grassland in bright yellow-green, thick with grass, golden tufts and dandelions, a lone oak in about one chunk in eight and the odd bush |
| Shrubland | `lotrmod:gondor_shrubland` | lightly wooded | Olive grass crowded with low bushes (`gondor_scrub`, eight a chunk, mostly the three-wide `gondor_shrub`, with some of spruce and some of birch, `gondor_shrub_spruce` and `gondor_shrub_birch`), patches of round oaks where vanilla's biome noise peaks (`trees_gondor_patch`), bare earth and gravel showing through |
| Forest | `lotrmod:gondor_forest` | well wooded | A mixed wood (`trees_gondor_forest`, nine a chunk) of oak (vanilla's, its large oak, and a long-trunked oak under a round crown, `gondor_tall_oak`), spruce and pine, aspen, birch and beech, small and large, each kept a little apart from the others as Fangorn's are (`gondor_forest_*`); moss and podzol on the floor, ferns and bushes |
| Marble hills | `lotrmod:gondor_marble_hills` | the rockiest ground | Knolls and crags up to some 35 blocks high, broken into ledges, with grassy hollows between them (see below) |
| Wetlands | `lotrmod:gondor_wetlands` | the wettest ground | Ground held about the waterline, so it breaks into shallow pools and grassy islands, filled with water as the Vale of Anduin's are; rather more water than land, cattails (thinner than the Vale's), sugar cane and the odd oak, muddy banks, pools floored with clay and sand |

- **Ground** (`GondorTerrain`): the plains roll a few blocks about y 67. The
  marble hills and wetlands have ground of their own, faded in across the
  edge of their zone so the land doesn't step.
- **Marble**: `lotrmod:gondor_rock`, the original's Gondor rock (its
  texture), white flecked with grey. In the marble hills everything near the
  surface above y 70 is Gondor rock, with patches of calcite through it
  (about a fifth, where the `lotrmod:gondor_calcite` noise is high), so the
  crags stand bare and white, with
  grass only on level ledges where the `lotrmod:gondor_marble_grass` noise
  allows, scarcer above y 80 and y 92: the `lotrmod:gondor_marble_hills`
  rule at the head of the surface rule. Bushes and oaks grow on the grass.
- **Boulders** (`gondor_boulders`, `_dense`, `_large`): rough lumps of
  Gondor rock with a little calcite (`lotrmod:boulder`) in three sizes
  (`gondor_boulder_small`, `_medium`, `_large`; the hills' big ones are `gondor_boulder_hills`): a stone or two about half the
  time, a lump most of the rest, and now and then a heap seven blocks across.
  One in five chunks on the plains, forest and wetlands, one in three in the
  shrubland and hills, and in the hills a big one in about one chunk in eight
  as well.
- **Grass** (`patch_gondor_grass`): mostly vanilla grass and tall grass with
  a sprinkling of Rohan's dry grass and wild oats for golden heads, and
  `patch_gondor_flowers`: dandelions, oxeye daisies, cornflowers and bluets.
- **Colours**: vanilla's clear sky; grass bright yellow-green on the plains,
  olive in the shrubland, deep green under the trees and fresh green in the
  wetlands.
