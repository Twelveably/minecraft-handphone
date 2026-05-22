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

public record RenameContactPacket(String sourcePhoneNumber, String contactPhoneNumber, String name) {
    private static final int MAX_PHONE_NUMBER_LENGTH = 7;
    private static final int MAX_NAME_LENGTH = 32;

    public static void encode(RenameContactPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.sourcePhoneNumber, MAX_PHONE_NUMBER_LENGTH);
        buffer.writeUtf(packet.contactPhoneNumber, MAX_PHONE_NUMBER_LENGTH);
        buffer.writeUtf(packet.name, MAX_NAME_LENGTH);
    }

    public static RenameContactPacket decode(FriendlyByteBuf buffer) {
        return new RenameContactPacket(
                buffer.readUtf(MAX_PHONE_NUMBER_LENGTH),
                buffer.readUtf(MAX_PHONE_NUMBER_LENGTH),
                buffer.readUtf(MAX_NAME_LENGTH)
        );
    }

    public static void handle(RenameContactPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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
        String name = packet.name.trim();
        if (name.isEmpty()) {
            player.displayClientMessage(Component.literal("Contact name cannot be empty."), true);
            syncContacts(player, phone);
            return;
        }

        if (!PhoneContacts.renameContact(player.server, localPhoneNumber, packet.contactPhoneNumber, name)) {
            player.displayClientMessage(Component.literal("Contact not found."), true);
        }

        syncContacts(player, phone);
    }

    private static void syncContacts(ServerPlayer player, ItemStack phone) {
        RequestContactsPacket.syncContacts(player, phone);
    }
}
