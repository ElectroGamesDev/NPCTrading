package com.electro.npctrading.ui;

import au.ellie.hyui.builders.PageBuilder;
import com.electro.npctrading.manager.TradeManager;
import com.electro.npctrading.model.TradeOffer;
import com.electro.npctrading.model.Trader;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.List;

import static com.hypixel.hytale.logger.HytaleLogger.getLogger;

public class TradeUI {

    private final TradeManager tradeManager;

    public TradeUI(@Nonnull TradeManager tradeManager) {
        this.tradeManager = tradeManager;
    }

    public void openTradeGUI(@Nonnull PlayerRef playerRef,
                             @Nonnull Store<EntityStore> store, @Nonnull Trader trader) {
        List<TradeOffer> displayedTrades = trader.getDisplayedTrades();

        String html = buildHTML(trader, displayedTrades, playerRef);

        PageBuilder page = PageBuilder.pageForPlayer(playerRef)
                .withLifetime(CustomPageLifetime.CanDismiss)
                .fromHtml(html);

        setupEventListeners(page, playerRef, store, trader, displayedTrades);

        page.open(store);
    }

    //  HTML Builder
    private String buildHTML(@Nonnull Trader trader, @Nonnull List<TradeOffer> trades,
                             @Nonnull PlayerRef playerRef) {
        StringBuilder sb = new StringBuilder();

        sb.append("""
            <style>
                .trade-container {
                    anchor-width: 820;
                    anchor-height: 540;
                }

                /* Title Bar */
                .title-bar {
                    layout: center;
                    flex-weight: 0;
                    background-color: #2a2a2a(0.95);
                    padding: 14;
                    border-radius: 10 10 0 0;
                }

                .trader-name {
                    color: #FFFFFF;
                    font-size: 20;
                    font-weight: bold;
                    text-align: center;
                }

                /* Main Content */
                .main-content {
                    layout: top;
                    flex-weight: 1;
                    padding: 24 20 10 20;
                }
                
                /* List Container */
                .list-container {
                    layout-mode: TopScrolling;
                    flex-weight: 1;
                }

                /* Trade Grid */
                .trades-row {
                    layout: center;
                    flex-weight: 0;
                }

                .trade-card {
                    layout: top;
                    flex-weight: 0;
                    anchor-width: 230;
                    padding: 12;
                    background-color: #1c2835(0.50);
                    border-radius: 8;
                }

                .trade-spacer {
                    flex-weight: 0;
                    anchor-width: 14;
                }

                .row-spacer {
                    flex-weight: 0;
                    anchor-height: 14;
                }

                /* Output Item */
                .output-section {
                    layout: top;
                    flex-weight: 0;
                }

                .output-icon-row {
                    layout: center;
                    flex-weight: 0;
                    padding: 6;
                }

                .output-icon {
                    anchor-width: 52;
                    anchor-height: 52;
                }

                .output-name {
                    color: #ffffff;
                    font-size: 13;
                    font-weight: bold;
                    text-align: center;
                    padding-top: 4;
                }

                .output-qty {
                    color: #aaaaaa;
                    font-size: 11;
                    text-align: center;
                    padding-top: 2;
                }

                /* Divider */
                .divider {
                    flex-weight: 0;
                    anchor-height: 1;
                    background-color: #ffffff(0.08);
                    margin-top: 8;
                    margin-bottom: 8;
                }

                /* Cost Section */
                .cost-section {
                    layout: top;
                    flex-weight: 0;
                }

                .cost-label {
                    color: #888888;
                    font-size: 10;
                    text-align: center;
                }

                .cost-row {
                    layout: center;
                    flex-weight: 0;
                    padding-top: 4;
                }

                .cost-icon {
                    anchor-width: 34;
                    anchor-height: 34;
                }

                .cost-amount {
                    color: #FF6B6B;
                    font-size: 13;
                    font-weight: bold;
                    padding-left: 6;
                }

                .cost-amount-affordable {
                    color: #7BED7B;
                    font-size: 13;
                    font-weight: bold;
                    padding-left: 6;
                }

                /* Buy Button */
                .buy-btn {
                    background-color: #2ecc71(0.9);
                    margin-top: 8;
                    border-radius: 4;
                    padding: 6;
                }

                .buy-btn-text {
                    color: #ffffff;
                    font-size: 12;
                    font-weight: bold;
                    text-align: center;
                }

                /* Can't Afford */
                .need-section {
                    layout: center;
                    flex-weight: 0;
                    margin-top: 8;
                    padding: 6;
                    background-color: #8B0000(0.35);
                    border-radius: 4;
                }

                .need-text {
                    color: #FF4444;
                    font-size: 11;
                    font-weight: bold;
                    text-align: center;
                }

                /* Rotation Footer */
                .footer-bar {
                    layout: center;
                    flex-weight: 0;
                    padding: 10;
                    border-radius: 0 0 10 10;
                }

                .rotation-text {
                    color: #AAAAAA;
                    font-size: 11;
                    text-align: center;
                }

                .rotation-time {
                    color: #FFD700;
                    font-size: 11;
                    font-weight: bold;
                    padding-left: 4;
                }

                /* Empty State */
                .empty-state {
                    layout: center;
                    flex-weight: 1;
                }

                .empty-text {
                    color: #555555;
                    font-size: 14;
                    text-align: center;
                }

                .spacer-small {
                    flex-weight: 0;
                    anchor-height: 6;
                }
            </style>

            <div class="page-overlay">
                <div class="decorated-container trade-container" data-hyui-title=\"""")
                    .append(escapeHtml(trader.getName()))
                    .append("\">")
                    .append("""
                    <!-- Title Bar -->
                    <div class="title-bar">
    """);

        sb.append("""
                    </div>
                    
                    <div class=\\"row-spacer\\"></div>

                    <!-- Main Content -->
                    <div class="main-content">
                        <div class="list-container" style="anchor-height: 420;">
            """);

        // Build trade cards
        sb.append(buildTradeCards(trades, playerRef));

        sb.append("""
                        </div>
                    </div>
            """);

        // Rotation footer
        if (trader.isRotateItems() && trader.getTrades().size() > trader.getDisplayedItemCount()) {
            String timeRemaining = getRotationTimeRemaining(trader);
            sb.append("""
                    <!-- Rotation Footer -->
                    <div class="footer-bar">
                        <p class="rotation-text">Items rotate in</p>
                        <p class="rotation-time">%s</p>
                    </div>
                """.formatted(timeRemaining));
        }

        sb.append("""
                </div>
            </div>
            """);

        return sb.toString();
    }

