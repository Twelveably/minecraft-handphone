package com.twelveably.handphone.network;

import com.twelveably.handphone.client.ClientPhoneHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncLoudspeakerStatePacket(String localPhoneNumber, boolean enabled) {
    private static final int MAX_PHONE_NUMBER_LENGTH = 7;

    public static void encode(SyncLoudspeakerStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.localPhoneNumber, MAX_PHONE_NUMBER_LENGTH);
        buffer.writeBoolean(packet.enabled);
    }

    public static SyncLoudspeakerStatePacket decode(FriendlyByteBuf buffer) {
        return new SyncLoudspeakerStatePacket(buffer.readUtf(MAX_PHONE_NUMBER_LENGTH), buffer.readBoolean());
    }

    public static void handle(SyncLoudspeakerStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPhoneHooks.setLoudspeakerState(packet.localPhoneNumber, packet.enabled));
    }
}
