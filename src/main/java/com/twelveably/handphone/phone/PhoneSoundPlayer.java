package com.twelveably.handphone.phone;

import com.twelveably.handphone.Config;
import com.twelveably.handphone.network.HandphoneNetwork;
import com.twelveably.handphone.network.PlayPhoneSoundPacket;
import com.twelveably.handphone.network.RingtoneStatePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public final class PhoneSoundPlayer {
    private PhoneSoundPlayer() {
    }

    public static void playAtPhone(MinecraftServer server, String phoneNumber, String soundId) {
        PhoneLocator.findPhone(server, phoneNumber).ifPresent(phone -> {
            if (!phone.isLoaded() || phone.level() == null || !phone.canEmitSound()) {
                return;
            }

            BlockPos soundPos = phone.soundPos();
            double x = soundPos.getX() + 0.5D;
            double y = soundPos.getY() + 0.5D;
            double z = soundPos.getZ() + 0.5D;
            double radius = Math.max(1.0D, Config.phoneSoundRadiusBlocks);
            double radiusSq = radius * radius;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                double distanceSq = player.distanceToSqr(x, y, z);
                if (player.serverLevel() != phone.level() || distanceSq > radiusSq) {
                    continue;
                }

                HandphoneNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PlayPhoneSoundPacket(soundId, x, y, z, linearVolume(distanceSq, radius)));
            }
        });
    }

    public static void updateRingtoneAtPhone(MinecraftServer server, String phoneNumber, String soundId) {
        PhoneLocator.findPhone(server, phoneNumber).ifPresent(phone -> {
            if (!phone.isLoaded() || phone.level() == null || !phone.canEmitSound()) {
                return;
            }

            BlockPos soundPos = phone.soundPos();
            double x = soundPos.getX() + 0.5D;
            double y = soundPos.getY() + 0.5D;
            double z = soundPos.getZ() + 0.5D;
            double radius = Math.max(1.0D, Config.phoneSoundRadiusBlocks);
            double radiusSq = radius * radius;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                double distanceSq = player.distanceToSqr(x, y, z);
                if (player.serverLevel() != phone.level() || distanceSq > radiusSq) {
                    continue;
                }

                HandphoneNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RingtoneStatePacket(true, phoneNumber, soundId, x, y, z, linearVolume(distanceSq, radius)));
            }
        });
    }

    public static void stopRingtone(MinecraftServer server, String phoneNumber) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            HandphoneNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RingtoneStatePacket(false, phoneNumber, "", 0.0D, 0.0D, 0.0D, 0.0F));
        }
    }

    private static float linearVolume(double distanceSq, double radius) {
        double distance = Math.sqrt(distanceSq);
        double volume = 1.0D - distance / radius;
        return (float) Math.max(0.05D, Math.min(1.0D, volume));
    }
}