    //  Trade Cards Builder

    private String buildTradeCards(@Nonnull List<TradeOffer> trades, @Nonnull PlayerRef playerRef) {
        StringBuilder sb = new StringBuilder();

        if (trades.isEmpty()) {
            sb.append("""
                            <div class="empty-state">
                                <p class="empty-text">No trades available</p>
                            </div>
                """);
            return sb.toString();
        }

        int itemsPerRow = 3;

        for (int rowStart = 0; rowStart < trades.size(); rowStart += itemsPerRow) {
            if (rowStart > 0) {
                sb.append("                        <div class=\"row-spacer\"></div>\n");
            }

            sb.append("                        <div class=\"trades-row\">\n");

            int rowEnd = Math.min(rowStart + itemsPerRow, trades.size());
            for (int i = rowStart; i < rowEnd; i++) {
                if (i > rowStart) {
                    sb.append("                            <div class=\"trade-spacer\"></div>\n");
                }

                TradeOffer offer = trades.get(i);
                int playerHas = tradeManager.getItemCount(playerRef, offer.inputItem());
                boolean canAfford = playerHas >= offer.inputQuantity();
                int deficit = offer.inputQuantity() - playerHas;

                String costAmountClass = canAfford ? "cost-amount-affordable" : "cost-amount";

                sb.append("""
                                            <div class="trade-card">
                                                <!-- Output Item -->
                                                <div class="output-section">
                                                    <div class="output-icon-row">
                                                        <span class="output-icon item-icon" data-hyui-item-id="%s"></span>
                                                    </div>
                                                    <p class="output-name">%s</p>
                                                    <p class="output-qty">x%d</p>
                                                </div>

                                                <div class="divider"></div>

                                                <!-- Cost -->
                                                <div class="cost-section">
                                                    <p class="cost-label">Cost</p>
                                                    <div class="cost-row">
                                                        <span class="cost-icon item-icon" data-hyui-item-id="%s"></span>
                                                        <p class="%s">%d</p>
                                                    </div>
                                                </div>

                    """.formatted(
                        offer.outputItem(),
                        formatItemName(offer.outputItem()),
                        offer.outputQuantity(),
                        offer.inputItem(),
                        costAmountClass,
                        offer.inputQuantity()
                ));

                // Buy button or deficit message
                if (canAfford) {
                    sb.append("""
                                                <button id="trade-%d" class="buy-btn">
                                                    <p class="buy-btn-text">Purchase</p>
                                                </button>
                        """.formatted(i));
                } else {
                    sb.append("""
                                                <button id="trade-%d" class="need-section">
                                                    <p class="need-text">Need %d more</p>
                                                </button>
                        """.formatted(i, deficit));
                }

                sb.append("                            </div>\n");
            }

            sb.append("                        </div>\n");
        }

        return sb.toString();
    }

