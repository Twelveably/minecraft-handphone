package com.twelveably.handphone.client.phone;

import com.mojang.blaze3d.systems.RenderSystem;
import com.twelveably.handphone.Handphone;
import com.twelveably.handphone.client.PhoneHomeScreen;
import com.twelveably.handphone.network.ContactActionPacket;
import com.twelveably.handphone.network.HandphoneNetwork;
import com.twelveably.handphone.network.RenameContactPacket;
import com.twelveably.handphone.network.RequestChatMessagesPacket;
import com.twelveably.handphone.network.SyncCallStatePacket;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class ChatWindow implements PhoneWindow {
    private static final ResourceLocation CHAT_WINDOW = new ResourceLocation(Handphone.MODID, "textures/gui/chat_window.png");
    private static final ResourceLocation CALL_BUTTON = new ResourceLocation(Handphone.MODID, "textures/gui/buttons/call.png");
    private static final ResourceLocation SEND_BUTTON = new ResourceLocation(Handphone.MODID, "textures/gui/buttons/add.png");
    private static final int WINDOW_WIDTH = 141;
    private static final int WINDOW_HEIGHT = 225;
    private static final int CHAT_WINDOW_TEXTURE_WIDTH = 200;
    private static final int CHAT_WINDOW_TEXTURE_HEIGHT = 320;
    private static final int BACK_BUTTON_X = 13;
    private static final int BACK_BUTTON_Y = 26;
    private static final int BACK_BUTTON_WIDTH = 30;
    private static final int BACK_BUTTON_HEIGHT = 14;
    private static final String BACK_BUTTON_TEXT = "Back";
    private static final int CALL_BUTTON_X = 96;
    private static final int CALL_BUTTON_Y = 26;
    private static final int CALL_BUTTON_WIDTH = 30;
    private static final int CALL_BUTTON_HEIGHT = 14;
    private static final String CALL_BUTTON_TEXT = "Call";
    private static final int CONTACT_NAME_Y = 30;
    private static final int CONTACT_NAME_COLOR = 0xFFFFFFFF;
    private static final int CONTACT_NAME_EDIT_COLOR = 0xFF0B7F22;
    private static final int CONTACT_NAME_MAX_WIDTH = 48;
    private static final int MESSAGE_TOP_Y = 45;
    private static final int MESSAGE_BOTTOM_Y = 186;
    private static final int MESSAGE_GAP = 4;
    private static final int MESSAGE_LINE_HEIGHT = 9;
    private static final int MESSAGE_PADDING_X = 4;
    private static final int MESSAGE_PADDING_Y = 3;
    private static final int MESSAGE_TEXT_WIDTH = 82;
    private static final int MESSAGE_BUBBLE_MAX_WIDTH = 94;
    private static final int MESSAGE_BUBBLE_MIN_WIDTH = 18;
    private static final int INCOMING_MESSAGE_X = 15;
    private static final int OUTGOING_MESSAGE_RIGHT_X = 126;
    private static final int INCOMING_BUBBLE_COLOR = 0xFFFFFFFF;
    private static final int OUTGOING_BUBBLE_COLOR = 0xFFD7F5DF;
    private static final int CALL_LOG_BUBBLE_COLOR = 0xFFE9EDF2;
    private static final int INCOMING_MESSAGE_COLOR = 0xFF20242A;
    private static final int OUTGOING_MESSAGE_COLOR = 0xFF0B7F22;
    private static final int CALL_LOG_TEXT_COLOR = 0xFF5F6873;
    private static final int INPUT_X = 15;
    private static final int INPUT_Y = 192;
    private static final int INPUT_WIDTH = 78;
    private static final int INPUT_HEIGHT = 17;
    private static final int INPUT_TEXT_X = 19;
    private static final int INPUT_TEXT_Y = 196;
    private static final int INPUT_BORDER_COLOR = 0xFF2B3038;
    private static final int INPUT_BACKGROUND_COLOR = 0xFFD8E4F7;
    private static final int INPUT_TEXT_COLOR = 0xFF20242A;
    private static final int CURSOR_COLOR = 0xFF20242A;
    private static final int SEND_BUTTON_X = 97;
    private static final int SEND_BUTTON_Y = 192;
    private static final int SEND_BUTTON_WIDTH = 30;
    private static final int SEND_BUTTON_HEIGHT = 17;
    private static final String SEND_BUTTON_TEXT = "Send";
    private static final int SEND_BUTTON_TEXT_COLOR = 0xFFFFFFFF;
    private static final int MAX_INPUT_WIDTH = INPUT_WIDTH - 8;
    private static final int MAX_MESSAGE_LENGTH = 64;
    private static final int KEY_BACKSPACE = 259;
    private static final int KEY_ENTER = 257;
    private static final int KEY_NUMPAD_ENTER = 335;

    private final PhoneHomeScreen.PhoneContact contact;
    private final StringBuilder messageInput = new StringBuilder();
    private final StringBuilder nameInput = new StringBuilder();
    private boolean inputFocused = true;
    private boolean nameFocused;
    private boolean requestedMessages;
    private int messageScrollOffset;

    public ChatWindow(PhoneHomeScreen.PhoneContact contact) {
        this.contact = contact;
    }

    public boolean isChatFor(String phoneNumber) {
        return contact.getPhoneNumber().equals(phoneNumber);
    }

    @Override
    public void render(PhoneHomeScreen phoneScreen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RenderSystem.texParameter(3553, 10241, 9728);
        RenderSystem.texParameter(3553, 10240, 9728);

        graphics.blit(CHAT_WINDOW, 0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, 0, 0, CHAT_WINDOW_TEXTURE_WIDTH, CHAT_WINDOW_TEXTURE_HEIGHT, CHAT_WINDOW_TEXTURE_WIDTH, CHAT_WINDOW_TEXTURE_HEIGHT);
        graphics.blit(CALL_BUTTON, BACK_BUTTON_X, BACK_BUTTON_Y, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT, 0, 0, 56, 24, 56, 24);
        renderButtonText(graphics, phoneScreen.getPhoneFont(), BACK_BUTTON_TEXT, BACK_BUTTON_X, BACK_BUTTON_Y, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT);
        graphics.blit(CALL_BUTTON, CALL_BUTTON_X, CALL_BUTTON_Y, CALL_BUTTON_WIDTH, CALL_BUTTON_HEIGHT, 0, 0, 56, 24, 56, 24);
        renderButtonText(graphics, phoneScreen.getPhoneFont(), CALL_BUTTON_TEXT, CALL_BUTTON_X, CALL_BUTTON_Y, CALL_BUTTON_WIDTH, CALL_BUTTON_HEIGHT);
        renderHeader(phoneScreen, graphics);
        requestMessagesIfNeeded(phoneScreen);
        renderMessages(phoneScreen, graphics);
        renderInput(phoneScreen, graphics);
        graphics.blit(SEND_BUTTON, SEND_BUTTON_X, SEND_BUTTON_Y, SEND_BUTTON_WIDTH, SEND_BUTTON_HEIGHT, 0, 0, 56, 24, 56, 24);
        renderButtonText(graphics, phoneScreen.getPhoneFont(), SEND_BUTTON_TEXT, SEND_BUTTON_X, SEND_BUTTON_Y, SEND_BUTTON_WIDTH, SEND_BUTTON_HEIGHT);
    }

    @Override
    public boolean mouseClicked(PhoneHomeScreen phoneScreen, double localX, double localY, int button) {
        if (button == 0 && isOverBackButton(localX, localY)) {
            finishNameEdit(phoneScreen);
            phoneScreen.openWindow(new MessagingWindow());
            return true;
        }

        if (button == 0 && isOverCallButton(localX, localY)) {
            finishNameEdit(phoneScreen);
            HandphoneNetwork.CHANNEL.sendToServer(new ContactActionPacket(phoneScreen.getActivePhoneNumber(), ContactActionPacket.Action.CALL, contact.getName(), contact.getPhoneNumber()));
            phoneScreen.openWindow(new CallWindow(contact, SyncCallStatePacket.State.OUTGOING_RINGING));
            return true;
        }

        if (button == 0 && isOverContactName(localX, localY)) {
            nameFocused = true;
            inputFocused = false;
            nameInput.setLength(0);
            nameInput.append(contact.getName());
            return true;
        }

        if (button == 0 && isOverInput(localX, localY)) {
            finishNameEdit(phoneScreen);
            inputFocused = true;
            return true;
        }

        if (button == 0 && isOverSendButton(localX, localY)) {
            finishNameEdit(phoneScreen);
            sendMessage(phoneScreen);
            return true;
        }

        if (button == 0) {
            finishNameEdit(phoneScreen);
            inputFocused = false;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(PhoneHomeScreen phoneScreen, double localX, double localY, double scrollDelta) {
        if (!isOverMessages(localX, localY)) {
            return false;
        }

        int direction = scrollDelta > 0.0D ? 1 : -1;
        messageScrollOffset += direction;
        clampMessageScroll(phoneScreen);
        return true;
    }

    @Override
    public boolean keyPressed(PhoneHomeScreen phoneScreen, int keyCode, int scanCode, int modifiers) {
        if (nameFocused) {
            if (keyCode == KEY_ENTER || keyCode == KEY_NUMPAD_ENTER) {
                finishNameEdit(phoneScreen);
                return true;
            }

            if (keyCode == KEY_BACKSPACE && nameInput.length() > 0) {
                nameInput.deleteCharAt(nameInput.length() - 1);
                return true;
            }

            return keyCode == KEY_BACKSPACE;
        }

        if (!inputFocused) {
            return false;
        }

        if ((keyCode == KEY_ENTER || keyCode == KEY_NUMPAD_ENTER) && messageInput.length() > 0) {
            sendMessage(phoneScreen);
            return true;
        }

        if (keyCode == KEY_BACKSPACE && messageInput.length() > 0) {
            messageInput.deleteCharAt(messageInput.length() - 1);
            return true;
        }

        return keyCode == KEY_BACKSPACE;
    }

    @Override
    public boolean charTyped(PhoneHomeScreen phoneScreen, char codePoint, int modifiers) {
        if (nameFocused) {
            if (Character.isISOControl(codePoint)) {
                return false;
            }

            if (phoneScreen.getPhoneFont().width(nameInput.toString() + codePoint) <= CONTACT_NAME_MAX_WIDTH) {
                nameInput.append(codePoint);
            }
            return true;
        }

        if (!inputFocused || Character.isISOControl(codePoint) || messageInput.length() >= MAX_MESSAGE_LENGTH) {
            return false;
        }

        messageInput.append(codePoint);
        return true;
    }

    private void renderHeader(PhoneHomeScreen phoneScreen, GuiGraphics graphics) {
        Font font = phoneScreen.getPhoneFont();
        String name = nameFocused ? nameInput.toString() : contact.getName();
        String visibleName = fitText(font, name, CONTACT_NAME_MAX_WIDTH);
        int contactNameX = (WINDOW_WIDTH - font.width(visibleName)) / 2;
        graphics.drawString(font, visibleName, contactNameX, CONTACT_NAME_Y, nameFocused ? CONTACT_NAME_EDIT_COLOR : CONTACT_NAME_COLOR, false);
        if (nameFocused && (System.currentTimeMillis() / 450L) % 2L == 0L) {
            int cursorX = contactNameX + font.width(visibleName) + 1;
            graphics.fill(cursorX, CONTACT_NAME_Y - 1, cursorX + 1, CONTACT_NAME_Y + 9, CONTACT_NAME_EDIT_COLOR);
        }
    }

    private void renderMessages(PhoneHomeScreen phoneScreen, GuiGraphics graphics) {
        Font font = phoneScreen.getPhoneFont();
        List<PhoneHomeScreen.ChatMessage> messages = phoneScreen.getChatMessages(contact.getPhoneNumber());
        clampMessageScroll(phoneScreen);

        int messageIndex = messages.size() - 1 - messageScrollOffset;
        int nextBubbleBottom = MESSAGE_BOTTOM_Y;
        while (messageIndex >= 0 && nextBubbleBottom > MESSAGE_TOP_Y) {
            PhoneHomeScreen.ChatMessage message = messages.get(messageIndex);
            List<String> lines = wrapMessage(font, message.text());
            int bubbleWidth = getBubbleWidth(font, lines);
            int bubbleHeight = lines.size() * MESSAGE_LINE_HEIGHT + MESSAGE_PADDING_Y * 2;
            int bubbleY = nextBubbleBottom - bubbleHeight;
            if (bubbleY < MESSAGE_TOP_Y) {
                break;
            }

            int bubbleX = isCallLogMessage(message.text()) ? (WINDOW_WIDTH - bubbleWidth) / 2 : message.outgoing() ? OUTGOING_MESSAGE_RIGHT_X - bubbleWidth : INCOMING_MESSAGE_X;
            renderBubble(graphics, font, message, lines, bubbleX, bubbleY, bubbleWidth, bubbleHeight);
            nextBubbleBottom = bubbleY - MESSAGE_GAP;
            messageIndex--;
        }
    }

    private void renderBubble(GuiGraphics graphics, Font font, PhoneHomeScreen.ChatMessage message, List<String> lines, int bubbleX, int bubbleY, int bubbleWidth, int bubbleHeight) {
        boolean callLogMessage = isCallLogMessage(message.text());
        int bubbleColor = callLogMessage ? CALL_LOG_BUBBLE_COLOR : message.outgoing() ? OUTGOING_BUBBLE_COLOR : INCOMING_BUBBLE_COLOR;
        int textColor = callLogMessage ? CALL_LOG_TEXT_COLOR : message.outgoing() ? OUTGOING_MESSAGE_COLOR : INCOMING_MESSAGE_COLOR;
        graphics.fill(bubbleX, bubbleY, bubbleX + bubbleWidth, bubbleY + bubbleHeight, bubbleColor);

        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(font, lines.get(i), bubbleX + MESSAGE_PADDING_X, bubbleY + MESSAGE_PADDING_Y + i * MESSAGE_LINE_HEIGHT, textColor, false);
        }
    }

    private boolean isCallLogMessage(String message) {
        return message.startsWith("Voice call")
                || message.startsWith("Missed voice call")
                || message.startsWith("Declined voice call");
    }

    private List<String> wrapMessage(Font font, String message) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < message.length(); i++) {
            char character = message.charAt(i);
            if (font.width(line.toString() + character) > MESSAGE_TEXT_WIDTH && line.length() > 0) {
                lines.add(line.toString());
                line.setLength(0);
            }

            line.append(character);
        }

        if (line.length() > 0 || lines.isEmpty()) {
            lines.add(line.toString());
        }

        return lines;
    }

    private int getBubbleWidth(Font font, List<String> lines) {
        int textWidth = 0;
        for (String line : lines) {
            textWidth = Math.max(textWidth, font.width(line));
        }

        return Math.min(MESSAGE_BUBBLE_MAX_WIDTH, Math.max(MESSAGE_BUBBLE_MIN_WIDTH, textWidth + MESSAGE_PADDING_X * 2));
    }

    private void renderInput(PhoneHomeScreen phoneScreen, GuiGraphics graphics) {
        graphics.fill(INPUT_X - 1, INPUT_Y - 1, INPUT_X + INPUT_WIDTH + 1, INPUT_Y + INPUT_HEIGHT + 1, INPUT_BORDER_COLOR);
        graphics.fill(INPUT_X, INPUT_Y, INPUT_X + INPUT_WIDTH, INPUT_Y + INPUT_HEIGHT, INPUT_BACKGROUND_COLOR);

        String inputText = getVisibleInputText(phoneScreen.getPhoneFont());
        graphics.drawString(phoneScreen.getPhoneFont(), inputText, INPUT_TEXT_X, INPUT_TEXT_Y, INPUT_TEXT_COLOR, false);

        if (inputFocused && (System.currentTimeMillis() / 450L) % 2L == 0L) {
            int cursorX = INPUT_TEXT_X + phoneScreen.getPhoneFont().width(inputText) + 1;
            graphics.fill(cursorX, INPUT_TEXT_Y - 1, cursorX + 1, INPUT_TEXT_Y + 9, CURSOR_COLOR);
        }
    }

    private void renderButtonText(GuiGraphics graphics, Font font, String text, int buttonX, int buttonY, int buttonWidth, int buttonHeight) {
        int textX = buttonX + (buttonWidth - font.width(text)) / 2;
        int textY = buttonY + (buttonHeight - 8) / 2;
        graphics.drawString(font, text, textX, textY, SEND_BUTTON_TEXT_COLOR, false);
    }

    private void sendMessage(PhoneHomeScreen phoneScreen) {
        String message = messageInput.toString().trim();
        if (message.isEmpty()) {
            return;
        }

        phoneScreen.sendChatMessage(contact, message);
        messageInput.setLength(0);
        messageScrollOffset = 0;
        inputFocused = true;
    }

    private void requestMessagesIfNeeded(PhoneHomeScreen phoneScreen) {
        if (!requestedMessages) {
            requestedMessages = true;
            HandphoneNetwork.CHANNEL.sendToServer(new RequestChatMessagesPacket(phoneScreen.getActivePhoneNumber(), contact.getPhoneNumber()));
        }
    }

    private String getVisibleInputText(Font font) {
        String inputText = messageInput.toString();
        while (!inputText.isEmpty() && font.width(inputText) > MAX_INPUT_WIDTH) {
            inputText = inputText.substring(1);
        }

        return inputText;
    }

    private String fitText(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }

        String fittedText = text;
        while (!fittedText.isEmpty() && font.width(fittedText + "...") > maxWidth) {
            fittedText = fittedText.substring(0, fittedText.length() - 1);
        }

        return fittedText.isEmpty() ? "" : fittedText + "...";
    }

    private void finishNameEdit(PhoneHomeScreen phoneScreen) {
        if (!nameFocused) {
            return;
        }

        nameFocused = false;
        String name = nameInput.toString().trim();
        if (name.isEmpty() || name.equals(contact.getName())) {
            return;
        }

        contact.setName(name);
        phoneScreen.renameContact(contact.getPhoneNumber(), name);
        HandphoneNetwork.CHANNEL.sendToServer(new RenameContactPacket(phoneScreen.getActivePhoneNumber(), contact.getPhoneNumber(), name));
    }

    private void clampMessageScroll(PhoneHomeScreen phoneScreen) {
        int maxScrollOffset = Math.max(0, phoneScreen.getChatMessages(contact.getPhoneNumber()).size() - 1);
        if (messageScrollOffset < 0) {
            messageScrollOffset = 0;
        } else if (messageScrollOffset > maxScrollOffset) {
            messageScrollOffset = maxScrollOffset;
        }
    }

    private boolean isOverBackButton(double localX, double localY) {
        return localX >= BACK_BUTTON_X && localX <= BACK_BUTTON_X + BACK_BUTTON_WIDTH
                && localY >= BACK_BUTTON_Y && localY <= BACK_BUTTON_Y + BACK_BUTTON_HEIGHT;
    }

    private boolean isOverInput(double localX, double localY) {
        return localX >= INPUT_X && localX <= INPUT_X + INPUT_WIDTH
                && localY >= INPUT_Y && localY <= INPUT_Y + INPUT_HEIGHT;
    }

    private boolean isOverCallButton(double localX, double localY) {
        return localX >= CALL_BUTTON_X && localX <= CALL_BUTTON_X + CALL_BUTTON_WIDTH
                && localY >= CALL_BUTTON_Y && localY <= CALL_BUTTON_Y + CALL_BUTTON_HEIGHT;
    }

    private boolean isOverContactName(double localX, double localY) {
        return localX >= 45 && localX <= 95
                && localY >= CONTACT_NAME_Y - 2 && localY <= CONTACT_NAME_Y + 10;
    }

    private boolean isOverMessages(double localX, double localY) {
        return localX >= 0 && localX <= WINDOW_WIDTH
                && localY >= MESSAGE_TOP_Y && localY <= MESSAGE_BOTTOM_Y;
    }

    private boolean isOverSendButton(double localX, double localY) {
        return localX >= SEND_BUTTON_X && localX <= SEND_BUTTON_X + SEND_BUTTON_WIDTH
                && localY >= SEND_BUTTON_Y && localY <= SEND_BUTTON_Y + SEND_BUTTON_HEIGHT;
    }
}
