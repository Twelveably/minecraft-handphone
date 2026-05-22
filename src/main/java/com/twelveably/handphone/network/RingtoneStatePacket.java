package com.twelveably.handphone.network;

import com.twelveably.handphone.client.ClientRingtoneManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RingtoneStatePacket(boolean active, String phoneNumber, String soundId, double x, double y, double z, float volume) {
    private static final int MAX_PHONE_NUMBER_LENGTH = 7;
    private static final int MAX_SOUND_ID_LENGTH = 128;

    public static void encode(RingtoneStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
        buffer.writeUtf(packet.phoneNumber, MAX_PHONE_NUMBER_LENGTH);
        buffer.writeUtf(packet.soundId, MAX_SOUND_ID_LENGTH);
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeFloat(packet.volume);
    }

    public static RingtoneStatePacket decode(FriendlyByteBuf buffer) {
        return new RingtoneStatePacket(buffer.readBoolean(), buffer.readUtf(MAX_PHONE_NUMBER_LENGTH), buffer.readUtf(MAX_SOUND_ID_LENGTH), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readFloat());
    }

    public static void handle(RingtoneStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientRingtoneManager.setRingtone(packet.active, packet.phoneNumber, packet.soundId, packet.x, packet.y, packet.z, packet.volume));
    }
}
