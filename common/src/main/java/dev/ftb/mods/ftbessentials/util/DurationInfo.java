package dev.ftb.mods.ftbessentials.util;

import dev.ftb.mods.ftblibrary.util.TimeUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Map;
import java.util.function.Supplier;

public record DurationInfo(String desc, long until) {
    private static final DurationInfo INDEFINITE = new DurationInfo("until further notice", -1L);
    private static final Supplier<IllegalArgumentException> INVALID_FORMAT
            = () -> new IllegalArgumentException("Expected format: <number>[smhdw]");

    // map time unit to milliseconds
    private static final Map<Character,Integer> UNIT_MAP = Map.of(
            's', 1_000,
            'm', 60_000,
            'h', 3_600_000,
            'd', 86_400_000,
            'w', 604_800_000
    );

    public static DurationInfo fromString(String durationStr) {
        if (durationStr.isEmpty() || durationStr.startsWith("*")) {
            return INDEFINITE;
        }
        if (durationStr.length() < 2) {
            throw INVALID_FORMAT.get();
        }
        char unit = durationStr.charAt(durationStr.length() - 1);
        String count = durationStr.substring(0, durationStr.length() - 1);
        if (!NumberUtils.isParsable(count) || !UNIT_MAP.containsKey(unit)) {
            throw INVALID_FORMAT.get();
        }
        long duration = Math.max(0, (long) (Double.parseDouble(count) * UNIT_MAP.get(unit)));
        return new DurationInfo("for " + TimeUtils.prettyTimeString(duration / 1000L),System.currentTimeMillis() + duration);
    }

    public static long getSeconds(String durationStr) {
        DurationInfo info = fromString(durationStr.isEmpty() ? "0s" : durationStr);
        return (info.until() - System.currentTimeMillis()) / 1000L;
    }
}
