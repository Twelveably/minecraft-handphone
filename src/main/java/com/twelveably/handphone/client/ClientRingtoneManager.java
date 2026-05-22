package com.twelveably.handphone.client;

import com.twelveably.handphone.Handphone;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Handphone.MODID, value = Dist.CLIENT)
public final class ClientRingtoneManager {
    private static final long REPLAY_DELAY_MS = 1000L;
    private static final long UPDATE_TIMEOUT_MS = 2500L;
    private static final Map<String, Ringtone> RINGTONES_BY_PHONE = new HashMap<>();

    private ClientRingtoneManager() {
    }

    public static void setRingtone(boolean active, String phoneNumber, String soundId, double x, double y, double z, float volume) {
        if (!active) {
            Ringtone ringtone = RINGTONES_BY_PHONE.remove(phoneNumber);
            if (ringtone != null) {
                ringtone.active = false;
                if (ringtone.currentSound != null) {
                    Minecraft.getInstance().getSoundManager().stop(ringtone.currentSound);
                }
            }
            return;
        }

        Ringtone ringtone = RINGTONES_BY_PHONE.computeIfAbsent(phoneNumber, ignored -> new Ringtone());
        float nextVolume = Math.max(0.0F, Math.min(1.0F, volume));
        if (ringtone.currentSound != null && isPlaying(ringtone) && shouldRestart(ringtone, soundId, x, y, z, nextVolume)) {
            Minecraft.getInstance().getSoundManager().stop(ringtone.currentSound);
            ringtone.currentSound = null;
            ringtone.nextPlayAtMillis = 0L;
        }
        ringtone.active = true;
        ringtone.soundId = soundId;
        ringtone.x = x;
        ringtone.y = y;
        ringtone.z = z;
        ringtone.volume = nextVolume;
        ringtone.lastUpdateAtMillis = Util.getMillis();
        playIfReady(ringtone);
    }

    public static void clearRingtones() {
        for (Ringtone ringtone : RINGTONES_BY_PHONE.values()) {
            ringtone.active = false;
            if (ringtone.currentSound != null) {
                Minecraft.getInstance().getSoundManager().stop(ringtone.currentSound);
            }
        }
        RINGTONES_BY_PHONE.clear();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Iterator<Map.Entry<String, Ringtone>> iterator = RINGTONES_BY_PHONE.entrySet().iterator();
        while (iterator.hasNext()) {
            Ringtone ringtone = iterator.next().getValue();
            if (!ringtone.active && !isPlaying(ringtone)) {
                iterator.remove();
                continue;
            }

            if (ringtone.active && Util.getMillis() - ringtone.lastUpdateAtMillis > UPDATE_TIMEOUT_MS) {
                ringtone.active = false;
                if (ringtone.currentSound != null) {
                    Minecraft.getInstance().getSoundManager().stop(ringtone.currentSound);
                    ringtone.currentSound = null;
                }
            }

            playIfReady(ringtone);
        }
    }

    private static void playIfReady(Ringtone ringtone) {
        if (!ringtone.active || Minecraft.getInstance().level == null) {
            return;
        }

        if (isPlaying(ringtone)) {
            ringtone.nextPlayAtMillis = 0L;
            return;
        }

        long now = Util.getMillis();
        if (ringtone.currentSound != null && ringtone.nextPlayAtMillis <= 0L) {
            ringtone.nextPlayAtMillis = now + REPLAY_DELAY_MS;
            return;
        }

        if (ringtone.nextPlayAtMillis > now) {
            return;
        }

        SoundEvent soundEvent = getConfiguredSound(ringtone.soundId);
        SimpleSoundInstance sound = new SimpleSoundInstance(soundEvent, SoundSource.PLAYERS, ringtone.volume, 1.0F, Minecraft.getInstance().level.random, ringtone.x, ringtone.y, ringtone.z);
        Minecraft.getInstance().getSoundManager().play(sound);
        ringtone.currentSound = sound;
        ringtone.nextPlayAtMillis = 0L;
    }

    private static boolean isPlaying(Ringtone ringtone) {
        return ringtone.currentSound != null && Minecraft.getInstance().getSoundManager().isActive(ringtone.currentSound);
    }

    private static boolean shouldRestart(Ringtone ringtone, String soundId, double x, double y, double z, float volume) {
        if (!ringtone.soundId.equals(soundId)) {
            return true;
        }

        double dx = ringtone.x - x;
        double dy = ringtone.y - y;
        double dz = ringtone.z - z;
        return dx * dx + dy * dy + dz * dz > 0.25D || Math.abs(ringtone.volume - volume) > 0.05F;
    }

    private static SoundEvent getConfiguredSound(String soundId) {
        try {
            SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.get(new ResourceLocation(soundId));
            return soundEvent == null ? SoundEvents.NOTE_BLOCK_PLING.get() : soundEvent;
        } catch (IllegalArgumentException ignored) {
            return SoundEvents.NOTE_BLOCK_PLING.get();
        }
    }

    private static class Ringtone {
        private boolean active;
        private String soundId = "";
        private double x;
        private double y;
        private double z;
        private float volume = 1.0F;
        private long lastUpdateAtMillis;
        private long nextPlayAtMillis;
        private SimpleSoundInstance currentSound;
    }
}
