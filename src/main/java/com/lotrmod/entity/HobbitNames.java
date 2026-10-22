package com.lotrmod.entity;

import com.lotrmod.LOTRMod;
import net.minecraft.util.RandomSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hobbit names, from the original mod's name lists in
 * {@code data/lotrmod/names/}: forenames, surnames, the words a smial's name
 * ends in ("Bag End", "Brandy Hall"), and the notices hobbits pin to their
 * garden gates.
 *
 * <p>The lists are read from the mod jar rather than through the resource
 * manager, as the original did, so worldgen threads can use them at any time.
 */
public final class HobbitNames {
    private HobbitNames() {}

    private static final Map<String, List<String>> BANKS = new ConcurrentHashMap<>();

    /** A married couple sharing a surname, and their home's name. */
    public record Household(String husband, String wife, String surname, String home) {}

    public static Household household(RandomSource random) {
        String surname = pick("hobbit_surname", random);
        String husband = pick("hobbit_male", random);
        String wife = pick("hobbit_female", random);
        String home = pick("hobbit_home", random);
        return new Household(husband + " " + surname, wife + " " + surname, surname, home);
    }

    /** A garden-gate notice, split onto two sign lines where the list marks a {@code #}. */
    public static String[] gateSign(RandomSource random) {
        String[] split = pick("hobbit_sign", random).split("#");
        return new String[]{"", split[0], split.length < 2 ? "" : split[1], ""};
    }

    private static String pick(String bank, RandomSource random) {
        List<String> names = BANKS.computeIfAbsent(bank, HobbitNames::load);
        return names.isEmpty() ? "Unnamed" : names.get(random.nextInt(names.size()));
    }

    private static List<String> load(String bank) {
        String path = "/data/" + LOTRMod.MODID + "/names/" + bank + ".txt";
        try (InputStream in = HobbitNames.class.getResourceAsStream(path)) {
            if (in == null) {
                LOTRMod.LOGGER.error("Missing hobbit name list {}", path);
                return List.of();
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return reader.lines().map(String::strip).filter(line -> !line.isEmpty()).toList();
            }
        } catch (IOException e) {
            LOTRMod.LOGGER.error("Failed to read hobbit name list {}", path, e);
            return List.of();
        }
    }
}
