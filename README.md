# RuneLite Death Tracker

A local RuneLite sidebar journal for Old School RuneScape deaths.

## What it records

- Each local-player death, with time, world, tile, region, instance flag, recent combat opponent, skull and Protect Item state.
- The last complete equipment and inventory snapshots before death, including quantities and item prices at capture time.
- A PNG of the next rendered game frame after death (enabled by default).
- Carried item value and an **estimated standard PvM gravestone fee**.
- Actual reclaim fee, unrecovered item value, other costs, reclaim method, and notes. Enter these in **Record actual cost** after reclaiming. Explicit in-game chat messages such as “You paid 42,000 coins to reclaim your items…” also fill the reclaim fee automatically.
- A collapsed, expandable death list, lifetime sum of recorded actual costs, and CSV export.

## Accuracy

The estimated fee uses current RuneLite item prices and the standard gravestone price tiers. It approximates which items are protected. Death rules vary by activity, PvP, account mode, item type, and discounts, so the game's death interface is authoritative. Actual cost is only filled automatically when a game message explicitly confirms a reclaim payment; use the edit dialog for other payments and item losses. The total counts recorded actual values only.

## Data

Data stays on your computer under `~/.runelite/plugin-data/death-tracker/<character>/`. `deaths.json` is the journal, PNGs are screenshots, and **Export CSV** writes `deaths.csv`. The plugin does not upload data.

## Use it locally now

On this Windows computer, double-click `run-dev.bat`. It selects the installed JDK and launches a development RuneLite client with Death Tracker loaded. Open the **Death Tracker** skull icon in the RuneLite sidebar. Keep the command window open while playing; closing it stops that client. This is a separate RuneLite launch, not an installation into the regular launcher.

If you sign in with a Jagex Account, RuneLite's [development login guide](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) explains the extra one-time setup. Treat the generated credentials file as sensitive.

## Install through normal RuneLite

To make Death Tracker appear in RuneLite's Plugin Hub for ordinary installation:

1. Test the plugin in the development client, including a real death and reclaim. Deaths can be expensive, so use a safe activity or low-value gear for the first test.
2. Put this project in your own public GitHub repository and commit it. Change the `author` field in `runelite-plugin.properties` to your preferred name. Add a license.
3. Fork [runelite/plugin-hub](https://github.com/runelite/plugin-hub). Add a file such as `plugins/death-tracker` containing the repository URL and the exact commit hash, then open a pull request. Follow the [Plugin Hub submission instructions](https://github.com/runelite/plugin-hub#submitting-a-plugin).
4. Once RuneLite reviews and merges it, search for **Death Tracker** in the Plugin Hub and click **Install**.

The project cannot be installed from the public Plugin Hub until that review is complete.

## Develop and build

Use JDK 11 or newer. On Windows:

```powershell
.\gradlew.bat test
.\gradlew.bat run
```

The `run` task launches a development RuneLite client with the plugin loaded. The project follows the [RuneLite example plugin](https://github.com/runelite/example-plugin) layout. To distribute through the [Plugin Hub](https://github.com/runelite/plugin-hub), submit the repository to its review process.
