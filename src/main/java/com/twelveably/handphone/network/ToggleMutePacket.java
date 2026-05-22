package com.twelveably.handphone.network;

import com.twelveably.handphone.voice.PhoneCallManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ToggleMutePacket(String sourcePhoneNumber) {
    private static final int MAX_PHONE_NUMBER_LENGTH = 7;

    public static void encode(ToggleMutePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.sourcePhoneNumber, MAX_PHONE_NUMBER_LENGTH);
    }

    public static ToggleMutePacket decode(FriendlyByteBuf buffer) {
        return new ToggleMutePacket(buffer.readUtf(MAX_PHONE_NUMBER_LENGTH));
    }

    public static void handle(ToggleMutePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            PhoneCallManager.toggleMute(player, packet.sourcePhoneNumber);
        }
    }
}
