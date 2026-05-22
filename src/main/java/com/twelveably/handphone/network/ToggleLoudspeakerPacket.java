package com.twelveably.handphone.network;

import com.twelveably.handphone.voice.PhoneCallManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ToggleLoudspeakerPacket(String sourcePhoneNumber) {
    private static final int MAX_PHONE_NUMBER_LENGTH = 7;

    public static void encode(ToggleLoudspeakerPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.sourcePhoneNumber, MAX_PHONE_NUMBER_LENGTH);
    }

    public static ToggleLoudspeakerPacket decode(FriendlyByteBuf buffer) {
        return new ToggleLoudspeakerPacket(buffer.readUtf(MAX_PHONE_NUMBER_LENGTH));
    }

    public static void handle(ToggleLoudspeakerPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            PhoneCallManager.toggleLoudspeaker(player, packet.sourcePhoneNumber);
        }
    }
}
