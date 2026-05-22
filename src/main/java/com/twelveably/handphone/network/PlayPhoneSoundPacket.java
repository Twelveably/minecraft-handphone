package com.twelveably.handphone.network;

import com.twelveably.handphone.client.ClientPhoneSoundPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PlayPhoneSoundPacket(String soundId, double x, double y, double z, float volume) {
    private static final int MAX_SOUND_ID_LENGTH = 128;

    public static void encode(PlayPhoneSoundPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.soundId, MAX_SOUND_ID_LENGTH);
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeFloat(packet.volume);
    }

    public static PlayPhoneSoundPacket decode(FriendlyByteBuf buffer) {
        return new PlayPhoneSoundPacket(buffer.readUtf(MAX_SOUND_ID_LENGTH), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readFloat());
    }

    public static void handle(PlayPhoneSoundPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPhoneSoundPlayer.play(packet.soundId, packet.x, packet.y, packet.z, packet.volume));
    }
}
