package com.twelveably.handphone.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientProfilePictures {
    private static final Map<UUID, ResourceLocation> SKINS_BY_PROFILE_ID = new HashMap<>();
    private static final int UNKNOWN_HEAD_BACKGROUND = 0xFF2F3437;
    private static final int UNKNOWN_HEAD_BORDER = 0xFFDFE4E8;
    private static final int UNKNOWN_HEAD_TEXT = 0xFFFFFFFF;

    private ClientProfilePictures() {
    }

    public static void drawHead(GuiGraphics graphics, Font font, String profileId, String fallbackKey, int x, int y, int size) {
        if (isUnknownProfile(profileId)) {
            drawUnknownHead(graphics, font, x, y, size);
            return;
        }

        ResourceLocation skin = getSkin(profileId, fallbackKey);
        graphics.blit(skin, x, y, size, size, 8.0F, 8.0F, 8, 8, 64, 64);
        graphics.blit(skin, x, y, size, size, 40.0F, 8.0F, 8, 8, 64, 64);
    }

    public static ResourceLocation getSkin(String profileId, String fallbackKey) {
        UUID uuid = parseProfileId(profileId);
        if (uuid != null) {
            ResourceLocation liveSkin = getLiveSkin(uuid);
            if (liveSkin != null) {
                SKINS_BY_PROFILE_ID.putIfAbsent(uuid, liveSkin);
            }

            ResourceLocation cachedSkin = SKINS_BY_PROFILE_ID.get(uuid);
            return cachedSkin == null ? DefaultPlayerSkin.getDefaultSkin(uuid) : cachedSkin;
        }

        UUID fallbackId = UUID.nameUUIDFromBytes(("handphone:" + fallbackKey).getBytes(StandardCharsets.UTF_8));
        return DefaultPlayerSkin.getDefaultSkin(fallbackId);
    }

    public static boolean isUnknownProfile(String profileId) {
        return parseProfileId(profileId) == null;
    }

    private static void drawUnknownHead(GuiGraphics graphics, Font font, int x, int y, int size) {
        graphics.fill(x, y, x + size, y + size, UNKNOWN_HEAD_BORDER);
        graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, UNKNOWN_HEAD_BACKGROUND);
        String text = "?";
        int textX = x + (size - font.width(text)) / 2;
        int textY = y + (size - 8) / 2;
        graphics.drawString(font, text, textX, textY, UNKNOWN_HEAD_TEXT, false);
    }

    private static ResourceLocation getLiveSkin(UUID uuid) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            Player player = minecraft.level.getPlayerByUUID(uuid);
            if (player instanceof AbstractClientPlayer clientPlayer) {
                return clientPlayer.getSkinTextureLocation();
            }
        }

        LocalPlayer localPlayer = minecraft.player;
        if (localPlayer != null && localPlayer.getUUID().equals(uuid)) {
            return localPlayer.getSkinTextureLocation();
        }

        return null;
    }

    private static UUID parseProfileId(String profileId) {
        if (profileId == null || profileId.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(profileId);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
