package com.twelveably.handphone.network;

import com.twelveably.handphone.client.ClientPhoneHooks;
import com.twelveably.handphone.phone.PhoneChatMessageData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record SyncChatMessagesPacket(String phoneNumber, List<PhoneChatMessageData> messages) {
    private static final int MAX_PHONE_NUMBER_LENGTH = 7;
    private static final int MAX_MESSAGE_LENGTH = 64;
    private static final int MAX_MESSAGES = 512;

    public static void encode(SyncChatMessagesPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.phoneNumber, MAX_PHONE_NUMBER_LENGTH);
        int firstMessageIndex = Math.max(0, packet.messages.size() - MAX_MESSAGES);
        buffer.writeVarInt(packet.messages.size() - firstMessageIndex);
        for (int i = firstMessageIndex; i < packet.messages.size(); i++) {
            PhoneChatMessageData message = packet.messages.get(i);
            buffer.writeUtf(truncateMessage(message.message()), MAX_MESSAGE_LENGTH);
            buffer.writeBoolean(message.outgoing());
            buffer.writeLong(message.timestampMillis());
        }
    }

    public static SyncChatMessagesPacket decode(FriendlyByteBuf buffer) {
        String phoneNumber = buffer.readUtf(MAX_PHONE_NUMBER_LENGTH);
        int messageCount = Math.min(buffer.readVarInt(), MAX_MESSAGES);
        List<PhoneChatMessageData> messages = new ArrayList<>();
        for (int i = 0; i < messageCount; i++) {
            messages.add(new PhoneChatMessageData(buffer.readUtf(MAX_MESSAGE_LENGTH), buffer.readBoolean(), buffer.readLong()));
        }

        return new SyncChatMessagesPacket(phoneNumber, messages);
    }

    public static void handle(SyncChatMessagesPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPhoneHooks.setChatMessages(packet.phoneNumber, packet.messages));
    }

    private static String truncateMessage(String message) {
        return message.length() > MAX_MESSAGE_LENGTH ? message.substring(0, MAX_MESSAGE_LENGTH) : message;
    }
}
