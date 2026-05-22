package com.twelveably.handphone.client.phone;

import com.twelveably.handphone.Handphone;
import com.twelveably.handphone.client.PhoneHomeScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class AddContactWindow implements PhoneWindow {
    private static final ResourceLocation ADD_BUTTON = new ResourceLocation(Handphone.MODID, "textures/gui/buttons/add.png");
    private static final ResourceLocation BACK_BUTTON = new ResourceLocation(Handphone.MODID, "textures/gui/buttons/chat.png");
    private static final int WINDOW_WIDTH = 141;
    private static final int BACK_BUTTON_X = 13;
    private static final int BACK_BUTTON_Y = 26;
    private static final int BACK_BUTTON_WIDTH = 30;
    private static final int BACK_BUTTON_HEIGHT = 14;
    private static final String BACK_BUTTON_TEXT = "Back";
    private static final int ADD_BUTTON_WIDTH = 40;
    private static final int ADD_BUTTON_HEIGHT = 17;
    private static final int ADD_BUTTON_X = (WINDOW_WIDTH - ADD_BUTTON_WIDTH) / 2;
    private static final int ADD_BUTTON_Y = 151;
    private static final String ADD_BUTTON_TEXT = "Add";
    private static final int ADD_BUTTON_TEXT_COLOR = 0xFFFFFFFF;
    private static final String TITLE_TEXT = "Add Contact";
    private static final int TITLE_Y = 52;
    private static final String NAME_LABEL_TEXT = "Name:";
    private static final int NAME_LABEL_X = 18;
    private static final int NAME_LABEL_Y = 67;
    private static final String NUMBER_LABEL_TEXT = "Number:";
    private static final int NUMBER_LABEL_X = 18;
    private static final int NUMBER_LABEL_Y = 108;
    private static final int LABEL_TEXT_COLOR = 0xFF20242A;
    private static final int NAME_INPUT_X = 18;
    private static final int NAME_INPUT_Y = 79;
    private static final int NAME_INPUT_WIDTH = 105;
    private static final int NAME_INPUT_HEIGHT = 18;
    private static final int NAME_INPUT_TEXT_X = 22;
    private static final int NAME_INPUT_TEXT_Y = 84;
    private static final int MAX_NAME_WIDTH = NAME_INPUT_WIDTH - 8;
    private static final int INPUT_X = 18;
    private static final int INPUT_Y = 120;
    private static final int INPUT_WIDTH = 105;
    private static final int INPUT_HEIGHT = 18;
    private static final int INPUT_BORDER_COLOR = 0xFF2B3038;
    private static final int INPUT_BACKGROUND_COLOR = 0xFFD8E4F7;
    private static final int INPUT_TEXT_X = 22;
    private static final int INPUT_TEXT_Y = 125;
    private static final int MAX_PHONE_NUMBER_WIDTH = INPUT_WIDTH - 8;
    private static final int INPUT_TEXT_COLOR = 0xFF20242A;
    private static final int CURSOR_COLOR = 0xFF20242A;
    private static final int MAX_PHONE_NUMBER_LENGTH = 7;
    private static final int KEY_BACKSPACE = 259;
    private static final String DEFAULT_CONTACT_NAME = "New Contact";

    private final StringBuilder nameInput = new StringBuilder();
    private final StringBuilder phoneNumberInput = new StringBuilder();
    private boolean nameInputFocused = true;
    private boolean phoneInputFocused;

    @Override
    public void render(PhoneHomeScreen phoneScreen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RenderSystem.texParameter(3553, 10241, 9728);
        RenderSystem.texParameter(3553, 10240, 9728);
        graphics.blit(BACK_BUTTON, BACK_BUTTON_X, BACK_BUTTON_Y, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT, 0, 0, 56, 24, 56, 24);
        renderButtonText(graphics, phoneScreen.getPhoneFont(), BACK_BUTTON_TEXT, BACK_BUTTON_X, BACK_BUTTON_Y, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT);
        renderLabels(phoneScreen, graphics);
        renderNameForm(phoneScreen, graphics);
        renderNameInput(phoneScreen, graphics);
        renderPhoneNumberForm(phoneScreen, graphics);
        renderPhoneNumberInput(phoneScreen, graphics);
        graphics.blit(ADD_BUTTON, ADD_BUTTON_X, ADD_BUTTON_Y, ADD_BUTTON_WIDTH, ADD_BUTTON_HEIGHT, 0, 0, 56, 24, 56, 24);
        renderButtonText(graphics, phoneScreen.getPhoneFont(), ADD_BUTTON_TEXT, ADD_BUTTON_X, ADD_BUTTON_Y, ADD_BUTTON_WIDTH, ADD_BUTTON_HEIGHT);
    }

    @Override
    public boolean mouseClicked(PhoneHomeScreen phoneScreen, double localX, double localY, int button) {
        if (button == 0 && isOverBackButton(localX, localY)) {
            phoneScreen.openWindow(new MessagingWindow());
            return true;
        }

        if (button == 0 && isOverNameInput(localX, localY)) {
            nameInputFocused = true;
            phoneInputFocused = false;
            return true;
        }

        if (button == 0 && isOverInput(localX, localY)) {
            nameInputFocused = false;
            phoneInputFocused = true;
            return true;
        }

        if (button == 0) {
            nameInputFocused = false;
            phoneInputFocused = false;
        }

        if (button == 0 && isOverAddButton(localX, localY)) {
            if (phoneNumberInput.length() > 0) {
                String contactName = nameInput.length() > 0 ? nameInput.toString() : DEFAULT_CONTACT_NAME;
                phoneScreen.addContact(contactName, phoneNumberInput.toString());
                phoneScreen.openWindow(new MessagingWindow());
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean keyPressed(PhoneHomeScreen phoneScreen, int keyCode, int scanCode, int modifiers) {
        if (!nameInputFocused && !phoneInputFocused) {
            return false;
        }

        if (keyCode == KEY_BACKSPACE && nameInputFocused && nameInput.length() > 0) {
            nameInput.deleteCharAt(nameInput.length() - 1);
            return true;
        }

        if (keyCode == KEY_BACKSPACE && phoneInputFocused && phoneNumberInput.length() > 0) {
            phoneNumberInput.deleteCharAt(phoneNumberInput.length() - 1);
            return true;
        }

        return keyCode == KEY_BACKSPACE;
    }

    @Override
    public boolean charTyped(PhoneHomeScreen phoneScreen, char codePoint, int modifiers) {
        if (nameInputFocused) {
            if (Character.isISOControl(codePoint) || phoneScreen.getPhoneFont().width(nameInput.toString() + codePoint) > MAX_NAME_WIDTH) {
                return false;
            }

            nameInput.append(codePoint);
            return true;
        }

        if (!phoneInputFocused || !Character.isDigit(codePoint) || phoneNumberInput.length() >= MAX_PHONE_NUMBER_LENGTH
                || phoneScreen.getPhoneFont().width(phoneNumberInput.toString() + codePoint) > MAX_PHONE_NUMBER_WIDTH) {
            return false;
        }

        phoneNumberInput.append(codePoint);
        return true;
    }

    private boolean isOverBackButton(double localX, double localY) {
        return localX >= BACK_BUTTON_X && localX <= BACK_BUTTON_X + BACK_BUTTON_WIDTH
                && localY >= BACK_BUTTON_Y && localY <= BACK_BUTTON_Y + BACK_BUTTON_HEIGHT;
    }

    private boolean isOverAddButton(double localX, double localY) {
        return localX >= ADD_BUTTON_X && localX <= ADD_BUTTON_X + ADD_BUTTON_WIDTH
                && localY >= ADD_BUTTON_Y && localY <= ADD_BUTTON_Y + ADD_BUTTON_HEIGHT;
    }

    private boolean isOverInput(double localX, double localY) {
        return localX >= INPUT_X && localX <= INPUT_X + INPUT_WIDTH
                && localY >= INPUT_Y && localY <= INPUT_Y + INPUT_HEIGHT;
    }

    private boolean isOverNameInput(double localX, double localY) {
        return localX >= NAME_INPUT_X && localX <= NAME_INPUT_X + NAME_INPUT_WIDTH
                && localY >= NAME_INPUT_Y && localY <= NAME_INPUT_Y + NAME_INPUT_HEIGHT;
    }

    private void renderLabels(PhoneHomeScreen phoneScreen, GuiGraphics graphics) {
        Font font = phoneScreen.getPhoneFont();
        int titleX = (WINDOW_WIDTH - font.width(TITLE_TEXT)) / 2;
        graphics.drawString(font, TITLE_TEXT, titleX, TITLE_Y, LABEL_TEXT_COLOR, false);
        graphics.drawString(font, NAME_LABEL_TEXT, NAME_LABEL_X, NAME_LABEL_Y, LABEL_TEXT_COLOR, false);
        graphics.drawString(font, NUMBER_LABEL_TEXT, NUMBER_LABEL_X, NUMBER_LABEL_Y, LABEL_TEXT_COLOR, false);
    }

    private void renderNameForm(PhoneHomeScreen phoneScreen, GuiGraphics graphics) {
        graphics.fill(NAME_INPUT_X - 1, NAME_INPUT_Y - 1, NAME_INPUT_X + NAME_INPUT_WIDTH + 1, NAME_INPUT_Y + NAME_INPUT_HEIGHT + 1, INPUT_BORDER_COLOR);
        graphics.fill(NAME_INPUT_X, NAME_INPUT_Y, NAME_INPUT_X + NAME_INPUT_WIDTH, NAME_INPUT_Y + NAME_INPUT_HEIGHT, INPUT_BACKGROUND_COLOR);
    }

    private void renderPhoneNumberForm(PhoneHomeScreen phoneScreen, GuiGraphics graphics) {
        graphics.fill(INPUT_X - 1, INPUT_Y - 1, INPUT_X + INPUT_WIDTH + 1, INPUT_Y + INPUT_HEIGHT + 1, INPUT_BORDER_COLOR);
        graphics.fill(INPUT_X, INPUT_Y, INPUT_X + INPUT_WIDTH, INPUT_Y + INPUT_HEIGHT, INPUT_BACKGROUND_COLOR);
    }

    private void renderNameInput(PhoneHomeScreen phoneScreen, GuiGraphics graphics) {
        String inputText = nameInput.toString();
        graphics.drawString(phoneScreen.getPhoneFont(), inputText, NAME_INPUT_TEXT_X, NAME_INPUT_TEXT_Y, INPUT_TEXT_COLOR, false);

        if (nameInputFocused && (System.currentTimeMillis() / 450L) % 2L == 0L) {
            int cursorX = NAME_INPUT_TEXT_X + phoneScreen.getPhoneFont().width(inputText) + 1;
            graphics.fill(cursorX, NAME_INPUT_TEXT_Y - 1, cursorX + 1, NAME_INPUT_TEXT_Y + 9, CURSOR_COLOR);
        }
    }

    private void renderPhoneNumberInput(PhoneHomeScreen phoneScreen, GuiGraphics graphics) {
        String inputText = phoneNumberInput.toString();
        graphics.drawString(phoneScreen.getPhoneFont(), inputText, INPUT_TEXT_X, INPUT_TEXT_Y, INPUT_TEXT_COLOR, false);

        if (phoneInputFocused && (System.currentTimeMillis() / 450L) % 2L == 0L) {
            int cursorX = INPUT_TEXT_X + phoneScreen.getPhoneFont().width(inputText) + 1;
            graphics.fill(cursorX, INPUT_TEXT_Y - 1, cursorX + 1, INPUT_TEXT_Y + 9, CURSOR_COLOR);
        }
    }

    private void renderButtonText(GuiGraphics graphics, Font font, String text, int buttonX, int buttonY, int buttonWidth, int buttonHeight) {
        int textX = buttonX + (buttonWidth - font.width(text)) / 2;
        int textY = buttonY + (buttonHeight - 8) / 2;
        graphics.drawString(font, text, textX, textY, ADD_BUTTON_TEXT_COLOR, false);
    }
}
