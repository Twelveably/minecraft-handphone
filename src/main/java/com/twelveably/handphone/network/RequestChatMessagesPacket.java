package com.twelveably.handphone.network;

import com.twelveably.handphone.phone.PhoneChats;
import com.twelveably.handphone.phone.PhoneInventory;
import com.twelveably.handphone.phone.PhoneNumbers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public record RequestChatMessagesPacket(String sourcePhoneNumber, String phoneNumber) {
    private static final int MAX_PHONE_NUMBER_LENGTH = 7;

    public static void encode(RequestChatMessagesPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.sourcePhoneNumber, MAX_PHONE_NUMBER_LENGTH);
        buffer.writeUtf(packet.phoneNumber, MAX_PHONE_NUMBER_LENGTH);
    }

    public static RequestChatMessagesPacket decode(FriendlyByteBuf buffer) {
        return new RequestChatMessagesPacket(buffer.readUtf(MAX_PHONE_NUMBER_LENGTH), buffer.readUtf(MAX_PHONE_NUMBER_LENGTH));
    }

    public static void handle(RequestChatMessagesPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player == null) {
            return;
        }

        ItemStack phone = PhoneInventory.getPhoneByNumber(player, packet.sourcePhoneNumber);
        if (phone.isEmpty()) {
            return;
        }

        PhoneNumbers.ensureAssigned(phone, player.serverLevel());
        PhoneChats.migrateLegacyMessages(player.server, phone);
        String localPhoneNumber = PhoneNumbers.getPhoneNumber(phone).orElse(packet.sourcePhoneNumber);
        PhoneChats.clearUnreadCount(player.server, localPhoneNumber, packet.phoneNumber);
        HandphoneNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncChatMessagesPacket(packet.phoneNumber, PhoneChats.getMessages(player.server, localPhoneNumber, packet.phoneNumber)));
    }
}
