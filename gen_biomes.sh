#!/bin/bash
# One-shot generator for the 28 region biome JSONs, the Shire's two rings
# (shire_outskirts, shire_woodlands; see ShireZones), Lindon's woods
# (lindon_woodlands; see MiddleEarthBiomeSource), Gondor's four other landscapes
# (gondor_forest, gondor_shrubland, gondor_marble_hills, gondor_wetlands; see
# GondorTerrain), Rivendell's four (rivendell_birch_forest, rivendell_maple_forest,
# rivendell_peaks, rivendell_cherry_grove; see RivendellTerrain), Lothlorien's two
# (lothlorien_forest, lothlorien_old_growth; see LothlorienTerrain), plus the
# ocean biome.
#
# Every biome's vegetation list is written as an ascending subsequence of ONE
# canonical master order (see MASTER below). That is what keeps NeoForge's
# feature-order sort acyclic: two biomes can never disagree about which of two
# shared features comes first, because neither ever reorders the master list.
set -e

OUT="$(cd "$(dirname "$0")" && pwd)/src/main/resources/data/lotrmod/worldgen/biome"
mkdir -p "$OUT"

# ---------------------------------------------------------------- master order
#  1 patch_dead_bush      2 patch_cactus_desert  3 trees_savanna
#  4 trees_jungle         5 bamboo_vegetation    6 trees_plains
#  7 trees_birch          8 trees_flower_forest  9 trees_meadow
#  The Shire's trees (aspen and shire pine are ours, ported from the original):
# 10 lotrmod:trees_shire           11 lotrmod:trees_shire_woody
# 12 lotrmod:trees_shire_outskirts 13 lotrmod:trees_shire_thicket
# 14 lotrmod:trees_aspen_stand     15 lotrmod:trees_shire_woodlands
# 16 lotrmod:shire_bushes
#  Eriador's (beech and chestnut are ours too, ported from the original):
# 17 lotrmod:trees_eriador         18 lotrmod:trees_eriador_grove
# 19 lotrmod:trees_beech_copse     20 lotrmod:trees_aspen_copse
# 21 lotrmod:trees_marsh (vanilla's swamp oak, one tree per three chunks)
#  Lindon's (birch first, then aspen, beech and oak, as the original's):
# 22 lotrmod:trees_lindon          23 lotrmod:trees_lindon_woodlands
# 24 lotrmod:trees_lindon_grove    25 lotrmod:trees_lindon_birch_copse
# 26 lotrmod:trees_lindon_aspen_copse           27 lotrmod:trees_lindon_beech_copse
#  and its woods' red poplars (vanilla 26.3's, which 1.21.1 lacks):
# 28 lotrmod:trees_lindon_poplar   29 lotrmod:trees_lindon_poplar_stand
#  Fangorn's (its giant oaks and beeches are a structure, lotrmod:fangorn_tree):
# 30 lotrmod:trees_fangorn         31 lotrmod:fangorn_logs
# 32 lotrmod:fangorn_bushes
#  Rohan's (trees, copses and scrub; its own grasses, dry and lush):
# 33 lotrmod:trees_rohan           34 lotrmod:trees_rohan_lone
# 35 lotrmod:trees_rohan_copse     36 lotrmod:rohan_scrub
# 37 lotrmod:rohan_copse_scrub     38 lotrmod:patch_rohan_tall_grass
# 39 lotrmod:patch_rohan_grass     40 lotrmod:patch_rohan_lush_grass
#  Mirkwood's (mirk oak and pine are ours, ported from the original):
# 41 lotrmod:trees_mirkwood         42 lotrmod:trees_mirkwood_understory
# 43 lotrmod:mirkwood_logs          44 lotrmod:mirkwood_webs
#  The Dead Lands' (the original's Brown Lands: dead oaks and withered grass):
# 45 lotrmod:trees_dead_lands       46 lotrmod:patch_dead_lands_grass
#  Gondor's (its five landscapes' trees, bushes, grass and flowers):
# 46a lotrmod:trees_gondor_forest   46b lotrmod:trees_gondor_hills
# 46c lotrmod:trees_gondor_patch    46d lotrmod:trees_gondor_lone
# 46e lotrmod:trees_gondor_scattered 46f lotrmod:gondor_scrub
# 46g lotrmod:gondor_bushes         46h lotrmod:patch_gondor_grass
# 46i lotrmod:patch_gondor_grass_light 46j lotrmod:patch_gondor_flowers
# 46k lotrmod:patch_cattail_gondor
#  Rivendell's (its five landscapes' trees, bushes and flowers):
# 46l lotrmod:trees_rivendell_birch 46m lotrmod:trees_rivendell_maple
# 46n lotrmod:trees_rivendell_plains 46o lotrmod:trees_rivendell_peaks
# 46p lotrmod:trees_rivendell_cherry 46q lotrmod:rivendell_bushes
# 46r lotrmod:rivendell_maple_bushes 46s lotrmod:patch_rivendell_flowers
# 46t lotrmod:patch_rivendell_forest_flowers
#  Lothlorien's (its three landscapes' mallorns, bushes, logs and flowers):
# 46u lotrmod:trees_lothlorien_old_growth 46v lotrmod:trees_lothlorien_forest
# 46w lotrmod:trees_lothlorien_plains     46x lotrmod:trees_lothlorien_grove
# 46y lotrmod:trees_lothlorien_understory 46z lotrmod:lothlorien_logs
# 46za lotrmod:lothlorien_bushes          46zb lotrmod:patch_lothlorien_flowers
# 46zc lotrmod:patch_lothlorien_forest_flowers 46zd lotrmod:patch_fallen_mallorn_leaves
# 47 patch_grass_savanna 48 patch_grass_jungle  49 patch_grass_taiga
# 50 patch_grass_taiga_2 51 patch_grass_forest  52 patch_grass_plain
# 53 patch_grass_normal  54 patch_tall_grass    55 patch_large_fern
# 56 flower_meadow       57 flower_plains       58 flower_flower_forest
# 59 forest_flowers      59a flower_cherry      60 flower_default
# 61 flower_swamp
# 62 lotrmod:fangorn_plants
# 63 patch_berry_common  64 patch_pumpkin       65 patch_sugar_cane
# 66 lotrmod:patch_cattail_marsh
# 67 lotrmod:patch_cattail
# 68 patch_melon         69 patch_waterlily     70 brown_mushroom_normal
# 71 red_mushroom_normal
# 72 lotrmod:ent_jars
# 73 seagrass_normal     74 kelp_cold
#
# Only Eriador, Rohan, the Dead Lands, Gondor, Rivendell and Lothlorien have anything in
# local_modifications, each its own boulders (lotrmod:eriador_boulders,
# lotrmod:rohan_boulders, lotrmod:dead_lands_boulders, Gondor's marble ones,
# always in the order gondor_boulders, gondor_boulders_dense,
# gondor_boulders_large, lotrmod:rivendell_boulders and lotrmod:lothlorien_boulders), so that step has no
# other order to keep. Likewise surface_structures: only Harad's well and the
# Dead Lands' stone ruins; and fluid_springs: only Rivendell's waterfalls,
# lotrmod:rivendell_waterfalls or lotrmod:rivendell_waterfalls_peaks.
#
# Names without a namespace are minecraft's.
#
# The ocean is ours too (lotrmod:ocean), not minecraft:ocean. Vanilla's ocean
# orders its vegetation differently (mushrooms before sugar cane, sugar cane
# before pumpkin), so pairing it with any biome that follows the master order
# closes a cycle — vale_of_anduin + minecraft:ocean did exactly that. Never
# point the dimension at a vanilla biome.

