package com.twelveably.handphone.phone;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhoneChatSavedData extends SavedData {
    private static final String PHONES_KEY = "Phones";
    private static final String PHONE_NUMBER_KEY = "PhoneNumber";
    private static final String CONVERSATIONS_KEY = "Conversations";
    private static final String CONVERSATION_NUMBER_KEY = "ConversationNumber";
    private static final String MESSAGES_KEY = "Messages";
    private static final String MESSAGE_KEY = "Message";
    private static final String OUTGOING_KEY = "Outgoing";
    private static final String TIMESTAMP_KEY = "TimestampMillis";
    private static final String UNREAD_COUNT_KEY = "UnreadCount";
    private static final int MAX_MESSAGES_PER_CONVERSATION = 200;
    private static final int MAX_CONVERSATIONS_PER_PHONE = 50;

    private final Map<String, Map<String, List<PhoneChatMessageData>>> messagesByPhone = new HashMap<>();
    private final Map<String, Map<String, Integer>> unreadCountsByPhone = new HashMap<>();

    public static PhoneChatSavedData load(CompoundTag tag) {
        PhoneChatSavedData data = new PhoneChatSavedData();
        ListTag phoneTags = tag.getList(PHONES_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < phoneTags.size(); i++) {
            CompoundTag phoneTag = phoneTags.getCompound(i);
            String phoneNumber = phoneTag.getString(PHONE_NUMBER_KEY);
            Map<String, List<PhoneChatMessageData>> conversations = data.messagesByPhone.computeIfAbsent(phoneNumber, ignored -> new HashMap<>());
            ListTag conversationTags = phoneTag.getList(CONVERSATIONS_KEY, Tag.TAG_COMPOUND);
            for (int c = 0; c < conversationTags.size(); c++) {
                CompoundTag conversationTag = conversationTags.getCompound(c);
                String conversationNumber = conversationTag.getString(CONVERSATION_NUMBER_KEY);
                List<PhoneChatMessageData> messages = conversations.computeIfAbsent(conversationNumber, ignored -> new ArrayList<>());
                int unreadCount = conversationTag.getInt(UNREAD_COUNT_KEY);
                if (unreadCount > 0) {
                    data.unreadCountsByPhone.computeIfAbsent(phoneNumber, ignored -> new HashMap<>()).put(conversationNumber, unreadCount);
                }
                ListTag messageTags = conversationTag.getList(MESSAGES_KEY, Tag.TAG_COMPOUND);
                for (int m = 0; m < messageTags.size(); m++) {
                    CompoundTag messageTag = messageTags.getCompound(m);
                    messages.add(new PhoneChatMessageData(messageTag.getString(MESSAGE_KEY), messageTag.getBoolean(OUTGOING_KEY), messageTag.getLong(TIMESTAMP_KEY)));
                }
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag phoneTags = new ListTag();
        messagesByPhone.forEach((phoneNumber, conversations) -> {
            CompoundTag phoneTag = new CompoundTag();
            phoneTag.putString(PHONE_NUMBER_KEY, phoneNumber);
            ListTag conversationTags = new ListTag();
            conversations.forEach((conversationNumber, messages) -> {
                CompoundTag conversationTag = new CompoundTag();
                conversationTag.putString(CONVERSATION_NUMBER_KEY, conversationNumber);
                ListTag messageTags = new ListTag();
                for (PhoneChatMessageData message : messages) {
                    CompoundTag messageTag = new CompoundTag();
                    messageTag.putString(MESSAGE_KEY, message.message());
                    messageTag.putBoolean(OUTGOING_KEY, message.outgoing());
                    messageTag.putLong(TIMESTAMP_KEY, message.timestampMillis());
                    messageTags.add(messageTag);
                }
                conversationTag.put(MESSAGES_KEY, messageTags);
                int unreadCount = unreadCountsByPhone.getOrDefault(phoneNumber, Map.of()).getOrDefault(conversationNumber, 0);
                if (unreadCount > 0) {
                    conversationTag.putInt(UNREAD_COUNT_KEY, unreadCount);
                }
                conversationTags.add(conversationTag);
            });
            phoneTag.put(CONVERSATIONS_KEY, conversationTags);
            phoneTags.add(phoneTag);
        });
        tag.put(PHONES_KEY, phoneTags);
        return tag;
    }

    public List<PhoneChatMessageData> getMessages(String phoneNumber, String conversationNumber) {
        return new ArrayList<>(messagesByPhone
                .getOrDefault(phoneNumber, Map.of())
                .getOrDefault(conversationNumber, List.of()));
    }

    public List<String> getConversationNumbers(String phoneNumber) {
        return new ArrayList<>(messagesByPhone
                .getOrDefault(phoneNumber, Map.of())
                .keySet());
    }

    public boolean hasPhoneData(String phoneNumber) {
        return messagesByPhone.containsKey(phoneNumber) || unreadCountsByPhone.containsKey(phoneNumber);
    }

    public void movePhoneData(String oldPhoneNumber, String newPhoneNumber) {
        if (oldPhoneNumber.equals(newPhoneNumber) || hasPhoneData(newPhoneNumber)) {
            return;
        }

        Map<String, List<PhoneChatMessageData>> messages = messagesByPhone.remove(oldPhoneNumber);
        if (messages != null) {
            messagesByPhone.put(newPhoneNumber, messages);
        }
        Map<String, Integer> unreadCounts = unreadCountsByPhone.remove(oldPhoneNumber);
        if (unreadCounts != null) {
            unreadCountsByPhone.put(newPhoneNumber, unreadCounts);
        }
        if (messages != null || unreadCounts != null) {
            setDirty();
        }
    }

    public void addMessage(String phoneNumber, String conversationNumber, String message, boolean outgoing, long timestampMillis) {
        Map<String, List<PhoneChatMessageData>> conversations = messagesByPhone.computeIfAbsent(phoneNumber, ignored -> new HashMap<>());
        if (!conversations.containsKey(conversationNumber) && conversations.size() >= MAX_CONVERSATIONS_PER_PHONE) {
            removeOldestConversation(conversations);
        }

        List<PhoneChatMessageData> messages = conversations.computeIfAbsent(conversationNumber, ignored -> new ArrayList<>());
        messages.add(new PhoneChatMessageData(message, outgoing, timestampMillis));
        if (!outgoing) {
            unreadCountsByPhone.computeIfAbsent(phoneNumber, ignored -> new HashMap<>()).merge(conversationNumber, 1, Integer::sum);
        }
        while (messages.size() > MAX_MESSAGES_PER_CONVERSATION) {
            messages.remove(0);
        }
        setDirty();
    }

    public int getUnreadCount(String phoneNumber, String conversationNumber) {
        return unreadCountsByPhone.getOrDefault(phoneNumber, Map.of()).getOrDefault(conversationNumber, 0);
    }

    public void clearUnreadCount(String phoneNumber, String conversationNumber) {
        Map<String, Integer> unreadCounts = unreadCountsByPhone.get(phoneNumber);
        if (unreadCounts == null || !unreadCounts.containsKey(conversationNumber)) {
            return;
        }

        unreadCounts.remove(conversationNumber);
        if (unreadCounts.isEmpty()) {
            unreadCountsByPhone.remove(phoneNumber);
        }
        setDirty();
    }

    private static void removeOldestConversation(Map<String, List<PhoneChatMessageData>> conversations) {
        String oldestConversation = null;
        long oldestTime = Long.MAX_VALUE;
        for (Map.Entry<String, List<PhoneChatMessageData>> entry : conversations.entrySet()) {
            long latestTime = entry.getValue().isEmpty() ? 0L : entry.getValue().get(entry.getValue().size() - 1).timestampMillis();
            if (latestTime < oldestTime) {
                oldestTime = latestTime;
                oldestConversation = entry.getKey();
            }
        }
        if (oldestConversation != null) {
            conversations.remove(oldestConversation);
        }
    }
}
