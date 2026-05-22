package com.twelveably.handphone.network;

import com.twelveably.handphone.phone.PhoneChats;
import com.twelveably.handphone.phone.PhoneContacts;
import com.twelveably.handphone.phone.PhoneInventory;
import com.twelveably.handphone.phone.PhoneLocator;
import com.twelveably.handphone.phone.PhoneNumbers;
import com.twelveably.handphone.phone.PhoneSoundPlayer;
import com.twelveably.handphone.phone.PhoneSignals;
import com.twelveably.handphone.Config;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Optional;
import java.util.function.Supplier;

public record ChatMessagePacket(String sourcePhoneNumber, String contactName, String targetPhoneNumber, String message) {
    private static final int MAX_NAME_LENGTH = 32;
    private static final int MAX_PHONE_NUMBER_LENGTH = 7;
    private static final int MAX_MESSAGE_LENGTH = 64;

    public static void encode(ChatMessagePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.sourcePhoneNumber, MAX_PHONE_NUMBER_LENGTH);
        buffer.writeUtf(packet.contactName, MAX_NAME_LENGTH);
        buffer.writeUtf(packet.targetPhoneNumber, MAX_PHONE_NUMBER_LENGTH);
        buffer.writeUtf(packet.message, MAX_MESSAGE_LENGTH);
    }

    public static ChatMessagePacket decode(FriendlyByteBuf buffer) {
        return new ChatMessagePacket(buffer.readUtf(MAX_PHONE_NUMBER_LENGTH), buffer.readUtf(MAX_NAME_LENGTH), buffer.readUtf(MAX_PHONE_NUMBER_LENGTH), buffer.readUtf(MAX_MESSAGE_LENGTH));
    }

    public static void handle(ChatMessagePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender == null) {
            return;
        }

        ItemStack senderPhone = PhoneInventory.getBestPhoneForContact(sender, packet.sourcePhoneNumber, packet.targetPhoneNumber);
        if (senderPhone.isEmpty()) {
            sender.displayClientMessage(Component.literal("You need a phone to send messages."), true);
            return;
        }

        PhoneNumbers.ensureAssigned(senderPhone, sender.serverLevel());
        Optional<String> senderPhoneNumber = PhoneNumbers.getPhoneNumber(senderPhone);
        if (senderPhoneNumber.isEmpty()) {
            sender.displayClientMessage(Component.literal("Your phone has no number."), true);
            return;
        }

        String targetPhoneNumber = packet.targetPhoneNumber.trim();
        String message = packet.message.trim();
        if (message.length() > MAX_MESSAGE_LENGTH) {
            message = message.substring(0, MAX_MESSAGE_LENGTH);
        }

        if (message.isEmpty()) {
            return;
        }
        if (!PhoneSignals.isEnabled()) {
            sender.displayClientMessage(Component.literal(packet.contactName + " is not reachable."), true);
            return;
        }
        String deliveredMessage = message;
        long timestampMillis = System.currentTimeMillis();

        Optional<PhoneLocator.LocatedPhone> target = PhoneLocator.findPhone(sender.server, targetPhoneNumber);
        if (target.isEmpty()) {
            sender.displayClientMessage(Component.literal(packet.contactName + " is not reachable."), true);
            return;
        }

        Optional<ItemStack> targetPhone = target.get().phoneStack(targetPhoneNumber);
        if (targetPhone.isEmpty()) {
            sender.displayClientMessage(Component.literal(packet.contactName + " is not reachable."), true);
            return;
        }

        PhoneChats.migrateLegacyMessages(sender.server, senderPhone);
        PhoneChats.migrateLegacyMessages(sender.server, targetPhone.get());
        PhoneChats.addMessage(sender.server, senderPhoneNumber.get(), targetPhoneNumber, deliveredMessage, true, timestampMillis);
        PhoneChats.addMessage(sender.server, targetPhoneNumber, senderPhoneNumber.get(), deliveredMessage, false, timestampMillis);
        PhoneSoundPlayer.playAtPhone(sender.server, senderPhoneNumber.get(), Config.messageSentSound);
        PhoneSoundPlayer.playAtPhone(sender.server, targetPhoneNumber, Config.messageReceivedSound);

        HandphoneNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sender),
                new SyncChatMessagesPacket(targetPhoneNumber, PhoneChats.getMessages(sender.server, senderPhoneNumber.get(), targetPhoneNumber)));
        target.get().player().ifPresent(targetPlayer -> {
            String displayName = PhoneContacts.getContactName(sender.server, targetPhoneNumber, senderPhoneNumber.get(), senderPhoneNumber.get());
            HandphoneNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> targetPlayer),
                    new ReceiveChatMessagePacket(senderPhoneNumber.get(), displayName, deliveredMessage, false, timestampMillis));
            HandphoneNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> targetPlayer),
                    new SyncChatMessagesPacket(senderPhoneNumber.get(), PhoneChats.getMessages(sender.server, targetPhoneNumber, senderPhoneNumber.get())));
        });
    }

}