ORES='"minecraft:ore_dirt","minecraft:ore_gravel","minecraft:ore_granite_upper","minecraft:ore_granite_lower","minecraft:ore_diorite_upper","minecraft:ore_diorite_lower","minecraft:ore_andesite_upper","minecraft:ore_andesite_lower","minecraft:ore_tuff","minecraft:ore_coal_upper","minecraft:ore_coal_lower","minecraft:ore_iron_upper","minecraft:ore_iron_middle","minecraft:ore_iron_small","minecraft:ore_gold","minecraft:ore_gold_lower","minecraft:ore_redstone","minecraft:ore_redstone_lower","minecraft:ore_diamond","minecraft:ore_diamond_large","minecraft:ore_diamond_buried","minecraft:ore_lapis","minecraft:ore_lapis_buried","minecraft:ore_copper"'

CARVERS='"minecraft:cave","minecraft:cave_extra_underground","minecraft:canyon"'
# Rohan's, Gondor's, Rivendell's and Lothlorien's ravines are a seventh as common as vanilla's, to leave their plains whole
CARVERS_ROHAN='"minecraft:cave","minecraft:cave_extra_underground","lotrmod:canyon_rare"'

mon() {
  case "$1" in
    desert) cat <<'EOF'
{"type":"minecraft:spider","weight":100,"minCount":1,"maxCount":4},
{"type":"minecraft:husk","weight":95,"minCount":4,"maxCount":4},
{"type":"minecraft:skeleton","weight":100,"minCount":4,"maxCount":4},
{"type":"minecraft:creeper","weight":100,"minCount":4,"maxCount":4}
EOF
    ;;
    cold) cat <<'EOF'
{"type":"minecraft:spider","weight":100,"minCount":1,"maxCount":4},
{"type":"minecraft:zombie","weight":95,"minCount":4,"maxCount":4},
{"type":"minecraft:stray","weight":80,"minCount":4,"maxCount":4},
{"type":"minecraft:creeper","weight":100,"minCount":4,"maxCount":4}
EOF
    ;;
    ocean) cat <<'EOF'
{"type":"minecraft:spider","weight":100,"minCount":1,"maxCount":4},
{"type":"minecraft:zombie","weight":95,"minCount":4,"maxCount":4},
{"type":"minecraft:skeleton","weight":100,"minCount":4,"maxCount":4},
{"type":"minecraft:creeper","weight":100,"minCount":4,"maxCount":4},
{"type":"minecraft:drowned","weight":100,"minCount":1,"maxCount":1}
EOF
    ;;
    # Spiders above all, for the original's Mirkwood spiders, and cave spiders among them
    mirkwood) cat <<'EOF'
{"type":"minecraft:spider","weight":300,"minCount":2,"maxCount":5},
{"type":"minecraft:cave_spider","weight":60,"minCount":2,"maxCount":4},
{"type":"minecraft:zombie","weight":60,"minCount":2,"maxCount":4},
{"type":"minecraft:skeleton","weight":60,"minCount":2,"maxCount":4},
{"type":"minecraft:creeper","weight":60,"minCount":1,"maxCount":3},
{"type":"minecraft:witch","weight":5,"minCount":1,"maxCount":1}
EOF
    ;;
    *) cat <<'EOF'
{"type":"minecraft:spider","weight":100,"minCount":1,"maxCount":4},
{"type":"minecraft:zombie","weight":95,"minCount":4,"maxCount":4},
{"type":"minecraft:skeleton","weight":100,"minCount":4,"maxCount":4},
{"type":"minecraft:creeper","weight":100,"minCount":4,"maxCount":4},
{"type":"minecraft:slime","weight":100,"minCount":4,"maxCount":4}
EOF
    ;;
  esac
}

