package com.twelveably.handphone.network;

import com.twelveably.handphone.client.ClientPhoneHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncMuteStatePacket(String localPhoneNumber, boolean muted) {
    private static final int MAX_PHONE_NUMBER_LENGTH = 7;

    public static void encode(SyncMuteStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.localPhoneNumber, MAX_PHONE_NUMBER_LENGTH);
        buffer.writeBoolean(packet.muted);
    }

    public static SyncMuteStatePacket decode(FriendlyByteBuf buffer) {
        return new SyncMuteStatePacket(buffer.readUtf(MAX_PHONE_NUMBER_LENGTH), buffer.readBoolean());
    }

    public static void handle(SyncMuteStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPhoneHooks.setMuteState(packet.localPhoneNumber, packet.muted));
    }
}