    //  Event Listeners
    private void setupEventListeners(@Nonnull PageBuilder page, @Nonnull PlayerRef playerRef, @Nonnull Store<EntityStore> store,
                                     @Nonnull Trader trader, @Nonnull List<TradeOffer> trades) {
        for (int i = 0; i < trades.size(); i++) {
            final TradeOffer offer = trades.get(i);

            if (!tradeManager.canAfford(playerRef, offer)) continue;

            page.addEventListener("trade-" + i, CustomUIEventBindingType.Activating, (ignored, ctx) -> {
                tradeManager.executeTrade(playerRef, offer);
                openTradeGUI(playerRef, store, trader);
            });
        }
    }

    //  Helpers
    private String getRotationTimeRemaining(@Nonnull Trader trader) {
        long currentTime = System.currentTimeMillis();
        long intervalMs = trader.getRotationIntervalMinutes() * 60L * 1000L;
        long elapsed = currentTime - trader.getLastRotationTime();
        long remainingMs = Math.max(0, intervalMs - elapsed);

        long totalSeconds = remainingMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        if (minutes > 0) {
            return String.format("%dm %02ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    public String formatItemName(@Nonnull String itemId) {
        String name = Message.translation(Item.getAssetMap().getAsset(itemId).getTranslationKey()).getAnsiMessage();

        // Fallback since sometimes it fails to translate. This isn't 100% accurate, but its close enough
//        if (name.startsWith("server.items.")) {
//            name = name.substring("server.items.".length());
//            if (name.endsWith(".name")) {
//                name = name.substring(0, name.length() - ".name".length());
//            }
//
//            // Remove the next underscore
//            name = name.replaceFirst("[^_]*_", "");
//
//            name = name.replace("_", " ");
//        }

        if (name.startsWith("server.")) {
            // Remove ending ".name"
            name = name.replaceFirst("(?i)\\.name$", "");

            // Remove everything before the last dot
            int lastDot = name.lastIndexOf('.');
            if (lastDot != -1) {
                name = name.substring(lastDot + 1);
            }

            // Replace underscores with spaces
            name = name.replace("_", " ");
        }

        return name;
    }

    private String escapeHtml(@Nonnull String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}