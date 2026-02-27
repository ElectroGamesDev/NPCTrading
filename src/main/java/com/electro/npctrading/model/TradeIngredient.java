package com.electro.npctrading.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

public class TradeIngredient {
    public enum Type { ITEM, CURRENCY }

    private final Type type;
    @Nullable private final String itemId;
    private final int quantity;
    @Nullable private final String currency;
    private final double amount;

    // Item ingredient
    public TradeIngredient(@Nonnull String itemId, int quantity) {
        this.type = Type.ITEM;
        this.itemId = itemId;
        this.quantity = quantity;
        this.currency = null;
        this.amount = 0;
    }

    // Currency ingredient
    public TradeIngredient(@Nullable String currency, double amount) {
        this.type = Type.CURRENCY;
        this.itemId = null;
        this.quantity = 0;
        this.currency = currency;
        this.amount = amount;
    }

    @Nonnull
    public Type getType() { return type; }

    @Nullable
    public String getItemId() { return itemId; }

    public int getQuantity() { return quantity; }

    @Nullable
    public String getCurrency() { return currency; }

    public double getAmount() { return amount; }

    @Nonnull
    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", type.name());
        if (type == Type.ITEM) {
            map.put("itemId", itemId);
            map.put("quantity", quantity);
        } else {
            if (currency != null && !currency.isEmpty()) map.put("currency", currency);
            map.put("amount", amount);
        }
        return map;
    }

    @Nullable
    public static TradeIngredient deserialize(@Nonnull Map<?, ?> map) {
        try {
            Object typeObj = map.get("type");
            Type type = typeObj != null ? Type.valueOf((String) typeObj) : Type.ITEM;
            if (type == Type.ITEM) {
                String itemId = (String) map.get("itemId");
                Object qtyObj = map.get("quantity");
                int quantity = qtyObj instanceof Number ? ((Number) qtyObj).intValue() : 1;
                if (itemId == null) return null;
                return new TradeIngredient(itemId, quantity);
            } else {
                String currency = (String) map.get("currency");
                Object amtObj = map.get("amount");
                double amount = amtObj instanceof Number ? ((Number) amtObj).doubleValue() : 0.0;
                return new TradeIngredient(currency, amount);
            }
        } catch (Exception e) {
            return null;
        }
    }
}
