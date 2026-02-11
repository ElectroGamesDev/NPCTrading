package com.electro.npctrading.commands;

import com.electro.npctrading.NPCTradingPlugin;
import com.electro.npctrading.model.Trader;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

public class MainCommand extends AbstractPlayerCommand{
    private final NPCTradingPlugin plugin;

    public MainCommand(@Nonnull NPCTradingPlugin plugin) {
        super("npctrading", "NPC Trading Commands");
        this.requirePermission("npctrading.admin");
        this.addAliases("nt");
        this.setAllowsExtraArguments(true);
        this.plugin = plugin;
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        String[] args = context.getInputString().split(" ");

        if (args.length > 1) {
            String traderName = args[1].toLowerCase();
            List<String> traderNames = new ArrayList<>();

            Collection<Trader> traders = plugin.getTradersManager().getAllTraders();

            for (Trader trader : traders) {
                String normalizedName = trader.getName().replace(" ", "_");
                traderNames.add(normalizedName);

                normalizedName = normalizedName.toLowerCase();

                if (normalizedName.equals(traderName)) {
                    plugin.getTradeUI().openTradeGUI(playerRef, store, trader);
                    return;
                }
            }

            if (traderNames.isEmpty()) {
                playerRef.sendMessage(Message.raw("There are no traders to trade with.").color(Color.RED));
                return;
            }

            playerRef.sendMessage(Message.raw("A trader with that name could not be found. Available traders: ").color(Color.RED));
            for (String name : traderNames) {
                playerRef.sendMessage(Message.raw(name).color(Color.GREEN));
            }

            return;
        }

        plugin.getTradersUI().openTradersGUI(playerRef, store);
    }

}