using System;

// daily challenge without kiloo's server. the word comes from a fixed list,
// picked by the utc date, so it stays the same all day and survives restarts
public static class SsogDaily
{
	private static readonly string[] Words = new string[]
	{
		"SUBWAY", "TRAIN", "SPRAY", "JAKE", "TRICKY", "FRESH", "SPIKE", "YUTANI",
		"COINS", "JETPACK", "MAGNET", "HOVER", "BOARD", "GUARD", "DOG", "TRACKS",
		"METRO", "RUNNER", "TAGGER", "PAINT", "TUNNEL", "RAILS", "JUMP", "ROLL",
		"DASH", "SURF", "CREW", "STREET", "URBAN", "CHASE", "ESCAPE", "SNEAKER",
		"KILOO", "TOKEN", "MISSION", "RECORD", "COMBO", "BOOST", "SCORE", "CITY",
		"STATION", "PLATFORM", "TICKET", "LUCY", "NINJA", "FRANK", "KING", "FRIZZY"
	};

	private static readonly DateTime Epoch = new DateTime(2012, 1, 1, 0, 0, 0, DateTimeKind.Utc);

	public static void Sync()
	{
		DateTime now = DateTime.UtcNow;
		DateTime today = new DateTime(now.Year, now.Month, now.Day, 0, 0, 0, DateTimeKind.Utc);
		int day = (today - Epoch).Days;
		PlayerInfo.Instance.InitDailyWord(Pick(day), today.AddDays(1));
	}

	// every word once per cycle, in a different order each cycle
	private static string Pick(int day)
	{
		int n = Words.Length;
		int[] order = new int[n];
		for (int i = 0; i < n; i++)
		{
			order[i] = i;
		}
		Random random = new Random(day / n * 7919 + 17);
		for (int i = n - 1; i > 0; i--)
		{
			int j = random.Next(0, i + 1);
			int t = order[i];
			order[i] = order[j];
			order[j] = t;
		}
		return Words[order[day % n]];
	}
}
