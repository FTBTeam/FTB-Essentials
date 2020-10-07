package com.feed_the_beast.mods.ftbessentials.util;

import net.minecraft.stats.IStatFormatter;
import net.minecraft.stats.ServerStatisticsManager;
import net.minecraft.stats.Stats;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author LatvianModder
 */
public class Leaderboard<N extends Number>
{
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
				.string(IStatFormatter.TIME::format)
		;

		add("deaths_per_hour", 0D)
				.value(stats -> {
					int d = stats.getValue(Stats.CUSTOM.get(Stats.DEATHS));
					int t = stats.getValue(Stats.CUSTOM.get(Stats.PLAY_ONE_MINUTE));
					return d <= 0 || t < 72000 ? 0D : (double) d * 72000D / (double) t;
				})
				.string(IStatFormatter.DECIMAL_FORMAT::format)
		;

		add("player_kills", 0)
				.value(stats -> stats.getValue(Stats.CUSTOM.get(Stats.PLAYER_KILLS)))
		;

		add("mob_kills", 0)
				.value(stats -> stats.getValue(Stats.CUSTOM.get(Stats.MOB_KILLS)))
		;

		add("damage_dealt", 0)
				.value(stats -> stats.getValue(Stats.CUSTOM.get(Stats.DAMAGE_DEALT)))
				.string(IStatFormatter.DIVIDE_BY_TEN::format)
		;

		add("jumps", 0)
				.value(stats -> stats.getValue(Stats.CUSTOM.get(Stats.JUMP)))
		;

		add("distance_walked", 0)
				.value(stats -> stats.getValue(Stats.CUSTOM.get(Stats.WALK_ONE_CM)))
				.string(IStatFormatter.DISTANCE::format)
		;

		add("time_since_death", 0)
				.value(stats -> stats.getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH)))
				.string(IStatFormatter.TIME::format)
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
		stringGetter = num -> IStatFormatter.DEFAULT.format(num.intValue());
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
}
