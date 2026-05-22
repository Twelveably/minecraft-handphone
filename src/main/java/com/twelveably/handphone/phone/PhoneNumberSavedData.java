package com.twelveably.handphone.phone;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class PhoneNumberSavedData extends SavedData {
    private static final String PHONES_KEY = "Phones";
    private static final String PHONE_ID_KEY = "PhoneId";
    private static final String PHONE_NUMBER_KEY = "PhoneNumber";

    private final Map<UUID, String> numbersByPhoneId = new HashMap<>();
    private final Set<String> usedNumbers = new HashSet<>();

    public static PhoneNumberSavedData load(CompoundTag tag) {
        PhoneNumberSavedData data = new PhoneNumberSavedData();
        ListTag phones = tag.getList(PHONES_KEY, Tag.TAG_COMPOUND);

        for (int i = 0; i < phones.size(); i++) {
            CompoundTag phoneTag = phones.getCompound(i);
            if (!phoneTag.hasUUID(PHONE_ID_KEY) || !phoneTag.contains(PHONE_NUMBER_KEY, Tag.TAG_STRING)) {
                continue;
            }

            UUID phoneId = phoneTag.getUUID(PHONE_ID_KEY);
            String phoneNumber = phoneTag.getString(PHONE_NUMBER_KEY);
            data.numbersByPhoneId.put(phoneId, phoneNumber);
            data.usedNumbers.add(phoneNumber);
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag phones = new ListTag();

        numbersByPhoneId.forEach((phoneId, phoneNumber) -> {
            CompoundTag phoneTag = new CompoundTag();
            phoneTag.putUUID(PHONE_ID_KEY, phoneId);
            phoneTag.putString(PHONE_NUMBER_KEY, phoneNumber);
            phones.add(phoneTag);
        });

        tag.put(PHONES_KEY, phones);
        return tag;
    }

    public Optional<String> getNumber(UUID phoneId) {
        return Optional.ofNullable(numbersByPhoneId.get(phoneId));
    }

    public boolean isNumberUsed(String phoneNumber) {
        return usedNumbers.contains(phoneNumber);
    }

    public void putNumber(UUID phoneId, String phoneNumber) {
        numbersByPhoneId.put(phoneId, phoneNumber);
        usedNumbers.add(phoneNumber);
        setDirty();
    }

    public void claimNumber(UUID phoneId, String phoneNumber) {
        numbersByPhoneId.entrySet().removeIf(entry -> entry.getValue().equals(phoneNumber));
        numbersByPhoneId.put(phoneId, phoneNumber);
        rebuildUsedNumbers();
        setDirty();
    }

    private void rebuildUsedNumbers() {
        usedNumbers.clear();
        usedNumbers.addAll(numbersByPhoneId.values());
    }
}
