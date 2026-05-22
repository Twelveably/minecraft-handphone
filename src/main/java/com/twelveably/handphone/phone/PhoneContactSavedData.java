package com.twelveably.handphone.phone;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class PhoneContactSavedData extends SavedData {
    private static final String PHONES_KEY = "Phones";
    private static final String PHONE_NUMBER_KEY = "PhoneNumber";
    private static final String CONTACTS_KEY = "Contacts";
    private static final String NAME_KEY = "Name";
    private static final String CONTACT_NUMBER_KEY = "ContactNumber";
    private static final String PROFILE_ID_KEY = "ProfileId";
    private static final int MAX_CONTACTS_PER_PHONE = 128;

    private final Map<String, List<ContactEntry>> contactsByPhone = new HashMap<>();

    public static PhoneContactSavedData load(CompoundTag tag) {
        PhoneContactSavedData data = new PhoneContactSavedData();
        ListTag phoneTags = tag.getList(PHONES_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < phoneTags.size(); i++) {
            CompoundTag phoneTag = phoneTags.getCompound(i);
            String phoneNumber = phoneTag.getString(PHONE_NUMBER_KEY);
            List<ContactEntry> contacts = data.contactsByPhone.computeIfAbsent(phoneNumber, ignored -> new ArrayList<>());
            ListTag contactTags = phoneTag.getList(CONTACTS_KEY, Tag.TAG_COMPOUND);
            for (int c = 0; c < contactTags.size(); c++) {
                CompoundTag contactTag = contactTags.getCompound(c);
                contacts.add(new ContactEntry(contactTag.getString(NAME_KEY), contactTag.getString(CONTACT_NUMBER_KEY), contactTag.getString(PROFILE_ID_KEY)));
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag phoneTags = new ListTag();
        contactsByPhone.forEach((phoneNumber, contacts) -> {
            CompoundTag phoneTag = new CompoundTag();
            phoneTag.putString(PHONE_NUMBER_KEY, phoneNumber);
            ListTag contactTags = new ListTag();
            for (ContactEntry contact : contacts) {
                CompoundTag contactTag = new CompoundTag();
                contactTag.putString(NAME_KEY, contact.name());
                contactTag.putString(CONTACT_NUMBER_KEY, contact.phoneNumber());
                contactTag.putString(PROFILE_ID_KEY, contact.profileId());
                contactTags.add(contactTag);
            }
            phoneTag.put(CONTACTS_KEY, contactTags);
            phoneTags.add(phoneTag);
        });
        tag.put(PHONES_KEY, phoneTags);
        return tag;
    }

    public List<ContactEntry> getContacts(String phoneNumber) {
        return new ArrayList<>(contactsByPhone.getOrDefault(phoneNumber, List.of()));
    }

    public boolean hasPhoneData(String phoneNumber) {
        return contactsByPhone.containsKey(phoneNumber);
    }

    public void movePhoneData(String oldPhoneNumber, String newPhoneNumber) {
        if (oldPhoneNumber.equals(newPhoneNumber) || contactsByPhone.containsKey(newPhoneNumber)) {
            return;
        }

        List<ContactEntry> contacts = contactsByPhone.remove(oldPhoneNumber);
        if (contacts != null) {
            contactsByPhone.put(newPhoneNumber, contacts);
            setDirty();
        }
    }

    public boolean addContact(String phoneNumber, String name, String contactNumber, String profileId) {
        List<ContactEntry> contacts = contactsByPhone.computeIfAbsent(phoneNumber, ignored -> new ArrayList<>());
        for (int i = 0; i < contacts.size(); i++) {
            ContactEntry contact = contacts.get(i);
            if (contact.phoneNumber().equals(contactNumber)) {
                contacts.set(i, new ContactEntry(name, contactNumber, contact.profileId().isBlank() ? profileId : contact.profileId()));
                setDirty();
                return true;
            }
        }

        if (contacts.size() >= MAX_CONTACTS_PER_PHONE) {
            return false;
        }

        contacts.add(new ContactEntry(name, contactNumber, profileId));
        setDirty();
        return true;
    }

    public boolean removeContact(String phoneNumber, int index) {
        List<ContactEntry> contacts = contactsByPhone.get(phoneNumber);
        if (contacts == null || index < 0 || index >= contacts.size()) {
            return false;
        }

        contacts.remove(index);
        if (contacts.isEmpty()) {
            contactsByPhone.remove(phoneNumber);
        }
        setDirty();
        return true;
    }

    public boolean renameContact(String phoneNumber, String contactNumber, String name) {
        List<ContactEntry> contacts = contactsByPhone.get(phoneNumber);
        if (contacts == null) {
            return false;
        }

        for (int i = 0; i < contacts.size(); i++) {
            ContactEntry contact = contacts.get(i);
            if (contact.phoneNumber().equals(contactNumber)) {
                contacts.set(i, new ContactEntry(name, contact.phoneNumber(), contact.profileId()));
                setDirty();
                return true;
            }
        }

        return false;
    }

    public record ContactEntry(String name, String phoneNumber, String profileId) {
    }
}
