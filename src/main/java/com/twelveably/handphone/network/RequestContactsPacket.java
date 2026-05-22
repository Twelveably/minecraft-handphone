package com.twelveably.handphone.network;

import com.twelveably.handphone.phone.PhoneChats;
import com.twelveably.handphone.phone.PhoneContacts;
import com.twelveably.handphone.phone.PhoneInventory;
import com.twelveably.handphone.phone.PhoneNumbers;
import com.twelveably.handphone.voice.PhoneCallManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public record RequestContactsPacket(String phoneNumber) {
    private static final int MAX_PHONE_NUMBER_LENGTH = 7;

    public static void encode(RequestContactsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.phoneNumber, MAX_PHONE_NUMBER_LENGTH);
    }

    public static RequestContactsPacket decode(FriendlyByteBuf buffer) {
        return new RequestContactsPacket(buffer.readUtf(MAX_PHONE_NUMBER_LENGTH));
    }

    public static void handle(RequestContactsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player == null) {
            return;
        }

        ItemStack phone = PhoneInventory.getPhoneByNumber(player, packet.phoneNumber);
        if (phone.isEmpty()) {
            return;
        }

        PhoneNumbers.ensureAssigned(phone, player.serverLevel());
        PhoneContacts.migrateLegacyContacts(player.server, phone);
        PhoneChats.migrateLegacyMessages(player.server, phone);
        syncContacts(player, phone);
        PhoneCallManager.syncCallStateForPhone(player, phone);
    }

    static void syncContacts(ServerPlayer player, ItemStack phone) {
        HandphoneNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncContactsPacket(PhoneContacts.getContacts(player.server, phone)));
    }
}
