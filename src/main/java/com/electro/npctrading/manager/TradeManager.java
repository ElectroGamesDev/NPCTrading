package com.electro.npctrading.manager;

import com.electro.npctrading.NPCTradingPlugin;
import com.electro.npctrading.model.TradeOffer;
import com.electro.npctrading.model.Trader;
import com.electro.npctrading.ui.TradeUI;
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
        return getItemCount(playerRef, offer.inputItem()) >= offer.inputQuantity();
    }

    public boolean executeTrade(@Nonnull PlayerRef playerRef, @Nonnull TradeOffer offer) {
        var ref = playerRef.getReference();
        if (ref == null || !ref.isValid())
            return false;

        Store<EntityStore> store = ref.getStore();
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null)
            return false;

        Inventory inventory = player.getInventory();

        String inputItemName = plugin.getTradeUI().formatItemName(offer.inputItem());
        String outputItemName = plugin.getTradeUI().formatItemName(offer.outputItem());

        // Check if the player has enough input items
        int totalAvailable = getItemCount(playerRef, offer.inputItem());
        if (totalAvailable < offer.inputQuantity()) {
            player.sendMessage(Message.raw("You do not have enough " + inputItemName + "(s) for this trade.").color(Color.RED));
            return false;
        }

        // Remove input items from inventory
        int remaining = offer.inputQuantity();
        remaining = removeItemFromContainer(inventory.getHotbar(), offer.inputItem(), remaining);
        if (remaining > 0) {
            remaining = removeItemFromContainer(inventory.getStorage(), offer.inputItem(), remaining);
        }
        if (remaining > 0) {
            remaining = removeItemFromContainer(inventory.getBackpack(), offer.inputItem(), remaining);
        }

        if (remaining > 0) {
            player.sendMessage(Message.raw("You do not have enough resources for this trade.").color(Color.RED));
            return false;
        }

        // Give output items to the player
        giveItem(inventory, offer.outputItem(), offer.outputQuantity());
        player.sendMessage(Message.raw("You have successfully traded for " + outputItemName + "!").color(Color.GREEN));

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
                    // Remove entire stack
                    container.removeItemStackFromSlot(i);
                    remaining -= stackCount;
                } else {
                    // Reduce stack quantity

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

        // Try to stack with existing items first, then fill empty slots
        remaining = addItemToContainer(inventory.getHotbar(), itemId, remaining);
        if (remaining > 0) {
            remaining = addItemToContainer(inventory.getStorage(), itemId, remaining);
        }
        if (remaining > 0) {
            addItemToContainer(inventory.getBackpack(), itemId, remaining);
        }
    }

    private int addItemToContainer(@Nonnull ItemContainer container, @Nonnull String itemId, int quantity) {
        int remaining = quantity;
        int capacity = container.getCapacity();
        int maxStackSize = 100;

        if (Item.getAssetMap().getAsset(itemId) != null) {
            maxStackSize = Item.getAssetMap().getAsset(itemId).getMaxStack();
        }

        // Try to stack with existing items
        for (short i = 0; i < capacity && remaining > 0; i++) {
            ItemStack item = container.getItemStack(i);
            if (item != null && item.isValid() && item.getItemId().equals(itemId)) {
                int currentCount = item.getQuantity();
                int canAdd = maxStackSize - currentCount;

                if (canAdd > 0) {
                    int toAdd = Math.min(canAdd, remaining);
                    int currentQuantity = item.getQuantity();

                    container.removeItemStackFromSlot(i);
                    container.addItemStack(new ItemStack(itemId, currentQuantity + toAdd));

                    remaining -= toAdd;
                }
            }
        }

        // Place in empty slots
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