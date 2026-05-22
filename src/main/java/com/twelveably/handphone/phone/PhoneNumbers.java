package com.twelveably.handphone.phone;

import com.twelveably.handphone.Handphone;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

public final class PhoneNumbers {
    private static final String DATA_NAME = Handphone.MODID + "_phone_numbers";
    private static final String PHONE_ID_KEY = "PhoneId";
    private static final String PHONE_NUMBER_KEY = "PhoneNumber";
    private static final int MIN_PHONE_NUMBER = 1_000_000;
    private static final int PHONE_NUMBER_RANGE = 9_000_000;

    private PhoneNumbers() {
    }

    public static void ensureAssigned(ItemStack stack, ServerLevel level) {
        CompoundTag tag = stack.getOrCreateTag();
        PhoneNumberSavedData data = getData(level);
        UUID phoneId = getOrCreatePhoneId(tag);

        Optional<String> existingNumber = data.getNumber(phoneId);
        if (existingNumber.isPresent()) {
            tag.putString(PHONE_NUMBER_KEY, existingNumber.get());
            return;
        }

        String phoneNumber = tag.contains(PHONE_NUMBER_KEY, CompoundTag.TAG_STRING)
                ? tag.getString(PHONE_NUMBER_KEY)
                : generateUniqueNumber(data, level);

        if (data.isNumberUsed(phoneNumber)) {
            phoneNumber = generateUniqueNumber(data, level);
        }

        data.putNumber(phoneId, phoneNumber);
        tag.putString(PHONE_NUMBER_KEY, phoneNumber);
    }

    public static Optional<String> getPhoneNumber(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(PHONE_NUMBER_KEY, CompoundTag.TAG_STRING)) {
            return Optional.empty();
        }

        return Optional.of(tag.getString(PHONE_NUMBER_KEY));
    }

    public static boolean isAssignedNumber(String phoneNumber, ServerLevel level) {
        return getData(level).isNumberUsed(phoneNumber);
    }

    public static boolean claimNumber(ItemStack stack, ServerLevel level, String phoneNumber) {
        String trimmedPhoneNumber = phoneNumber.trim();
        if (!trimmedPhoneNumber.matches("\\d{1,7}")) {
            return false;
        }

        CompoundTag tag = stack.getOrCreateTag();
        UUID phoneId = getOrCreatePhoneId(tag);
        getData(level).claimNumber(phoneId, trimmedPhoneNumber);
        tag.putString(PHONE_NUMBER_KEY, trimmedPhoneNumber);
        return true;
    }

    private static PhoneNumberSavedData getData(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(PhoneNumberSavedData::load, PhoneNumberSavedData::new, DATA_NAME);
    }

    private static UUID getOrCreatePhoneId(CompoundTag tag) {
        if (tag.hasUUID(PHONE_ID_KEY)) {
            return tag.getUUID(PHONE_ID_KEY);
        }

        UUID phoneId = UUID.randomUUID();
        tag.putUUID(PHONE_ID_KEY, phoneId);
        return phoneId;
    }

    private static String generateUniqueNumber(PhoneNumberSavedData data, ServerLevel level) {
        String phoneNumber;

        do {
            phoneNumber = String.valueOf(MIN_PHONE_NUMBER + level.random.nextInt(PHONE_NUMBER_RANGE));
        } while (data.isNumberUsed(phoneNumber));

        return phoneNumber;
    }
}
