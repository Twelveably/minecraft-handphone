package com.twelveably.handphone.network;

import com.twelveably.handphone.client.ClientPhoneHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record
ReceiveChatMessagePacket(String conversationPhoneNumber, String contactName, String message, boolean outgoing, long timestampMillis) {
    private static final int MAX_NAME_LENGTH = 32;
    private static final int MAX_PHONE_NUMBER_LENGTH = 7;
    private static final int MAX_MESSAGE_LENGTH = 64;

    public static void encode(ReceiveChatMessagePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.conversationPhoneNumber, MAX_PHONE_NUMBER_LENGTH);
        buffer.writeUtf(packet.contactName, MAX_NAME_LENGTH);
        buffer.writeUtf(packet.message, MAX_MESSAGE_LENGTH);
        buffer.writeBoolean(packet.outgoing);
        buffer.writeLong(packet.timestampMillis);
    }

    public static ReceiveChatMessagePacket decode(FriendlyByteBuf buffer) {
        return new ReceiveChatMessagePacket(
                buffer.readUtf(MAX_PHONE_NUMBER_LENGTH),
                buffer.readUtf(MAX_NAME_LENGTH),
                buffer.readUtf(MAX_MESSAGE_LENGTH),
                buffer.readBoolean(),
                buffer.readLong()
        );
    }

    public static void handle(ReceiveChatMessagePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPhoneHooks.receiveChatMessage(
                packet.conversationPhoneNumber,
                packet.contactName,
                packet.message,
                packet.outgoing,
                packet.timestampMillis
        ));
    }
}
