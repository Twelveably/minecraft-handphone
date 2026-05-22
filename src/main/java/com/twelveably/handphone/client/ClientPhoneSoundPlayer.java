package com.twelveably.handphone.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class ClientPhoneSoundPlayer {
    private ClientPhoneSoundPlayer() {
    }

    public static void play(String soundId, double x, double y, double z, float volume) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        minecraft.level.playLocalSound(x, y, z, getConfiguredSound(soundId), SoundSource.PLAYERS, Math.max(0.0F, Math.min(1.0F, volume)), 1.0F, false);
    }

    private static SoundEvent getConfiguredSound(String soundId) {
        try {
            SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.get(new ResourceLocation(soundId));
            return soundEvent == null ? SoundEvents.NOTE_BLOCK_PLING.get() : soundEvent;
        } catch (IllegalArgumentException ignored) {
            return SoundEvents.NOTE_BLOCK_PLING.get();
        }
    }
}
