package com.twelveably.handphone.network;

import com.twelveably.handphone.phone.PhoneContacts;
import com.twelveably.handphone.phone.PhoneInventory;
import com.twelveably.handphone.phone.PhoneNumbers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record DeleteContactPacket(String sourcePhoneNumber, int contactIndex) {
    private static final int MAX_PHONE_NUMBER_LENGTH = 7;

    public static void encode(DeleteContactPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.sourcePhoneNumber, MAX_PHONE_NUMBER_LENGTH);
        buffer.writeVarInt(packet.contactIndex);
    }

    public static DeleteContactPacket decode(FriendlyByteBuf buffer) {
        return new DeleteContactPacket(buffer.readUtf(MAX_PHONE_NUMBER_LENGTH), buffer.readVarInt());
    }

    public static void handle(DeleteContactPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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
        PhoneContacts.migrateLegacyContacts(player.server, phone);
        String localPhoneNumber = PhoneNumbers.getPhoneNumber(phone).orElse(packet.sourcePhoneNumber);
        if (!PhoneContacts.removeContact(player.server, localPhoneNumber, packet.contactIndex)) {
            player.displayClientMessage(Component.literal("Select a valid contact to delete."), true);
        }

        RequestContactsPacket.syncContacts(player, phone);
    }
}
