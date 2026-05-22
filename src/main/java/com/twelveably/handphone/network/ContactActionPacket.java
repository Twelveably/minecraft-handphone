package com.twelveably.handphone.network;

import com.twelveably.handphone.phone.PhoneInventory;
import com.twelveably.handphone.phone.PhoneNumbers;
import com.twelveably.handphone.voice.PhoneCallManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ContactActionPacket(String sourcePhoneNumber, Action action, String contactName, String phoneNumber) {
    private static final int MAX_NAME_LENGTH = 32;
    private static final int MAX_PHONE_NUMBER_LENGTH = 7;

    public static void encode(ContactActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.sourcePhoneNumber, MAX_PHONE_NUMBER_LENGTH);
        buffer.writeEnum(packet.action);
        buffer.writeUtf(packet.contactName, MAX_NAME_LENGTH);
        buffer.writeUtf(packet.phoneNumber, MAX_PHONE_NUMBER_LENGTH);
    }

    public static ContactActionPacket decode(FriendlyByteBuf buffer) {
        return new ContactActionPacket(buffer.readUtf(MAX_PHONE_NUMBER_LENGTH), buffer.readEnum(Action.class), buffer.readUtf(MAX_NAME_LENGTH), buffer.readUtf(MAX_PHONE_NUMBER_LENGTH));
    }

    public static void handle(ContactActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer caller = context.getSender();
        if (caller == null) {
            return;
        }

        String phoneNumber = packet.phoneNumber.trim();
        if (!PhoneNumbers.isAssignedNumber(phoneNumber, caller.serverLevel())) {
            caller.displayClientMessage(Component.literal("No phone has number " + phoneNumber + "."), true);
            return;
        }

        if (packet.action == Action.CHAT) {
            var target = PhoneInventory.findOnlineHolder(caller, phoneNumber);
            if (target.isEmpty()) {
                caller.displayClientMessage(Component.literal(packet.contactName + " is not online."), true);
                return;
            }

            ServerPlayer targetPlayer = target.get();
            caller.displayClientMessage(Component.literal("Chat request sent to " + targetPlayer.getName().getString() + "."), true);
            targetPlayer.displayClientMessage(Component.literal(caller.getName().getString() + " wants to chat with you."), true);
        } else {
            ItemStack callerPhone = PhoneInventory.getBestPhoneForContact(caller, packet.sourcePhoneNumber, phoneNumber);
            PhoneNumbers.ensureAssigned(callerPhone, caller.serverLevel());
            String callerPhoneNumber = PhoneNumbers.getPhoneNumber(callerPhone).orElse("");
            PhoneCallManager.requestOrEndCall(caller, packet.contactName, phoneNumber, callerPhoneNumber);
        }
    }

    public enum Action {
        CHAT,
        CALL
    }

}
