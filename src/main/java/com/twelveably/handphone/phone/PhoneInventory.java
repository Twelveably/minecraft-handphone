package com.twelveably.handphone.phone;

import com.twelveably.handphone.Handphone;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class PhoneInventory {
    private PhoneInventory() {
    }

    public static boolean isPhone(ItemStack stack) {
        return stack.is(Handphone.HANDPHONE.get());
    }

    public static boolean isPhoneWithNumber(ItemStack stack, String phoneNumber) {
        return isPhone(stack) && PhoneNumbers.getPhoneNumber(stack).filter(phoneNumber::equals).isPresent();
    }

    public static ItemStack getHeldPhone(ServerPlayer player) {
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (isPhone(mainHand)) {
            return mainHand;
        }

        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        if (isPhone(offHand)) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }

    public static ItemStack getUsablePhone(ServerPlayer player) {
        ItemStack heldPhone = getHeldPhone(player);
        if (!heldPhone.isEmpty()) {
            return heldPhone;
        }

        for (ItemStack stack : player.getInventory().items) {
            if (isPhone(stack)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    public static ItemStack getPhoneByNumber(ServerPlayer player, String phoneNumber) {
        String trimmedPhoneNumber = phoneNumber.trim();
        if (!trimmedPhoneNumber.isEmpty()) {
            ItemStack matchingPhone = findCarriedPhoneByNumber(player, trimmedPhoneNumber);
            if (!matchingPhone.isEmpty()) {
                return matchingPhone;
            }
        }

        return getHeldPhone(player);
    }

    public static ItemStack findCarriedPhoneByNumber(ServerPlayer player, String phoneNumber) {
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (isPhoneWithNumber(mainHand, phoneNumber)) {
            return mainHand;
        }

        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        if (isPhoneWithNumber(offHand, phoneNumber)) {
            return offHand;
        }

        for (ItemStack stack : player.getInventory().items) {
            if (isPhoneWithNumber(stack, phoneNumber)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    public static boolean isCarryingPhone(ServerPlayer player, String phoneNumber) {
        return !findCarriedPhoneByNumber(player, phoneNumber).isEmpty();
    }

    public static Optional<ServerPlayer> findOnlineHolder(ServerPlayer sourcePlayer, String phoneNumber) {
        for (ServerPlayer player : sourcePlayer.server.getPlayerList().getPlayers()) {
            if (isCarryingPhone(player, phoneNumber)) {
                return Optional.of(player);
            }
        }

        return Optional.empty();
    }

    public static ItemStack getBestPhoneForContact(ServerPlayer player, String sourcePhoneNumber, String targetPhoneNumber) {
        ItemStack sourcePhone = findCarriedPhoneByNumber(player, sourcePhoneNumber);
        if (!sourcePhone.isEmpty()) {
            return sourcePhone;
        }

        ItemStack heldPhone = getHeldPhone(player);
        if (isPhoneForContact(player, heldPhone, targetPhoneNumber)) {
            return heldPhone;
        }

        for (ItemStack stack : player.getInventory().items) {
            if (isPhoneForContact(player, stack, targetPhoneNumber)) {
                return stack;
            }
        }

        return getUsablePhone(player);
    }

    private static boolean isPhoneForContact(ServerPlayer player, ItemStack stack, String targetPhoneNumber) {
        if (!isPhone(stack)) {
            return false;
        }

        PhoneNumbers.ensureAssigned(stack, player.serverLevel());
        PhoneContacts.migrateLegacyContacts(player.server, stack);
        return PhoneNumbers.getPhoneNumber(stack)
                .map(localPhoneNumber -> PhoneContacts.hasContact(player.server, localPhoneNumber, targetPhoneNumber))
                .orElse(false);
    }
}
