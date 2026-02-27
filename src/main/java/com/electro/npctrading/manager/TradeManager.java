package com.electro.npctrading.manager;

import com.electro.npctrading.NPCTradingPlugin;
import com.electro.npctrading.model.TradeIngredient;
import com.electro.npctrading.model.TradeOffer;
import com.electro.npctrading.model.Trader;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.UUID;

public class TradeManager {
    private final NPCTradingPlugin plugin;

    public TradeManager(NPCTradingPlugin plugin) {
        this.plugin = plugin;
    }

    public int getItemCount(@Nonnull PlayerRef playerRef, @Nonnull String itemId) {
        var ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) return 0;

        Store<EntityStore> store = ref.getStore();
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return 0;

        Inventory inventory = player.getInventory();
        int count = 0;

        count += countItemInContainer(inventory.getStorage(), itemId);
        count += countItemInContainer(inventory.getBackpack(), itemId);
        count += countItemInContainer(inventory.getHotbar(), itemId);

        return count;
    }

    public boolean canAfford(@Nonnull PlayerRef playerRef, @Nonnull TradeOffer offer) {
        offer.tickRestock();
        if (!offer.isInStock()) return false;

        UUID uuid = playerRef.getUuid();
        VaultManager vault = plugin.getVaultManager();

        for (TradeIngredient input : offer.getInputs()) {
            if (input.getType() == TradeIngredient.Type.ITEM) {
                if (input.getItemId() == null) {
                    return true;
                }

                if (getItemCount(playerRef, input.getItemId()) < input.getQuantity()) {
                    return false;
                }
            } else {
                if (!vault.isEnabled() || !vault.has(uuid, input.getCurrency(), input.getAmount())) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean executeTrade(@Nonnull PlayerRef playerRef, @Nonnull TradeOffer offer, @Nonnull Trader trader) {
        var ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) return false;

        Store<EntityStore> store = ref.getStore();
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return false;

        Inventory inventory = player.getInventory();
        UUID uuid = playerRef.getUuid();
        VaultManager vault = plugin.getVaultManager();

        offer.tickRestock();

        if (!offer.isInStock()) {
            player.sendMessage(Message.raw("This item is out of stock.").color(Color.RED));
            return false;
        }

        // Verify all inputs are available before touching anything
        for (TradeIngredient input : offer.getInputs()) {
            if (input.getType() == TradeIngredient.Type.ITEM) {
                if (getItemCount(playerRef, input.getItemId()) < input.getQuantity()) {
                    String name = plugin.getTradeUI().formatItemName(input.getItemId());
                    player.sendMessage(Message.raw("You do not have enough " + name + " for this trade.").color(Color.RED));
                    return false;
                }
            } else {
                if (!vault.isEnabled()) {
                    player.sendMessage(Message.raw("Economy is not available on this server.").color(Color.RED));
                    return false;
                }
                if (!vault.has(uuid, input.getCurrency(), input.getAmount())) {
                    String currencyLabel = vault.formatCurrency(input.getAmount(), input.getCurrency());
                    player.sendMessage(Message.raw("You need " + currencyLabel + " for this trade.").color(Color.RED));
                    return false;
                }
            }
        }

        // Remove all inputs
        for (TradeIngredient input : offer.getInputs()) {
            if (input.getType() == TradeIngredient.Type.ITEM) {
                int remaining = input.getQuantity();
                remaining = removeItemFromContainer(inventory.getHotbar(), input.getItemId(), remaining);
                if (remaining > 0) remaining = removeItemFromContainer(inventory.getStorage(), input.getItemId(), remaining);
                if (remaining > 0) remaining = removeItemFromContainer(inventory.getBackpack(), input.getItemId(), remaining);
                if (remaining > 0) {
                    // Unexpected shortfall — abort (items already removed, but this shouldn't happen after the check above)
                    player.sendMessage(Message.raw("Trade failed: inventory changed during trade.").color(Color.RED));
                    return false;
                }
            } else {
                if (!vault.withdraw(uuid, input.getCurrency(), input.getAmount())) {
                    player.sendMessage(Message.raw("Trade failed: could not withdraw funds.").color(Color.RED));
                    return false;
                }
            }
        }

        // Give all outputs
        for (TradeIngredient output : offer.getOutputs()) {
            if (output.getType() == TradeIngredient.Type.ITEM) {
                giveItem(inventory, output.getItemId(), output.getQuantity());
            } else {
                vault.deposit(uuid, output.getCurrency(), output.getAmount());
            }
        }

        offer.consumeStock();
        plugin.getTradersManager().saveTrader(trader);

        player.sendMessage(Message.raw("Trade successful!").color(Color.GREEN));
        return true;
    }

    private int removeItemFromContainer(@Nonnull ItemContainer container, @Nonnull String itemId, int quantity) {
        int remaining = quantity;
        int capacity = container.getCapacity();

        for (short i = 0; i < capacity && remaining > 0; i++) {
            ItemStack item = container.getItemStack(i);
            if (item != null && item.isValid() && item.getItemId().equals(itemId)) {
                int stackCount = item.getQuantity();

                if (stackCount <= remaining) {
                    container.removeItemStackFromSlot(i);
                    remaining -= stackCount;
                } else {
                    container.removeItemStackFromSlot(i);
                    container.addItemStack(new ItemStack(itemId, stackCount - remaining));
                    remaining = 0;
                }
            }
        }

        return remaining;
    }

    private void giveItem(@Nonnull Inventory inventory, @Nonnull String itemId, int quantity) {
        int remaining = quantity;

        remaining = addItemToContainer(inventory.getHotbar(), itemId, remaining);
        if (remaining > 0) remaining = addItemToContainer(inventory.getStorage(), itemId, remaining);
        if (remaining > 0) addItemToContainer(inventory.getBackpack(), itemId, remaining);
    }

    private int addItemToContainer(@Nonnull ItemContainer container, @Nonnull String itemId, int quantity) {
        int remaining = quantity;
        int capacity = container.getCapacity();
        int maxStackSize = 100;

        if (Item.getAssetMap().getAsset(itemId) != null) {
            maxStackSize = Item.getAssetMap().getAsset(itemId).getMaxStack();
        }

        // Stack with existing items first
        for (short i = 0; i < capacity && remaining > 0; i++) {
            ItemStack item = container.getItemStack(i);
            if (item != null && item.isValid() && item.getItemId().equals(itemId)) {
                int currentCount = item.getQuantity();
                int canAdd = maxStackSize - currentCount;

                if (canAdd > 0) {
                    int toAdd = Math.min(canAdd, remaining);
                    container.removeItemStackFromSlot(i);
                    container.addItemStack(new ItemStack(itemId, currentCount + toAdd));
                    remaining -= toAdd;
                }
            }
        }

        // Fill empty slots
        for (short i = 0; i < capacity && remaining > 0; i++) {
            ItemStack item = container.getItemStack(i);
            if (item == null || !item.isValid()) {
                int toPlace = Math.min(maxStackSize, remaining);
                container.addItemStack(new ItemStack(itemId, toPlace));
                remaining -= toPlace;
            }
        }

        return remaining;
    }

    private int countItemInContainer(@Nonnull ItemContainer container, @Nonnull String itemId) {
        int count = 0;
        int capacity = container.getCapacity();

        for (short i = 0; i < capacity; i++) {
            ItemStack item = container.getItemStack(i);
            if (item != null && item.isValid() && item.getItemId().equals(itemId)) {
                count += item.getQuantity();
            }
        }

        return count;
    }
}
