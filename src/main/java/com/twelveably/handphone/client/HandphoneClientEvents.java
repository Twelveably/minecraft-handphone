package com.twelveably.handphone.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.twelveably.handphone.Handphone;
import com.twelveably.handphone.network.HandphoneNetwork;
import com.twelveably.handphone.network.ToggleLoudspeakerPacket;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Handphone.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HandphoneClientEvents {
    private static final KeyMapping LOUDSPEAKER_KEY = new KeyMapping(
            "key.handphone.loudspeaker",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.handphone"
    );
    private static final KeyMapping MUTE_NOTIFICATIONS_KEY = new KeyMapping(
            "key.handphone.mute_notifications",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            "key.categories.handphone"
    );

    private HandphoneClientEvents() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(LOUDSPEAKER_KEY);
        event.register(MUTE_NOTIFICATIONS_KEY);
        MinecraftForge.EVENT_BUS.register(ForgeEvents.class);
    }

    public static final class ForgeEvents {
        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            while (LOUDSPEAKER_KEY.consumeClick()) {
                HandphoneNetwork.CHANNEL.sendToServer(new ToggleLoudspeakerPacket(ClientPhoneHooks.getOpenPhoneNumber()));
            }

            while (MUTE_NOTIFICATIONS_KEY.consumeClick()) {
                ClientNotificationSounds.toggleMuted();
            }
        }

        @SubscribeEvent
        public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientPhoneHooks.clearCallStates();
            ClientRingtoneManager.clearRingtones();
        }
    }
}
