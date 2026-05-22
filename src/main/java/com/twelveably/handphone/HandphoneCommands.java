package com.twelveably.handphone;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.twelveably.handphone.phone.PhoneChats;
import com.twelveably.handphone.phone.PhoneContacts;
import com.twelveably.handphone.phone.PhoneInventory;
import com.twelveably.handphone.phone.PhoneNumbers;
import com.twelveably.handphone.phone.PhoneSignals;
import com.twelveably.handphone.voice.PhoneCallManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class HandphoneCommands {
    private HandphoneCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("handphone")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("signal")
                        .then(Commands.literal("enable").executes(context -> setSignal(context.getSource(), true)))
                        .then(Commands.literal("disable").executes(context -> setSignal(context.getSource(), false)))
                        .then(Commands.literal("status").executes(context -> showStatus(context.getSource()))))
                .then(Commands.literal("number")
                        .then(Commands.literal("set")
                                .then(Commands.argument("phoneNumber", StringArgumentType.word())
                                        .executes(context -> setHeldPhoneNumber(context.getSource(), StringArgumentType.getString(context, "phoneNumber")))))));
    }

    private static int setSignal(CommandSourceStack source, boolean enabled) {
        PhoneSignals.setEnabled(enabled);
        if (!enabled && source.getServer() != null) {
            PhoneCallManager.cancelPendingCalls("No signal.");
        }
        source.sendSuccess(() -> Component.literal("Handphone signal " + (enabled ? "enabled." : "disabled.")), true);
        return 1;
    }

    private static int showStatus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Handphone signal is " + (PhoneSignals.isEnabled() ? "enabled." : "disabled.")), false);
        return PhoneSignals.isEnabled() ? 1 : 0;
    }

    private static int setHeldPhoneNumber(CommandSourceStack source, String phoneNumber) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.literal("Only players can change a held phone number."));
            return 0;
        }

        ItemStack phone = PhoneInventory.getHeldPhone(player);
        if (phone.isEmpty()) {
            source.sendFailure(Component.literal("Hold a phone in either hand first."));
            return 0;
        }

        PhoneNumbers.ensureAssigned(phone, player.serverLevel());
        PhoneContacts.migrateLegacyContacts(player.server, phone);
        PhoneChats.migrateLegacyMessages(player.server, phone);
        String oldPhoneNumber = PhoneNumbers.getPhoneNumber(phone).orElse("");
        String newPhoneNumber = phoneNumber.trim();
        boolean targetAlreadyHasData = PhoneContacts.hasPhoneData(player.server, newPhoneNumber)
                || PhoneChats.hasPhoneData(player.server, newPhoneNumber);
        if (!PhoneNumbers.claimNumber(phone, player.serverLevel(), phoneNumber)) {
            source.sendFailure(Component.literal("Phone number must be 1 to 7 digits."));
            return 0;
        }
        if (!oldPhoneNumber.isBlank() && !targetAlreadyHasData) {
            PhoneContacts.movePhoneData(player.server, oldPhoneNumber, newPhoneNumber);
            PhoneChats.movePhoneData(player.server, oldPhoneNumber, newPhoneNumber);
        }

        source.sendSuccess(() -> Component.literal("Held phone number set to " + newPhoneNumber + "."), true);
        return 1;
    }
}
