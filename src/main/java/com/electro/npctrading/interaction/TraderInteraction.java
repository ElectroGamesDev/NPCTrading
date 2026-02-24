package com.electro.npctrading.interaction;

import com.electro.hycitizens.HyCitizensPlugin;
import com.electro.hycitizens.models.CitizenData;
import com.electro.npctrading.NPCTradingPlugin;
import com.electro.npctrading.manager.NPCBindManager;
import com.electro.npctrading.model.Trader;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.awt.*;
import java.util.Collection;

public class TraderInteraction {
    public TraderInteraction(NPCTradingPlugin plugin) {
        PluginIdentifier id = new PluginIdentifier("com.electro", "HyCitizens");
        if (PluginManager.get().getPlugin(id) == null)
            return;

        HyCitizensPlugin.get().getCitizensManager().addCitizenInteractListener(event -> {
            CitizenData citizen = event.getCitizen();
            PlayerRef player = event.getPlayer();
            Ref<EntityStore> ref = player.getReference();

            if (ref == null)
                return;

            // Handing binding/linking
            NPCBindManager.BindingSession bindingSession = plugin.getNPCBindManager().getActiveSession(player.getUuid());
            if (bindingSession != null) {
                bindingSession.trader.addCitizenId(citizen.getId());
                plugin.getTradersManager().saveTrader(bindingSession.trader);
                plugin.getNPCBindManager().completeBinding(player.getUuid());

                citizen.setForceFKeyInteractionText(true);

                Ref<EntityStore> citizenRef = citizen.getNpcRef();
                World world = Universe.get().getWorld(citizen.getWorldUUID());

                if (world != null && citizenRef != null && citizenRef.isValid()) {
                    world.execute(() -> {
                        HyCitizensPlugin.get().getCitizensManager().setInteractionComponent(citizenRef.getStore(), citizenRef, citizen);
                    });
                }

                player.sendMessage(Message.raw("You have successfully linked " + bindingSession.trader.getName() + " to " + citizen.getName() + "!")
                        .color(Color.GREEN));

                return;
            }

            // Handle opening trading menu
            Trader trader = plugin.getTradersManager().getTraderByCitizenId(citizen.getId());
            if (trader != null) {
                plugin.getTradeUI().openTradeGUI(player, ref.getStore(), trader);
            }
        });
    }
}
