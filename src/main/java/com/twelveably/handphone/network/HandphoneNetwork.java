package com.twelveably.handphone.network;

import com.twelveably.handphone.Handphone;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class HandphoneNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(Handphone.MODID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private HandphoneNetwork() {
    }

    public static void register() {
        int packetId = 0;
        CHANNEL.messageBuilder(RequestContactsPacket.class, packetId++)
                .encoder(RequestContactsPacket::encode)
                .decoder(RequestContactsPacket::decode)
                .consumerMainThread(RequestContactsPacket::handle)
                .add();
        CHANNEL.messageBuilder(AddContactPacket.class, packetId++)
                .encoder(AddContactPacket::encode)
                .decoder(AddContactPacket::decode)
                .consumerMainThread(AddContactPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncContactsPacket.class, packetId++)
                .encoder(SyncContactsPacket::encode)
                .decoder(SyncContactsPacket::decode)
                .consumerMainThread(SyncContactsPacket::handle)
                .add();
        CHANNEL.messageBuilder(ContactActionPacket.class, packetId++)
                .encoder(ContactActionPacket::encode)
                .decoder(ContactActionPacket::decode)
                .consumerMainThread(ContactActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(DeleteContactPacket.class, packetId)
                .encoder(DeleteContactPacket::encode)
                .decoder(DeleteContactPacket::decode)
                .consumerMainThread(DeleteContactPacket::handle)
                .add();
        packetId++;
        CHANNEL.messageBuilder(ChatMessagePacket.class, packetId++)
                .encoder(ChatMessagePacket::encode)
                .decoder(ChatMessagePacket::decode)
                .consumerMainThread(ChatMessagePacket::handle)
                .add();
        CHANNEL.messageBuilder(ReceiveChatMessagePacket.class, packetId++)
                .encoder(ReceiveChatMessagePacket::encode)
                .decoder(ReceiveChatMessagePacket::decode)
                .consumerMainThread(ReceiveChatMessagePacket::handle)
                .add();
        CHANNEL.messageBuilder(RequestChatMessagesPacket.class, packetId++)
                .encoder(RequestChatMessagesPacket::encode)
                .decoder(RequestChatMessagesPacket::decode)
                .consumerMainThread(RequestChatMessagesPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncChatMessagesPacket.class, packetId++)
                .encoder(SyncChatMessagesPacket::encode)
                .decoder(SyncChatMessagesPacket::decode)
                .consumerMainThread(SyncChatMessagesPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncCallStatePacket.class, packetId)
                .encoder(SyncCallStatePacket::encode)
                .decoder(SyncCallStatePacket::decode)
                .consumerMainThread(SyncCallStatePacket::handle)
                .add();
        packetId++;
        CHANNEL.messageBuilder(RespondCallPacket.class, packetId)
                .encoder(RespondCallPacket::encode)
                .decoder(RespondCallPacket::decode)
                .consumerMainThread(RespondCallPacket::handle)
                .add();
        packetId++;
        CHANNEL.messageBuilder(ToggleLoudspeakerPacket.class, packetId)
                .encoder(ToggleLoudspeakerPacket::encode)
                .decoder(ToggleLoudspeakerPacket::decode)
                .consumerMainThread(ToggleLoudspeakerPacket::handle)
                .add();
        packetId++;
        CHANNEL.messageBuilder(SyncLoudspeakerStatePacket.class, packetId)
                .encoder(SyncLoudspeakerStatePacket::encode)
                .decoder(SyncLoudspeakerStatePacket::decode)
                .consumerMainThread(SyncLoudspeakerStatePacket::handle)
                .add();
        packetId++;
        CHANNEL.messageBuilder(ToggleMutePacket.class, packetId)
                .encoder(ToggleMutePacket::encode)
                .decoder(ToggleMutePacket::decode)
                .consumerMainThread(ToggleMutePacket::handle)
                .add();
        packetId++;
        CHANNEL.messageBuilder(SyncMuteStatePacket.class, packetId)
                .encoder(SyncMuteStatePacket::encode)
                .decoder(SyncMuteStatePacket::decode)
                .consumerMainThread(SyncMuteStatePacket::handle)
                .add();
        packetId++;
        CHANNEL.messageBuilder(RenameContactPacket.class, packetId)
                .encoder(RenameContactPacket::encode)
                .decoder(RenameContactPacket::decode)
                .consumerMainThread(RenameContactPacket::handle)
                .add();
        packetId++;
        CHANNEL.messageBuilder(PlayPhoneSoundPacket.class, packetId)
                .encoder(PlayPhoneSoundPacket::encode)
                .decoder(PlayPhoneSoundPacket::decode)
                .consumerMainThread(PlayPhoneSoundPacket::handle)
                .add();
        packetId++;
        CHANNEL.messageBuilder(RingtoneStatePacket.class, packetId)
                .encoder(RingtoneStatePacket::encode)
                .decoder(RingtoneStatePacket::decode)
                .consumerMainThread(RingtoneStatePacket::handle)
                .add();
        packetId++;
        CHANNEL.messageBuilder(MarkConversationReadPacket.class, packetId)
                .encoder(MarkConversationReadPacket::encode)
                .decoder(MarkConversationReadPacket::decode)
                .consumerMainThread(MarkConversationReadPacket::handle)
                .add();
    }
}
