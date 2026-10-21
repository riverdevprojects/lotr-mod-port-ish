#!/bin/bash
# One-shot generator for the 28 region biome JSONs, plus the ocean biome.
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
# 10 lotrmod:trees_marsh (vanilla's swamp oak, one tree per three chunks)
# 11 patch_grass_savanna 12 patch_grass_jungle  13 patch_grass_taiga_2
# 14 patch_grass_forest  15 patch_grass_plain   16 patch_grass_normal
# 17 patch_tall_grass    18 patch_large_fern    19 flower_meadow
# 20 flower_flower_forest 21 forest_flowers     22 flower_default
# 23 flower_swamp        24 patch_berry_common  25 patch_pumpkin
# 26 patch_sugar_cane    27 lotrmod:patch_cattail_marsh
# 28 lotrmod:patch_cattail                      29 patch_melon
# 30 patch_waterlily     31 brown_mushroom_normal 32 red_mushroom_normal
# 33 seagrass_normal     34 kelp_cold
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
    forest) printf '%s' '{"type":"minecraft:sheep","weight":12,"minCount":4,"maxCount":4},{"type":"minecraft:pig","weight":10,"minCount":4,"maxCount":4},{"type":"minecraft:chicken","weight":10,"minCount":4,"maxCount":4},{"type":"minecraft:cow","weight":8,"minCount":4,"maxCount":4},{"type":"minecraft:fox","weight":8,"minCount":2,"maxCount":4},{"type":"minecraft:wolf","weight":5,"minCount":4,"maxCount":4}' ;;
    *)      printf '%s' '{"type":"minecraft:sheep","weight":12,"minCount":4,"maxCount":4},{"type":"minecraft:pig","weight":10,"minCount":4,"maxCount":4},{"type":"minecraft:chicken","weight":10,"minCount":4,"maxCount":4},{"type":"minecraft:cow","weight":8,"minCount":4,"maxCount":4},{"type":"minecraft:horse","weight":5,"minCount":2,"maxCount":6},{"type":"minecraft:donkey","weight":1,"minCount":1,"maxCount":3},{"type":"minecraft:rabbit","weight":4,"minCount":2,"maxCount":3}' ;;
  esac
}

