package com.twelveably.handphone.network;

import com.twelveably.handphone.voice.PhoneCallManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RespondCallPacket(String sourcePhoneNumber, boolean accepted) {
    private static final int MAX_PHONE_NUMBER_LENGTH = 7;

    public static void encode(RespondCallPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.sourcePhoneNumber, MAX_PHONE_NUMBER_LENGTH);
        buffer.writeBoolean(packet.accepted);
    }

    public static RespondCallPacket decode(FriendlyByteBuf buffer) {
        return new RespondCallPacket(buffer.readUtf(MAX_PHONE_NUMBER_LENGTH), buffer.readBoolean());
    }

    public static void handle(RespondCallPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            PhoneCallManager.respondToPendingCall(player, packet.sourcePhoneNumber, packet.accepted);
        }
    }
}
