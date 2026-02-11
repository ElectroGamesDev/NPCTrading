package com.electro.npctrading.manager;

import com.electro.npctrading.model.Trader;
import com.electro.npctrading.util.ConfigManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static com.hypixel.hytale.logger.HytaleLogger.getLogger;

public class TradersManager {
    private final ConfigManager config;
    private final Map<UUID, Trader> traders;

    public TradersManager(@Nonnull ConfigManager config) {
        this.config = config;
        this.traders = new LinkedHashMap<>();
        loadTraders();
    }

    public void loadTraders() {
        traders.clear();

        Map<String, Object> allData = config.getAll();

        config.beginBatch();
        for (String key : allData.keySet()) {
            Object value = config.get(key);

            if (value instanceof Map<?, ?> map) {
                Trader trader = Trader.deserialize(map);
                if (trader != null) {
                    traders.put(trader.getUuid(), trader);
                } else {
                    getLogger().atWarning().log("Failed to deserialize trader at key: " + key);
                }
            }
        }
        config.endBatch();

        getLogger().atInfo().log("Loaded " + traders.size() + " trader(s)");
    }

    public void saveTraders() {
        config.beginBatch();

        // Clear existing traders in config
        Map<String, Object> allData = config.getAll();
        for (String key : allData.keySet()) {
            config.set(key, null);
        }

        // Save all current traders
        for (Trader trader : traders.values()) {
            config.set(trader.getUuid().toString(), trader.serialize());
        }

        config.endBatch();
    }

    @Nonnull
    public Trader createTrader(@Nonnull String name) {
        UUID uuid = UUID.randomUUID();
        Trader trader = new Trader(uuid, name);
        traders.put(uuid, trader);
        saveTrader(trader);
        return trader;
    }

    @Nonnull
    public Trader createTrader(@Nonnull UUID uuid, @Nonnull String name) {
        if (traders.containsKey(uuid)) {
            throw new IllegalArgumentException("Trader with UUID " + uuid + " already exists");
        }
        Trader trader = new Trader(uuid, name);
        traders.put(uuid, trader);
        saveTrader(trader);
        return trader;
    }

    public void saveTrader(@Nonnull Trader trader) {
        traders.put(trader.getUuid(), trader);
        config.set(trader.getUuid().toString(), trader.serialize());
    }

    public boolean deleteTrader(@Nonnull UUID uuid) {
        Trader removed = traders.remove(uuid);
        if (removed != null) {
            config.set(uuid.toString(), null);
            return true;
        }
        return false;
    }

    @Nullable
    public Trader getTrader(@Nonnull UUID uuid) {
        return traders.get(uuid);
    }

    @Nullable
    public Trader getTraderByName(@Nonnull String name) {
        for (Trader trader : traders.values()) {
            if (trader.getName().equalsIgnoreCase(name)) {
                return trader;
            }
        }
        return null;
    }

    @Nullable
    public Trader getTraderByCitizenId(@Nonnull String citizenId) {
        for (Trader trader : traders.values()) {
            if (trader.hasCitizenId(citizenId)) {
                return trader;
            }
        }
        return null;
    }

    @Nonnull
    public Collection<Trader> getAllTraders() {
        return new ArrayList<>(traders.values());
    }

    @Nonnull
    public Set<UUID> getTraderUUIDs() {
        return new HashSet<>(traders.keySet());
    }

    public boolean hasTrader(@Nonnull UUID uuid) {
        return traders.containsKey(uuid);
    }

    public int getTraderCount() {
        return traders.size();
    }

    public void reload() {
        loadTraders();
    }
}