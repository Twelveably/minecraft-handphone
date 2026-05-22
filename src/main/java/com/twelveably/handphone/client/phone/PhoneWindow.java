package com.twelveably.handphone.client.phone;

import com.twelveably.handphone.client.PhoneHomeScreen;
import net.minecraft.client.gui.GuiGraphics;

public interface PhoneWindow {
    void render(PhoneHomeScreen phoneScreen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

    default boolean mouseClicked(PhoneHomeScreen phoneScreen, double localX, double localY, int button) {
        return false;
    }

    default boolean mouseScrolled(PhoneHomeScreen phoneScreen, double localX, double localY, double scrollDelta) {
        return false;
    }

    default boolean keyPressed(PhoneHomeScreen phoneScreen, int keyCode, int scanCode, int modifiers) {
        return false;
    }

    default boolean charTyped(PhoneHomeScreen phoneScreen, char codePoint, int modifiers) {
        return false;
    }
}