crt() {
  case "$1" in
    desert) printf '%s' '{"type":"minecraft:rabbit","weight":4,"minCount":2,"maxCount":3}' ;;
    cold)   printf '%s' '{"type":"minecraft:rabbit","weight":10,"minCount":2,"maxCount":3},{"type":"minecraft:polar_bear","weight":1,"minCount":1,"maxCount":2},{"type":"minecraft:fox","weight":8,"minCount":2,"maxCount":4}' ;;
    barren|ocean) printf '%s' '' ;;
    # Horse country: herds of horses, with the odd donkey, sheep and cattle
    rohan)  printf '%s' '{"type":"minecraft:horse","weight":14,"minCount":3,"maxCount":8},{"type":"minecraft:donkey","weight":2,"minCount":1,"maxCount":3},{"type":"minecraft:sheep","weight":8,"minCount":3,"maxCount":5},{"type":"minecraft:cow","weight":6,"minCount":2,"maxCount":4},{"type":"minecraft:rabbit","weight":6,"minCount":2,"maxCount":3},{"type":"minecraft:chicken","weight":3,"minCount":2,"maxCount":4}' ;;
    forest) printf '%s' '{"type":"minecraft:sheep","weight":12,"minCount":4,"maxCount":4},{"type":"minecraft:pig","weight":10,"minCount":4,"maxCount":4},{"type":"minecraft:chicken","weight":10,"minCount":4,"maxCount":4},{"type":"minecraft:cow","weight":8,"minCount":4,"maxCount":4},{"type":"minecraft:fox","weight":8,"minCount":2,"maxCount":4},{"type":"minecraft:wolf","weight":5,"minCount":4,"maxCount":4}' ;;
    # Few beasts brave it: wolves, and the odd fox and rabbit
    mirkwood) printf '%s' '{"type":"minecraft:wolf","weight":4,"minCount":2,"maxCount":4},{"type":"minecraft:fox","weight":2,"minCount":1,"maxCount":2},{"type":"minecraft:rabbit","weight":2,"minCount":1,"maxCount":2}' ;;
    *)      printf '%s' '{"type":"minecraft:sheep","weight":12,"minCount":4,"maxCount":4},{"type":"minecraft:pig","weight":10,"minCount":4,"maxCount":4},{"type":"minecraft:chicken","weight":10,"minCount":4,"maxCount":4},{"type":"minecraft:cow","weight":8,"minCount":4,"maxCount":4},{"type":"minecraft:horse","weight":5,"minCount":2,"maxCount":6},{"type":"minecraft:donkey","weight":1,"minCount":1,"maxCount":3},{"type":"minecraft:rabbit","weight":4,"minCount":2,"maxCount":3}' ;;
  esac
}

