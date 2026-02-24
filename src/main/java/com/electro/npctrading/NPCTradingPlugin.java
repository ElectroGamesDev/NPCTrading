package com.electro.npctrading;

import com.electro.hycitizens.HyCitizensPlugin;
import com.electro.hycitizens.models.CitizenData;
import com.electro.npctrading.commands.MainCommand;
import com.electro.npctrading.interaction.TraderInteraction;
import com.electro.npctrading.manager.NPCBindManager;
import com.electro.npctrading.manager.TradeManager;
import com.electro.npctrading.manager.TradersManager;
import com.electro.npctrading.model.Trader;
import com.electro.npctrading.ui.TradeUI;
import com.electro.npctrading.ui.TradersUI;
import com.electro.npctrading.util.ConfigManager;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.PluginManager;

import javax.annotation.Nonnull;
import java.nio.file.Paths;

public class NPCTradingPlugin extends JavaPlugin {
    private static NPCTradingPlugin instance;
    private ConfigManager configManager;
    private TradersManager tradersManager;
    private NPCBindManager npcBindManager;
    private TradersUI tradersUI;
    private TradeUI tradeUI;
    private TradeManager tradeManager;
    private TraderInteraction traderInteraction;

    public NPCTradingPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        // Initialize managers
        configManager = new ConfigManager(Paths.get("mods", "NPCTradingData"));
        tradersManager = new TradersManager(configManager);
        npcBindManager = new NPCBindManager();
        tradersUI = new TradersUI(this);
        tradeManager = new TradeManager(this);
        tradeUI = new TradeUI(tradeManager);
        traderInteraction = new TraderInteraction(this);

        // Register commands
        getCommandRegistry().registerCommand(new MainCommand(this));
    }

    @Override
    protected void start() {
    }

    @Override
    protected void shutdown() {
    }

    public static NPCTradingPlugin get() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public TradersManager getTradersManager() {
        return tradersManager;
    }

    public NPCBindManager getNPCBindManager() {
        return npcBindManager;
    }

    public TradersUI getTradersUI() {
        return tradersUI;
    }

    public TradeUI getTradeUI() {
        return tradeUI;
    }

    public TradeManager getTradeManager() {
        return tradeManager;
    }
}
