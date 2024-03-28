package dev.ftb.mods.ftbessentials.util;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ftb.mods.ftbessentials.commands.impl.misc.LeaderboardCommand;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Leaderboard<N extends Number> {
	private static final DecimalFormat DECIMAL_FORMAT = Util.make(new DecimalFormat("########0.00"), decimalFormat -> decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT)));

	private static final Map<String, Leaderboard<?>> MAP = new LinkedHashMap<>();

	public static <T extends Number> Leaderboard<T> add(String name, T defaultValue) {
		Leaderboard<T> leaderboard = new Leaderboard<>(name, defaultValue);
		MAP.put(name, leaderboard);
		return leaderboard;
	}

	public static LiteralArgumentBuilder<CommandSourceStack> buildCommand() {
		LiteralArgumentBuilder<CommandSourceStack> res = Commands.literal("leaderboard");
		for (Leaderboard<?> leaderboard : MAP.values()) {
			res = res.then(Commands.literal(leaderboard.name)
					.executes(context -> LeaderboardCommand.leaderboard(context.getSource(), leaderboard, false))
			);
		}
		return res;
	}

	static {
		add("deaths", 0)
				.withValueGetter(stats -> stats.getValue(Stats.CUSTOM.get(Stats.DEATHS)))
		;

		add("time_played", 0)
				.withValueGetter(stats -> stats.getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)))
				.formatTime()
		;

		add("deaths_per_hour", 0D)
				.withValueGetter(stats -> {
					int d = stats.getValue(Stats.CUSTOM.get(Stats.DEATHS));
					int t = stats.getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
					return d <= 0 || t < 72000 ? 0D : (double) d * 72000D / (double) t;
				})
				.withStringGetter(value -> DECIMAL_FORMAT.format(value.doubleValue()))
		;

		add("player_kills", 0)
				.withValueGetter(stats -> stats.getValue(Stats.CUSTOM.get(Stats.PLAYER_KILLS)))
		;

		add("mob_kills", 0)
				.withValueGetter(stats -> stats.getValue(Stats.CUSTOM.get(Stats.MOB_KILLS)))
		;

		add("damage_dealt", 0)
				.withValueGetter(stats -> stats.getValue(Stats.CUSTOM.get(Stats.DAMAGE_DEALT)))
				.formatDivideByTen()
		;

		add("jumps", 0)
				.withValueGetter(stats -> stats.getValue(Stats.CUSTOM.get(Stats.JUMP)))
		;

		add("distance_walked", 0)
				.withValueGetter(stats -> stats.getValue(Stats.CUSTOM.get(Stats.WALK_ONE_CM)))
				.formatDistance()
		;

		add("time_since_death", 0)
				.withValueGetter(stats -> stats.getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH)))
				.formatTime()
		;
	}

	private final String name;
	private final N defaultValue;
	private Function<ServerStatsCounter, N> valueGetter;
	private Predicate<N> filter;
	private Function<N, String> stringGetter;

	public Leaderboard(String name, N defaultValue) {
		this.name = name;
		this.defaultValue = defaultValue;

		valueGetter = stats -> this.defaultValue;
		filter = num -> !num.equals(this.defaultValue);
		stringGetter = num -> NumberFormat.getIntegerInstance(Locale.US).format(num.intValue());
	}

	public String getName() {
		return name;
	}

	/**
	 * Remove the underscores from the name and capitalize the first letter of each word
	 * @return the formatted name
	 */
	public String formattedName() {
		var parts = this.name.split("_");
		return Stream.of(parts)
				.map(s -> Character.toTitleCase(s.charAt(0)) + s.substring(1).toLowerCase())
				.collect(Collectors.joining(" "));
	}

	public Leaderboard<N> withValueGetter(Function<ServerStatsCounter, N> v) {
		valueGetter = v;
		return this;
	}

	public Leaderboard<N> withFilter(Predicate<N> f) {
		filter = f;
		return this;
	}

	public Leaderboard<N> withStringGetter(Function<N, String> s) {
		stringGetter = s;
		return this;
	}

	public N getValue(ServerStatsCounter stats) {
		return valueGetter.apply(stats);
	}

	public boolean test(N num) {
		return filter.test(num);
	}

	public String asString(N num) {
		return stringGetter.apply(num);
	}

	public Leaderboard<N> formatDivideByTen() {
		return withStringGetter(value -> DECIMAL_FORMAT.format(value.doubleValue() * 0.1D));
	}

	public Leaderboard<N> formatDistance() {
		return withStringGetter(value -> {
			double d0 = value.doubleValue() / 100.0D;
			double d1 = d0 / 1000.0D;
			if (d1 > 0.5D) {
				return DECIMAL_FORMAT.format(d1) + " km";
			} else {
				return d0 > 0.5D ? DECIMAL_FORMAT.format(d0) + " m" : value + " cm";
			}
		});
	}

	public Leaderboard<N> formatTime() {
		return withStringGetter(value -> {
			double d0 = value.doubleValue() / 20.0D;
			double d1 = d0 / 60.0D;
			double d2 = d1 / 60.0D;
			double d3 = d2 / 24.0D;
			double d4 = d3 / 365.0D;
			if (d4 > 0.5D) {
				return DECIMAL_FORMAT.format(d4) + " y";
			} else if (d3 > 0.5D) {
				return DECIMAL_FORMAT.format(d3) + " d";
			} else if (d2 > 0.5D) {
				return DECIMAL_FORMAT.format(d2) + " h";
			} else {
				return d1 > 0.5D ? DECIMAL_FORMAT.format(d1) + " m" : d0 + " s";
			}
		});
	}
}