# biome <name> <temp> <downfall> <precip> <sky> <fog> <water> <waterfog> <grass> <foliage> <spawnprofile> <veg-csv> <toplayer-csv> [<underground-structures-csv> <surface-structures-csv> <local-modifications-csv> <fluid-springs-csv>]
biome() {
  local name="$1" temp="$2" down="$3" precip="$4" sky="$5" fog="$6" water="$7" wfog="$8" grass="$9" foliage="${10}" prof="${11}" veg="${12}" top="${13}" ustruct="${14}" sstruct="${15}" localmod="${16}" springs="${17}"
  {
    printf '{\n'
    printf '  "has_precipitation": %s,\n' "$precip"
    printf '  "temperature": %s,\n' "$temp"
    printf '  "downfall": %s,\n' "$down"
    printf '  "effects": {\n'
    printf '    "sky_color": %s,\n' "$sky"
    printf '    "fog_color": %s,\n' "$fog"
    printf '    "water_color": %s,\n' "$water"
    printf '    "water_fog_color": %s,\n' "$wfog"
    printf '    "grass_color": %s,\n' "$grass"
    printf '    "foliage_color": %s,\n' "$foliage"
    printf '    "mood_sound": {\n'
    printf '      "sound": "minecraft:ambient.cave",\n'
    printf '      "tick_delay": 6000,\n'
    printf '      "block_search_extent": 8,\n'
    printf '      "offset": 2.0\n'
    printf '    }\n'
    printf '  },\n'
    printf '  "spawners": {\n'
    printf '    "monster": [%s],\n' "$(mon "$prof" | tr -d '\n')"
    if [ "$prof" = ocean ]; then
      printf '    "creature": [],\n'
      printf '    "water_creature": [{"type":"minecraft:squid","weight":1,"minCount":1,"maxCount":4}],\n'
      printf '    "water_ambient": [{"type":"minecraft:cod","weight":10,"minCount":3,"maxCount":6}]\n'
    else
      printf '    "creature": [%s]\n' "$(crt "$prof")"
    fi
    printf '  },\n'
    printf '  "spawn_costs": {},\n'
    local carvers="$CARVERS"
    case "$name" in rohan|gondor*|rivendell*|lothlorien*) carvers="$CARVERS_ROHAN" ;; esac
    printf '  "carvers": { "air": [%s] },\n' "$carvers"
    printf '  "features": [\n'
    printf '    [],\n'      # raw_generation
    printf '    [],\n'      # lakes
    printf '    [%s],\n' "$localmod"   # local_modifications
    printf '    [%s],\n' "$ustruct"   # underground_structures
    printf '    [%s],\n' "$sstruct"   # surface_structures
    printf '    [],\n'      # strongholds
    printf '    [%s],\n' "$ORES"
    printf '    ["minecraft:amethyst_geode"],\n'
    printf '    [%s],\n' "$springs"   # fluid_springs
    printf '    [%s],\n' "$veg"
    printf '    [%s]\n' "$top"
    printf '  ]\n'
    printf '}\n'
  } > "$OUT/$name.json"
}

