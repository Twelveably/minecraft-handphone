package com.twelveably.handphone.phone;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class PhoneChats {
    private static final String DATA_NAME = "handphone_chats";
    private static final String CHATS_KEY = "Chats";
    private static final String PHONE_NUMBER_KEY = "PhoneNumber";
    private static final String MESSAGES_KEY = "Messages";
    private static final String MESSAGE_KEY = "Message";
    private static final String OUTGOING_KEY = "Outgoing";
    private static final String TIMESTAMP_KEY = "TimestampMillis";

    private PhoneChats() {
    }

    public static List<PhoneChatMessageData> getMessages(ItemStack stack, String phoneNumber) {
        return PhoneNumbers.getPhoneNumber(stack)
                .map(localPhoneNumber -> getMessagesFromData(stack, localPhoneNumber, phoneNumber))
                .orElseGet(() -> getLegacyMessages(stack, phoneNumber));
    }

    public static List<PhoneChatMessageData> getMessages(MinecraftServer server, String localPhoneNumber, String phoneNumber) {
        return getData(server).getMessages(localPhoneNumber, phoneNumber);
    }

    public static List<String> getConversationNumbers(MinecraftServer server, String localPhoneNumber) {
        return getData(server).getConversationNumbers(localPhoneNumber);
    }

    public static String getLatestMessage(MinecraftServer server, String localPhoneNumber, String phoneNumber) {
        List<PhoneChatMessageData> messages = getMessages(server, localPhoneNumber, phoneNumber);
        return messages.isEmpty() ? "" : messages.get(messages.size() - 1).message();
    }

    public static long getLatestMessageTimeMillis(MinecraftServer server, String localPhoneNumber, String phoneNumber) {
        List<PhoneChatMessageData> messages = getMessages(server, localPhoneNumber, phoneNumber);
        return messages.isEmpty() ? 0L : messages.get(messages.size() - 1).timestampMillis();
    }

    public static int getUnreadCount(MinecraftServer server, String localPhoneNumber, String phoneNumber) {
        return getData(server).getUnreadCount(localPhoneNumber, phoneNumber);
    }

    public static boolean hasPhoneData(MinecraftServer server, String phoneNumber) {
        return getData(server).hasPhoneData(phoneNumber);
    }

    public static void movePhoneData(MinecraftServer server, String oldPhoneNumber, String newPhoneNumber) {
        getData(server).movePhoneData(oldPhoneNumber, newPhoneNumber);
    }

    public static void clearUnreadCount(MinecraftServer server, String localPhoneNumber, String phoneNumber) {
        getData(server).clearUnreadCount(localPhoneNumber, phoneNumber);
    }

    private static List<PhoneChatMessageData> getMessagesFromData(ItemStack stack, String localPhoneNumber, String phoneNumber) {
        return getLegacyMessages(stack, phoneNumber);
    }

    private static List<PhoneChatMessageData> getLegacyMessages(ItemStack stack, String phoneNumber) {
        List<PhoneChatMessageData> messages = new ArrayList<>();
        CompoundTag conversation = getConversation(stack, phoneNumber, false);
        if (conversation == null || !conversation.contains(MESSAGES_KEY, Tag.TAG_LIST)) {
            return messages;
        }

        ListTag messageTags = conversation.getList(MESSAGES_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < messageTags.size(); i++) {
            CompoundTag messageTag = messageTags.getCompound(i);
            if (!messageTag.contains(MESSAGE_KEY, Tag.TAG_STRING)) {
                continue;
            }

            long timestampMillis = messageTag.contains(TIMESTAMP_KEY, Tag.TAG_LONG) ? messageTag.getLong(TIMESTAMP_KEY) : 0L;
            messages.add(new PhoneChatMessageData(messageTag.getString(MESSAGE_KEY), messageTag.getBoolean(OUTGOING_KEY), timestampMillis));
        }

        return messages;
    }

    public static String getLatestMessage(ItemStack stack, String phoneNumber) {
        List<PhoneChatMessageData> messages = getMessages(stack, phoneNumber);
        if (messages.isEmpty()) {
            return "";
        }

        return messages.get(messages.size() - 1).message();
    }

    public static long getLatestMessageTimeMillis(ItemStack stack, String phoneNumber) {
        List<PhoneChatMessageData> messages = getMessages(stack, phoneNumber);
        if (messages.isEmpty()) {
            return 0L;
        }

        return messages.get(messages.size() - 1).timestampMillis();
    }

    public static void addMessage(ItemStack stack, String phoneNumber, String message, boolean outgoing) {
        addMessage(stack, phoneNumber, message, outgoing, System.currentTimeMillis());
    }

    public static void addMessage(ItemStack stack, String phoneNumber, String message, boolean outgoing, long timestampMillis) {
        PhoneNumbers.getPhoneNumber(stack).ifPresent(localPhoneNumber -> addMessage(stack, localPhoneNumber, phoneNumber, message, outgoing, timestampMillis));
    }

    public static void addMessage(MinecraftServer server, String localPhoneNumber, String phoneNumber, String message, boolean outgoing, long timestampMillis) {
        getData(server).addMessage(localPhoneNumber, phoneNumber, message, outgoing, timestampMillis);
    }

    private static void addMessage(ItemStack stack, String localPhoneNumber, String phoneNumber, String message, boolean outgoing, long timestampMillis) {
        addLegacyMessage(stack, phoneNumber, message, outgoing, timestampMillis);
    }

    public static void migrateLegacyMessages(MinecraftServer server, ItemStack stack) {
        PhoneNumbers.getPhoneNumber(stack).ifPresent(localPhoneNumber -> {
            CompoundTag tag = stack.getTag();
            if (tag == null || !tag.contains(CHATS_KEY, Tag.TAG_LIST)) {
                return;
            }

            ListTag conversations = tag.getList(CHATS_KEY, Tag.TAG_COMPOUND);
            for (int i = 0; i < conversations.size(); i++) {
                CompoundTag conversation = conversations.getCompound(i);
                String conversationNumber = conversation.getString(PHONE_NUMBER_KEY);
                if (!getMessages(server, localPhoneNumber, conversationNumber).isEmpty()) {
                    continue;
                }

                for (PhoneChatMessageData message : getLegacyMessages(stack, conversationNumber)) {
                    addMessage(server, localPhoneNumber, conversationNumber, message.message(), message.outgoing(), message.timestampMillis());
                }
            }
            tag.remove(CHATS_KEY);
        });
    }

    private static void addLegacyMessage(ItemStack stack, String phoneNumber, String message, boolean outgoing, long timestampMillis) {
        CompoundTag conversation = getConversation(stack, phoneNumber, true);
        if (conversation == null) {
            return;
        }

        ListTag messageTags = conversation.getList(MESSAGES_KEY, Tag.TAG_COMPOUND);
        CompoundTag messageTag = new CompoundTag();
        messageTag.putString(MESSAGE_KEY, message);
        messageTag.putBoolean(OUTGOING_KEY, outgoing);
        messageTag.putLong(TIMESTAMP_KEY, timestampMillis);
        messageTags.add(messageTag);
        conversation.put(MESSAGES_KEY, messageTags);
    }

    private static PhoneChatSavedData getData(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(PhoneChatSavedData::load, PhoneChatSavedData::new, DATA_NAME);
    }

    private static CompoundTag getConversation(ItemStack stack, String phoneNumber, boolean create) {
        CompoundTag tag = create ? stack.getOrCreateTag() : stack.getTag();
        if (tag == null) {
            return null;
        }

        ListTag conversations = tag.getList(CHATS_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < conversations.size(); i++) {
            CompoundTag conversation = conversations.getCompound(i);
            if (conversation.contains(PHONE_NUMBER_KEY, Tag.TAG_STRING) && phoneNumber.equals(conversation.getString(PHONE_NUMBER_KEY))) {
                return conversation;
            }
        }

        if (!create) {
            return null;
        }

        CompoundTag conversation = new CompoundTag();
        conversation.putString(PHONE_NUMBER_KEY, phoneNumber);
        conversation.put(MESSAGES_KEY, new ListTag());
        conversations.add(conversation);
        tag.put(CHATS_KEY, conversations);
        return conversation;
    }
}
