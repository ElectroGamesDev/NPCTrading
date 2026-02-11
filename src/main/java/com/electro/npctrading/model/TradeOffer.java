package com.electro.npctrading.model;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;

public record TradeOffer(
        @Nonnull String inputItem,
        int inputQuantity,
        @Nonnull String outputItem,
        int outputQuantity
) {
    public TradeOffer {
        if (inputQuantity <= 0 || outputQuantity <= 0) {
            throw new IllegalArgumentException("Quantities must be positive");
        }
    }

    @Nonnull
    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("inputItem", inputItem);
        map.put("inputQuantity", inputQuantity);
        map.put("outputItem", outputItem);
        map.put("outputQuantity", outputQuantity);
        return map;
    }

    @Nonnull
    public static TradeOffer deserialize(@Nonnull Map<?, ?> map) {
        String inputItem = (String) map.get("inputItem");
        int inputQuantity = ((Number) map.get("inputQuantity")).intValue();
        String outputItem = (String) map.get("outputItem");
        int outputQuantity = ((Number) map.get("outputQuantity")).intValue();

        return new TradeOffer(inputItem, inputQuantity, outputItem, outputQuantity);
    }
}