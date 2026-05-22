package com.twelveably.handphone.client;

import com.twelveably.handphone.Handphone;
import com.twelveably.handphone.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Handphone.MODID, value = Dist.CLIENT)
public final class ClientNotificationSounds {
    private static final long CALL_SOUND_INTERVAL_MS = 3000L;
    private static final long INCOMING_CALL_REPLAY_DELAY_MS = 2000L;
    private static boolean muted;
    private static NotificationSound loopingCallSound;
    private static long nextCallSoundAtMillis;
    private static SimpleSoundInstance currentIncomingCallSound;
    private static long nextIncomingCallReplayCheckAtMillis;

    private ClientNotificationSounds() {
    }

    public static void toggleMuted() {
        muted = !muted;
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(Component.literal("Phone notification sounds " + (muted ? "muted." : "unmuted.")), true);
        }
    }

    public static void play(NotificationSound sound) {
        if (muted) {
            return;
        }

        ResourceLocation soundId = getConfiguredSoundId(sound.configuredSound());
        SoundEvent soundEvent = soundId == null ? null : BuiltInRegistries.SOUND_EVENT.get(soundId);
        if (soundEvent == null) {
            soundEvent = SoundEvents.NOTE_BLOCK_PLING.get();
        }

        SimpleSoundInstance soundInstance = SimpleSoundInstance.forUI(soundEvent, 1.0F, 1.0F);
        Minecraft.getInstance().getSoundManager().play(soundInstance);
        if (sound == NotificationSound.INCOMING_CALL) {
            currentIncomingCallSound = soundInstance;
            nextIncomingCallReplayCheckAtMillis = 0L;
        }
    }

    public static void startLoopingCallSound(NotificationSound sound) {
        if (sound != NotificationSound.CALLING && sound != NotificationSound.INCOMING_CALL) {
            return;
        }

        loopingCallSound = sound;
        nextCallSoundAtMillis = Util.getMillis();
        nextIncomingCallReplayCheckAtMillis = 0L;
        if (sound == NotificationSound.INCOMING_CALL) {
            play(NotificationSound.INCOMING_CALL);
            return;
        }

        playLoopingCallSoundIfDue();
    }

    public static void stopCallSounds() {
        loopingCallSound = null;
        currentIncomingCallSound = null;
        nextIncomingCallReplayCheckAtMillis = 0L;
        stop(NotificationSound.CALLING);
        stop(NotificationSound.INCOMING_CALL);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            playLoopingCallSoundIfDue();
        }
    }

    private static void playLoopingCallSoundIfDue() {
        if (loopingCallSound == null || muted || Minecraft.getInstance().level == null) {
            return;
        }

        if (loopingCallSound == NotificationSound.INCOMING_CALL) {
            playIncomingCallSoundIfDue();
            return;
        }

        long now = Util.getMillis();
        if (now < nextCallSoundAtMillis) {
            return;
        }
        play(loopingCallSound);
        nextCallSoundAtMillis = now + CALL_SOUND_INTERVAL_MS;
    }

    private static void playIncomingCallSoundIfDue() {
        long now = Util.getMillis();
        if (currentIncomingCallSound != null && Minecraft.getInstance().getSoundManager().isActive(currentIncomingCallSound)) {
            nextIncomingCallReplayCheckAtMillis = 0L;
            return;
        }

        if (nextIncomingCallReplayCheckAtMillis <= 0L) {
            nextIncomingCallReplayCheckAtMillis = now + INCOMING_CALL_REPLAY_DELAY_MS;
            return;
        }

        if (now >= nextIncomingCallReplayCheckAtMillis) {
            play(NotificationSound.INCOMING_CALL);
        }
    }

    private static void stop(NotificationSound sound) {
        ResourceLocation soundId = getConfiguredSoundId(sound.configuredSound());
        if (soundId != null) {
            Minecraft.getInstance().getSoundManager().stop(soundId, SoundSource.MASTER);
        }
    }

    private static ResourceLocation getConfiguredSoundId(String soundId) {
        try {
            return new ResourceLocation(soundId);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public enum NotificationSound {
        MESSAGE_SENT {
            @Override
            String configuredSound() {
                return Config.messageSentSound;
            }
        },
        MESSAGE_RECEIVED {
            @Override
            String configuredSound() {
                return Config.messageReceivedSound;
            }
        },
        CALLING {
            @Override
            String configuredSound() {
                return Config.callingSound;
            }
        },
        INCOMING_CALL {
            @Override
            String configuredSound() {
                return Config.incomingCallSound;
            }
        };

        abstract String configuredSound();
    }
}
