package com.twelveably.handphone.client.phone;

import com.twelveably.handphone.Handphone;
import com.twelveably.handphone.client.PhoneHomeScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class HomeWindow implements PhoneWindow {
    private static final ResourceLocation VOICE_CHAT_ICON = new ResourceLocation(Handphone.MODID, "textures/gui/app_icons/voice_chat.png");
    private static final int TEXTURE_WIDTH = 141;
    private static final int APP_ICON_WIDTH = 34;
    private static final int APP_ICON_HEIGHT = 37;
    private static final int APP_ICON_X = 15;
    private static final int APP_ICON_Y = 170;
    private static final int CLOCK_Y = 42;
    private static final float CLOCK_SCALE = 2.65F;
    private static final int CLOCK_COLOR = 0xFF2B3340;
    private static final int UNREAD_BADGE_COLOR = 0xFFE11919;
    private static final int UNREAD_BADGE_TEXT_COLOR = 0xFFFFFFFF;
    private static final ZoneId JAKARTA_ZONE = ZoneId.of("Asia/Jakarta");
    private static final DateTimeFormatter CLOCK_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void render(PhoneHomeScreen phoneScreen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderClock(phoneScreen, graphics);
        RenderSystem.texParameter(3553, 10241, 9728);
        RenderSystem.texParameter(3553, 10240, 9728);
        graphics.blit(VOICE_CHAT_ICON, APP_ICON_X, APP_ICON_Y, APP_ICON_WIDTH, APP_ICON_HEIGHT, 0, 0, 48, 52, 48, 52);
        renderUnreadBadge(phoneScreen, graphics);
    }

    @Override
    public boolean mouseClicked(PhoneHomeScreen phoneScreen, double localX, double localY, int button) {
        if (button == 0 && isOverAppIcon(localX, localY)) {
            phoneScreen.openWindow(new MessagingWindow());
            return true;
        }

        return false;
    }

    private void renderClock(PhoneHomeScreen phoneScreen, GuiGraphics graphics) {
        Font font = phoneScreen.getPhoneFont();
        String clockText = LocalTime.now(JAKARTA_ZONE).format(CLOCK_FORMATTER);
        int clockWidth = font.width(clockText);

        graphics.pose().pushPose();
        graphics.pose().translate(TEXTURE_WIDTH / 2.0F, CLOCK_Y, 0);
        graphics.pose().scale(CLOCK_SCALE, CLOCK_SCALE, 1.0F);
        graphics.drawString(font, clockText, -clockWidth / 2, 0, CLOCK_COLOR, false);
        graphics.pose().popPose();
    }

    private boolean isOverAppIcon(double localX, double localY) {
        return localX >= APP_ICON_X && localX <= APP_ICON_X + APP_ICON_WIDTH
                && localY >= APP_ICON_Y && localY <= APP_ICON_Y + APP_ICON_HEIGHT;
    }

    private void renderUnreadBadge(PhoneHomeScreen phoneScreen, GuiGraphics graphics) {
        int unreadCount = phoneScreen.getTotalUnreadMessages();
        if (unreadCount <= 0) {
            return;
        }

        Font font = phoneScreen.getPhoneFont();
        String text = unreadCount > 99 ? "99+" : Integer.toString(unreadCount);
        int badgeWidth = Math.max(10, font.width(text) + 4);
        int badgeX = APP_ICON_X + APP_ICON_WIDTH - badgeWidth + 3;
        int badgeY = APP_ICON_Y - 2;
        graphics.fill(badgeX, badgeY, badgeX + badgeWidth, badgeY + 10, UNREAD_BADGE_COLOR);
        graphics.drawString(font, text, badgeX + (badgeWidth - font.width(text)) / 2, badgeY + 1, UNREAD_BADGE_TEXT_COLOR, false);
    }
}
