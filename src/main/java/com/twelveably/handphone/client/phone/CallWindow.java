package com.twelveably.handphone.client.phone;

import com.mojang.blaze3d.systems.RenderSystem;
import com.twelveably.handphone.Handphone;
import com.twelveably.handphone.client.ClientPhoneHooks;
import com.twelveably.handphone.client.ClientProfilePictures;
import com.twelveably.handphone.client.PhoneHomeScreen;
import com.twelveably.handphone.network.ContactActionPacket;
import com.twelveably.handphone.network.HandphoneNetwork;
import com.twelveably.handphone.network.RespondCallPacket;
import com.twelveably.handphone.network.SyncCallStatePacket;
import com.twelveably.handphone.network.ToggleLoudspeakerPacket;
import com.twelveably.handphone.network.ToggleMutePacket;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class CallWindow implements PhoneWindow {
    private static final ResourceLocation CALL_WINDOW = new ResourceLocation(Handphone.MODID, "textures/gui/call_window.png");
    private static final ResourceLocation HANGUP_BUTTON = new ResourceLocation(Handphone.MODID, "textures/gui/buttons/hangup.png");
    private static final ResourceLocation ANSWER_BUTTON = new ResourceLocation(Handphone.MODID, "textures/gui/buttons/answer.png");
    private static final int WINDOW_WIDTH = 141;
    private static final int WINDOW_HEIGHT = 225;
    private static final int CALL_WINDOW_TEXTURE_WIDTH = 200;
    private static final int CALL_WINDOW_TEXTURE_HEIGHT = 320;
    private static final int HEAD_X = 51;
    private static final int HEAD_Y = 85;
    private static final int HEAD_SIZE = 39;
    private static final int NAME_Y = 133;
    private static final int TIMER_Y = 147;
    private static final int SPEAKER_Y = 158;
    private static final int TEXT_COLOR = 0xFF20242A;
    private static final int SPEAKER_COLOR = 0xFF0B7F22;
    private static final int SINGLE_BUTTON_X = 58;
    private static final int MUTE_BUTTON_X = 34;
    private static final int SPEAKER_BUTTON_X = 82;
    private static final int ACCEPT_BUTTON_X = 43;
    private static final int DECLINE_BUTTON_X = 78;
    private static final int HANGUP_BUTTON_Y = 164;
    private static final int HANGUP_BUTTON_SIZE = 20;
    private static final String ACCEPT_TEXT = "";
    private static final String DECLINE_TEXT = "";
    private static final int BUTTON_TEXT_COLOR = 0xFFFFFFFF;

    private final PhoneHomeScreen.PhoneContact contact;
    private final SyncCallStatePacket.State state;
    private final long startedAtMillis;
    private final String remoteProfileId;

    public CallWindow(PhoneHomeScreen.PhoneContact contact, SyncCallStatePacket.State state) {
        this(contact, state, System.currentTimeMillis(), "");
    }

    public CallWindow(PhoneHomeScreen.PhoneContact contact, SyncCallStatePacket.State state, long startedAtMillis) {
        this(contact, state, startedAtMillis, "");
    }

    public CallWindow(PhoneHomeScreen.PhoneContact contact, SyncCallStatePacket.State state, long startedAtMillis, String remoteProfileId) {
        this.contact = contact;
        this.state = state;
        this.startedAtMillis = startedAtMillis > 0L ? startedAtMillis : System.currentTimeMillis();
        this.remoteProfileId = remoteProfileId;
    }

    @Override
    public void render(PhoneHomeScreen phoneScreen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RenderSystem.texParameter(3553, 10241, 9728);
        RenderSystem.texParameter(3553, 10240, 9728);

        graphics.blit(CALL_WINDOW, 0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, 0, 0, CALL_WINDOW_TEXTURE_WIDTH, CALL_WINDOW_TEXTURE_HEIGHT, CALL_WINDOW_TEXTURE_WIDTH, CALL_WINDOW_TEXTURE_HEIGHT);
        Font font = phoneScreen.getPhoneFont();
        renderAvatar(graphics, font);
        renderCenteredText(graphics, font, limitName(contact.getName()), NAME_Y);
        renderCenteredText(graphics, font, getStatusText(), TIMER_Y);
        if (ClientPhoneHooks.isLoudspeakerEnabled(phoneScreen.getActivePhoneNumber())) {
            renderCenteredText(graphics, font, "Speaker", SPEAKER_Y, SPEAKER_COLOR);
        }
        renderButtons(graphics, font);
    }

    @Override
    public boolean mouseClicked(PhoneHomeScreen phoneScreen, double localX, double localY, int button) {
        if (button == 0 && state == SyncCallStatePacket.State.INCOMING_RINGING && isOverButton(localX, localY, ACCEPT_BUTTON_X)) {
            HandphoneNetwork.CHANNEL.sendToServer(new RespondCallPacket(phoneScreen.getActivePhoneNumber(), true));
            return true;
        }

        if (button == 0 && state == SyncCallStatePacket.State.INCOMING_RINGING && isOverButton(localX, localY, DECLINE_BUTTON_X)) {
            HandphoneNetwork.CHANNEL.sendToServer(new RespondCallPacket(phoneScreen.getActivePhoneNumber(), false));
            phoneScreen.openWindow(new MessagingWindow());
            return true;
        }

        if (button == 0 && state == SyncCallStatePacket.State.ACTIVE && isOverButton(localX, localY, MUTE_BUTTON_X)) {
            HandphoneNetwork.CHANNEL.sendToServer(new ToggleMutePacket(phoneScreen.getActivePhoneNumber()));
            return true;
        }

        if (button == 0 && state == SyncCallStatePacket.State.ACTIVE && isOverButton(localX, localY, SPEAKER_BUTTON_X)) {
            HandphoneNetwork.CHANNEL.sendToServer(new ToggleLoudspeakerPacket(phoneScreen.getActivePhoneNumber()));
            return true;
        }

        if (button == 0 && isOverButton(localX, localY, SINGLE_BUTTON_X)) {
            HandphoneNetwork.CHANNEL.sendToServer(new ContactActionPacket(phoneScreen.getActivePhoneNumber(), ContactActionPacket.Action.CALL, contact.getName(), contact.getPhoneNumber()));
            phoneScreen.openWindow(new MessagingWindow());
            return true;
        }

        return false;
    }

    private void renderAvatar(GuiGraphics graphics, Font font) {
        ClientProfilePictures.drawHead(graphics, font, remoteProfileId, contact.getPhoneNumber(), HEAD_X, HEAD_Y, HEAD_SIZE);
    }

    private void renderCenteredText(GuiGraphics graphics, Font font, String text, int y) {
        renderCenteredText(graphics, font, text, y, TEXT_COLOR);
    }

    private void renderCenteredText(GuiGraphics graphics, Font font, String text, int y, int color) {
        int x = (WINDOW_WIDTH - font.width(text)) / 2;
        graphics.drawString(font, text, x, y, color, false);
    }

    private void renderButtons(GuiGraphics graphics, Font font) {
        if (state == SyncCallStatePacket.State.INCOMING_RINGING) {
            graphics.blit(ANSWER_BUTTON, ACCEPT_BUTTON_X, HANGUP_BUTTON_Y, HANGUP_BUTTON_SIZE, HANGUP_BUTTON_SIZE, 0, 0, 28, 28, 28, 28);
            graphics.blit(HANGUP_BUTTON, DECLINE_BUTTON_X, HANGUP_BUTTON_Y, HANGUP_BUTTON_SIZE, HANGUP_BUTTON_SIZE, 0, 0, 28, 28, 28, 28);
            renderButtonText(graphics, font, ACCEPT_TEXT, ACCEPT_BUTTON_X);
            renderButtonText(graphics, font, DECLINE_TEXT, DECLINE_BUTTON_X);
            return;
        }

        if (state == SyncCallStatePacket.State.ACTIVE) {
            graphics.blit(HANGUP_BUTTON, MUTE_BUTTON_X, HANGUP_BUTTON_Y, HANGUP_BUTTON_SIZE, HANGUP_BUTTON_SIZE, 0, 0, 28, 28, 28, 28);
            graphics.blit(HANGUP_BUTTON, SINGLE_BUTTON_X, HANGUP_BUTTON_Y, HANGUP_BUTTON_SIZE, HANGUP_BUTTON_SIZE, 0, 0, 28, 28, 28, 28);
            graphics.blit(HANGUP_BUTTON, SPEAKER_BUTTON_X, HANGUP_BUTTON_Y, HANGUP_BUTTON_SIZE, HANGUP_BUTTON_SIZE, 0, 0, 28, 28, 28, 28);
            renderButtonText(graphics, font, ClientPhoneHooks.isMuted(ClientPhoneHooks.getOpenPhoneNumber()) ? "M" : "m", MUTE_BUTTON_X);
            renderButtonText(graphics, font, DECLINE_TEXT, SINGLE_BUTTON_X);
            renderButtonText(graphics, font, ClientPhoneHooks.isLoudspeakerEnabled(ClientPhoneHooks.getOpenPhoneNumber()) ? "S" : "s", SPEAKER_BUTTON_X);
            return;
        }

        graphics.blit(HANGUP_BUTTON, SINGLE_BUTTON_X, HANGUP_BUTTON_Y, HANGUP_BUTTON_SIZE, HANGUP_BUTTON_SIZE, 0, 0, 28, 28, 28, 28);
        renderButtonText(graphics, font, DECLINE_TEXT, SINGLE_BUTTON_X);
    }

    private void renderButtonText(GuiGraphics graphics, Font font, String text, int buttonX) {
        int textX = buttonX + (HANGUP_BUTTON_SIZE - font.width(text)) / 2;
        int textY = HANGUP_BUTTON_Y + 6;
        graphics.drawString(font, text, textX, textY, BUTTON_TEXT_COLOR, false);
    }

    private String getStatusText() {
        if (state == SyncCallStatePacket.State.OUTGOING_CALLING) {
            return "Calling...";
        }

        if (state == SyncCallStatePacket.State.OUTGOING_RINGING) {
            long elapsedMillis = Math.max(0L, System.currentTimeMillis() - startedAtMillis);
            return elapsedMillis < 3000L ? "Calling..." : "Ringing...";
        }

        if (state == SyncCallStatePacket.State.INCOMING_RINGING) {
            return "Incoming call...";
        }

        long elapsedSeconds = Math.max(0L, (System.currentTimeMillis() - startedAtMillis) / 1000L);
        long minutes = elapsedSeconds / 60L;
        long seconds = elapsedSeconds % 60L;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private static String limitName(String name) {
        return name.length() <= 7 ? name : name.substring(0, 7);
    }

    private boolean isOverButton(double localX, double localY, int buttonX) {
        return localX >= buttonX && localX <= buttonX + HANGUP_BUTTON_SIZE
                && localY >= HANGUP_BUTTON_Y && localY <= HANGUP_BUTTON_Y + HANGUP_BUTTON_SIZE;
    }
}