V() { # join feature short-names into a quoted csv; bare names are minecraft's
  local out=""
  for f in "$@"; do
    [ -n "$out" ] && out="$out,"
    case "$f" in
      *:*) out="$out\"$f\"" ;;
      *)   out="$out\"minecraft:$f\"" ;;
    esac
  done
  printf '%s' "$out"
}

FREEZE='"minecraft:freeze_top_layer"'

# ── palettes ────────────────────────────────────────────────────────────────
# sky        fog        water     waterfog  grass     foliage
P_PLAIN="7907327 12638463 4159204 329011 9470285 7842607"
P_FOREST="7972607 12638463 4159204 329011 7975533 5159473"
P_DARK="7972607 12638463 4159204 329011 6975545 4215066"
P_MOUNT="8232447 12638463 4159204 329011 8626540 6713893"
P_SNOW="8364543 12638463 4020182 329011 8434839 7842607"
P_DESERT="7254527 12638463 4159204 329011 11048095 10387789"
P_STEPPE="7907327 12638463 4159204 329011 10595616 9470285"
P_MORDOR="4994063 6187100 4159204 329011 6971191 6971191"
P_BARREN="7838783 12638463 4159204 329011 10387789 10066329"
P_SWAMP="7907327 12638463 6388580 2302743 9154376 6975545"
P_RIVER="7907327 12638463 4159204 329011 9470285 7842607"
P_OCEAN="8103167 12638463 4159204 329011 9470285 7842607"
# The Shire's bright grass is the original mod's; the woodlands' is vanilla's
# colour for the original's climate (0.8, 0.9)
P_SHIRE="7907327 12638463 4159204 329011 8111137 4568097"
P_SHIRE_WOOD="7907327 12638463 4159204 329011 6997070 4568097"
# Fangorn's colours start from the original's (vanilla's grass and leaves for
# its climate, 0.7 and 0.8; its own sky; its fog tint over vanilla's daytime
# fog) and are taken half way to grey, the grass and leaves a little darker
P_FANGORN="8623745 4218962 4159204 329011 8100461 6259533"
# Eriador's is vanilla's colour for the original's climate (0.9, 0.8)
P_ERIADOR="7907327 12638463 4159204 329011 6932037 4502806"
# So is Lindon's (0.9, 0.9). Its woodlands' grass is vanilla's for (0.9, 1.0)
# taken a third of the way to the dappled forest's orange, for an autumn floor
P_LINDON="7907327 12638463 4159204 329011 6277186 3651602"
# Rohan's grass colour is a fallback: in game it is worked out from the
# ground (client/RohanGrassColors), straw over the swells, sage in the swales.
# A pale, wide sky over a warm, hazy horizon, and dusty olive leaves
P_ROHAN="8956382 13884130 4159204 329011 12888412 8163141"
P_LINDON_WOOD="7907327 12638463 4159204 329011 8759095 2865935"
# Mirkwood's grass, leaves and black water are the original's; its sky is a
# dull grey-green and its fog near black, both far darker than the original's
P_MIRKWOOD="4807766 1580828 1708838 657930 2841381 2503461"
# The Dead Lands' sky and dead brown grass are the original Brown Lands';
# its leaves are vanilla's for the original's climate (1.0, 0.2)
P_DEAD_LANDS="8878434 12638463 4159204 329011 11373417 11445290"
# Gondor's: a clear blue sky over bright yellow-green grass on the plains,
# olive in the shrubland, deep green under the forest, fresh green by the water
P_GONDOR="7907327 12638463 4159204 329011 10337868 6986293"
P_GONDOR_FOREST="7907327 12638463 4159204 329011 7314236 4880940"
P_GONDOR_SHRUB="7907327 12638463 4159204 329011 10860622 7245366"
P_GONDOR_HILLS="7907327 12638463 4159204 329011 9418824 6196787"
P_GONDOR_WET="7907327 12638463 4159204 329011 8826956 6263872"
# Rivendell's: a deep clear sky and bright water over lush grass on the valley
# floor; a cooler green in the birch wood; an autumn floor under the maples,
# whose vanilla leaves (the bushes' oak) turn orange too; a cold grey-green on
# the peaks; and vanilla's cherry grove colours among the cherries
# Lothlorien: a clear sky over a golden haze and leaves of gold (vanilla's
# leaves and vines too); gold grass in the fields, olive green under the trees
P_LOTHLORIEN="8830187 15784096 4165568 997962 13217866 14264368"
P_LOTHLORIEN_FOREST="8830187 15784096 4165568 997962 9214016 14264368"
P_LOTHLORIEN_OLD="8830187 15784096 4165568 997962 8031288 14264368"
P_RIVENDELL="7841791 12638463 4566000 329011 8956490 6137902"
P_RIVENDELL_BIRCH="7841791 12638463 4566000 329011 7316287 6200884"
P_RIVENDELL_MAPLE="7841791 12638463 4566000 329011 10394948 12093995"
P_RIVENDELL_PEAKS="8364543 12638463 4020182 329011 8626540 6713893"
P_RIVENDELL_CHERRY="8364543 12638463 6141935 329011 11983713 11983713"

