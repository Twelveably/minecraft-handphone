package com.twelveably.handphone.network;

import com.twelveably.handphone.client.ClientPhoneHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncCallStatePacket(State state, String localPhoneNumber, String contactName, String phoneNumber, String remoteProfileId) {
    private static final int MAX_NAME_LENGTH = 32;
    private static final int MAX_PHONE_NUMBER_LENGTH = 7;
    private static final int MAX_PROFILE_ID_LENGTH = 36;

    public static void encode(SyncCallStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.state);
        buffer.writeUtf(packet.localPhoneNumber, MAX_PHONE_NUMBER_LENGTH);
        buffer.writeUtf(packet.contactName, MAX_NAME_LENGTH);
        buffer.writeUtf(packet.phoneNumber, MAX_PHONE_NUMBER_LENGTH);
        buffer.writeUtf(packet.remoteProfileId, MAX_PROFILE_ID_LENGTH);
    }

    public static SyncCallStatePacket decode(FriendlyByteBuf buffer) {
        return new SyncCallStatePacket(buffer.readEnum(State.class), buffer.readUtf(MAX_PHONE_NUMBER_LENGTH), buffer.readUtf(MAX_NAME_LENGTH), buffer.readUtf(MAX_PHONE_NUMBER_LENGTH), buffer.readUtf(MAX_PROFILE_ID_LENGTH));
    }

    public static void handle(SyncCallStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPhoneHooks.setCallState(packet.state, packet.localPhoneNumber, packet.contactName, packet.phoneNumber, packet.remoteProfileId));
    }

    public enum State {
        NONE,
        OUTGOING_CALLING,
        OUTGOING_RINGING,
        INCOMING_RINGING,
        ACTIVE
    }
}
