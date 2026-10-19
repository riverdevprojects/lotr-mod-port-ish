#!/bin/bash
# Copies vanilla's overworld terrain into the Middle-earth dimension.
#
# Middle-earth's terrain is vanilla's: the same height splines, 3D noise, caves,
# aquifers and ore veins. What differs is where two of the climate values those
# splines read come from. Continentalness and erosion are built from the maps
# (density_function/middleearth/continents.json and erosion.json, written by
# hand around the lotrmod:climate density function) instead of from noise alone.
#
# Vanilla's density functions refer to each other by name, so every one that
# depends on continentalness or erosion, directly or through another, is copied
# under lotrmod:middleearth/ with those references renamed. The rest (ridges,
# caves, the base 3D noise) is used straight from vanilla.
#
# The noise settings get the same treatment: vanilla's overworld settings and
# noise router, renamed, followed by the surface rule already at the end of
# noise_settings/middleearth.json, which is ours and is kept as it is.
#
# The vanilla files come from the Minecraft resources jar that ModDevGradle
# unpacks into build/moddev/artifacts, so run a Gradle build once first.
set -e

ROOT="$(cd "$(dirname "$0")" && pwd)"
JAR="$(ls "$ROOT"/build/moddev/artifacts/*client-extra*.jar 2>/dev/null | head -1)"
if [ -z "$JAR" ]; then
  echo "no Minecraft resources jar in build/moddev/artifacts; run ./gradlew compileJava first" >&2
  exit 1
fi

DATA="$ROOT/src/main/resources/data/lotrmod/worldgen"
VANILLA="$(mktemp -d)"
trap 'rm -rf "$VANILLA"' EXIT
unzip -q -o "$JAR" -d "$VANILLA" \
  'data/minecraft/worldgen/density_function/overworld/*' \
  'data/minecraft/worldgen/noise_settings/overworld.json'
V="$VANILLA/data/minecraft/worldgen"

# Everything downstream of continentalness and erosion
RENAME='s#"minecraft:overworld/(continents|erosion|offset|factor|jaggedness|depth|sloped_cheese)"#"lotrmod:middleearth/\1"#g'

mkdir -p "$DATA/density_function/middleearth"
for f in offset factor jaggedness depth sloped_cheese; do
  sed -E "$RENAME" "$V/density_function/overworld/$f.json" > "$DATA/density_function/middleearth/$f.json"
done

NS="$DATA/noise_settings/middleearth.json"
{
  sed -n '/"surface_rule"/q;p' "$V/noise_settings/overworld.json" | sed -E "$RENAME"
  sed -n '/"surface_rule"/,$p' "$NS"
} > "$NS.tmp"
mv "$NS.tmp" "$NS"

echo "copied vanilla terrain from $(basename "$JAR")"