# ── the 28 regions ──────────────────────────────────────────────────────────
biome lindon                   0.9 0.9 true $P_LINDON temp   "$(V lotrmod:trees_lindon lotrmod:trees_lindon_grove lotrmod:trees_lindon_birch_copse lotrmod:trees_lindon_aspen_copse lotrmod:trees_lindon_beech_copse patch_grass_plain patch_tall_grass flower_plains flower_default)" ""
biome blue_mountains           0.2 0.4 true $P_MOUNT  cold   "$(V patch_grass_taiga_2 patch_grass_normal)" "$FREEZE"
biome eriador                  0.9 0.8 true $P_ERIADOR temp  "$(V lotrmod:trees_eriador lotrmod:trees_eriador_grove lotrmod:trees_beech_copse lotrmod:trees_aspen_copse patch_grass_plain patch_tall_grass flower_plains flower_default patch_berry_common)" "" "" "" "$(V lotrmod:eriador_boulders)"
biome arnor                    0.6 0.6 true $P_PLAIN  temp   "$(V trees_plains trees_birch patch_grass_plain patch_tall_grass flower_default)" ""
biome misty_mountains          0.15 0.4 true $P_MOUNT cold   "$(V patch_grass_taiga_2 patch_grass_normal)" "$FREEZE"
biome grey_mountains           0.15 0.4 true $P_MOUNT cold   "$(V patch_grass_taiga_2 patch_grass_normal)" "$FREEZE"
biome white_mountains          0.2 0.4 true $P_MOUNT  cold   "$(V patch_grass_taiga_2 patch_grass_normal)" "$FREEZE"
biome mountains_of_shadow      0.5 0.0 false $P_MORDOR barren "$(V patch_dead_bush)" ""
biome gondor                   0.8 0.6 true $P_GONDOR temp   "$(V lotrmod:trees_gondor_lone lotrmod:gondor_bushes lotrmod:patch_gondor_grass lotrmod:patch_gondor_flowers)" "" "" "" "$(V lotrmod:gondor_boulders)"
biome harad                    2.0 0.0 false $P_DESERT desert "$(V patch_dead_bush patch_cactus_desert patch_grass_savanna)" "" "$(V fossil_upper fossil_lower)" "$(V desert_well)"
biome lothlorien               0.8 0.8 true $P_LOTHLORIEN temp "$(V lotrmod:trees_lothlorien_plains lotrmod:trees_lothlorien_grove lotrmod:patch_lothlorien_flowers patch_grass_plain patch_tall_grass)" ""
biome mirkwood                 0.6 0.8 true $P_MIRKWOOD mirkwood "$(V lotrmod:trees_mirkwood lotrmod:trees_mirkwood_understory lotrmod:mirkwood_logs lotrmod:mirkwood_webs patch_grass_taiga patch_large_fern brown_mushroom_normal red_mushroom_normal)" ""
biome dale                     0.6 0.6 true $P_PLAIN  temp   "$(V trees_plains trees_birch patch_grass_plain patch_tall_grass flower_default patch_pumpkin)" ""
biome erebor                   0.3 0.4 true $P_MOUNT  cold   "$(V patch_grass_taiga_2 patch_grass_normal)" "$FREEZE"
biome iron_hills               0.4 0.4 true $P_MOUNT  cold   "$(V patch_grass_normal)" ""
biome rohan                    0.8 0.3 true $P_ROHAN  rohan  "$(V lotrmod:trees_rohan lotrmod:trees_rohan_lone lotrmod:trees_rohan_copse lotrmod:rohan_scrub lotrmod:rohan_copse_scrub lotrmod:patch_rohan_tall_grass lotrmod:patch_rohan_grass lotrmod:patch_rohan_lush_grass)" "" "" "" "$(V lotrmod:rohan_boulders)"
biome mordor                   1.5 0.0 false $P_MORDOR barren "$(V patch_dead_bush)" ""
biome rhun                     0.9 0.3 true $P_STEPPE temp   "$(V patch_grass_savanna patch_grass_plain patch_tall_grass flower_default)" ""
biome fangorn_forest           0.7 0.8 true $P_FANGORN forest "$(V lotrmod:trees_fangorn lotrmod:fangorn_logs lotrmod:fangorn_bushes patch_grass_taiga patch_tall_grass patch_large_fern forest_flowers flower_default lotrmod:fangorn_plants brown_mushroom_normal red_mushroom_normal lotrmod:ent_jars)" ""
biome anduin_river             0.7 0.8 true $P_RIVER  temp   "$(V patch_grass_normal patch_tall_grass patch_sugar_cane lotrmod:patch_cattail)" ""
biome vale_of_anduin           0.75 0.9 true $P_SWAMP temp   "$(V lotrmod:trees_marsh patch_grass_normal patch_tall_grass patch_large_fern flower_swamp patch_sugar_cane lotrmod:patch_cattail_marsh patch_waterlily brown_mushroom_normal red_mushroom_normal)" ""
biome dead_lands               1.0 0.2 false $P_DEAD_LANDS barren "$(V lotrmod:trees_dead_lands lotrmod:patch_dead_lands_grass)" "" "" "$(V lotrmod:dead_lands_stone_ruin)" "$(V lotrmod:dead_lands_boulders)"
biome celduin                  0.6 0.8 true $P_RIVER  temp   "$(V patch_grass_normal patch_tall_grass patch_sugar_cane lotrmod:patch_cattail)" ""
biome eastern_rhovanian_plains 0.8 0.4 true $P_STEPPE temp   "$(V patch_grass_savanna patch_grass_plain patch_tall_grass flower_default)" ""
biome sea_of_rhun              0.8 0.6 true $P_RIVER  temp   "$(V patch_grass_normal patch_tall_grass patch_sugar_cane lotrmod:patch_cattail)" ""
biome forodwaith               0.0 0.5 true $P_SNOW   cold   "$(V patch_grass_taiga_2)" "$FREEZE"
biome the_shire                0.8 0.9 true $P_SHIRE  temp   "$(V lotrmod:trees_shire lotrmod:trees_shire_woody lotrmod:shire_bushes patch_grass_plain patch_tall_grass flower_plains flower_default patch_pumpkin)" ""
biome rivendell                0.8 0.7 true $P_RIVENDELL temp "$(V lotrmod:trees_rivendell_plains lotrmod:patch_rivendell_flowers patch_grass_plain patch_tall_grass flower_plains)" ""

