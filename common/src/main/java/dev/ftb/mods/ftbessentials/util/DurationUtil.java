package dev.ftb.mods.ftbessentials.util;

import dev.ftb.mods.ftblibrary.util.TimeUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Map;

public class DurationUtil {
    private static final DurationInfo INDEFINITE = new DurationInfo("until further notice", -1L);

    // map time unit to milliseconds
    private static final Map<Character,Integer> UNIT_MAP = Map.of(
            's', 1_000,
            'm', 60_000,
            'h', 3_600_000,
            'd',	86_400_000,
            'w',	604_800_000
    );

    public static DurationInfo calculateUntil(String durationStr) {
        if (durationStr.isEmpty()) {
            return INDEFINITE;
        }
        if (durationStr.length() < 2) {
            return null;
        }
        char unit = durationStr.charAt(durationStr.length() - 1);
        String count = durationStr.substring(0, durationStr.length() - 1);
        if (!NumberUtils.isParsable(count) || !UNIT_MAP.containsKey(unit)) {
            return null;
        }
        long duration = Math.max(0, (long) (Double.parseDouble(count) * UNIT_MAP.get(unit)));
        return new DurationInfo("for " + TimeUtils.prettyTimeString(duration / 1000L),System.currentTimeMillis() + duration);
    }

    public record DurationInfo(String desc, long until) { }
}
