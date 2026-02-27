package com.electro.npctrading.model;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TradeOffer {
    private final List<TradeIngredient> inputs;
    private final List<TradeIngredient> outputs;

    private final int maxStock;
    private int currentStock;
    private final int restockAmount;
    private final long restockIntervalMs;
    private long lastRestockTime;

    public TradeOffer(@Nonnull List<TradeIngredient> inputs, @Nonnull List<TradeIngredient> outputs,
                      int maxStock, int currentStock, int restockAmount,
                      long restockIntervalMs, long lastRestockTime) {
        this.inputs = new ArrayList<>(inputs);
        this.outputs = new ArrayList<>(outputs);
        this.maxStock = maxStock;
        this.currentStock = currentStock;
        this.restockAmount = restockAmount;
        this.restockIntervalMs = restockIntervalMs;
        this.lastRestockTime = lastRestockTime;
    }

    public TradeOffer(@Nonnull List<TradeIngredient> inputs, @Nonnull List<TradeIngredient> outputs) {
        this(inputs, outputs, -1, 0, 0, 0L, 0L);
    }

    @Nonnull
    public List<TradeIngredient> getInputs() { return new ArrayList<>(inputs); }

    @Nonnull
    public List<TradeIngredient> getOutputs() { return new ArrayList<>(outputs); }

    public int getMaxStock() { return maxStock; }

    public int getCurrentStock() { return currentStock; }

    public int getRestockAmount() { return restockAmount; }

    public long getRestockIntervalMs() { return restockIntervalMs; }

    public long getLastRestockTime() { return lastRestockTime; }

    public boolean isUnlimitedStock() { return maxStock < 0; }

    public boolean isInStock() {
        return maxStock < 0 || currentStock > 0;
    }

    public boolean consumeStock() {
        if (maxStock < 0) return true;
        if (currentStock <= 0) return false;
        currentStock--;
        return true;
    }

    public void tickRestock() {
        if (maxStock < 0 || restockAmount <= 0 || restockIntervalMs <= 0) return;
        long now = System.currentTimeMillis();
        if (now - lastRestockTime >= restockIntervalMs) {
            currentStock = Math.min(maxStock, currentStock + restockAmount);
            lastRestockTime = now;
        }
    }

    @Nonnull
    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();

        List<Map<String, Object>> inputList = new ArrayList<>();
        for (TradeIngredient i : inputs) inputList.add(i.serialize());
        map.put("inputs", inputList);

        List<Map<String, Object>> outputList = new ArrayList<>();
        for (TradeIngredient o : outputs) outputList.add(o.serialize());
        map.put("outputs", outputList);

        map.put("maxStock", maxStock);
        map.put("currentStock", currentStock);
        map.put("restockAmount", restockAmount);
        map.put("restockIntervalMs", restockIntervalMs);
        map.put("lastRestockTime", lastRestockTime);

        return map;
    }

    @Nonnull
    public static TradeOffer deserialize(@Nonnull Map<?, ?> map) {
        // Backwards compatibility
        if (map.containsKey("inputItem")) {
            String inputItem = (String) map.get("inputItem");
            int inputQty = ((Number) map.get("inputQuantity")).intValue();
            String outputItem = (String) map.get("outputItem");
            int outputQty = ((Number) map.get("outputQuantity")).intValue();
            List<TradeIngredient> inputs = List.of(new TradeIngredient(inputItem, inputQty));
            List<TradeIngredient> outputs = List.of(new TradeIngredient(outputItem, outputQty));
            return new TradeOffer(inputs, outputs);
        }

        // New format
        List<TradeIngredient> inputs = new ArrayList<>();
        Object inputsObj = map.get("inputs");
        if (inputsObj instanceof List<?> inputList) {
            for (Object o : inputList) {
                if (o instanceof Map<?, ?> m) {
                    TradeIngredient ing = TradeIngredient.deserialize(m);
                    if (ing != null) inputs.add(ing);
                }
            }
        }

        List<TradeIngredient> outputs = new ArrayList<>();
        Object outputsObj = map.get("outputs");
        if (outputsObj instanceof List<?> outputList) {
            for (Object o : outputList) {
                if (o instanceof Map<?, ?> m) {
                    TradeIngredient ing = TradeIngredient.deserialize(m);
                    if (ing != null) outputs.add(ing);
                }
            }
        }

        int maxStock = map.containsKey("maxStock") ? ((Number) map.get("maxStock")).intValue() : -1;
        int currentStock = map.containsKey("currentStock") ? ((Number) map.get("currentStock")).intValue() : 0;
        int restockAmount = map.containsKey("restockAmount") ? ((Number) map.get("restockAmount")).intValue() : 0;
        long restockIntervalMs = map.containsKey("restockIntervalMs") ? ((Number) map.get("restockIntervalMs")).longValue() : 0L;
        long lastRestockTime = map.containsKey("lastRestockTime") ? ((Number) map.get("lastRestockTime")).longValue() : 0L;

        return new TradeOffer(inputs, outputs, maxStock, currentStock, restockAmount, restockIntervalMs, lastRestockTime);
    }
}
