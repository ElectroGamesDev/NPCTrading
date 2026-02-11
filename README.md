[![Join us on Discord](https://img.shields.io/badge/Discord-Join%20Community-7289DA?logo=discord&logoColor=white&style=for-the-badge)](https://discord.gg/Snqz9E58Dr)
[![Star on GitHub](https://img.shields.io/badge/GitHub-Source-181717?logo=github&logoColor=white&style=for-the-badge)](https://github.com/ElectroGamesDev/NPCTrading)
[![Support me on Ko-fi](https://img.shields.io/badge/Ko--fi-Support-FF5E5B?logo=kofi&logoColor=white&style=for-the-badge)](https://ko-fi.com/electrogames)

**NPC Trading** is a full-featured trading system plugin that allows you to create custom NPC traders for your server.  
Set up item exchanges, configure rotating trade offers, and integrate seamlessly with [HyCitizens](https://www.curseforge.com/hytale/mods/hycitizens) to create immersive merchant NPCs.

***

**Commands: /npctrading (or /nt)**  
**Quick Open: /nt {TraderName}**

## Features

### Trading System

Create and manage **Traders** that offer item exchanges to players.

Each Trader supports:

*   Custom **name**
*   Multiple **trade offers**
*   **Item rotation** - Automatically cycle through different trades on a timer
*   Configurable **rotation intervals** and **displayed item count**
*   Integration with **[HyCitizens](https://www.curseforge.com/hytale/mods/hycitizens) NPCs** (optional)
*   Direct trader access via command

### Trade Offers

Each trade offer includes:

*   **Input item** and quantity (what the player pays)
*   **Output item** and quantity (what the player receives)
*   Visual feedback for available/unavailable trades

### Item Rotation

Keep trades fresh and encourage players to check back regularly:

*   Enable/disable rotation per trader
*   Set custom rotation intervals (in minutes)
*   Control how many items display at once
*   Automatic rotation with countdown timer

***

## In-Game Management UI

Use:

/npctrading (or /nt)

This opens the **Trading UI**, where you can:

*   Create new Traders
*   Edit existing Traders
*   Manage trade offers
*   Configure item rotation settings
*   Link traders to [HyCitizens](https://www.curseforge.com/hytale/mods/hycitizens) NPCs
*   Delete traders

No configuration files required to use the plugin.

***

## [HyCitizens](https://www.curseforge.com/hytale/mods/hycitizens) Integration

NPC Trading integrates seamlessly with **[HyCitizens](https://www.curseforge.com/hytale/mods/hycitizens)** (optional dependency):

*   Link traders to any Citizen NPC
*   NPCs are fully customizable through the [HyCitizens](https://www.curseforge.com/hytale/mods/hycitizens) menu (appearance, position, animations, equipment, etc.)
*   Players interact with NPCs to open their trade menu
*   Multiple NPCs can share the same trader
*   Automatic binding system with timeout protection

**Note:** This plugin does not include an NPC spawner. You'll need **HyCitizens** or another NPC plugin to spawn the NPCs.

***

## Quick Access

Don't have an NPC set up yet? Use the quick command:

/nt {TraderName}

This opens the trader's menu directly without needing to interact with an NPC.

***

## Discord / Support

If you would like to join the community, suggest features, report bugs, or need some help, join the Discord community! [https://discord.gg/Snqz9E58Dr](https://discord.gg/Snqz9E58Dr)

***

## Support NPC Trading

Want to support NPC Trading? You can donate at [Ko-fi](https://ko-fi.com/electrogames) or share NPC Trading with your friends!

***

# Developer API

NPC Trading includes a full developer API for creating, editing, and managing traders programmatically.

***

## Getting the plugin instance:

```
NPCTradingPlugin plugin = NPCTradingPlugin.get();
```

TradersManager - The main entry point for all Trader operations:

```
TradersManager manager = plugin.getTradersManager();
```

Trader represents a single trader and all of its settings and trade offers.

## Trader fields:

```
uuid (UUID) – unique trader identifier.
name (String) – displayed trader name.
trades (List<TradeOffer>) – list of all trade offers.
citizenIds (List<String>) – list of linked HyCitizens NPC IDs.
rotateItems (boolean) – whether items rotate on a timer.
rotationIntervalMinutes (int) – minutes between rotations.
displayedItemCount (int) – how many trades show at once when rotating.
lastRotationTime (long) – timestamp of last rotation.
currentRotationOffset (int) – current rotation position.
```

## Creating a Trader

Create a new trader with a random UUID:

```
Trader trader = manager.createTrader("Blacksmith");
```

Create a trader with a specific UUID:

```
UUID uuid = UUID.randomUUID();
Trader trader = manager.createTrader(uuid, "Wandering Merchant");
```

This will add the trader to the registry and save it to storage.

## Managing Trade Offers

TradeOffer represents a single exchange (input → output):

```
TradeOffer offer = new TradeOffer(
    "Resource_Iron_Ore",  // Input item ID
    10,                    // Input quantity
    "Resource_Iron_Ingot", // Output item ID
    5                      // Output quantity
);
```

Add a trade offer to a trader:

```
trader.addTrade(offer);
manager.saveTrader(trader);
```

Remove a trade by index:

```
trader.removeTrade(0); // Removes first trade
manager.saveTrader(trader);
```

Clear all trades:

```
trader.clearTrades();
manager.saveTrader(trader);
```

Get all trades:

```
List<TradeOffer> allTrades = trader.getTrades();
```

Get currently displayed trades (respects rotation):

```
List<TradeOffer> displayedTrades = trader.getDisplayedTrades();
```

## Item Rotation Settings

Enable item rotation:

```
trader.setRotateItems(true);
trader.setRotationIntervalMinutes(5);  // Rotate every 5 minutes
trader.setDisplayedItemCount(3);       // Show 3 trades at a time
manager.saveTrader(trader);
```

Disable rotation:

```
trader.setRotateItems(false);
manager.saveTrader(trader);
```

Get rotation settings:

```
boolean rotating = trader.isRotateItems();
int interval = trader.getRotationIntervalMinutes();
int displayCount = trader.getDisplayedItemCount();
long lastRotation = trader.getLastRotationTime();
int offset = trader.getCurrentRotationOffset();
```

## Linking NPCs ([HyCitizens](https://www.curseforge.com/hytale/mods/hycitizens) Integration)

Add a Citizen NPC to a trader:

```
trader.addCitizenId(citizenId);
manager.saveTrader(trader);
```

Remove a Citizen NPC:

```
trader.removeCitizenId(citizenId);
manager.saveTrader(trader);
```

Check if a Citizen is linked:

```
boolean isLinked = trader.hasCitizenId(citizenId);
```

Get all linked Citizens:

```
List<String> citizenIds = trader.getCitizenIds();
```

Find trader by Citizen ID:

```
Trader trader = manager.getTraderByCitizenId(citizenId);
```

## Updating a Trader

After modifying a trader, save the changes:

```
trader.setName("Master Blacksmith");
trader.setRotationIntervalMinutes(10);
manager.saveTrader(trader);
```

## Deleting a Trader

Remove a trader from the system:

```
boolean deleted = manager.deleteTrader(traderUuid);
```

This will remove the trader from memory and delete the saved data.

## Getting Traders

Get trader by UUID:

```
Trader trader = manager.getTrader(uuid);
```

Get trader by name:

```
Trader trader = manager.getTraderByName("Blacksmith");
```

Get all traders:

```
Collection<Trader> traders = manager.getAllTraders();
```

Get all trader UUIDs:

```
Set<UUID> uuids = manager.getTraderUUIDs();
```

Check if trader exists:

```
boolean exists = manager.hasTrader(uuid);
```

Get trader count:

```
int count = manager.getTraderCount();
```

## Trade Execution API

The TradeManager handles player transactions:

```
TradeManager tradeManager = plugin.getTradeManager();
```

Check if a player can afford a trade:

```
boolean canAfford = tradeManager.canAfford(playerRef, offer);
```

Get item count in player inventory:

```
int count = tradeManager.getItemCount(playerRef, "Resource_Iron_Ore");
```

Execute a trade:

```
boolean success = tradeManager.executeTrade(playerRef, offer);
```

This will:

*   Verify the player has enough input items
*   Remove input items from the player's inventory
*   Add output items to the player's inventory
*   Send confirmation messages
*   Return true if successful, false otherwise

## Opening Trade UI Programmatically

Open a trader's menu for a player:

```
plugin.getTradeUI().openTradeGUI(playerRef, store, trader);
```

## NPC Binding System

The NPCBindManager handles the process of linking traders to NPCs:

```
NPCBindManager bindManager = plugin.getNPCBindManager();
```

Start a binding session (30-second timeout):

```
bindManager.startBinding(playerRef, trader);
```

Get active binding session:

```
NPCBindManager.BindingSession session = bindManager.getActiveSession(playerUuid);
if (session != null) {
    Trader trader = session.trader;
    PlayerRef player = session.playerRef;
}
```

Complete a binding:

```
bindManager.completeBinding(playerUuid);
```

Cancel a binding:

```
bindManager.cancelBinding(playerUuid);
```

## Reload System

Reload all traders from storage:

```
manager.reload();
```

## Credits

This plugin has been made possible by [HyUI](https://www.curseforge.com/hytale/mods/hyui).
