package com.electro.npctrading.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class Trader {
    private final UUID uuid;
    private String name;
    private final List<TradeOffer> trades;
    private final List<String> citizenIds;

    // Item rotation fields
    private boolean rotateItems;
    private int rotationIntervalMinutes;
    private int displayedItemCount;
    private long lastRotationTime;
    private int currentRotationOffset;

    public Trader(@Nonnull UUID uuid, @Nonnull String name) {
        this.uuid = uuid;
        this.name = name;
        this.trades = new ArrayList<>();
        this.citizenIds = new ArrayList<>();
        this.rotateItems = false;
        this.rotationIntervalMinutes = 5;
        this.displayedItemCount = 5;
        this.lastRotationTime = 0;
        this.currentRotationOffset = 0;
    }

    public Trader(@Nonnull UUID uuid, @Nonnull String name,
                  @Nonnull List<TradeOffer> trades, @Nonnull List<String> citizenIds) {
        this.uuid = uuid;
        this.name = name;
        this.trades = new ArrayList<>(trades);
        this.citizenIds = new ArrayList<>(citizenIds);
        this.rotateItems = false;
        this.rotationIntervalMinutes = 5;
        this.displayedItemCount = 5;
        this.lastRotationTime = 0;
        this.currentRotationOffset = 0;
    }

    @Nonnull
    public UUID getUuid() {
        return uuid;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public void setName(@Nonnull String name) {
        this.name = name;
    }

    @Nonnull
    public List<TradeOffer> getTrades() {
        return new ArrayList<>(trades);
    }

    @Nonnull
    public List<TradeOffer> getDisplayedTrades() {
        if (!rotateItems || trades.size() <= displayedItemCount) {
            return new ArrayList<>(trades);
        }

        // Check if rotation is needed
        long currentTime = System.currentTimeMillis();
        long intervalMs = rotationIntervalMinutes * 60L * 1000L;

        if (currentTime - lastRotationTime >= intervalMs) {
            currentRotationOffset = (currentRotationOffset + displayedItemCount) % trades.size();
            lastRotationTime = currentTime;
        }

        List<TradeOffer> displayed = new ArrayList<>();
        for (int i = 0; i < displayedItemCount && i < trades.size(); i++) {
            int index = (currentRotationOffset + i) % trades.size();
            displayed.add(trades.get(index));
        }

        return displayed;
    }

    public void addTrade(@Nonnull TradeOffer trade) {
        trades.add(trade);
    }

    public void removeTrade(int index) {
        if (index >= 0 && index < trades.size()) {
            trades.remove(index);
        }
    }

    public void clearTrades() {
        trades.clear();
    }

    @Nonnull
    public List<String> getCitizenIds() {
        return new ArrayList<>(citizenIds);
    }

    public void addCitizenId(@Nonnull String citizenId) {
        if (!citizenIds.contains(citizenId)) {
            citizenIds.add(citizenId);
        }
    }

    public void removeCitizenId(@Nonnull String citizenId) {
        citizenIds.remove(citizenId);
    }

    public void clearCitizenIds() {
        citizenIds.clear();
    }

    public boolean hasCitizenId(@Nonnull String citizenId) {
        return citizenIds.contains(citizenId);
    }

    public boolean isRotateItems() {
        return rotateItems;
    }

    public void setRotateItems(boolean rotateItems) {
        this.rotateItems = rotateItems;
    }

    public int getRotationIntervalMinutes() {
        return rotationIntervalMinutes;
    }

    public void setRotationIntervalMinutes(int rotationIntervalMinutes) {
        this.rotationIntervalMinutes = Math.max(1, rotationIntervalMinutes);
    }

    public int getDisplayedItemCount() {
        return displayedItemCount;
    }

    public void setDisplayedItemCount(int displayedItemCount) {
        this.displayedItemCount = Math.max(1, displayedItemCount);
    }

    public long getLastRotationTime() {
        return lastRotationTime;
    }

    public void setLastRotationTime(long lastRotationTime) {
        this.lastRotationTime = lastRotationTime;
    }

    public int getCurrentRotationOffset() {
        return currentRotationOffset;
    }

    public void setCurrentRotationOffset(int currentRotationOffset) {
        this.currentRotationOffset = currentRotationOffset;
    }

    @Nonnull
    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("uuid", uuid.toString());
        map.put("name", name);

        // Serialize trades
        List<Map<String, Object>> tradesList = new ArrayList<>();
        for (TradeOffer trade : trades) {
            tradesList.add(trade.serialize());
        }
        map.put("trades", tradesList);

        // Serialize citizen IDs
        map.put("citizenIds", citizenIds);

        // Serialize rotation settings
        map.put("rotateItems", rotateItems);
        map.put("rotationIntervalMinutes", rotationIntervalMinutes);
        map.put("displayedItemCount", displayedItemCount);
        map.put("lastRotationTime", lastRotationTime);
        map.put("currentRotationOffset", currentRotationOffset);

        return map;
    }

    @Nullable
    public static Trader deserialize(@Nonnull Map<?, ?> map) {
        try {
            UUID uuid = UUID.fromString((String) map.get("uuid"));
            String name = (String) map.get("name");

            // Deserialize trades
            List<TradeOffer> trades = new ArrayList<>();
            Object tradesObj = map.get("trades");

            if (tradesObj instanceof List<?> tradesList) {
                for (Object tradeObj : tradesList) {
                    if (tradeObj instanceof Map<?, ?> tradeMap) {
                        trades.add(TradeOffer.deserialize(tradeMap));
                    }
                }
            }

            // Deserialize citizen IDs
            List<String> citizenIds = new ArrayList<>();
            Object citizenIdsObj = map.get("citizenIds");

            if (citizenIdsObj instanceof List<?> citizenIdsList) {
                for (Object citizenIdObj : citizenIdsList) {
                    if (citizenIdObj instanceof String citizenIdStr) {
                        citizenIds.add(citizenIdStr);
                    }
                }
            }

            Trader trader = new Trader(uuid, name, trades, citizenIds);

            // Deserialize rotation settings
            if (map.containsKey("rotateItems")) {
                trader.setRotateItems((Boolean) map.get("rotateItems"));
            }
            if (map.containsKey("rotationIntervalMinutes")) {
                trader.setRotationIntervalMinutes(((Number) map.get("rotationIntervalMinutes")).intValue());
            }
            if (map.containsKey("displayedItemCount")) {
                trader.setDisplayedItemCount(((Number) map.get("displayedItemCount")).intValue());
            }
            if (map.containsKey("lastRotationTime")) {
                trader.setLastRotationTime(((Number) map.get("lastRotationTime")).longValue());
            }
            if (map.containsKey("currentRotationOffset")) {
                trader.setCurrentRotationOffset(((Number) map.get("currentRotationOffset")).intValue());
            }

            return trader;
        } catch (Exception e) {
            return null;
        }
    }
}