package com.feed_the_beast.mods.ftbessentials.util;

import net.minecraft.stats.ServerStatisticsManager;
import net.minecraft.stats.Stats;
import net.minecraft.util.Util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author LatvianModder
 */
public class Leaderboard<N extends Number>
{
	public static final DecimalFormat DECIMAL_FORMAT = Util.make(new DecimalFormat("########0.00"), decimalFormat -> decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT)));

	public static final Map<String, Leaderboard<?>> MAP = new LinkedHashMap<>();

	public static <T extends Number> Leaderboard<T> add(String name, T defaultValue)
	{
		Leaderboard<T> leaderboard = new Leaderboard<>(name, defaultValue);
		MAP.put(name, leaderboard);
		return leaderboard;
	}

	static
	{
		add("deaths", 0)
				.value(stats -> stats.getValue(Stats.CUSTOM.get(Stats.DEATHS)))
		;

		add("time_played", 0)
				.value(stats -> stats.getValue(Stats.CUSTOM.get(Stats.PLAY_ONE_MINUTE)))
				.formatTime()
		;

		add("deaths_per_hour", 0D)
				.value(stats -> {
					int d = stats.getValue(Stats.CUSTOM.get(Stats.DEATHS));
					int t = stats.getValue(Stats.CUSTOM.get(Stats.PLAY_ONE_MINUTE));
					return d <= 0 || t < 72000 ? 0D : (double) d * 72000D / (double) t;
				})
				.string(value -> DECIMAL_FORMAT.format(value.doubleValue()))
		;

		add("player_kills", 0)
				.value(stats -> stats.getValue(Stats.CUSTOM.get(Stats.PLAYER_KILLS)))
		;

		add("mob_kills", 0)
				.value(stats -> stats.getValue(Stats.CUSTOM.get(Stats.MOB_KILLS)))
		;

		add("damage_dealt", 0)
				.value(stats -> stats.getValue(Stats.CUSTOM.get(Stats.DAMAGE_DEALT)))
				.formatDivideByTen()
		;

		add("jumps", 0)
				.value(stats -> stats.getValue(Stats.CUSTOM.get(Stats.JUMP)))
		;

		add("distance_walked", 0)
				.value(stats -> stats.getValue(Stats.CUSTOM.get(Stats.WALK_ONE_CM)))
				.formatDistance()
		;

		add("time_since_death", 0)
				.value(stats -> stats.getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH)))
				.formatTime()
		;
	}

	public final String name;
	public final N defaultValue;
	public Function<ServerStatisticsManager, N> valueGetter;
	public Predicate<N> filter;
	public Function<N, String> stringGetter;

	public Leaderboard(String n, N def)
	{
		name = n;
		defaultValue = def;
		valueGetter = stats -> defaultValue;
		filter = num -> !num.equals(defaultValue);
		stringGetter = num -> NumberFormat.getIntegerInstance(Locale.US).format(num.intValue());
	}

	public Leaderboard<N> value(Function<ServerStatisticsManager, N> v)
	{
		valueGetter = v;
		return this;
	}

	public Leaderboard<N> filter(Predicate<N> f)
	{
		filter = f;
		return this;
	}

	public Leaderboard<N> string(Function<N, String> s)
	{
		stringGetter = s;
		return this;
	}

	public Leaderboard<N> formatDivideByTen()
	{
		return string(value -> DECIMAL_FORMAT.format(value.doubleValue() * 0.1D));
	}

	public Leaderboard<N> formatDistance()
	{
		return string(value -> {
			double d0 = value.doubleValue() / 100.0D;
			double d1 = d0 / 1000.0D;
			if (d1 > 0.5D)
			{
				return DECIMAL_FORMAT.format(d1) + " km";
			}
			else
			{
				return d0 > 0.5D ? DECIMAL_FORMAT.format(d0) + " m" : value + " cm";
			}
		});
	}

	public Leaderboard<N> formatTime()
	{
		return string(value -> {
			double d0 = value.doubleValue() / 20.0D;
			double d1 = d0 / 60.0D;
			double d2 = d1 / 60.0D;
			double d3 = d2 / 24.0D;
			double d4 = d3 / 365.0D;
			if (d4 > 0.5D)
			{
				return DECIMAL_FORMAT.format(d4) + " y";
			}
			else if (d3 > 0.5D)
			{
				return DECIMAL_FORMAT.format(d3) + " d";
			}
			else if (d2 > 0.5D)
			{
				return DECIMAL_FORMAT.format(d2) + " h";
			}
			else
			{
				return d1 > 0.5D ? DECIMAL_FORMAT.format(d1) + " m" : d0 + " s";
			}
		});
	}
}