# ── the Shire's rings (not regions on the map: see ShireZones) ─────────────
biome shire_outskirts          0.8 0.9 true $P_SHIRE  temp   "$(V lotrmod:trees_shire_outskirts lotrmod:trees_shire_thicket lotrmod:trees_aspen_stand lotrmod:shire_bushes patch_grass_forest patch_tall_grass forest_flowers flower_default brown_mushroom_normal)" ""
biome shire_woodlands          0.8 0.9 true $P_SHIRE_WOOD forest "$(V lotrmod:trees_shire_woodlands patch_grass_taiga patch_large_fern forest_flowers flower_default patch_berry_common brown_mushroom_normal)" ""

# ── Lindon's woods (not a region on the map: see MiddleEarthBiomeSource) ───
biome lindon_woodlands         0.9 1.0 true $P_LINDON_WOOD forest "$(V lotrmod:trees_lindon_woodlands lotrmod:trees_lindon_grove lotrmod:trees_lindon_birch_copse lotrmod:trees_lindon_aspen_copse lotrmod:trees_lindon_beech_copse lotrmod:trees_lindon_poplar lotrmod:trees_lindon_poplar_stand patch_grass_forest patch_tall_grass forest_flowers flower_default brown_mushroom_normal)" ""

# ── Gondor's other landscapes (not regions on the map: see GondorTerrain) ──
biome gondor_forest            0.8 0.8 true $P_GONDOR_FOREST forest "$(V lotrmod:fangorn_bushes lotrmod:trees_gondor_forest lotrmod:gondor_bushes lotrmod:patch_gondor_grass_light patch_large_fern forest_flowers brown_mushroom_normal)" "" "" "" "$(V lotrmod:gondor_boulders)"
biome gondor_shrubland         0.8 0.5 true $P_GONDOR_SHRUB temp "$(V lotrmod:trees_gondor_patch lotrmod:trees_gondor_scattered lotrmod:gondor_scrub lotrmod:patch_gondor_grass lotrmod:patch_gondor_flowers)" "" "" "" "$(V lotrmod:gondor_boulders_dense)"
biome gondor_marble_hills      0.8 0.5 true $P_GONDOR_HILLS temp "$(V lotrmod:trees_gondor_hills lotrmod:gondor_scrub lotrmod:patch_gondor_grass_light lotrmod:patch_gondor_flowers)" "" "" "" "$(V lotrmod:gondor_boulders_dense lotrmod:gondor_boulders_large)"
biome gondor_wetlands          0.8 0.9 true $P_GONDOR_WET temp "$(V lotrmod:trees_gondor_scattered lotrmod:gondor_bushes lotrmod:patch_gondor_grass_light lotrmod:patch_cattail_gondor patch_sugar_cane)" "" "" "" "$(V lotrmod:gondor_boulders)"

