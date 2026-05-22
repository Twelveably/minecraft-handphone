package com.twelveably.handphone.phone;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PhoneContacts {
    private static final String DATA_NAME = "handphone_contacts";
    private static final String CONTACTS_KEY = "Contacts";
    private static final String NAME_KEY = "Name";
    private static final String PHONE_NUMBER_KEY = "PhoneNumber";
    private static final String PROFILE_ID_KEY = "ProfileId";

    private PhoneContacts() {
    }

    public static List<PhoneContactData> getContacts(ItemStack stack) {
        return getContacts(null, stack);
    }

    public static List<PhoneContactData> getContacts(MinecraftServer server, ItemStack stack) {
        if (server != null) {
            migrateLegacyContacts(server, stack);
            String localPhoneNumber = PhoneNumbers.getPhoneNumber(stack).orElse("");
            if (!localPhoneNumber.isBlank()) {
                List<PhoneContactData> contacts = new ArrayList<>();
                Set<String> knownContactNumbers = new HashSet<>();
                for (PhoneContactSavedData.ContactEntry contact : getData(server).getContacts(localPhoneNumber)) {
                    knownContactNumbers.add(contact.phoneNumber());
                    String latestMessage = PhoneChats.getLatestMessage(server, localPhoneNumber, contact.phoneNumber());
                    long latestMessageTimeMillis = PhoneChats.getLatestMessageTimeMillis(server, localPhoneNumber, contact.phoneNumber());
                    int unreadCount = PhoneChats.getUnreadCount(server, localPhoneNumber, contact.phoneNumber());
                    contacts.add(new PhoneContactData(contact.name(), contact.phoneNumber(), latestMessage, latestMessageTimeMillis, contact.profileId(), unreadCount));
                }
                for (String conversationNumber : PhoneChats.getConversationNumbers(server, localPhoneNumber)) {
                    if (knownContactNumbers.contains(conversationNumber)) {
                        continue;
                    }

                    String latestMessage = PhoneChats.getLatestMessage(server, localPhoneNumber, conversationNumber);
                    long latestMessageTimeMillis = PhoneChats.getLatestMessageTimeMillis(server, localPhoneNumber, conversationNumber);
                    int unreadCount = PhoneChats.getUnreadCount(server, localPhoneNumber, conversationNumber);
                    contacts.add(new PhoneContactData(conversationNumber, conversationNumber, latestMessage, latestMessageTimeMillis, "", unreadCount));
                }
                return contacts;
            }
        }

        List<PhoneContactData> contacts = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(CONTACTS_KEY, Tag.TAG_LIST)) {
            return contacts;
        }

        ListTag contactTags = tag.getList(CONTACTS_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < contactTags.size(); i++) {
            CompoundTag contactTag = contactTags.getCompound(i);
            if (!contactTag.contains(NAME_KEY, Tag.TAG_STRING) || !contactTag.contains(PHONE_NUMBER_KEY, Tag.TAG_STRING)) {
                continue;
            }

            String phoneNumber = contactTag.getString(PHONE_NUMBER_KEY);
            String localPhoneNumber = PhoneNumbers.getPhoneNumber(stack).orElse("");
            String latestMessage = server == null || localPhoneNumber.isBlank() ? PhoneChats.getLatestMessage(stack, phoneNumber) : PhoneChats.getLatestMessage(server, localPhoneNumber, phoneNumber);
            long latestMessageTimeMillis = server == null || localPhoneNumber.isBlank() ? PhoneChats.getLatestMessageTimeMillis(stack, phoneNumber) : PhoneChats.getLatestMessageTimeMillis(server, localPhoneNumber, phoneNumber);
            int unreadCount = server == null || localPhoneNumber.isBlank() ? 0 : PhoneChats.getUnreadCount(server, localPhoneNumber, phoneNumber);
            contacts.add(new PhoneContactData(contactTag.getString(NAME_KEY), phoneNumber, latestMessage, latestMessageTimeMillis, contactTag.getString(PROFILE_ID_KEY), unreadCount));
        }

        return contacts;
    }

    public static void addContact(ItemStack stack, String name, String phoneNumber) {
        addContact(stack, name, phoneNumber, "");
    }

    public static void addContact(ItemStack stack, String name, String phoneNumber, String profileId) {
        CompoundTag tag = stack.getOrCreateTag();
        ListTag contactTags = tag.getList(CONTACTS_KEY, Tag.TAG_COMPOUND);

        CompoundTag contactTag = new CompoundTag();
        contactTag.putString(NAME_KEY, name);
        contactTag.putString(PHONE_NUMBER_KEY, phoneNumber);
        contactTag.putString(PROFILE_ID_KEY, profileId);
        contactTags.add(contactTag);

        tag.put(CONTACTS_KEY, contactTags);
    }

    public static boolean addContact(MinecraftServer server, String localPhoneNumber, String name, String phoneNumber, String profileId) {
        return getData(server).addContact(localPhoneNumber, name, phoneNumber, profileId);
    }

    public static String getContactName(ItemStack stack, String phoneNumber, String fallback) {
        CompoundTag contactTag = getContactTag(stack, phoneNumber);
        return contactTag == null ? fallback : contactTag.getString(NAME_KEY);
    }

    public static String getContactName(MinecraftServer server, String localPhoneNumber, String phoneNumber, String fallback) {
        return getData(server).getContacts(localPhoneNumber).stream()
                .filter(contact -> contact.phoneNumber().equals(phoneNumber))
                .map(PhoneContactSavedData.ContactEntry::name)
                .findFirst()
                .orElse(fallback);
    }

    public static String getContactProfileId(ItemStack stack, String phoneNumber) {
        CompoundTag contactTag = getContactTag(stack, phoneNumber);
        return contactTag == null ? "" : contactTag.getString(PROFILE_ID_KEY);
    }

    public static String getContactProfileId(MinecraftServer server, String localPhoneNumber, String phoneNumber) {
        return getData(server).getContacts(localPhoneNumber).stream()
                .filter(contact -> contact.phoneNumber().equals(phoneNumber))
                .map(PhoneContactSavedData.ContactEntry::profileId)
                .findFirst()
                .orElse("");
    }

    public static boolean hasContact(MinecraftServer server, String localPhoneNumber, String phoneNumber) {
        return getData(server).getContacts(localPhoneNumber).stream().anyMatch(contact -> contact.phoneNumber().equals(phoneNumber));
    }

    public static boolean hasPhoneData(MinecraftServer server, String phoneNumber) {
        return getData(server).hasPhoneData(phoneNumber);
    }

    public static void movePhoneData(MinecraftServer server, String oldPhoneNumber, String newPhoneNumber) {
        getData(server).movePhoneData(oldPhoneNumber, newPhoneNumber);
    }

    public static boolean removeContact(ItemStack stack, int index) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(CONTACTS_KEY, Tag.TAG_LIST)) {
            return false;
        }

        ListTag contactTags = tag.getList(CONTACTS_KEY, Tag.TAG_COMPOUND);
        if (index < 0 || index >= contactTags.size()) {
            return false;
        }

        contactTags.remove(index);
        tag.put(CONTACTS_KEY, contactTags);
        return true;
    }

    public static boolean removeContact(MinecraftServer server, String localPhoneNumber, int index) {
        return getData(server).removeContact(localPhoneNumber, index);
    }

    public static boolean renameContact(ItemStack stack, String phoneNumber, String name) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(CONTACTS_KEY, Tag.TAG_LIST)) {
            return false;
        }

        ListTag contactTags = tag.getList(CONTACTS_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < contactTags.size(); i++) {
            CompoundTag contactTag = contactTags.getCompound(i);
            if (phoneNumber.equals(contactTag.getString(PHONE_NUMBER_KEY))) {
                contactTag.putString(NAME_KEY, name);
                tag.put(CONTACTS_KEY, contactTags);
                return true;
            }
        }

        return false;
    }

    public static boolean renameContact(MinecraftServer server, String localPhoneNumber, String phoneNumber, String name) {
        return getData(server).renameContact(localPhoneNumber, phoneNumber, name);
    }

    public static void migrateLegacyContacts(MinecraftServer server, ItemStack stack) {
        String localPhoneNumber = PhoneNumbers.getPhoneNumber(stack).orElse("");
        CompoundTag tag = stack.getTag();
        if (localPhoneNumber.isBlank() || tag == null || !tag.contains(CONTACTS_KEY, Tag.TAG_LIST)) {
            return;
        }

        ListTag contactTags = tag.getList(CONTACTS_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < contactTags.size(); i++) {
            CompoundTag contactTag = contactTags.getCompound(i);
            if (!contactTag.contains(NAME_KEY, Tag.TAG_STRING) || !contactTag.contains(PHONE_NUMBER_KEY, Tag.TAG_STRING)) {
                continue;
            }

            getData(server).addContact(localPhoneNumber, contactTag.getString(NAME_KEY), contactTag.getString(PHONE_NUMBER_KEY), contactTag.getString(PROFILE_ID_KEY));
        }
        tag.remove(CONTACTS_KEY);
    }

    private static PhoneContactSavedData getData(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(PhoneContactSavedData::load, PhoneContactSavedData::new, DATA_NAME);
    }

    private static CompoundTag getContactTag(ItemStack stack, String phoneNumber) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(CONTACTS_KEY, Tag.TAG_LIST)) {
            return null;
        }

        ListTag contactTags = tag.getList(CONTACTS_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < contactTags.size(); i++) {
            CompoundTag contactTag = contactTags.getCompound(i);
            if (phoneNumber.equals(contactTag.getString(PHONE_NUMBER_KEY))) {
                return contactTag;
            }
        }

        return null;
    }
}
