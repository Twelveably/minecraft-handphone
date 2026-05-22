package com.twelveably.handphone.network;

import com.twelveably.handphone.phone.PhoneContacts;
import com.twelveably.handphone.phone.PhoneInventory;
import com.twelveably.handphone.phone.PhoneLocator;
import com.twelveably.handphone.phone.PhoneNumbers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record AddContactPacket(String sourcePhoneNumber, String name, String phoneNumber) {
    private static final int MAX_NAME_LENGTH = 6;
    private static final int MAX_PHONE_NUMBER_LENGTH = 7;

    public static void encode(AddContactPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.sourcePhoneNumber, MAX_PHONE_NUMBER_LENGTH);
        buffer.writeUtf(packet.name, MAX_NAME_LENGTH);
        buffer.writeUtf(packet.phoneNumber, MAX_PHONE_NUMBER_LENGTH);
    }

    public static AddContactPacket decode(FriendlyByteBuf buffer) {
        return new AddContactPacket(buffer.readUtf(MAX_PHONE_NUMBER_LENGTH), buffer.readUtf(MAX_NAME_LENGTH), buffer.readUtf(MAX_PHONE_NUMBER_LENGTH));
    }

    public static void handle(AddContactPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player == null) {
            return;
        }

        ItemStack phone = PhoneInventory.getPhoneByNumber(player, packet.sourcePhoneNumber);
        if (phone.isEmpty()) {
            return;
        }

        String phoneNumber = packet.phoneNumber.trim();
        if (phoneNumber.isEmpty() || phoneNumber.length() > MAX_PHONE_NUMBER_LENGTH || !phoneNumber.chars().allMatch(Character::isDigit)) {
            player.displayClientMessage(Component.literal("Invalid phone number."), true);
            return;
        }

        PhoneNumbers.ensureAssigned(phone, player.serverLevel());
        PhoneContacts.migrateLegacyContacts(player.server, phone);
        String localPhoneNumber = PhoneNumbers.getPhoneNumber(phone).orElse(packet.sourcePhoneNumber);
        if (PhoneNumbers.getPhoneNumber(phone).filter(phoneNumber::equals).isPresent()) {
            player.displayClientMessage(Component.literal("You cannot add your own phone number."), true);
            syncContacts(player, phone);
            return;
        }

        if (!PhoneNumbers.isAssignedNumber(phoneNumber, player.serverLevel())) {
            player.displayClientMessage(Component.literal("No phone has number " + phoneNumber + "."), true);
            syncContacts(player, phone);
            return;
        }

        String name = packet.name.trim();
        String profileId = PhoneLocator.findPhone(player.server, phoneNumber)
                .flatMap(PhoneLocator.LocatedPhone::player)
                .map(holder -> holder.getUUID().toString())
                .orElse("");
        if (!PhoneContacts.addContact(player.server, localPhoneNumber, name.isEmpty() ? "New Contact" : name, phoneNumber, profileId)) {
            player.displayClientMessage(Component.literal("Contact list is full."), true);
        }
        syncContacts(player, phone);
    }

    private static void syncContacts(ServerPlayer player, ItemStack phone) {
        RequestContactsPacket.syncContacts(player, phone);
    }
}
