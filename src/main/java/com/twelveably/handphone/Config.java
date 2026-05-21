package com.twelveably.handphone;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Handphone.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<String> MESSAGE_SENT_SOUND = BUILDER
            .comment("Sound event played locally when this phone sends a message. You can replace the OGG at assets/handphone/sounds/notifications/message_sent.ogg and set this to handphone:message_sent.")
            .define("messageSentSound", "handphone:message_sent");
    private static final ForgeConfigSpec.ConfigValue<String> MESSAGE_RECEIVED_SOUND = BUILDER
            .comment("Sound event played locally when this phone receives a message. You can replace the OGG at assets/handphone/sounds/notifications/message_received.ogg and set this to handphone:message_received.")
            .define("messageReceivedSound", "handphone:message_received");
    private static final ForgeConfigSpec.ConfigValue<String> CALLING_SOUND = BUILDER
            .comment("Sound event played locally on the caller phone while calling. You can replace the OGG at assets/handphone/sounds/notifications/calling.ogg and set this to handphone:calling.")
            .define("callingSound", "handphone:calling");
    private static final ForgeConfigSpec.ConfigValue<String> INCOMING_CALL_SOUND = BUILDER
            .comment("Sound event played locally on the receiver phone for incoming calls. You can replace the OGG at assets/handphone/sounds/notifications/incoming_call.ogg and set this to handphone:incoming_call.")
            .define("incomingCallSound", "handphone:incoming_call");
    private static final ForgeConfigSpec.ConfigValue<String> CALL_NOT_THROUGH_SOUND = BUILDER
            .comment("Sound event played on the caller phone when a call cannot reach the receiver. You can replace the OGG at assets/handphone/sounds/notifications/call_not_through.ogg and set this to handphone:call_not_through.")
            .define("callNotThroughSound", "handphone:call_not_through");
    private static final ForgeConfigSpec.DoubleValue CALL_AUDIO_RADIUS_BLOCKS = BUILDER
            .comment("Radius in blocks for phone microphone bleed and loudspeaker call audio.")
            .defineInRange("callAudioRadiusBlocks", 5.0D, 1.0D, 64.0D);
    private static final ForgeConfigSpec.DoubleValue PHONE_SOUND_RADIUS_BLOCKS = BUILDER
            .comment("Strict radius in blocks for phone notification and ring sounds.")
            .defineInRange("phoneSoundRadiusBlocks", 5.0D, 1.0D, 64.0D);
    private static final ForgeConfigSpec.DoubleValue PHONE_VOICE_NOISE_PERCENT = BUILDER
            .comment("White noise mixed into phone call voice audio, from 0 to 100 percent.")
            .defineInRange("phoneVoiceNoisePercent", 0.0D, 0.0D, 100.0D);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static String messageSentSound;
    public static String messageReceivedSound;
    public static String callingSound;
    public static String incomingCallSound;
    public static String callNotThroughSound;
    public static double callAudioRadiusBlocks;
    public static double phoneSoundRadiusBlocks;
    public static double phoneVoiceNoisePercent;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        messageSentSound = MESSAGE_SENT_SOUND.get();
        messageReceivedSound = MESSAGE_RECEIVED_SOUND.get();
        callingSound = CALLING_SOUND.get();
        incomingCallSound = INCOMING_CALL_SOUND.get();
        callNotThroughSound = CALL_NOT_THROUGH_SOUND.get();
        callAudioRadiusBlocks = CALL_AUDIO_RADIUS_BLOCKS.get();
        phoneSoundRadiusBlocks = PHONE_SOUND_RADIUS_BLOCKS.get();
        phoneVoiceNoisePercent = PHONE_VOICE_NOISE_PERCENT.get();
    }
}