# biome <name> <temp> <downfall> <precip> <sky> <fog> <water> <waterfog> <grass> <foliage> <spawnprofile> <veg-csv> <toplayer-csv> [<underground-structures-csv> <surface-structures-csv>]
biome() {
  local name="$1" temp="$2" down="$3" precip="$4" sky="$5" fog="$6" water="$7" wfog="$8" grass="$9" foliage="${10}" prof="${11}" veg="${12}" top="${13}" ustruct="${14}" sstruct="${15}"
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
    printf '  "carvers": { "air": [%s] },\n' "$CARVERS"
    printf '  "features": [\n'
    printf '    [],\n'      # raw_generation
    printf '    [],\n'      # lakes
    printf '    [],\n'      # local_modifications
    printf '    [%s],\n' "$ustruct"   # underground_structures
    printf '    [%s],\n' "$sstruct"   # surface_structures
    printf '    [],\n'      # strongholds
    printf '    [%s],\n' "$ORES"
    printf '    ["minecraft:amethyst_geode"],\n'
    printf '    [],\n'      # fluid_springs
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
P_GOLD="8233727 12638463 4159204 329011 12502092 11983724"
P_MOUNT="8232447 12638463 4159204 329011 8626540 6713893"
P_SNOW="8364543 12638463 4020182 329011 8434839 7842607"
P_DESERT="7254527 12638463 4159204 329011 11048095 10387789"
P_STEPPE="7907327 12638463 4159204 329011 10595616 9470285"
P_MORDOR="4994063 6187100 4159204 329011 6971191 6971191"
P_BARREN="7838783 12638463 4159204 329011 10387789 10066329"
P_SWAMP="7907327 12638463 6388580 2302743 9154376 6975545"
P_RIVER="7907327 12638463 4159204 329011 9470285 7842607"
P_OCEAN="8103167 12638463 4159204 329011 9470285 7842607"

# ── the 28 regions ──────────────────────────────────────────────────────────
biome lindon                   0.7 0.8 true $P_FOREST forest "$(V trees_birch trees_flower_forest patch_grass_forest patch_tall_grass patch_large_fern flower_flower_forest forest_flowers patch_berry_common brown_mushroom_normal red_mushroom_normal)" ""
biome blue_mountains           0.2 0.4 true $P_MOUNT  cold   "$(V patch_grass_taiga_2 patch_grass_normal)" "$FREEZE"
biome eriador                  0.7 0.6 true $P_PLAIN  temp   "$(V trees_plains trees_birch patch_grass_plain patch_tall_grass flower_default patch_berry_common)" ""
biome arnor                    0.6 0.6 true $P_PLAIN  temp   "$(V trees_plains trees_birch patch_grass_plain patch_tall_grass flower_default)" ""
biome misty_mountains          0.15 0.4 true $P_MOUNT cold   "$(V patch_grass_taiga_2 patch_grass_normal)" "$FREEZE"
biome grey_mountains           0.15 0.4 true $P_MOUNT cold   "$(V patch_grass_taiga_2 patch_grass_normal)" "$FREEZE"
biome white_mountains          0.2 0.4 true $P_MOUNT  cold   "$(V patch_grass_taiga_2 patch_grass_normal)" "$FREEZE"
biome mountains_of_shadow      0.5 0.0 false $P_MORDOR barren "$(V patch_dead_bush)" ""
biome gondor                   0.9 0.5 true $P_PLAIN  temp   "$(V trees_plains patch_grass_plain patch_tall_grass flower_default patch_melon)" ""
biome harad                    2.0 0.0 false $P_DESERT desert "$(V patch_dead_bush patch_cactus_desert patch_grass_savanna)" "" "$(V fossil_upper fossil_lower)" "$(V desert_well)"
biome lothlorien               0.8 0.8 true $P_GOLD   forest "$(V trees_flower_forest patch_grass_forest patch_tall_grass patch_large_fern flower_flower_forest forest_flowers)" ""
biome mirkwood                 0.6 0.8 true $P_DARK   forest "$(V patch_grass_forest patch_large_fern brown_mushroom_normal red_mushroom_normal)" ""
biome dale                     0.6 0.6 true $P_PLAIN  temp   "$(V trees_plains trees_birch patch_grass_plain patch_tall_grass flower_default patch_pumpkin)" ""
biome erebor                   0.3 0.4 true $P_MOUNT  cold   "$(V patch_grass_taiga_2 patch_grass_normal)" "$FREEZE"
biome iron_hills               0.4 0.4 true $P_MOUNT  cold   "$(V patch_grass_normal)" ""
biome rohan                    0.8 0.4 true $P_STEPPE temp   "$(V trees_savanna patch_grass_savanna patch_grass_plain patch_tall_grass flower_default)" ""
biome mordor                   1.5 0.0 false $P_MORDOR barren "$(V patch_dead_bush)" ""
biome rhun                     0.9 0.3 true $P_STEPPE temp   "$(V patch_grass_savanna patch_grass_plain patch_tall_grass flower_default)" ""
biome fangorn_forest           0.7 0.9 true $P_DARK   forest "$(V trees_flower_forest patch_grass_forest patch_tall_grass patch_large_fern forest_flowers brown_mushroom_normal red_mushroom_normal)" ""
biome anduin_river             0.7 0.8 true $P_RIVER  temp   "$(V patch_grass_normal patch_tall_grass patch_sugar_cane lotrmod:patch_cattail)" ""
biome vale_of_anduin           0.75 0.9 true $P_SWAMP temp   "$(V lotrmod:trees_marsh patch_grass_normal patch_tall_grass patch_large_fern flower_swamp patch_sugar_cane lotrmod:patch_cattail_marsh patch_waterlily brown_mushroom_normal red_mushroom_normal)" ""
biome dead_lands               0.6 0.1 false $P_BARREN barren "$(V patch_dead_bush)" ""
biome celduin                  0.6 0.8 true $P_RIVER  temp   "$(V patch_grass_normal patch_tall_grass patch_sugar_cane lotrmod:patch_cattail)" ""
biome eastern_rhovanian_plains 0.8 0.4 true $P_STEPPE temp   "$(V patch_grass_savanna patch_grass_plain patch_tall_grass flower_default)" ""
biome sea_of_rhun              0.8 0.6 true $P_RIVER  temp   "$(V patch_grass_normal patch_tall_grass patch_sugar_cane lotrmod:patch_cattail)" ""
biome forodwaith               0.0 0.5 true $P_SNOW   cold   "$(V patch_grass_taiga_2)" "$FREEZE"
biome the_shire                0.7 0.7 true $P_PLAIN  temp   "$(V trees_plains trees_birch patch_grass_plain patch_tall_grass flower_default patch_berry_common patch_pumpkin)" ""
biome rivendell                0.6 0.7 true $P_FOREST forest "$(V trees_birch trees_flower_forest patch_grass_forest patch_tall_grass patch_large_fern flower_flower_forest forest_flowers)" ""

# ── the sea (decided by the landmask, not a region on the map) ──────────────
biome ocean                    0.5 0.5 true $P_OCEAN  ocean  "$(V seagrass_normal kelp_cold)" ""

echo "wrote $(ls -1 "$OUT" | wc -l) biome files"
