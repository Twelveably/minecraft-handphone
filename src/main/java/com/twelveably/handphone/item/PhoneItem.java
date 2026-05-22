package com.twelveably.handphone.item;

import com.twelveably.handphone.client.ClientPhoneHooks;
import com.twelveably.handphone.phone.PhoneNumbers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;
import java.util.List;

public class PhoneItem extends Item {
    public PhoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level instanceof ServerLevel serverLevel) {
            PhoneNumbers.ensureAssigned(stack, serverLevel);
        }

        if (level.isClientSide) {
            String phoneNumber = PhoneNumbers.getPhoneNumber(stack).orElse("");
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPhoneHooks.openHomeScreen(phoneNumber));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (level instanceof ServerLevel serverLevel) {
            PhoneNumbers.ensureAssigned(stack, serverLevel);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String phoneNumber = PhoneNumbers.getPhoneNumber(stack).orElse("Unassigned");
        tooltip.add(Component.literal("   \uD83D\uDCDE +" + phoneNumber + "    ").withStyle(ChatFormatting.GRAY));
    }
}
