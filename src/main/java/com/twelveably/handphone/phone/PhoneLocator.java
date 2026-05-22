package com.twelveably.handphone.phone;

import com.twelveably.handphone.Handphone;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PhoneLocator {
    private static final Map<String, LocatedPhone> LOCATED_PHONES = new HashMap<>();
    private static final Map<UUID, String> DROPPED_PHONE_NUMBERS_BY_ENTITY = new HashMap<>();

    private PhoneLocator() {
    }

    public static Optional<LocatedPhone> findPhone(MinecraftServer server, String phoneNumber) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (ItemStack stack : player.getInventory().items) {
                if (PhoneInventory.isPhoneWithNumber(stack, phoneNumber)) {
                    return Optional.of(LocatedPhone.player(player, stack));
                }
            }

            for (ItemStack stack : player.getInventory().offhand) {
                if (PhoneInventory.isPhoneWithNumber(stack, phoneNumber)) {
                    return Optional.of(LocatedPhone.player(player, stack));
                }
            }
        }

        return Optional.ofNullable(LOCATED_PHONES.get(phoneNumber)).filter(LocatedPhone::isLoaded);
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ItemEntity itemEntity) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        indexDroppedPhone(level, itemEntity);
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ItemEntity itemEntity)) {
            return;
        }

        removeDroppedPhone(itemEntity);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        removeContainerAt(level, event.getPos());
    }

    @SubscribeEvent
    public static void onContainerClosed(PlayerContainerEvent.Close event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }

        scanNearbyContainers(level, event.getEntity().blockPosition());
    }

    @SubscribeEvent
    public static void onContainerOpened(PlayerContainerEvent.Open event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }

        scanNearbyContainers(level, event.getEntity().blockPosition());
    }

    private static void indexDroppedPhone(ServerLevel level, ItemEntity itemEntity) {
        ItemStack stack = itemEntity.getItem();
        if (!stack.is(Handphone.HANDPHONE.get())) {
            removeDroppedPhone(itemEntity);
            return;
        }

        PhoneNumbers.ensureAssigned(stack, level);
        Optional<String> phoneNumber = PhoneNumbers.getPhoneNumber(stack);
        if (phoneNumber.isEmpty()) {
            removeDroppedPhone(itemEntity);
            return;
        }

        String previousPhoneNumber = DROPPED_PHONE_NUMBERS_BY_ENTITY.put(itemEntity.getUUID(), phoneNumber.get());
        if (previousPhoneNumber != null && !previousPhoneNumber.equals(phoneNumber.get())) {
            LOCATED_PHONES.remove(previousPhoneNumber);
        }

        LOCATED_PHONES.put(phoneNumber.get(), LocatedPhone.dropped(level, itemEntity));
    }

    private static void scanNearbyContainers(ServerLevel level, BlockPos pos) {
        int radius = 8;
        BlockPos.betweenClosed(pos.offset(-radius, -radius, -radius), pos.offset(radius, radius, radius)).forEach(scanPos -> {
            BlockEntity blockEntity = level.getBlockEntity(scanPos);
            if (blockEntity != null) {
                indexContainer(level, scanPos.immutable(), blockEntity);
            }
        });
    }

    private static void removeDroppedPhone(ItemEntity itemEntity) {
        String phoneNumber = DROPPED_PHONE_NUMBERS_BY_ENTITY.remove(itemEntity.getUUID());
        if (phoneNumber != null) {
            LOCATED_PHONES.remove(phoneNumber);
        }
    }

    private static void indexContainer(ServerLevel level, BlockPos pos, BlockEntity blockEntity) {
        removeContainerAt(level, pos);
        if (!(blockEntity instanceof Container container)) {
            return;
        }

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.is(Handphone.HANDPHONE.get())) {
                continue;
            }

            PhoneNumbers.ensureAssigned(stack, level);
            PhoneNumbers.getPhoneNumber(stack).ifPresent(phoneNumber -> LOCATED_PHONES.put(phoneNumber, LocatedPhone.container(level, pos)));
        }
    }

    private static void removeContainerAt(ServerLevel level, BlockPos pos) {
        LOCATED_PHONES.entrySet().removeIf(entry -> entry.getValue().isContainerAt(level, pos));
    }

    public static class LocatedPhone {
        @Nullable
        private final ServerPlayer player;
        @Nullable
        private final ItemStack playerStack;
        @Nullable
        private final ServerLevel level;
        @Nullable
        private final ItemEntity itemEntity;
        @Nullable
        private final BlockPos containerPos;

        private LocatedPhone(@Nullable ServerPlayer player, @Nullable ItemStack playerStack, @Nullable ServerLevel level, @Nullable ItemEntity itemEntity, @Nullable BlockPos containerPos) {
            this.player = player;
            this.playerStack = playerStack;
            this.level = level;
            this.itemEntity = itemEntity;
            this.containerPos = containerPos;
        }

        private static LocatedPhone player(ServerPlayer player, ItemStack stack) {
            return new LocatedPhone(player, stack, player.serverLevel(), null, null);
        }

        private static LocatedPhone dropped(ServerLevel level, ItemEntity itemEntity) {
            return new LocatedPhone(null, null, level, itemEntity, null);
        }

        private static LocatedPhone container(ServerLevel level, BlockPos pos) {
            return new LocatedPhone(null, null, level, null, pos.immutable());
        }

        public boolean isLoaded() {
            if (player != null) {
                return true;
            }

            if (itemEntity != null) {
                return !itemEntity.isRemoved();
            }

            return level != null && containerPos != null && level.isLoaded(containerPos);
        }

        public Optional<ServerPlayer> player() {
            return Optional.ofNullable(player);
        }

        public Optional<ItemStack> phoneStack(String phoneNumber) {
            if (playerStack != null && PhoneInventory.isPhoneWithNumber(playerStack, phoneNumber)) {
                return Optional.of(playerStack);
            }

            if (itemEntity != null && !itemEntity.isRemoved() && PhoneInventory.isPhoneWithNumber(itemEntity.getItem(), phoneNumber)) {
                return Optional.of(itemEntity.getItem());
            }

            if (level == null || containerPos == null || !level.isLoaded(containerPos)) {
                return Optional.empty();
            }

            BlockEntity blockEntity = level.getBlockEntity(containerPos);
            if (!(blockEntity instanceof Container container)) {
                return Optional.empty();
            }

            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (PhoneInventory.isPhoneWithNumber(stack, phoneNumber)) {
                    return Optional.of(stack);
                }
            }

            return Optional.empty();
        }

        public ServerLevel level() {
            return level;
        }

        public BlockPos soundPos() {
            if (player != null) {
                return player.blockPosition();
            }

            if (itemEntity != null) {
                return itemEntity.blockPosition();
            }

            return containerPos;
        }

        public boolean canEmitSound() {
            return player != null || itemEntity != null;
        }

        private boolean isContainerAt(ServerLevel level, BlockPos pos) {
            return this.level == level && containerPos != null && containerPos.equals(pos);
        }
    }
}