# ── Rivendell's other landscapes (not regions on the map: see RivendellTerrain)
biome rivendell_birch_forest   0.7 0.8 true $P_RIVENDELL_BIRCH forest "$(V lotrmod:trees_rivendell_birch lotrmod:rivendell_bushes lotrmod:patch_rivendell_forest_flowers patch_grass_taiga patch_large_fern forest_flowers brown_mushroom_normal)" "" "" "" "$(V lotrmod:rivendell_boulders)"
biome rivendell_maple_forest   0.7 0.8 true $P_RIVENDELL_MAPLE forest "$(V lotrmod:trees_rivendell_maple lotrmod:rivendell_maple_bushes patch_grass_forest patch_large_fern forest_flowers)" "" "" "" "$(V lotrmod:rivendell_boulders)" "$(V lotrmod:rivendell_waterfalls)"
biome rivendell_peaks          0.28 0.5 true $P_RIVENDELL_PEAKS cold "$(V lotrmod:trees_rivendell_peaks patch_grass_taiga_2)" "$FREEZE" "" "" "" "$(V lotrmod:rivendell_waterfalls_peaks)"
biome rivendell_cherry_grove   0.1 0.8 true $P_RIVENDELL_CHERRY temp "$(V lotrmod:trees_rivendell_cherry patch_grass_plain flower_cherry)" "$FREEZE" "" "" "" "$(V lotrmod:rivendell_waterfalls_peaks)"

# ── Lothlorien's other landscapes (not regions on the map: see LothlorienTerrain)
biome lothlorien_forest        0.8 0.8 true $P_LOTHLORIEN_FOREST forest "$(V lotrmod:trees_lothlorien_forest lotrmod:lothlorien_bushes lotrmod:patch_lothlorien_forest_flowers lotrmod:patch_fallen_mallorn_leaves patch_grass_forest patch_tall_grass patch_large_fern forest_flowers)" ""
biome lothlorien_old_growth    0.8 0.9 true $P_LOTHLORIEN_OLD forest "$(V lotrmod:trees_lothlorien_old_growth lotrmod:trees_lothlorien_understory lotrmod:lothlorien_logs lotrmod:lothlorien_bushes lotrmod:patch_lothlorien_forest_flowers lotrmod:patch_fallen_mallorn_leaves patch_grass_taiga patch_large_fern forest_flowers brown_mushroom_normal)" "" "" "" "$(V lotrmod:lothlorien_boulders)"

# ── the sea (decided by the landmask, not a region on the map) ──────────────
biome ocean                    0.5 0.5 true $P_OCEAN  ocean  "$(V seagrass_normal kelp_cold)" ""

echo "wrote $(ls -1 "$OUT" | wc -l) biome files"
