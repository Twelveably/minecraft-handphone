package com.twelveably.handphone.network;

import com.twelveably.handphone.client.ClientPhoneHooks;
import com.twelveably.handphone.phone.PhoneContactData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record SyncContactsPacket(List<PhoneContactData> contacts) {
    private static final int MAX_CONTACTS = 128;
    private static final int MAX_NAME_LENGTH = 32;
    private static final int MAX_PHONE_NUMBER_LENGTH = 7;
    private static final int MAX_MESSAGE_LENGTH = 64;
    private static final int MAX_PROFILE_ID_LENGTH = 36;

    public static void encode(SyncContactsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.contacts.size());
        for (PhoneContactData contact : packet.contacts) {
            buffer.writeUtf(contact.name(), MAX_NAME_LENGTH);
            buffer.writeUtf(contact.phoneNumber(), MAX_PHONE_NUMBER_LENGTH);
            buffer.writeUtf(contact.latestMessage(), MAX_MESSAGE_LENGTH);
            buffer.writeLong(contact.latestMessageTimeMillis());
            buffer.writeUtf(contact.profileId(), MAX_PROFILE_ID_LENGTH);
            buffer.writeVarInt(contact.unreadCount());
        }
    }

    public static SyncContactsPacket decode(FriendlyByteBuf buffer) {
        int contactCount = Math.min(buffer.readVarInt(), MAX_CONTACTS);
        List<PhoneContactData> contacts = new ArrayList<>();
        for (int i = 0; i < contactCount; i++) {
            contacts.add(new PhoneContactData(buffer.readUtf(MAX_NAME_LENGTH), buffer.readUtf(MAX_PHONE_NUMBER_LENGTH), buffer.readUtf(MAX_MESSAGE_LENGTH), buffer.readLong(), buffer.readUtf(MAX_PROFILE_ID_LENGTH), buffer.readVarInt()));
        }

        return new SyncContactsPacket(contacts);
    }

    public static void handle(SyncContactsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPhoneHooks.setContacts(packet.contacts));
    }
}
