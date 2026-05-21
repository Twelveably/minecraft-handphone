package com.twelveably.handphone;

import com.mojang.logging.LogUtils;
import com.twelveably.handphone.item.PhoneItem;
import com.twelveably.handphone.network.HandphoneNetwork;
import com.twelveably.handphone.phone.PhoneLocator;
import com.twelveably.handphone.voice.PhoneCallManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(Handphone.MODID)
public class Handphone {
   public static final String MODID = "handphone";
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
   public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);
   public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
   public static final RegistryObject<Item> HANDPHONE = ITEMS.register("handphone", () -> new PhoneItem(new Properties().stacksTo(1)));
   public static final RegistryObject<SoundEvent> MESSAGE_SENT_SOUND = registerSound("message_sent");
   public static final RegistryObject<SoundEvent> MESSAGE_RECEIVED_SOUND = registerSound("message_received");
   public static final RegistryObject<SoundEvent> CALLING_SOUND = registerSound("calling");
   public static final RegistryObject<SoundEvent> INCOMING_CALL_SOUND = registerSound("incoming_call");
   public static final RegistryObject<SoundEvent> CALL_NOT_THROUGH_SOUND = registerSound("call_not_through");
   public static final RegistryObject<CreativeModeTab> HANDPHONE_TAB = CREATIVE_MODE_TABS.register(
      "handphone_tab",
      () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> HANDPHONE.get().getDefaultInstance())
            .displayItems((parameters, output) -> output.accept(HANDPHONE.get()))
            .build()
   );

   public Handphone() {
      IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
      modEventBus.addListener(this::commonSetup);
      ITEMS.register(modEventBus);
      SOUND_EVENTS.register(modEventBus);
      CREATIVE_MODE_TABS.register(modEventBus);
      MinecraftForge.EVENT_BUS.register(this);
      MinecraftForge.EVENT_BUS.register(PhoneLocator.class);
      MinecraftForge.EVENT_BUS.register(PhoneCallManager.class);
      MinecraftForge.EVENT_BUS.register(HandphoneCommands.class);
      modEventBus.addListener(this::addCreative);
      ModLoadingContext.get().registerConfig(Type.COMMON, Config.SPEC);
   }

   private void commonSetup(FMLCommonSetupEvent event) {
      event.enqueueWork(HandphoneNetwork::register);
      LOGGER.info("Handphone common setup complete");
   }

   private void addCreative(BuildCreativeModeTabContentsEvent event) {
      if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
         event.accept(HANDPHONE);
      }
   }

   private static RegistryObject<SoundEvent> registerSound(String name) {
      ResourceLocation id = new ResourceLocation(MODID, name);
      return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
   }

   @SubscribeEvent
   public void onServerStarting(ServerStartingEvent event) {
      LOGGER.info("Handphone server setup complete");
   }

   @SubscribeEvent
   public void onServerTick(ServerTickEvent event) {
      if (event.phase == Phase.END) {
         for (ServerLevel level : event.getServer().getAllLevels()) {
            PhoneCallManager.tick(level);
         }
      }
   }
}
