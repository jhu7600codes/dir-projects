using System;
using System.IO;
using UnityEngine;

// the cheat menu (CheatActivity, opened from the launcher icon's long-press menu)
// writes into the game's data folder, this applies it when the game starts or comes back
//   ssog_actions.txt   one-shot actions, one per line, deleted once applied
//   ssog_settings.txt  key=value toggles that stay on
public static class SsogCheats
{
	private const int Rich = 999999999;

	private static readonly PowerupType[] TieredPowerups = new PowerupType[]
	{
		PowerupType.jetpack,
		PowerupType.supersneakers,
		PowerupType.coinmagnet,
		PowerupType.doubleMultiplier
	};

	private static bool infiniteGold;

	private static bool hooked;

	public static void Apply()
	{
		try
		{
			string dir = Application.persistentDataPath;
			ReadSettings(Path.Combine(dir, "ssog_settings.txt"));
			if (!hooked)
			{
				hooked = true;
				PlayerInfo instance = PlayerInfo.Instance;
				instance.onCoinsChanged = (Action)Delegate.Combine(instance.onCoinsChanged, new Action(TopUp));
			}
			string actions = Path.Combine(dir, "ssog_actions.txt");
			if (File.Exists(actions))
			{
				string[] lines = ReadLines(actions);
				File.Delete(actions);
				foreach (string line in lines)
				{
					Run(line.Trim());
				}
			}
			TopUp();
			PlayerInfo.Instance.SaveIfDirty();
		}
		catch (Exception ex)
		{
			Debug.LogError("ssog cheats: " + ex);
		}
	}

	private static void ReadSettings(string path)
	{
		infiniteGold = false;
		if (!File.Exists(path))
		{
			return;
		}
		foreach (string line in ReadLines(path))
		{
			string[] kv = line.Trim().Split('=');
			if (kv.Length == 2 && kv[0] == "infinite_gold")
			{
				infiniteGold = kv[1] == "1";
			}
		}
	}

	// unity strips File.ReadAllLines out of its mscorlib
	private static string[] ReadLines(string path)
	{
		using (StreamReader reader = File.OpenText(path))
		{
			return reader.ReadToEnd().Split('\n');
		}
	}

	// runs on every coin change too, so spending just refills
	private static void TopUp()
	{
		if (infiniteGold && PlayerInfo.Instance.amountOfCoins < Rich)
		{
			PlayerInfo.Instance.amountOfCoins = Rich;
		}
	}

	private static void Run(string action)
	{
		try
		{
			PlayerInfo player = PlayerInfo.Instance;
			// "multiplier:23", "highscore:150000", typed in the menu
			string[] kv = action.Split(':');
			if (kv.Length == 2)
			{
				int value = int.Parse(kv[1]);
				if (kv[0] == "multiplier" && value >= 1)
				{
					// the multiplier is just mission set + 1, the setter clamps it to the last set
					Missions.Instance.currentMissionSet = value - 1;
				}
				else if (kv[0] == "highscore")
				{
					player.highestScore = value;
				}
				return;
			}
			switch (action)
			{
			case "gold":
				player.amountOfCoins = Rich;
				break;
			case "characters":
				UnlockCharacters(player);
				break;
			case "upgrades":
				foreach (PowerupType type in TieredPowerups)
				{
					int max = Upgrades.upgrades[type].numberOfTiers - 1;
					while (player.GetCurrentTier(type) < max)
					{
						player.IncreasePowerupTier(type);
					}
				}
				break;
			case "hoverboards":
				player.IncreaseUpgradeAmount(PowerupType.hoverboard, 10);
				break;
			case "headstarts":
				player.IncreaseUpgradeAmount(PowerupType.headstart500, 5);
				player.IncreaseUpgradeAmount(PowerupType.headstart2000, 5);
				break;
			case "boxes":
				player.IncreaseUpgradeAmount(PowerupType.mysterybox, 5);
				break;
			case "missions":
				SkipMissions(player);
				break;
			case "daily":
				SsogDaily.Sync();
				for (char c = player.GetNewDailyLetter(); c != '\0'; c = player.GetNewDailyLetter())
				{
					player.PickedupLetter(c);
				}
				break;
			}
		}
		catch (Exception ex)
		{
			Debug.LogError("ssog cheat " + action + ": " + ex);
		}
	}

	private static void UnlockCharacters(PlayerInfo player)
	{
		foreach (CharacterModels.ModelType model in Enum.GetValues(typeof(CharacterModels.ModelType)))
		{
			CharacterModels.Model data = CharacterModels.modelData[model];
			int missing = data.Price - player.GetCollectedTokens(model);
			if (data.Price > 0 && data.tokenType.Length > 0 && missing > 0)
			{
				player.CollectToken(data.tokenType[0], missing);
			}
		}
	}

	// finishes the 3 missions of the current set, which moves you to the next multiplier
	private static void SkipMissions(PlayerInfo player)
	{
		Missions missions = Missions.Instance;
		int set = player.currentMissionSet;
		for (int i = 0; i < 3; i++)
		{
			if (!missions.HasMoreMissions() || player.currentMissionSet != set)
			{
				break;
			}
			if (!missions.GetMissionInfo(i).complete)
			{
				missions.SkipMission(i);
			}
		}
	}
}
