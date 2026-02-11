package com.electro.npctrading.manager;

import com.electro.npctrading.model.Trader;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.hypixel.hytale.server.core.HytaleServer.SCHEDULED_EXECUTOR;

public class NPCBindManager {
    private final Map<UUID, BindingSession> activeSessions = new ConcurrentHashMap<>();

    public static class BindingSession {
        public final PlayerRef playerRef;
        public final Trader trader;
        public final ScheduledFuture<?> timeoutTask;

        public BindingSession(PlayerRef playerRef, Trader trader, ScheduledFuture<?> timeoutTask) {
            this.playerRef = playerRef;
            this.trader = trader;
            this.timeoutTask = timeoutTask;
        }
    }

    public void startBinding(@Nonnull PlayerRef playerRef, @Nonnull Trader trader) {
        UUID playerUUID = playerRef.getUuid();

        cancelBinding(playerUUID);

        // Create timeout task
        ScheduledFuture<?> timeoutTask = SCHEDULED_EXECUTOR.schedule(() -> {
            activeSessions.remove(playerUUID);
            playerRef.sendMessage(Message.raw("NPC link timed out!").color(Color.RED));
        }, 30, TimeUnit.SECONDS);

        activeSessions.put(playerUUID, new BindingSession(playerRef, trader, timeoutTask));
    }

    @Nullable
    public BindingSession getActiveSession(@Nonnull UUID playerUUID) {
        return activeSessions.get(playerUUID);
    }

    public void completeBinding(@Nonnull UUID playerUUID) {
        BindingSession session = activeSessions.remove(playerUUID);
        if (session != null && session.timeoutTask != null) {
            session.timeoutTask.cancel(false);
        }
    }

    public void cancelBinding(@Nonnull UUID playerUUID) {
        BindingSession session = activeSessions.remove(playerUUID);
        if (session != null && session.timeoutTask != null) {
            session.timeoutTask.cancel(false);
        }
    }
}