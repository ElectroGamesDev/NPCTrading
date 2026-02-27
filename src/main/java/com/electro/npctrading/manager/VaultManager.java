package com.electro.npctrading.manager;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import net.cfh.vault.VaultUnlockedServicesManager;
import net.milkbowl.vault2.economy.Economy;
import net.milkbowl.vault2.economy.EconomyResponse;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.UUID;

import static com.hypixel.hytale.logger.HytaleLogger.getLogger;

public class VaultManager {
    private boolean enabled = false;

    public void initialize() {
        PluginIdentifier id = new PluginIdentifier("TheNewEconomy", "VaultUnlocked");
        if (PluginManager.get().getPlugin(id) == null) {
            getLogger().atInfo().log("[NPCTrading] VaultUnlocked not installed, economy support disabled.");
            return;
        }
        enabled = true;
        getLogger().atInfo().log("[NPCTrading] VaultUnlocked detected, economy support enabled.");
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Nullable
    private Economy getEconomy() {
        if (!enabled) {
            return null;
        }

        return VaultUnlockedServicesManager.get().economy().orElse(null);
    }

    public boolean has(UUID playerUUID, @Nullable String currency, double amount) {
        Economy econ = getEconomy();
        if (econ == null) {
            return false;
        }

        try {
            BigDecimal bd = BigDecimal.valueOf(amount);
            if (currency == null || currency.isBlank()) {
                return econ.has("NPCTrading", playerUUID, bd);
            } else {
                PlayerRef player = Universe.get().getPlayer(playerUUID);
                if (player == null || player.getWorldUuid() == null) {
                    return false;
                }

                World world = Universe.get().getWorld(player.getWorldUuid());
                if (world == null) {
                    return false;
                }

                return econ.has("NPCTrading", playerUUID, world.getName(), currency, bd);
            }
        } catch (Exception e) {
            return false;
        }
    }

    public boolean withdraw(UUID playerUUID, @Nullable String currency, double amount) {
        Economy econ = getEconomy();
        if (econ == null) return false;
        try {
            BigDecimal bd = BigDecimal.valueOf(amount);
            EconomyResponse response;
            if (currency == null || currency.isBlank()) {
                response = econ.withdraw("NPCTrading", playerUUID, bd);
            } else {
                response = econ.withdraw("NPCTrading", playerUUID, currency, bd);
            }
            return response != null && response.transactionSuccess();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deposit(UUID playerUUID, @Nullable String currency, double amount) {
        Economy econ = getEconomy();
        if (econ == null) return false;
        try {
            BigDecimal bd = BigDecimal.valueOf(amount);
            EconomyResponse response;
            if (currency == null || currency.isBlank()) {
                response = econ.deposit("NPCTrading", playerUUID, bd);
            } else {
                response = econ.deposit("NPCTrading", playerUUID, currency, bd);
            }
            return response != null && response.transactionSuccess();
        } catch (Exception e) {
            return false;
        }
    }

    public String formatCurrency(double amount, @Nullable String currency) {
        String amountStr = amount == Math.floor(amount)
                ? String.valueOf((long) amount)
                : String.format("%.2f", amount);
        if (currency == null || currency.isBlank()) {
            return amountStr;
        }
        return amountStr + " " + currency;
    }
}
