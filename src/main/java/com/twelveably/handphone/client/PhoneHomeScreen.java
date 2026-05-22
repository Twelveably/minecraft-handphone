package com.twelveably.handphone.client;

import com.twelveably.handphone.client.phone.HomeWindow;
import com.twelveably.handphone.client.phone.CallWindow;
import com.twelveably.handphone.client.phone.ChatWindow;
import com.twelveably.handphone.client.phone.PhoneWindow;
import com.twelveably.handphone.Handphone;
import com.twelveably.handphone.network.AddContactPacket;
import com.twelveably.handphone.network.ChatMessagePacket;
import com.twelveably.handphone.network.HandphoneNetwork;
import com.twelveably.handphone.network.RequestContactsPacket;
import com.twelveably.handphone.network.SyncCallStatePacket;
import com.twelveably.handphone.phone.PhoneContactData;
import com.twelveably.handphone.phone.PhoneChatMessageData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PhoneHomeScreen extends Screen {
    private static final ResourceLocation HOME_SCREEN = new ResourceLocation(Handphone.MODID, "textures/gui/homescreen.png");
    private static final int TEXTURE_WIDTH = 141;
    private static final int TEXTURE_HEIGHT = 225;
    private static final int SCREEN_MARGIN = 16;
    private static final float MAX_IMAGE_SCALE = 1F;
    private static final long OPEN_DURATION_MS = 260L;
    private static final long CLOSE_DURATION_MS = 220L;

    private int imageX;
    private int imageY;
    private int imageWidth;
    private int imageHeight;
    private float imageScale;
    private long openedAt;
    private long closingStartedAt;
    private boolean closing;
    private PhoneWindow currentWindow = new HomeWindow();
    private String activePhoneNumber;
    private final List<PhoneContact> contacts = new ArrayList<>();
    private final Map<String, List<ChatMessage>> chatMessagesByNumber = new HashMap<>();
    private final Map<String, Integer> unreadMessagesByNumber = new HashMap<>();

    public PhoneHomeScreen(String activePhoneNumber) {
        super(Component.translatable("screen.handphone.home"));
        this.activePhoneNumber = activePhoneNumber;
        openedAt = Util.getMillis();
        SyncCallStatePacket.State callState = ClientPhoneHooks.getCurrentCallState(activePhoneNumber);
        if (callState != SyncCallStatePacket.State.NONE) {
            currentWindow = new CallWindow(new PhoneContact(
                    ClientPhoneHooks.getCurrentCallContactName(activePhoneNumber),
                    ClientPhoneHooks.getCurrentCallPhoneNumber(activePhoneNumber)
            ), callState, ClientPhoneHooks.getCurrentCallStartedAtMillis(activePhoneNumber), ClientPhoneHooks.getCurrentCallProfileId(activePhoneNumber));
        }
        HandphoneNetwork.CHANNEL.sendToServer(new RequestContactsPacket(activePhoneNumber));
    }

    @Override
    protected void init() {
        imageScale = Math.min(MAX_IMAGE_SCALE, Math.min((width - 24) / (float) TEXTURE_WIDTH, (height - 24) / (float) TEXTURE_HEIGHT));
        imageWidth = Math.round(TEXTURE_WIDTH * imageScale);
        imageHeight = Math.round(TEXTURE_HEIGHT * imageScale);

        imageX = width - imageWidth - SCREEN_MARGIN;
        imageY = height - imageHeight - SCREEN_MARGIN;
    }

    @Override
    public void tick() {
        if (closing && Util.getMillis() - closingStartedAt >= CLOSE_DURATION_MS && minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float easedProgress = getEasedAnimationProgress(Util.getMillis());
        int currentY = getAnimatedPhoneY(easedProgress);
        int backgroundAlpha = (int) (0x99 * (closing ? 1.0F - easedProgress : easedProgress));

        graphics.fill(0, 0, width, height, backgroundAlpha << 24);

        graphics.pose().pushPose();
        graphics.pose().translate(imageX, currentY, 0);
        graphics.pose().scale(imageScale, imageScale, 1.0F);
        graphics.blit(HOME_SCREEN, 0, 0, 0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        int localMouseX = Math.round((mouseX - imageX) / imageScale);
        int localMouseY = Math.round((mouseY - currentY) / imageScale);
        currentWindow.render(this, graphics, localMouseX, localMouseY, partialTick);
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && !closing && isInsidePhone(mouseX, mouseY)) {
            double localX = (mouseX - imageX) / imageScale;
            double localY = (mouseY - getCurrentPhoneY()) / imageScale;
            if (currentWindow.mouseClicked(this, localX, localY, button)) {
                return true;
            }
        }

        if (button == 0 && !closing && isOutsidePhone(mouseX, mouseY)) {
            startClosing();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (!closing && isInsidePhone(mouseX, mouseY)) {
            double localX = (mouseX - imageX) / imageScale;
            double localY = (mouseY - getCurrentPhoneY()) / imageScale;
            if (currentWindow.mouseScrolled(this, localX, localY, scrollDelta)) {
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, scrollDelta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!closing && currentWindow.keyPressed(this, keyCode, scanCode, modifiers)) {
            return true;
        }

        if (keyCode == 256 && !closing) {
            startClosing();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!closing && currentWindow.charTyped(this, codePoint, modifiers)) {
            return true;
        }

        return super.charTyped(codePoint, modifiers);
    }

    public void openWindow(PhoneWindow window) {
        currentWindow = window;
    }

    public void openChat(PhoneContact contact) {
        clearUnreadMessages(contact.getPhoneNumber());
        currentWindow = new ChatWindow(contact);
    }

    public Font getPhoneFont() {
        return font;
    }

    public List<PhoneContact> getContacts() {
        return contacts;
    }

    public String getActivePhoneNumber() {
        return activePhoneNumber;
    }

    public void setActivePhoneNumber(String activePhoneNumber) {
        if (!activePhoneNumber.isBlank()) {
            this.activePhoneNumber = activePhoneNumber;
        }
    }

    public void addContact(String phoneNumber) {
        addContact("New Contact", phoneNumber);
    }

    public void addContact(String name, String phoneNumber) {
        HandphoneNetwork.CHANNEL.sendToServer(new AddContactPacket(activePhoneNumber, name, phoneNumber));
    }

    public void setContacts(List<PhoneContactData> syncedContacts) {
        contacts.clear();
        for (PhoneContactData contact : syncedContacts) {
            contacts.add(new PhoneContact(contact.name(), contact.phoneNumber(), contact.latestMessage(), contact.latestMessageTimeMillis(), contact.profileId()));
            if (contact.unreadCount() > 0) {
                unreadMessagesByNumber.put(contact.phoneNumber(), contact.unreadCount());
            } else {
                unreadMessagesByNumber.remove(contact.phoneNumber());
            }
        }
        sortContactsByLatestMessage();
    }

    public List<ChatMessage> getChatMessages(String phoneNumber) {
        return chatMessagesByNumber.computeIfAbsent(phoneNumber, ignored -> new ArrayList<>());
    }

    public void sendChatMessage(PhoneContact contact, String message) {
        HandphoneNetwork.CHANNEL.sendToServer(new ChatMessagePacket(activePhoneNumber, contact.getName(), contact.getPhoneNumber(), message));
    }

    public void addChatMessage(String phoneNumber, String message, boolean outgoing) {
        addChatMessage(phoneNumber, message, outgoing, System.currentTimeMillis());
    }

    public void addChatMessage(String phoneNumber, String message, boolean outgoing, long timestampMillis) {
        ensureConversationContact(phoneNumber, phoneNumber);
        getChatMessages(phoneNumber).add(new ChatMessage(message, outgoing, timestampMillis));
        if (!outgoing && !isViewingChat(phoneNumber)) {
            unreadMessagesByNumber.merge(phoneNumber, 1, Integer::sum);
        }
        updateLatestMessage(phoneNumber, message, timestampMillis);
    }

    public void setChatMessages(String phoneNumber, List<PhoneChatMessageData> messages) {
        ensureConversationContact(phoneNumber, phoneNumber);
        List<ChatMessage> chatMessages = getChatMessages(phoneNumber);
        chatMessages.clear();
        for (PhoneChatMessageData message : messages) {
            chatMessages.add(new ChatMessage(message.message(), message.outgoing(), message.timestampMillis()));
        }

        if (!messages.isEmpty()) {
            PhoneChatMessageData latestMessage = messages.get(messages.size() - 1);
            updateLatestMessage(phoneNumber, latestMessage.message(), latestMessage.timestampMillis());
        }
    }

    public void updateLatestMessage(String phoneNumber, String latestMessage) {
        updateLatestMessage(phoneNumber, latestMessage, System.currentTimeMillis());
    }

    public void updateLatestMessage(String phoneNumber, String latestMessage, long timestampMillis) {
        for (PhoneContact contact : contacts) {
            if (contact.getPhoneNumber().equals(phoneNumber)) {
                contact.setLatestMessage(latestMessage);
                contact.setLatestMessageTimeMillis(timestampMillis);
                sortContactsByLatestMessage();
                return;
            }
        }

        PhoneContact contact = new PhoneContact(phoneNumber, phoneNumber, latestMessage, timestampMillis, "");
        contacts.add(contact);
        sortContactsByLatestMessage();
    }

    public void renameContact(String phoneNumber, String name) {
        for (PhoneContact contact : contacts) {
            if (contact.getPhoneNumber().equals(phoneNumber)) {
                contact.setName(name);
                return;
            }
        }
    }

    public int getUnreadMessages(String phoneNumber) {
        return unreadMessagesByNumber.getOrDefault(phoneNumber, 0);
    }

    public int getTotalUnreadMessages() {
        int total = 0;
        for (int count : unreadMessagesByNumber.values()) {
            total += count;
        }

        return total;
    }

    private void clearUnreadMessages(String phoneNumber) {
        unreadMessagesByNumber.remove(phoneNumber);
    }

    public boolean isViewingChat(String phoneNumber) {
        return currentWindow instanceof ChatWindow chatWindow && chatWindow.isChatFor(phoneNumber);
    }

    private void sortContactsByLatestMessage() {
        contacts.sort(Comparator
                .comparingLong(PhoneContact::getLatestMessageTimeMillis)
                .reversed()
                .thenComparing(PhoneContact::getName, String.CASE_INSENSITIVE_ORDER));
    }

    public String getContactName(String phoneNumber, String fallbackName) {
        for (PhoneContact contact : contacts) {
            if (contact.getPhoneNumber().equals(phoneNumber)) {
                return contact.getName();
            }
        }

        return fallbackName;
    }

    private void ensureConversationContact(String phoneNumber, String fallbackName) {
        for (PhoneContact contact : contacts) {
            if (contact.getPhoneNumber().equals(phoneNumber)) {
                return;
            }
        }

        contacts.add(new PhoneContact(fallbackName, phoneNumber, "", 0L, ""));
        sortContactsByLatestMessage();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean isInsidePhone(double mouseX, double mouseY) {
        return !isOutsidePhone(mouseX, mouseY);
    }

    private boolean isOutsidePhone(double mouseX, double mouseY) {
        return mouseX < imageX || mouseX > imageX + imageWidth || mouseY < imageY || mouseY > imageY + imageHeight;
    }

    private void startClosing() {
        closing = true;
        closingStartedAt = Util.getMillis();
    }

    public String getMinecraftClockText() {
        if (minecraft == null || minecraft.level == null) {
            return "--:--";
        }

        long dayTime = minecraft.level.getDayTime() % 24000L;
        int hour = (int) ((dayTime / 1000L + 6L) % 24L);
        int minute = (int) ((dayTime % 1000L) * 60L / 1000L);
        return String.format(Locale.ROOT, "%02d:%02d", hour, minute);
    }

    private static float progress(long elapsedMs, long durationMs) {
        return Math.min(1.0F, elapsedMs / (float) durationMs);
    }

    private static float easeOutCubic(float progress) {
        float inverse = 1.0F - progress;
        return 1.0F - inverse * inverse * inverse;
    }

    private static float easeInCubic(float progress) {
        return progress * progress * progress;
    }

    private static int lerp(int start, int end, float progress) {
        return Math.round(start + (end - start) * progress);
    }

    private float getEasedAnimationProgress(long now) {
        float animationProgress = closing
                ? progress(now - closingStartedAt, CLOSE_DURATION_MS)
                : progress(now - openedAt, OPEN_DURATION_MS);
        return closing ? easeInCubic(animationProgress) : easeOutCubic(animationProgress);
    }

    private int getAnimatedPhoneY(float easedProgress) {
        int hiddenY = height + 12;
        return closing
                ? lerp(imageY, hiddenY, easedProgress)
                : lerp(hiddenY, imageY, easedProgress);
    }

    private int getCurrentPhoneY() {
        return getAnimatedPhoneY(getEasedAnimationProgress(Util.getMillis()));
    }

    public static class PhoneContact {
        private String name;
        private final String phoneNumber;
        private String latestMessage;
        private long latestMessageTimeMillis;
        private String profileId;

        public PhoneContact(String name, String phoneNumber) {
            this(name, phoneNumber, "", 0L, "");
        }

        public PhoneContact(String name, String phoneNumber, String latestMessage, long latestMessageTimeMillis) {
            this(name, phoneNumber, latestMessage, latestMessageTimeMillis, "");
        }

        public PhoneContact(String name, String phoneNumber, String latestMessage, long latestMessageTimeMillis, String profileId) {
            this.name = name;
            this.phoneNumber = phoneNumber;
            this.latestMessage = latestMessage;
            this.latestMessageTimeMillis = latestMessageTimeMillis;
            this.profileId = profileId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public String getLatestMessage() {
            return latestMessage;
        }

        public void setLatestMessage(String latestMessage) {
            this.latestMessage = latestMessage;
        }

        public long getLatestMessageTimeMillis() {
            return latestMessageTimeMillis;
        }

        public void setLatestMessageTimeMillis(long latestMessageTimeMillis) {
            this.latestMessageTimeMillis = latestMessageTimeMillis;
        }

        public String getProfileId() {
            return profileId;
        }

        public void setProfileId(String profileId) {
            this.profileId = profileId;
        }
    }

    public record ChatMessage(String text, boolean outgoing, long timestampMillis) {
    }
}
