package com.electro.npctrading.ui;

import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.html.TemplateProcessor;
import com.electro.hycitizens.HyCitizensPlugin;
import com.electro.hycitizens.models.CitizenData;
import com.electro.npctrading.NPCTradingPlugin;
import com.electro.npctrading.model.Trader;
import com.electro.npctrading.model.TradeOffer;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Store;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TradersUI {
    private final NPCTradingPlugin plugin;

    // Session state per player to preserve data across UI transitions
    private static final Map<UUID, EditTraderState> editStates = new ConcurrentHashMap<>();

    public TradersUI(@Nonnull NPCTradingPlugin plugin) {
        this.plugin = plugin;
    }

    // Session State

    private static class EditTraderState {
        String name = "";
        List<TradePair> trades = new ArrayList<>();
        boolean rotateItems = false;
        int rotationInterval = 5;
        int displayCount = 5;
        @Nullable UUID traderUuid;

        // Item selection callback context
        int editingTradeIndex = -1;
        boolean editingInput = true; // true = input side, false = output side
    }

    private static class TradePair {
        String inputItemId;
        int inputQuantity;
        String outputItemId;
        int outputQuantity;

        TradePair() {
            this.inputItemId = null;
            this.inputQuantity = 1;
            this.outputItemId = null;
            this.outputQuantity = 1;
        }

        TradePair(String inputItemId, int inputQuantity, String outputItemId, int outputQuantity) {
            this.inputItemId = inputItemId;
            this.inputQuantity = inputQuantity;
            this.outputItemId = outputItemId;
            this.outputQuantity = outputQuantity;
        }

        boolean isComplete() {
            return inputItemId != null && outputItemId != null;
        }
    }

    private EditTraderState getOrCreateState(PlayerRef playerRef, @Nullable Trader trader) {
        UUID playerId = playerRef.getUuid();
        EditTraderState state = editStates.get(playerId);
        if (state == null) {
            state = new EditTraderState();
            if (trader != null) {
                state.traderUuid = trader.getUuid();
                state.name = trader.getName();
                state.rotateItems = trader.isRotateItems();
                state.rotationInterval = trader.getRotationIntervalMinutes();
                state.displayCount = trader.getDisplayedItemCount();
                for (TradeOffer offer : trader.getTrades()) {
                    state.trades.add(new TradePair(
                            offer.inputItem(), offer.inputQuantity(),
                            offer.outputItem(), offer.outputQuantity()
                    ));
                }
            }
            editStates.put(playerId, state);
        }
        return state;
    }

    private void clearState(PlayerRef playerRef) {
        editStates.remove(playerRef.getUuid());
    }

    // Shared Styles
    private String getSharedStyles() {
        return """
                <style>
                    .main-container {
                        layout: top;
                        background-color: #0d1117(0.98);
                        border-radius: 12;
                    }
                
                    .header {
                        layout: center;
                        flex-weight: 0;
                        background-color: #161b22;
                        padding: 20;
                        border-radius: 12 12 0 0;
                    }
                
                    .header-content {
                        layout: top;
                        flex-weight: 0;
                    }
                
                    .header-title {
                        color: #e6edf3;
                        font-size: 24;
                        font-weight: bold;
                        text-align: center;
                    }
                
                    .header-subtitle {
                        color: #8b949e;
                        font-size: 12;
                        padding-top: 4;
                        text-align: center;
                    }
                
                    .body {
                        layout: top;
                        flex-weight: 1;
                        padding: 20;
                    }
                
                    .footer {
                        layout: center;
                        flex-weight: 0;
                        background-color: #161b22;
                        padding: 16;
                        border-radius: 0 0 12 12;
                    }
                
                    .card {
                        layout: top;
                        flex-weight: 0;
                        background-color: #161b22;
                        padding: 16;
                        border-radius: 8;
                    }
                
                    .section {
                        layout: top;
                        flex-weight: 0;
                        background-color: #21262d(0.5);
                        padding: 16;
                        border-radius: 8;
                    }
                
                    .section-title {
                        color: #e6edf3;
                        font-size: 13;
                        font-weight: bold;
                        text-align: center;
                    }
                
                    .section-description {
                        color: #8b949e;
                        font-size: 12;
                        padding-top: 4;
                        padding-bottom: 12;
                        text-align: center;
                    }
                
                    .form-group {
                        layout: top;
                        flex-weight: 0;
                        padding-bottom: 6;
                    }
                
                    .form-row {
                        layout: center;
                        flex-weight: 0;
                    }
                
                    .form-label {
                        color: #e6edf3;
                        font-size: 12;
                        font-weight: bold;
                        padding-bottom: 6;
                        text-align: center;
                    }
                
                    .form-input {
                        flex-weight: 0;
                        anchor-height: 38;
                        background-color: #21262d;
                        border-radius: 6;
                    }
                
                    .form-hint {
                        color: #6e7681;
                        font-size: 12;
                        padding-top: 4;
                        text-align: center;
                    }
                
                    .checkbox-row {
                        layout: center;
                        flex-weight: 0;
                        padding-top: 8;
                        padding-bottom: 4;
                    }
                
                    .checkbox-label {
                        color: #e6edf3;
                        font-size: 12;
                        padding-left: -30;
                    }
                
                    .btn-row {
                        layout: center;
                        flex-weight: 0;
                    }
                
                    .btn-primary {
                        flex-weight: 0;
                        anchor-height: 40;
                        anchor-width: 140;
                        border-radius: 6;
                    }
                
                    .btn-secondary {
                        flex-weight: 0;
                        anchor-height: 40;
                        anchor-width: 140;
                        border-radius: 6;
                    }
                
                    .btn-danger {
                        flex-weight: 0;
                        anchor-height: 40;
                        anchor-width: 140;
                        border-radius: 6;
                    }
                
                    .btn-ghost {
                        flex-weight: 0;
                        anchor-height: 40;
                        anchor-width: 140;
                        border-radius: 6;
                    }
                
                    .btn-small {
                        anchor-height: 32;
                        anchor-width: 100;
                    }
                
                    .list-container {
                        layout-mode: TopScrolling;
                        flex-weight: 1;
                        padding: 4 16 4 16;
                    }
                
                    .list-item {
                        layout: left;
                        flex-weight: 0;
                        background-color: #21262d;
                        padding: 14;
                        border-radius: 8;
                    }
                
                    .list-item-content {
                        layout: top;
                        flex-weight: 1;
                        padding-left: 12;
                        padding-right: 12;
                    }
                
                    .list-item-title {
                        color: #e6edf3;
                        font-size: 14;
                        font-weight: bold;
                    }
                
                    .list-item-subtitle {
                        color: #8b949e;
                        font-size: 12;
                        padding-top: 2;
                    }
                
                    .list-item-actions {
                        layout: left;
                        flex-weight: 0;
                    }
                
                    .empty-state {
                        layout: center;
                        flex-weight: 1;
                        padding: 40;
                    }
                
                    .empty-state-content {
                        layout: top;
                        flex-weight: 0;
                    }
                
                    .empty-state-title {
                        color: #8b949e;
                        font-size: 16;
                        text-align: center;
                        padding-top: 16;
                    }
                
                    .empty-state-description {
                        color: #6e7681;
                        font-size: 12;
                        text-align: center;
                        padding-top: 8;
                    }
                
                    .divider {
                        flex-weight: 0;
                        anchor-height: 1;
                        background-color: #30363d;
                    }
                
                    .spacer-sm {
                        flex-weight: 0;
                        anchor-height: 8;
                    }
                
                    .spacer-md {
                        flex-weight: 0;
                        anchor-height: 16;
                    }
                
                    .spacer-h-sm {
                        flex-weight: 0;
                        anchor-width: 8;
                    }
                
                    .trade-row {
                        layout: center;
                        flex-weight: 0;
                        background-color: #21262d;
                        padding: 10;
                        border-radius: 6;
                    }
                
                    .trade-index {
                        color: #6e7681;
                        font-size: 11;
                        font-weight: bold;
                        anchor-width: 30;
                        text-align: center;
                    }
                
                    .trade-arrow {
                        color: #58a6ff;
                        font-size: 18;
                        font-weight: bold;
                        padding-left: 12;
                        padding-right: 12;
                    }
                
                    .item-slot-display {
                        layout: center;
                        flex-weight: 0;
                        anchor-width: 52;
                        anchor-height: 52;
                        background-color: #161b22;
                        border-radius: 6;
                    }

                    .item-slot-display-empty {
                        background-color: #161b22(0.5);
                    }

                    .slot-edit-btn {
                        flex-weight: 0;
                        anchor-width: 52;
                        anchor-height: 22;
                        border-radius: 4;
                    }
                
                    .slot-icon {
                        anchor-width: 36;
                        anchor-height: 36;
                    }
                
                    .slot-label {
                        color: #8b949e;
                        font-size: 10;
                        text-align: center;
                        padding-top: 2;
                    }
                
                    .slot-label-filled {
                        color: #e6edf3;
                    }
                
                    .slot-container {
                        layout: top;
                        flex-weight: 0;
                        anchor-width: 64;
                        anchor-height: 72;
                        background-color: #12161a;
                        border-radius: 6;
                        padding: 4;
                        align-items: center;
                    }
                    
                    .slot-background {
                        layout: top;
                        flex-weight: 0;
                        anchor-width: 64;
                        anchor-height: 72;
                        background-color: #0B0F14;
                    }
                    
                    .slot-background-big {
                        layout: top;
                        flex-weight: 0;
                        anchor-width: 90;
                        anchor-height: 90;
                        background-color: #535359;
                    }
                
                    .slot-inner {
                        layout: center;
                        flex-weight: 0;
                    }
                
                    .search-container {
                        layout: top;
                        flex-weight: 0;
                        padding: 0 16 8 16;
                    }
                
                    .search-input {
                        flex-weight: 0;
                        anchor-height: 40;
                        background-color: #21262d;
                        border-radius: 6;
                    }
                
                    .items-grid {
                        layout-mode: TopScrolling;
                        flex-weight: 1;
                        padding: 4 16 4 16;
                    }
                
                    .items-grid-row {
                        layout: center;
                        flex-weight: 0;
                    }
                
                    .item-pick-btn {
                        layout: top;
                        flex-weight: 0;
                        anchor-width: 80;
                        anchor-height: 80;
                        background-color: #21262d;
                        border-radius: 6;
                        padding: 6;
                    }
                
                    .s {
                        layout: top;
                        flex-weight: 0;
                        anchor-width: 80;
                        background-color: #21262d;
                        border-radius: 6;
                        padding: 6;
                    }

                    .item-pick-icon-container {
                        layout: center;
                        flex-weight: 0;
                        anchor-height: 80;
                        anchor-width: 80;
                    }
                
                    .item-pick-name {
                        color: #e6edf3;
                        font-size: 9;
                        text-align: center;
                        padding-top: 4;
                    }
                
                    .item-pick-spacer {
                        flex-weight: 0;
                        anchor-width: 6;
                    }
                
                    .item-pick-row-spacer {
                        flex-weight: 0;
                        anchor-height: 6;
                    }
                
                    .qty-section {
                        layout: top;
                        flex-weight: 0;
                        background-color: #21262d(0.5);
                        padding: 24;
                        border-radius: 8;
                    }
                
                    .qty-item-display {
                        layout: center;
                        flex-weight: 0;
                        padding-bottom: 16;
                    }
                
                    .qty-item-icon {
                        anchor-width: 48;
                        anchor-height: 48;
                    }
                
                    .qty-item-name {
                        color: #e6edf3;
                        font-size: 16;
                        font-weight: bold;
                        padding-left: 12;
                    }
                </style>
                """;
    }

    private TemplateProcessor createBaseTemplate() {
        return new TemplateProcessor()
                .registerComponent("formField", """
                        <div class="form-group">
                            <p class="form-label">{{$label}}</p>
                            <input type="text" id="{{$id}}" class="form-input" value="{{$value}}" 
                                   placeholder="{{$placeholder}}" maxlength="{{$maxlength|64}}" />
                            {{#if hint}}
                            <p class="form-hint">{{$hint}}</p>
                            {{/if}}
                        </div>
                        """)
                .registerComponent("numberField", """
                        <div class="form-group">
                            <p class="form-label">{{$label}}</p>
                            <input type="number" id="{{$id}}" class="form-input" 
                                   value="{{$value}}"
                                   placeholder="{{$placeholder}}"
                                   min="{{$min}}"
                                   max="{{$max}}"
                                   step="{{$step}}"
                                   data-hyui-max-decimal-places="{{$decimals|0}}" />
                            {{#if hint}}
                            <p class="form-hint">{{$hint}}</p>
                            {{/if}}
                        </div>
                        """);
    }

    private static class TraderDisplay {
        private final Trader trader;

        public TraderDisplay(Trader trader) {
            this.trader = trader;
        }

        public String getName() { return trader.getName(); }
        public UUID getUuid() { return trader.getUuid(); }
        public int getTradeCount() { return trader.getTrades().size(); }
        public int getCitizenCount() { return trader.getCitizenIds().size(); }
    }

    // Traders List GUI
    public void openTradersGUI(@Nonnull PlayerRef playerRef, @Nonnull Store<EntityStore> store) {
        Collection<Trader> traders = plugin.getTradersManager().getAllTraders();

        List<TraderDisplay> displayTraders = traders.stream()
                .map(TraderDisplay::new)
                .collect(Collectors.toList());

        TemplateProcessor template = createBaseTemplate()
                .setVariable("traders", displayTraders)
                .setVariable("hasTraders", !traders.isEmpty())
                .setVariable("traderCount", traders.size());

        String html = template.process(getSharedStyles() + """
                <div class="page-overlay">
                    <div class="main-container" style="anchor-width: 900; anchor-height: 700;">
                
                        <!-- Header -->
                        <div class="header">
                            <div class="header-content">
                                <p class="header-title">NPC Trading</p>
                                <p class="header-subtitle">Manage trading shops and link them to NPCs</p>
                            </div>
                        </div>
                
                        <!-- Body -->
                        <div class="body">
                        
                            <div class="spacer-md"></div>
                
                            <!-- Create Button -->
                            <div class="btn-row">
                                <button id="create-trader-btn" class="btn-primary" style="anchor-width: 260;">Create New Trader</button>
                            </div>
                
                            <div class="spacer-md"></div>
                
                            <!-- Traders List -->
                            {{#if hasTraders}}
                            <div class="list-container" style="anchor-height: 520;">
                                {{#each traders}}
                                <div class="list-item">
                                    <div class="list-item-content">
                                        <p class="list-item-title">{{$name}}</p>
                                        <p class="list-item-subtitle">{{$tradeCount}} trade(s) | {{$citizenCount}} NPC(s) linked</p>
                                    </div>
                                    <div class="list-item-actions">
                                        <button id="edit-{{$uuid}}" class="btn-secondary btn-small">Edit</button>
                                        <div class="spacer-h-sm"></div>
                                        <button id="delete-{{$uuid}}" class="btn-danger btn-small">Delete</button>
                                    </div>
                                </div>
                                <div class="spacer-sm"></div>
                                {{/each}}
                            </div>
                            {{else}}
                            <div class="empty-state">
                                <div class="empty-state-content">
                                    <p class="empty-state-title">No Traders Yet</p>
                                    <p class="empty-state-description">Click "Create New Trader" to add your first trading shop!</p>
                                </div>
                            </div>
                            {{/if}}
                
                        </div>
                
                    </div>
                </div>
                """);

        PageBuilder page = PageBuilder.pageForPlayer(playerRef)
                .withLifetime(CustomPageLifetime.CanDismiss)
                .fromHtml(html);

        setupTradersListeners(page, playerRef, store, traders);

        page.open(store);
    }

    private void setupTradersListeners(PageBuilder page, PlayerRef playerRef, Store<EntityStore> store,
                                       Collection<Trader> traders) {
        page.addEventListener("create-trader-btn", CustomUIEventBindingType.Activating, event -> {
            clearState(playerRef);
            openEditTraderGUI(playerRef, store, null);
        });

        for (Trader trader : traders) {
            String uuidStr = trader.getUuid().toString();

            page.addEventListener("edit-" + uuidStr, CustomUIEventBindingType.Activating, event -> {
                clearState(playerRef);
                openEditTraderGUI(playerRef, store, trader);
            });

            page.addEventListener("delete-" + uuidStr, CustomUIEventBindingType.Activating, event -> {
                if (!trader.getCitizenIds().isEmpty()) {
                    openDeleteConfirmationGUI(playerRef, store, trader);
                } else {
                    plugin.getTradersManager().deleteTrader(trader.getUuid());
                    playerRef.sendMessage(Message.raw("Trader '" + trader.getName() + "' deleted!").color(Color.GREEN));
                    openTradersGUI(playerRef, store);
                }
            });
        }
    }

    // Edit Trader GUI
    public void openEditTraderGUI(@Nonnull PlayerRef playerRef, @Nonnull Store<EntityStore> store, @Nullable Trader trader) {
        EditTraderState state = getOrCreateState(playerRef, trader);
        boolean isEditing = state.traderUuid != null;
        int citizenCount = 0;
        if (isEditing && trader != null) {
            citizenCount = trader.getCitizenIds().size();
        } else if (isEditing) {
            Trader existing = plugin.getTradersManager().getTrader(state.traderUuid);
            if (existing != null) citizenCount = existing.getCitizenIds().size();
        }

        // Build trades HTML directly as a string
        StringBuilder tradesHtml = new StringBuilder();
        for (int i = 0; i < state.trades.size(); i++) {
            TradePair pair = state.trades.get(i);

            String inputContent = pair.inputItemId != null
                    ? "<span class=\"slot-icon item-icon\" data-hyui-item-id=\"" + pair.inputItemId + "\"></span>"
                    : "<span class=\"slot-icon item-icon\" data-hyui-item-id=\"\"></span>";

            String outputContent = pair.outputItemId != null
                    ? "<span class=\"slot-icon item-icon\" data-hyui-item-id=\"" + pair.outputItemId + "\"></span>"
                    : "<span class=\"slot-icon item-icon\" data-hyui-item-id=\"\"></span>";

            String inputLabel = pair.inputItemId != null
                    ? "<p class=\"slot-label slot-label-filled\">" + pair.inputQuantity + "x</p>"
                    : "";

            String outputLabel = pair.outputItemId != null
                    ? "<p class=\"slot-label slot-label-filled\">" + pair.outputQuantity + "x</p>"
                    : "";

            tradesHtml.append("""
                <div class="trade-row">
                    <p class="trade-index">#%d</p>
                    <div class="spacer-h-sm"></div>
                    <div class="slot-background">
                        <button id="input-%d" class="slot-container" style="layout: top; flex-direction: column;">
                            %s
                            %s
                        </button>
                    </div>
                    <div class="spacer-h-sm"></div>
                    <p class="trade-arrow">-></p>
                    <div class="spacer-h-sm"></div>
                    <div class="slot-background">
                        <button id="output-%d" class="slot-container" style="layout: top; flex-direction: column;">
                            %s
                            %s
                        </button>
                    </div>
                    <div style="flex-weight: 0; anchor-width: 24;"></div>
                    <button id="delete-trade-%d" class="btn-danger btn-small">Delete</button>
                </div>
                <div class="spacer-sm"></div>
                """.formatted(
                    i + 1,
                    i, inputContent, inputLabel,
                    i, outputContent, outputLabel,
                    i
                ));
        }

        String tradesContent;
        if (state.trades.isEmpty()) {
            tradesContent = """
                    <div style="layout: center; flex-weight: 0; padding: 20;">
                        <p style="color: #6e7681; font-size: 12; text-align: center;">No trades yet. Click "Add Trade" below.</p>
                    </div>
                    """;
        } else {
            tradesContent = tradesHtml.toString();
        }

        TemplateProcessor template = createBaseTemplate()
                .setVariable("isEditing", isEditing)
                .setVariable("name", state.name)
                .setVariable("tradeCount", state.trades.size())
                .setVariable("rotateItems", state.rotateItems)
                .setVariable("rotationInterval", state.rotationInterval)
                .setVariable("displayCount", state.displayCount)
                .setVariable("citizenCount", citizenCount);

        // Concatenate trades directly into the HTML rather than using a template variable
        String html = template.process(getSharedStyles() + """
        <div class="page-overlay">
            <div class="main-container" style="anchor-width: 900; anchor-height: 900;">
        
                <!-- Header -->
                <div class="header">
                    <div class="header-content">
                        <p class="header-title">{{#if isEditing}}Edit Trader{{else}}Create Trader{{/if}}</p>
                        <p class="header-subtitle">Configure trades and NPC bindings</p>
                    </div>
                </div>
        
                <!-- Body -->
                <div class="body" style="layout-mode: TopScrolling;">
        
                    <!-- Basic Info Section -->
                    <div class="section">
                        <p class="section-title">Basic Information</p>
                        <p class="section-description">Set the trader's display name</p>
                        
                        <div style="layout: center; flex-weight: 0;">
                            <div style="anchor-width: 300; flex-weight: 0;">
                                {{@formField:id=trader-name,label=Trader Name,value={{$name}},placeholder=Enter a name,maxlength=32,hint=This will be displayed in trade UIs}}
                            </div>
                        </div>
                    </div>
        
                    <div class="spacer-md"></div>
        
                    <!-- Trades Section -->
                    <div class="section">
                        <p class="section-title">Trades ({{$tradeCount}})</p>
                        <p class="section-description">Click an item slot to choose an item. The first item slot is the cost, the second item slot is the received item.</p>
                        
                        <div class="spacer-sm"></div>
                        
                        """ + tradesContent + """
                        
                        <div class="spacer-sm"></div>
                        
                        <div class="btn-row">
                            <button id="add-trade-btn" class="btn-secondary" style="anchor-width: 180;">Add Trade</button>
                        </div>
                    </div>
        
                    <div class="spacer-md"></div>
        
                    <!-- Item Rotation Section -->
                    <div class="section">
                        <p class="section-title">Item Rotation</p>
                        <p class="section-description">Automatically rotate which items are displayed</p>
                        
                        <div class="checkbox-row">
                            <input type="checkbox" id="rotate-items-check" {{#if rotateItems}}checked{{/if}} />
                            <div style="layout: top; flex-weight: 0; text-align: center;">
                                <p class="checkbox-label">Enable Item Rotation</p>
                            </div>
                        </div>
                        
                        <div class="spacer-sm"></div>
                        <div class="form-row">
                            <div style="anchor-width: 250; flex-weight: 0;">
                                {{@numberField:id=rotation-interval,label=Rotation Interval (minutes),value={{$rotationInterval}},placeholder=5,min=1,max=1440,step=1,decimals=0,hint=How often to rotate items}}
                            </div>
                            <div class="spacer-h-sm"></div>
                            <div style="anchor-width: 250; flex-weight: 0;">
                                {{@numberField:id=display-count,label=Items to Display,value={{$displayCount}},placeholder=5,min=1,max=27,step=1,decimals=0,hint=How many items to show at once}}
                            </div>
                        </div>
                    </div>
        
                    <div class="spacer-md"></div>
        
                    <!-- NPC Management Section -->
                    {{#if isEditing}}
                    <div class="section">
                        <p class="section-title">NPC Management</p>
                        <p class="section-description">Link NPCs to this trader ({{$citizenCount}} linked)</p>
                        
                        <div class="form-row">
                            <button id="spawn-npc-btn" class="btn-primary" style="anchor-width: 180;">Spawn NPC</button>
                            <div class="spacer-h-sm"></div>
                            <button id="bind-npc-btn" class="btn-secondary" style="anchor-width: 180;">Link NPC</button>
                        </div>
                        
                        <div class="spacer-sm"></div>
                        <p class="form-hint">Spawn creates a new NPC, Link lets you link an existing NPC</p>
                        <div class="spacer-sm"></div>
                    </div>
                    {{/if}}
        
                </div>
        
                <!-- Footer -->
                <div class="footer">
                    <button id="cancel-btn" class="btn-ghost">Cancel</button>
                    <div class="spacer-h-sm"></div>
                    <button id="save-btn" class="btn-primary" style="anchor-width: 160;">{{#if isEditing}}Save{{else}}Create{{/if}}</button>
                </div>
        
            </div>
        </div>
        """);

        PageBuilder page = PageBuilder.pageForPlayer(playerRef)
                .withLifetime(CustomPageLifetime.CanDismiss)
                .fromHtml(html);

        setupEditTraderListeners(page, playerRef, store, trader, state);

        page.open(store);
    }

    private void setupEditTraderListeners(PageBuilder page, PlayerRef playerRef, Store<EntityStore> store,
                                          @Nullable Trader trader, EditTraderState state) {
        boolean isEditing = state.traderUuid != null;

        // Name input
        page.addEventListener("trader-name", CustomUIEventBindingType.ValueChanged, (event, ctx) -> {
            state.name = ctx.getValue("trader-name", String.class).orElse("");
        });

        // Trade item buttons - input side
        for (int i = 0; i < state.trades.size(); i++) {
            final int index = i;

            page.addEventListener("input-" + i, CustomUIEventBindingType.Activating, event -> {
                state.editingTradeIndex = index;
                state.editingInput = true;
                openItemSelectionGUI(playerRef, store, trader, "", 0);
            });

            page.addEventListener("output-" + i, CustomUIEventBindingType.Activating, event -> {
                state.editingTradeIndex = index;
                state.editingInput = false;
                openItemSelectionGUI(playerRef, store, trader, "", 0);
            });

            page.addEventListener("delete-trade-" + i, CustomUIEventBindingType.Activating, event -> {
                state.trades.remove(index);
                openEditTraderGUI(playerRef, store, trader);
            });
        }

        // Add trade button
        page.addEventListener("add-trade-btn", CustomUIEventBindingType.Activating, event -> {
            state.trades.add(new TradePair());
            openEditTraderGUI(playerRef, store, trader);
        });

        // Rotation toggle
        page.addEventListener("rotate-items-check", CustomUIEventBindingType.ValueChanged, (event, ctx) -> {
            state.rotateItems = ctx.getValue("rotate-items-check", Boolean.class).orElse(false);
        });

        // Rotation interval
        page.addEventListener("rotation-interval", CustomUIEventBindingType.ValueChanged, (event, ctx) -> {
            ctx.getValue("rotation-interval", Double.class).ifPresent(val -> {
                state.rotationInterval = val.intValue();
            });
        });

        // Display count
        page.addEventListener("display-count", CustomUIEventBindingType.ValueChanged, (event, ctx) -> {
            ctx.getValue("display-count", Double.class).ifPresent(val -> {
                state.displayCount = val.intValue();
            });
        });

        // Spawn NPC button
        if (isEditing && trader != null) {
            page.addEventListener("spawn-npc-btn", CustomUIEventBindingType.Activating, event -> {
                if (!isHyCitizensInstalled()) {
                    playerRef.sendMessage(Message.raw("You must install HyCitizens to spawn NPCs!").color(Color.RED));
                    playerRef.sendMessage(Message.raw("Download (Click)").color(Color.ORANGE)
                            .link("https://www.curseforge.com/hytale/mods/hycitizens"));
                    return;
                }

                spawnTraderNPC(playerRef, trader);
            });

            page.addEventListener("bind-npc-btn", CustomUIEventBindingType.Activating, event -> {
                if (!isHyCitizensInstalled()) {
                    playerRef.sendMessage(Message.raw("You must install HyCitizens to bind NPCs!").color(Color.RED));
                    playerRef.sendMessage(Message.raw("Download (Click)").color(Color.ORANGE)
                            .link("https://www.curseforge.com/hytale/mods/hycitizens"));
                    return;
                }

                plugin.getNPCBindManager().startBinding(playerRef, trader);
                playerRef.sendMessage(Message.raw("You have 30 seconds to punch an HyCitizens NPC to link it!").color(Color.YELLOW));
            });
        }

        // Save button
        page.addEventListener("save-btn", CustomUIEventBindingType.Activating, (event, ctx) -> {
            String name = state.name.trim();

            if (name.isEmpty()) {
                playerRef.sendMessage(Message.raw("Please enter a trader name!").color(Color.RED));
                return;
            }

            // Build trades from state
            List<TradeOffer> newTrades = new ArrayList<>();
            for (TradePair pair : state.trades) {
                if (pair.isComplete()) {
                    newTrades.add(new TradeOffer(
                            pair.inputItemId, pair.inputQuantity,
                            pair.outputItemId, pair.outputQuantity
                    ));
                }
            }

            if (isEditing && trader != null) {
                trader.setName(name);
                trader.clearTrades();
                for (TradeOffer trade : newTrades) {
                    trader.addTrade(trade);
                }
                trader.setRotateItems(state.rotateItems);
                trader.setRotationIntervalMinutes(state.rotationInterval);
                trader.setDisplayedItemCount(state.displayCount);

                plugin.getTradersManager().saveTrader(trader);
                playerRef.sendMessage(Message.raw("Trader '" + name + "' updated!").color(Color.GREEN));
            } else {
                Trader newTrader = plugin.getTradersManager().createTrader(name);
                for (TradeOffer trade : newTrades) {
                    newTrader.addTrade(trade);
                }
                newTrader.setRotateItems(state.rotateItems);
                newTrader.setRotationIntervalMinutes(state.rotationInterval);
                newTrader.setDisplayedItemCount(state.displayCount);

                plugin.getTradersManager().saveTrader(newTrader);
                playerRef.sendMessage(Message.raw("Trader '" + name + "' created!").color(Color.GREEN));
            }

            clearState(playerRef);
            openTradersGUI(playerRef, store);
        });

        // Cancel button
        page.addEventListener("cancel-btn", CustomUIEventBindingType.Activating, event -> {
            clearState(playerRef);
            openTradersGUI(playerRef, store);
        });
    }

    // Item Selection GUI
    private static final int ITEMS_PER_PAGE = 75;

    private void openItemSelectionGUI(@Nonnull PlayerRef playerRef, @Nonnull Store<EntityStore> store,
                                      @Nullable Trader trader, @Nonnull String searchFilter, int page) {
        // Get all items from the asset map
        Map<String, Item> itemMap = Item.getAssetMap().getAssetMap();
        String lowerFilter = searchFilter.toLowerCase().trim();

        // Filter items by search
        List<Map.Entry<String, Item>> filteredItems;
        if (lowerFilter.isEmpty()) {
            filteredItems = itemMap.entrySet().stream()
                    .sorted(Comparator.comparing(Map.Entry::getKey))
                    .collect(Collectors.toList());
        } else {
            filteredItems = itemMap.entrySet().stream()
                    .filter(entry -> {
                        String id = entry.getKey().toLowerCase();
                        String translation = Message.translation(entry.getValue().getTranslationKey()).getAnsiMessage().toLowerCase();

                        // Fallback if translation fails
                        if (translation.startsWith("server.")) {
                            // Remove ending ".name"
                            translation = translation.replaceFirst("(?i)\\.name$", "");

                            // Remove everything before the last dot
                            int lastDot = translation.lastIndexOf('.');
                            if (lastDot != -1) {
                                translation = translation.substring(lastDot + 1);
                            }

                            // Replace underscores with spaces
                            translation = translation.replace("_", " ");
                        }

                        return id.contains(lowerFilter) || translation.contains(lowerFilter);
                    })
                    .sorted(Comparator.comparing(Map.Entry::getKey))
                    .collect(Collectors.toList());
        }

        int totalItems = filteredItems.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE));
        int currentPage = Math.max(0, Math.min(page, totalPages - 1));
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalItems);

        List<Map.Entry<String, Item>> pageItems = filteredItems.subList(startIndex, endIndex);

        // Build items grid HTML
        int itemsPerRow = 8;
        StringBuilder itemsHtml = new StringBuilder();

        for (int rowStart = 0; rowStart < pageItems.size(); rowStart += itemsPerRow) {
            if (rowStart > 0) {
                itemsHtml.append("<div class=\"item-pick-row-spacer\"></div>\n");
            }

            itemsHtml.append("<div class=\"items-grid-row\">\n");

            int rowEnd = Math.min(rowStart + itemsPerRow, pageItems.size());
            for (int i = rowStart; i < rowEnd; i++) {
                if (i > rowStart) {
                    itemsHtml.append("<div class=\"item-pick-spacer\"></div>\n");
                }

                Map.Entry<String, Item> entry = pageItems.get(i);
                String itemId = entry.getKey();
                String displayName = Message.translation(entry.getValue().getTranslationKey()).getAnsiMessage();

                // Fallback since sometimes it fails to translate
//                if (displayName.startsWith("server.items.")) {
//                    displayName = displayName.substring("server.items.".length());
//                    if (displayName.endsWith(".name")) {
//                        displayName = displayName.substring(0, displayName.length() - ".name".length());
//                    }
//
//                    displayName = displayName.replace("_", " ");
//                }

                if (displayName.startsWith("server.")) {
                    // Remove ending ".name"
                    displayName = displayName.replaceFirst("(?i)\\.name$", "");

                    // Remove everything before the last dot
                    int lastDot = displayName.lastIndexOf('.');
                    if (lastDot != -1) {
                        displayName = displayName.substring(lastDot + 1);
                    }

                    // Replace underscores with spaces
                    displayName = displayName.replace("_", " ");
                }

                if (displayName.length() > 12) {
                    displayName = displayName.substring(0, 11) + "…";
                }

                itemsHtml.append("""
                     <div class="slot-background-big">
                        <button id="pick-%d" class="item-pick-cell" flex-direction: column;">
                            <span class="item-pick-icon-container item-pick-icon item-icon item-slot" data-hyui-item-id="%s"></span>
                            <p class="item-pick-name">%s</p>
                        </button>
                    </div>
                    """.formatted(i, itemId, displayName));
            }

            itemsHtml.append("</div>\n");
        }

        if (pageItems.isEmpty()) {
            itemsHtml.append("""
                    <div class="empty-state">
                        <p style="color: #6e7681; font-size: 13; text-align: center;">No items match your search.</p>
                    </div>
                    """);
        }

        String html = getSharedStyles() + """
                <div class="page-overlay">
                    <div class="main-container" style="anchor-width: 850; anchor-height: 750;">
                
                        <!-- Header -->
                        <div class="header">
                            <div class="header-content">
                                <p class="header-title">Select Item</p>
                                <p class="header-subtitle">Page %d of %d (%d items total)</p>
                            </div>
                        </div>
                
                        <!-- Search -->
                        <div class="search-container" style="padding-top: 12;">
                            <div style="layout: center; flex-weight: 0;">
                                <input type="text" id="item-search" class="search-input" style="flex-weight: 1;"
                                       value="%s" placeholder="Search items..." maxlength="64" />
                                <div class="spacer-h-sm"></div>
                                <button id="search-btn" class="btn-primary" style="anchor-width: 130;">Search</button>
                            </div>
                        </div>
                
                        <!-- Items Grid -->
                        <div class="items-grid">
                            %s
                        </div>
                
                        <!-- Footer with pagination -->
                        <div class="footer">
                            <button id="back-btn" class="btn-ghost">Back</button>
                            <div style="flex-weight: 1;"></div>
                            <button id="prev-page-btn" class="btn-secondary btn-small" %s>Prev</button>
                            <div class="spacer-h-sm"></div>
                            <p style="color: #8b949e; font-size: 12;">%d / %d</p>
                            <div class="spacer-h-sm"></div>
                            <button id="next-page-btn" class="btn-secondary btn-small" %s>Next</button>
                        </div>
                
                    </div>
                </div>
                """.formatted(
                currentPage + 1, totalPages, totalItems,
                searchFilter,
                itemsHtml.toString(),
                currentPage <= 0 ? "disabled" : "",
                currentPage + 1, totalPages,
                currentPage >= totalPages - 1 ? "disabled" : ""
        );

        PageBuilder pageBuilder = PageBuilder.pageForPlayer(playerRef)
                .withLifetime(CustomPageLifetime.CanDismiss)
                .fromHtml(html);

        // Store search text for the search button
        final String[] searchText = {searchFilter};

        pageBuilder.addEventListener("item-search", CustomUIEventBindingType.ValueChanged, (event, ctx) -> {
            searchText[0] = ctx.getValue("item-search", String.class).orElse("");
        });

        // Search button — reloads page 0 with the current search text
        pageBuilder.addEventListener("search-btn", CustomUIEventBindingType.Activating, event -> {
            openItemSelectionGUI(playerRef, store, trader, searchText[0], 0);
        });

        // Pagination
        if (currentPage > 0) {
            pageBuilder.addEventListener("prev-page-btn", CustomUIEventBindingType.Activating, event -> {
                openItemSelectionGUI(playerRef, store, trader, searchFilter, currentPage - 1);
            });
        }
        if (currentPage < totalPages - 1) {
            pageBuilder.addEventListener("next-page-btn", CustomUIEventBindingType.Activating, event -> {
                openItemSelectionGUI(playerRef, store, trader, searchFilter, currentPage + 1);
            });
        }

        // Item pick handlers
        for (int i = 0; i < pageItems.size(); i++) {
            final String itemId = pageItems.get(i).getKey();
            final int idx = i;

            pageBuilder.addEventListener("pick-" + idx, CustomUIEventBindingType.Activating, event -> {
                openQuantitySelectionGUI(playerRef, store, trader, itemId);
            });
        }

        // Back button
        pageBuilder.addEventListener("back-btn", CustomUIEventBindingType.Activating, event -> {
            openEditTraderGUI(playerRef, store, trader);
        });

        pageBuilder.open(store);
    }

    // Quantity Selection GUI

    private void openQuantitySelectionGUI(@Nonnull PlayerRef playerRef, @Nonnull Store<EntityStore> store,
                                          @Nullable Trader trader, @Nonnull String itemId) {
        EditTraderState state = editStates.get(playerRef.getUuid());
        if (state == null) return;

        // Get current quantity if editing an existing slot
        int currentQty = 1;
        if (state.editingTradeIndex >= 0 && state.editingTradeIndex < state.trades.size()) {
            TradePair pair = state.trades.get(state.editingTradeIndex);
            if (state.editingInput && pair.inputItemId != null && pair.inputItemId.equals(itemId)) {
                currentQty = pair.inputQuantity;
            } else if (!state.editingInput && pair.outputItemId != null && pair.outputItemId.equals(itemId)) {
                currentQty = pair.outputQuantity;
            }
        }

        // Get display name
        Item item = Item.getAssetMap().getAssetMap().get(itemId);
        String displayName = itemId;
        if (item != null) {
            displayName = Message.translation(item.getTranslationKey()).getAnsiMessage();

            // Fallback since sometimes it fails to translate
//            if (displayName.startsWith("server.items.")) {
//                displayName = displayName.substring("server.items.".length());
//                if (displayName.endsWith(".name")) {
//                    displayName = displayName.substring(0, displayName.length() - ".name".length());
//                }
//
//                displayName = displayName.replace("_", " ");
//            }

            if (displayName.startsWith("server.")) {
                // Remove ending ".name"
                displayName = displayName.replaceFirst("(?i)\\.name$", "");

                // Remove everything before the last dot
                int lastDot = displayName.lastIndexOf('.');
                if (lastDot != -1) {
                    displayName = displayName.substring(lastDot + 1);
                }

                // Replace underscores with spaces
                displayName = displayName.replace("_", " ");
            }

        }

        TemplateProcessor template = createBaseTemplate()
                .setVariable("itemId", itemId)
                .setVariable("displayName", displayName)
                .setVariable("currentQty", currentQty);

        String html = template.process(getSharedStyles() + """
                <div class="page-overlay">
                    <div class="main-container" style="anchor-width: 500; anchor-height: 400;">
                
                        <!-- Header -->
                        <div class="header">
                            <div class="header-content">
                                <p class="header-title">Set Quantity</p>
                                <p class="header-subtitle">Choose how many of this item</p>
                            </div>
                        </div>
                
                        <!-- Body -->
                        <div class="body">
                            <div class="qty-section">
                                <!-- Item Display -->
                                <div class="qty-item-display">
                                    <span class="qty-item-icon item-icon" data-hyui-item-id="{{$itemId}}"></span>
                                    <p class="qty-item-name">{{$displayName}}</p>
                                </div>
                                <!-- Quantity Input -->
                                <div style="layout: center; flex-weight: 0;">
                                    <div style="anchor-width: 250; flex-weight: 0;">
                                        {{@numberField:id=quantity-input,label=Quantity,value={{$currentQty}},placeholder=1,min=1,max=9999,step=1,decimals=0,hint=Number of items for this trade}}
                                    </div>
                                </div>
                            </div>
                        </div>
                
                        <!-- Footer -->
                        <div class="footer">
                            <button id="qty-back-btn" class="btn-ghost">Back</button>
                            <div class="spacer-h-sm"></div>
                            <button id="qty-confirm-btn" class="btn-primary" style="anchor-width: 160;">Confirm</button>
                        </div>
                
                    </div>
                </div>
                """);

        PageBuilder page = PageBuilder.pageForPlayer(playerRef)
                .withLifetime(CustomPageLifetime.CanDismiss)
                .fromHtml(html);

        final int[] quantity = {currentQty};

        page.addEventListener("quantity-input", CustomUIEventBindingType.ValueChanged, (event, ctx) -> {
            ctx.getValue("quantity-input", Double.class).ifPresent(val -> {
                quantity[0] = Math.max(1, val.intValue());
            });
        });

        page.addEventListener("qty-confirm-btn", CustomUIEventBindingType.Activating, event -> {
            // Apply the selection to the state
            if (state.editingTradeIndex >= 0 && state.editingTradeIndex < state.trades.size()) {
                TradePair pair = state.trades.get(state.editingTradeIndex);
                if (state.editingInput) {
                    pair.inputItemId = itemId;
                    pair.inputQuantity = quantity[0];
                } else {
                    pair.outputItemId = itemId;
                    pair.outputQuantity = quantity[0];
                }
            }
            openEditTraderGUI(playerRef, store, trader);
        });

        page.addEventListener("qty-back-btn", CustomUIEventBindingType.Activating, event -> {
            openItemSelectionGUI(playerRef, store, trader, "", 0);
        });

        page.open(store);
    }

    // Delete Confirmation GUI

    private void openDeleteConfirmationGUI(PlayerRef playerRef, Store<EntityStore> store, Trader trader) {
        String html = getSharedStyles() + """
                <div class="page-overlay">
                    <div class="main-container" style="anchor-width: 600; anchor-height: 350;">
                
                        <div class="header">
                            <div class="header-content">
                                <p class="header-title">Delete Trader</p>
                            </div>
                        </div>
                        
                        <div class="section">
                            <p style="color: #FF0000; font-size: 14; text-align: center;">This trader has NPCs linked to it!</p>
                        </div>
                        
                        <div class="spacer-md"></div>
                        <div class="spacer-md"></div>
                
                        <div class="body">
                            <div>
                                <button id="cancel-delete-btn" class="btn-ghost">Cancel</button>
                            </div>
                            
                            <div class="spacer-md"></div>
                            <div class="spacer-md"></div>
                            
                            <div>
                                <button id="delete-keep-npcs-btn" class="btn-secondary" style="anchor-width: 250;">Delete & Keep NPCs</button>
                            </div>
                            
                            <div class="spacer-md"></div>
                            <div class="spacer-md"></div>
                            
                            <div>
                                <button id="delete-remove-npcs-btn" class="btn-danger" style="anchor-width: 275;">Delete & Remove NPCs</button>
                            </div>
                        </div>
                    </div>
                </div>
                """;

        PageBuilder page = PageBuilder.pageForPlayer(playerRef)
                .withLifetime(CustomPageLifetime.CanDismiss)
                .fromHtml(html);

        page.addEventListener("cancel-delete-btn", CustomUIEventBindingType.Activating, event -> {
            openTradersGUI(playerRef, store);
        });

        page.addEventListener("delete-keep-npcs-btn", CustomUIEventBindingType.Activating, event -> {
            plugin.getTradersManager().deleteTrader(trader.getUuid());
            playerRef.sendMessage(Message.raw("Trader '" + trader.getName() + "' deleted! NPCs kept.").color(Color.GREEN));
            openTradersGUI(playerRef, store);
        });

        page.addEventListener("delete-remove-npcs-btn", CustomUIEventBindingType.Activating, event -> {
            if (isHyCitizensInstalled()) {
                removeTraderNPCs(trader);
            }
            plugin.getTradersManager().deleteTrader(trader.getUuid());
            playerRef.sendMessage(Message.raw("Trader '" + trader.getName() + "' and all NPCs deleted!").color(Color.GREEN));
            openTradersGUI(playerRef, store);
        });

        page.open(store);
    }

    // HyCitizens Integration

    private boolean isHyCitizensInstalled() {
        PluginIdentifier id = new PluginIdentifier("com.electro", "HyCitizens");
        return PluginManager.get().getPlugin(id) != null;
    }

    private void spawnTraderNPC(PlayerRef playerRef, Trader trader) {
        try {
            HyCitizensPlugin hyCitizens = HyCitizensPlugin.get();

            com.hypixel.hytale.math.vector.Vector3d position = new com.hypixel.hytale.math.vector.Vector3d(playerRef.getTransform().getPosition());
            com.hypixel.hytale.math.vector.Vector3f rotation = new com.hypixel.hytale.math.vector.Vector3f(playerRef.getTransform().getRotation());
            java.util.UUID worldUUID = playerRef.getWorldUuid();

            if (worldUUID == null) {
                playerRef.sendMessage(Message.raw("Failed to spawn NPC!").color(Color.RED));
                return;
            }

            CitizenData citizen = new CitizenData(
                    java.util.UUID.randomUUID().toString(),
                    trader.getName(),
                    "PlayerTestModel_V",
                    worldUUID,
                    position,
                    rotation,
                    1.0f,
                    null,
                    new java.util.ArrayList<>(),
                    "",
                    "",
                    java.util.List.of(),
                    false,
                    false,
                    null,
                    null,
                    0L,
                    true
            );

            citizen.setForceFKeyInteractionText(true);
            citizen.setGroup("Traders");

            hyCitizens.getCitizensManager().addCitizen(citizen, true);

            // Bind the NPC to this trader
            trader.addCitizenId(citizen.getId());
            plugin.getTradersManager().saveTrader(trader);

            playerRef.sendMessage(Message.raw("Trading NPC spawned!").color(Color.GREEN));
        } catch (Exception e) {
            playerRef.sendMessage(Message.raw("Error spawning NPC: " + e.getMessage()).color(Color.RED));
        }
    }

    private void removeTraderNPCs(Trader trader) {
        try {
            HyCitizensPlugin hyCitizens = HyCitizensPlugin.get();

            for (String citizenId : trader.getCitizenIds()) {
                hyCitizens.getCitizensManager().removeCitizen(citizenId);
            }
        } catch (Exception ignored) {
        }
    }
}