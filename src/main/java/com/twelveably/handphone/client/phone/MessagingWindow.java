package com.twelveably.handphone.client.phone;

import com.twelveably.handphone.Handphone;
import com.twelveably.handphone.client.ClientProfilePictures;
import com.twelveably.handphone.client.PhoneHomeScreen;
import com.twelveably.handphone.network.DeleteContactPacket;
import com.twelveably.handphone.network.HandphoneNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MessagingWindow implements PhoneWindow {
    private static final ResourceLocation CONTACTS_WINDOW = new ResourceLocation(Handphone.MODID, "textures/gui/chat_window.png");
    private static final ResourceLocation ADD_BUTTON = new ResourceLocation(Handphone.MODID, "textures/gui/buttons/add.png");
    private static final ResourceLocation DELETE_BUTTON = new ResourceLocation(Handphone.MODID, "textures/gui/buttons/delete.png");
    private static final ResourceLocation CHAT_BUTTON = new ResourceLocation(Handphone.MODID, "textures/gui/buttons/chat.png");
    private static final ResourceLocation CALL_BUTTON = new ResourceLocation(Handphone.MODID, "textures/gui/buttons/call.png");
    private static final int WINDOW_WIDTH = 141;
    private static final int WINDOW_HEIGHT = 225;
    private static final int CONTACTS_WINDOW_TEXTURE_WIDTH = 200;
    private static final int CONTACTS_WINDOW_TEXTURE_HEIGHT = 320;
    private static final int BACK_BUTTON_X = 13;
    private static final int BACK_BUTTON_Y = 26;
    private static final int BACK_BUTTON_WIDTH = 30;
    private static final int BACK_BUTTON_HEIGHT = 14;
    private static final String BACK_BUTTON_TEXT = "Back";
    private static final int ADD_BUTTON_X = 16;
    private static final int ADD_BUTTON_Y = 195;
    private static final int ADD_BUTTON_WIDTH = 40;
    private static final int ADD_BUTTON_HEIGHT = 17;
    private static final int DELETE_BUTTON_X = 84;
    private static final int DELETE_BUTTON_Y = 195;
    private static final int DELETE_BUTTON_WIDTH = 40;
    private static final int DELETE_BUTTON_HEIGHT = 17;
    private static final String ADD_BUTTON_TEXT = "Add";
    private static final String DELETE_BUTTON_TEXT = "Del";
    private static final int ACTION_BUTTON_TEXT_COLOR = 0xFFFFFFFF;
    private static final String EMPTY_CONTACT_TEXT = "No contact yet....";
    private static final int EMPTY_CONTACT_CENTER_X = 70;
    private static final int EMPTY_CONTACT_CENTER_Y = 113;
    private static final int EMPTY_CONTACT_COLOR = 0xFF5F6873;
    private static final int CONTACT_START_X = 17;
    private static final int CONTACT_ROW_BACKGROUND_X = 13;
    private static final int CONTACT_ROW_BACKGROUND_WIDTH = 112;
    private static final int CONTACT_START_Y = 52;
    private static final int CONTACT_ROW_HEIGHT = 41;
    private static final int CONTACT_ROW_BACKGROUND_HEIGHT = 35;
    private static final int CONTACT_ROW_HOVER_COLOR = 0xAAFFFFFF;
    private static final int MAX_VISIBLE_CONTACTS = 3;
    private static final int SCROLLBAR_X = 128;
    private static final int SCROLLBAR_Y = CONTACT_START_Y;
    private static final int SCROLLBAR_WIDTH = 3;
    private static final int SCROLLBAR_HEIGHT = MAX_VISIBLE_CONTACTS * CONTACT_ROW_HEIGHT - 8;
    private static final int SCROLLBAR_TRACK_COLOR = 0x55343A42;
    private static final int SCROLLBAR_THUMB_COLOR = 0xCC5F6873;
    private static final int MIN_SCROLLBAR_THUMB_HEIGHT = 14;
    private static final int HEAD_SIZE = 28;
    private static final int NAME_X = 49;
    private static final int NAME_WIDTH = 75;
    private static final int NAME_HEIGHT = 10;
    private static final int NAME_COLOR = 0xFF222832;
    private static final int TIME_RIGHT_X = 123;
    private static final int TIME_COLOR = 0xFF5F6873;
    private static final int SNIPPET_X = 49;
    private static final int SNIPPET_Y_OFFSET = 15;
    private static final int SNIPPET_WIDTH = 71;
    private static final int SNIPPET_COLOR = 0xFF5F6873;
    private static final int DELETE_ROW_BUTTON_X = 113;
    private static final int DELETE_ROW_BUTTON_Y_OFFSET = -2;
    private static final int DELETE_ROW_BUTTON_SIZE = 10;
    private static final String DELETE_ROW_BUTTON_TEXT = "X";
    private static final int DELETE_ROW_BUTTON_COLOR = 0xFFE21B1B;
    private static final int DELETE_ROW_BUTTON_TEXT_COLOR = 0xFFFFFFFF;
    private static final int UNREAD_BADGE_COLOR = 0xFF16A34A;
    private static final int UNREAD_BADGE_TEXT_COLOR = 0xFFFFFFFF;
    private static final int KEY_BACKSPACE = 259;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private int selectedContactIndex = -1;
    private int firstVisibleContactIndex;
    private boolean deleteMode;

    @Override
    public void render(PhoneHomeScreen phoneScreen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RenderSystem.texParameter(3553, 10241, 9728);
        RenderSystem.texParameter(3553, 10240, 9728);
        graphics.blit(CONTACTS_WINDOW, 0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, 0, 0, CONTACTS_WINDOW_TEXTURE_WIDTH, CONTACTS_WINDOW_TEXTURE_HEIGHT, CONTACTS_WINDOW_TEXTURE_WIDTH, CONTACTS_WINDOW_TEXTURE_HEIGHT);
        graphics.blit(CHAT_BUTTON, BACK_BUTTON_X, BACK_BUTTON_Y, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT, 0, 0, 56, 24, 56, 24);
        renderActionButtonText(graphics, phoneScreen.getPhoneFont(), BACK_BUTTON_TEXT, BACK_BUTTON_X, BACK_BUTTON_Y, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT);
        if (phoneScreen.getContacts().isEmpty()) {
            renderEmptyContactText(phoneScreen, graphics);
        } else {
            renderContacts(phoneScreen, graphics, mouseX, mouseY);
            renderScrollbar(phoneScreen, graphics);
        }
        graphics.blit(ADD_BUTTON, ADD_BUTTON_X, ADD_BUTTON_Y, ADD_BUTTON_WIDTH, ADD_BUTTON_HEIGHT, 0, 0, 56, 24, 56, 24);
        graphics.blit(DELETE_BUTTON, DELETE_BUTTON_X, DELETE_BUTTON_Y, DELETE_BUTTON_WIDTH, DELETE_BUTTON_HEIGHT, 0, 0, 56, 24, 56, 24);
        renderActionButtonText(graphics, phoneScreen.getPhoneFont(), ADD_BUTTON_TEXT, ADD_BUTTON_X, ADD_BUTTON_Y, ADD_BUTTON_WIDTH, ADD_BUTTON_HEIGHT);
        renderActionButtonText(graphics, phoneScreen.getPhoneFont(), DELETE_BUTTON_TEXT, DELETE_BUTTON_X, DELETE_BUTTON_Y, DELETE_BUTTON_WIDTH, DELETE_BUTTON_HEIGHT);
    }

    @Override
    public boolean mouseClicked(PhoneHomeScreen phoneScreen, double localX, double localY, int button) {
        clampScroll(phoneScreen);

        if (button == 0 && isOverBackButton(localX, localY)) {
            phoneScreen.openWindow(new HomeWindow());
            return true;
        }

        if (button == 0 && isOverAddButton(localX, localY)) {
            phoneScreen.openWindow(new AddContactWindow());
            return true;
        }

        if (button == 0 && isOverDeleteButton(localX, localY)) {
            deleteMode = !deleteMode;
            selectedContactIndex = -1;
            return true;
        }

        if (button == 0) {
            if (deleteMode) {
                int deleteContactIndex = getDeleteRowButtonIndexAt(phoneScreen, localX, localY);
                if (deleteContactIndex >= 0) {
                    HandphoneNetwork.CHANNEL.sendToServer(new DeleteContactPacket(phoneScreen.getActivePhoneNumber(), deleteContactIndex));
                    selectedContactIndex = -1;
                    return true;
                }
            }

            int rowContactIndex = getContactRowIndexAt(phoneScreen, localX, localY);
            if (rowContactIndex >= 0) {
                PhoneHomeScreen.PhoneContact contact = phoneScreen.getContacts().get(rowContactIndex);
                phoneScreen.openChat(contact);
                selectedContactIndex = -1;
                deleteMode = false;
                return true;
            }

            selectedContactIndex = -1;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(PhoneHomeScreen phoneScreen, double localX, double localY, double scrollDelta) {
        if (!isOverContactViewport(localX, localY) || !canScroll(phoneScreen)) {
            return false;
        }

        int direction = scrollDelta > 0.0D ? -1 : 1;
        firstVisibleContactIndex += direction;
        clampScroll(phoneScreen);
        selectedContactIndex = -1;
        return true;
    }

    @Override
    public boolean keyPressed(PhoneHomeScreen phoneScreen, int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public boolean charTyped(PhoneHomeScreen phoneScreen, char codePoint, int modifiers) {
        return false;
    }

    private boolean isOverBackButton(double localX, double localY) {
        return localX >= BACK_BUTTON_X && localX <= BACK_BUTTON_X + BACK_BUTTON_WIDTH
                && localY >= BACK_BUTTON_Y && localY <= BACK_BUTTON_Y + BACK_BUTTON_HEIGHT;
    }

    private boolean isOverAddButton(double localX, double localY) {
        return localX >= ADD_BUTTON_X && localX <= ADD_BUTTON_X + ADD_BUTTON_WIDTH
                && localY >= ADD_BUTTON_Y && localY <= ADD_BUTTON_Y + ADD_BUTTON_HEIGHT;
    }

    private boolean isOverDeleteButton(double localX, double localY) {
        return localX >= DELETE_BUTTON_X && localX <= DELETE_BUTTON_X + DELETE_BUTTON_WIDTH
                && localY >= DELETE_BUTTON_Y && localY <= DELETE_BUTTON_Y + DELETE_BUTTON_HEIGHT;
    }

    private void renderEmptyContactText(PhoneHomeScreen phoneScreen, GuiGraphics graphics) {
        int textWidth = phoneScreen.getPhoneFont().width(EMPTY_CONTACT_TEXT);
        graphics.drawString(phoneScreen.getPhoneFont(), EMPTY_CONTACT_TEXT, EMPTY_CONTACT_CENTER_X - textWidth / 2, EMPTY_CONTACT_CENTER_Y, EMPTY_CONTACT_COLOR, false);
    }

    private void renderContacts(PhoneHomeScreen phoneScreen, GuiGraphics graphics, int mouseX, int mouseY) {
        List<PhoneHomeScreen.PhoneContact> contacts = phoneScreen.getContacts();
        clampScroll(phoneScreen);
        int hoveredContactIndex = getContactRowIndexAt(phoneScreen, mouseX, mouseY);

        for (int visibleIndex = 0; visibleIndex < MAX_VISIBLE_CONTACTS; visibleIndex++) {
            int contactIndex = firstVisibleContactIndex + visibleIndex;
            if (contactIndex >= contacts.size()) {
                break;
            }

            int rowY = CONTACT_START_Y + visibleIndex * CONTACT_ROW_HEIGHT;
            if (contactIndex == hoveredContactIndex) {
                renderContactRowHover(graphics, rowY);
            }
            renderContactRow(phoneScreen, graphics, contacts.get(contactIndex), contactIndex, rowY);
        }
    }

    private void renderContactRowHover(GuiGraphics graphics, int rowY) {
        graphics.fill(CONTACT_ROW_BACKGROUND_X, rowY - 3, CONTACT_ROW_BACKGROUND_X + CONTACT_ROW_BACKGROUND_WIDTH, rowY - 3 + CONTACT_ROW_BACKGROUND_HEIGHT, CONTACT_ROW_HOVER_COLOR);
    }

    private void renderScrollbar(PhoneHomeScreen phoneScreen, GuiGraphics graphics) {
        if (!canScroll(phoneScreen)) {
            return;
        }

        int trackBottom = SCROLLBAR_Y + SCROLLBAR_HEIGHT;
        graphics.fill(SCROLLBAR_X, SCROLLBAR_Y, SCROLLBAR_X + SCROLLBAR_WIDTH, trackBottom, SCROLLBAR_TRACK_COLOR);

        int contactCount = phoneScreen.getContacts().size();
        int thumbHeight = Math.max(MIN_SCROLLBAR_THUMB_HEIGHT, SCROLLBAR_HEIGHT * MAX_VISIBLE_CONTACTS / contactCount);
        int maxFirstVisibleContactIndex = Math.max(1, contactCount - MAX_VISIBLE_CONTACTS);
        int maxThumbTravel = SCROLLBAR_HEIGHT - thumbHeight;
        int thumbY = SCROLLBAR_Y + maxThumbTravel * firstVisibleContactIndex / maxFirstVisibleContactIndex;
        graphics.fill(SCROLLBAR_X, thumbY, SCROLLBAR_X + SCROLLBAR_WIDTH, thumbY + thumbHeight, SCROLLBAR_THUMB_COLOR);
    }

    private void renderContactRow(PhoneHomeScreen phoneScreen, GuiGraphics graphics, PhoneHomeScreen.PhoneContact contact, int index, int rowY) {
        Font font = phoneScreen.getPhoneFont();
        renderPlayerHead(graphics, font, contact, CONTACT_START_X, rowY);
        int nameColor = NAME_COLOR;
        String timeText = formatTime(contact.getLatestMessageTimeMillis());
        int timeX = timeText.isEmpty() ? TIME_RIGHT_X : TIME_RIGHT_X - font.width(timeText);
        graphics.drawString(font, fitText(font, contact.getName(), Math.max(24, timeX - NAME_X - 4)), NAME_X, rowY + 3, nameColor, false);
        if (!timeText.isEmpty()) {
            graphics.drawString(font, timeText, timeX, rowY + 3, TIME_COLOR, false);
        }

        renderSnippet(graphics, font, contact, rowY);
        renderUnreadBadge(phoneScreen, graphics, font, contact, rowY);

        if (deleteMode) {
            renderDeleteRowButton(graphics, font, rowY);
        }
    }

    private void renderUnreadBadge(PhoneHomeScreen phoneScreen, GuiGraphics graphics, Font font, PhoneHomeScreen.PhoneContact contact, int rowY) {
        int unreadCount = phoneScreen.getUnreadMessages(contact.getPhoneNumber());
        if (unreadCount <= 0) {
            return;
        }

        String text = unreadCount > 99 ? "99+" : Integer.toString(unreadCount);
        int badgeWidth = Math.max(10, font.width(text) + 4);
        int badgeX = TIME_RIGHT_X - badgeWidth;
        int badgeY = rowY + 21;
        graphics.fill(badgeX, badgeY, badgeX + badgeWidth, badgeY + 10, UNREAD_BADGE_COLOR);
        graphics.drawString(font, text, badgeX + (badgeWidth - font.width(text)) / 2, badgeY + 1, UNREAD_BADGE_TEXT_COLOR, false);
    }

    private void renderDeleteRowButton(GuiGraphics graphics, Font font, int rowY) {
        int buttonY = rowY + DELETE_ROW_BUTTON_Y_OFFSET;
        graphics.fill(DELETE_ROW_BUTTON_X, buttonY, DELETE_ROW_BUTTON_X + DELETE_ROW_BUTTON_SIZE, buttonY + DELETE_ROW_BUTTON_SIZE, DELETE_ROW_BUTTON_COLOR);

        int textX = DELETE_ROW_BUTTON_X + (DELETE_ROW_BUTTON_SIZE - font.width(DELETE_ROW_BUTTON_TEXT)) / 2;
        int textY = buttonY + (DELETE_ROW_BUTTON_SIZE - 8) / 2;
        graphics.drawString(font, DELETE_ROW_BUTTON_TEXT, textX, textY, DELETE_ROW_BUTTON_TEXT_COLOR, false);
    }

    private void renderSnippet(GuiGraphics graphics, Font font, PhoneHomeScreen.PhoneContact contact, int rowY) {
        String snippet = contact.getLatestMessage().isEmpty() ? contact.getPhoneNumber() : contact.getLatestMessage();
        graphics.drawString(font, fitText(font, snippet, SNIPPET_WIDTH), SNIPPET_X, rowY + SNIPPET_Y_OFFSET, SNIPPET_COLOR, false);
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

    private String formatTime(long timestampMillis) {
        if (timestampMillis <= 0L) {
            return "";
        }

        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestampMillis), ZoneId.systemDefault()).format(TIME_FORMATTER);
    }

    private void renderActionButtonText(GuiGraphics graphics, Font font, String text, int buttonX, int buttonY, int buttonWidth, int buttonHeight) {
        int textX = buttonX + (buttonWidth - font.width(text)) / 2;
        int textY = buttonY + (buttonHeight - 8) / 2;
        graphics.drawString(font, text, textX, textY, ACTION_BUTTON_TEXT_COLOR, false);
    }

    private void renderPlayerHead(GuiGraphics graphics, Font font, PhoneHomeScreen.PhoneContact contact, int x, int y) {
        ClientProfilePictures.drawHead(graphics, font, contact.getProfileId(), contact.getPhoneNumber(), x, y, HEAD_SIZE);
    }

    private int getContactRowIndexAt(PhoneHomeScreen phoneScreen, double localX, double localY) {
        clampScroll(phoneScreen);

        for (int visibleIndex = 0; visibleIndex < MAX_VISIBLE_CONTACTS; visibleIndex++) {
            int contactIndex = firstVisibleContactIndex + visibleIndex;
            if (contactIndex >= phoneScreen.getContacts().size()) {
                break;
            }

            int rowY = CONTACT_START_Y + visibleIndex * CONTACT_ROW_HEIGHT;
            if (localX >= CONTACT_START_X && localX <= CONTACT_ROW_BACKGROUND_X + CONTACT_ROW_BACKGROUND_WIDTH
                    && localY >= rowY && localY <= rowY + HEAD_SIZE) {
                return contactIndex;
            }
        }

        return -1;
    }

    private int getDeleteRowButtonIndexAt(PhoneHomeScreen phoneScreen, double localX, double localY) {
        clampScroll(phoneScreen);

        for (int visibleIndex = 0; visibleIndex < MAX_VISIBLE_CONTACTS; visibleIndex++) {
            int contactIndex = firstVisibleContactIndex + visibleIndex;
            if (contactIndex >= phoneScreen.getContacts().size()) {
                break;
            }

            int buttonY = CONTACT_START_Y + visibleIndex * CONTACT_ROW_HEIGHT + DELETE_ROW_BUTTON_Y_OFFSET;
            if (localX >= DELETE_ROW_BUTTON_X && localX <= DELETE_ROW_BUTTON_X + DELETE_ROW_BUTTON_SIZE
                    && localY >= buttonY && localY <= buttonY + DELETE_ROW_BUTTON_SIZE) {
                return contactIndex;
            }
        }

        return -1;
    }

    private boolean isOverContactViewport(double localX, double localY) {
        return localX >= CONTACT_START_X && localX <= CONTACT_ROW_BACKGROUND_X + CONTACT_ROW_BACKGROUND_WIDTH
                && localY >= CONTACT_START_Y && localY <= CONTACT_START_Y + MAX_VISIBLE_CONTACTS * CONTACT_ROW_HEIGHT;
    }

    private boolean canScroll(PhoneHomeScreen phoneScreen) {
        return phoneScreen.getContacts().size() > MAX_VISIBLE_CONTACTS;
    }

    private void clampScroll(PhoneHomeScreen phoneScreen) {
        int maxFirstVisibleContactIndex = Math.max(0, phoneScreen.getContacts().size() - MAX_VISIBLE_CONTACTS);
        if (firstVisibleContactIndex < 0) {
            firstVisibleContactIndex = 0;
        } else if (firstVisibleContactIndex > maxFirstVisibleContactIndex) {
            firstVisibleContactIndex = maxFirstVisibleContactIndex;
        }

        if (!hasSelectedContact(phoneScreen)) {
            selectedContactIndex = -1;
        }
    }

    private boolean hasSelectedContact(PhoneHomeScreen phoneScreen) {
        return selectedContactIndex >= 0 && selectedContactIndex < phoneScreen.getContacts().size();
    }
}
