package com.twelveably.handphone.client;

import com.twelveably.handphone.client.phone.CallWindow;
import com.twelveably.handphone.client.phone.MessagingWindow;
import com.twelveably.handphone.network.HandphoneNetwork;
import com.twelveably.handphone.network.MarkConversationReadPacket;
import com.twelveably.handphone.network.SyncCallStatePacket;
import com.twelveably.handphone.phone.PhoneContactData;
import com.twelveably.handphone.phone.PhoneChatMessageData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClientPhoneHooks {
    private static final Map<String, CachedCallState> CALL_STATES_BY_PHONE = new HashMap<>();
    private static final Map<String, Boolean> LOUDSPEAKER_STATES_BY_PHONE = new HashMap<>();
    private static final Map<String, Boolean> MUTE_STATES_BY_PHONE = new HashMap<>();

    private ClientPhoneHooks() {
    }

    public static void openHomeScreen(String phoneNumber) {
        Minecraft.getInstance().setScreen(new PhoneHomeScreen(phoneNumber));
    }

    public static void setContacts(List<PhoneContactData> contacts) {
        if (Minecraft.getInstance().screen instanceof PhoneHomeScreen phoneHomeScreen) {
            phoneHomeScreen.setContacts(contacts);
        }
    }

    public static void receiveChatMessage(String conversationPhoneNumber, String contactName, String message, boolean outgoing, long timestampMillis) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof PhoneHomeScreen phoneHomeScreen) {
            String resolvedName = phoneHomeScreen.getContactName(conversationPhoneNumber, contactName);
            phoneHomeScreen.addChatMessage(conversationPhoneNumber, message, outgoing, timestampMillis);
            phoneHomeScreen.updateLatestMessage(conversationPhoneNumber, message, timestampMillis);
            if (!outgoing && phoneHomeScreen.isViewingChat(conversationPhoneNumber)) {
                HandphoneNetwork.CHANNEL.sendToServer(new MarkConversationReadPacket(phoneHomeScreen.getActivePhoneNumber(), conversationPhoneNumber));
            }
            if (!outgoing && minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.literal("Message from " + resolvedName + "."), true);
            }
        } else if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.literal(contactName + ": " + message), true);
        }
    }

    public static void setChatMessages(String phoneNumber, List<PhoneChatMessageData> messages) {
        if (Minecraft.getInstance().screen instanceof PhoneHomeScreen phoneHomeScreen) {
            phoneHomeScreen.setChatMessages(phoneNumber, messages);
        }
    }

    public static void setCallState(SyncCallStatePacket.State state, String localPhoneNumber, String contactName, String remotePhoneNumber, String remoteProfileId) {
        if (state == SyncCallStatePacket.State.NONE) {
            CALL_STATES_BY_PHONE.remove(localPhoneNumber);
            LOUDSPEAKER_STATES_BY_PHONE.remove(localPhoneNumber);
            MUTE_STATES_BY_PHONE.remove(localPhoneNumber);
        } else {
            CachedCallState previousState = getCachedCallState(localPhoneNumber);
            long startedAtMillis = shouldKeepCallTimer(previousState, state, remotePhoneNumber)
                    ? previousState.startedAtMillis()
                    : System.currentTimeMillis();
            String cachedProfileId = remoteProfileId.isBlank() ? previousState.remoteProfileId() : remoteProfileId;
            CALL_STATES_BY_PHONE.put(localPhoneNumber, new CachedCallState(state, contactName, remotePhoneNumber, cachedProfileId, startedAtMillis));
        }

        if (Minecraft.getInstance().screen instanceof PhoneHomeScreen phoneHomeScreen) {
            if (phoneHomeScreen.getActivePhoneNumber().isBlank()) {
                phoneHomeScreen.setActivePhoneNumber(localPhoneNumber);
            }

            if (!phoneHomeScreen.getActivePhoneNumber().equals(localPhoneNumber)) {
                return;
            }

            if (state != SyncCallStatePacket.State.NONE) {
                phoneHomeScreen.openWindow(new CallWindow(new PhoneHomeScreen.PhoneContact(
                        contactName.isBlank() ? remotePhoneNumber : contactName,
                        remotePhoneNumber
                ), state, getCurrentCallStartedAtMillis(localPhoneNumber), getCurrentCallProfileId(localPhoneNumber)));
            } else {
                phoneHomeScreen.openWindow(new MessagingWindow());
            }
        }
    }

    public static void clearCallStates() {
        CALL_STATES_BY_PHONE.clear();
        LOUDSPEAKER_STATES_BY_PHONE.clear();
        MUTE_STATES_BY_PHONE.clear();
    }

    public static SyncCallStatePacket.State getCurrentCallState(String localPhoneNumber) {
        return getCachedCallState(localPhoneNumber).state();
    }

    public static String getCurrentCallContactName(String localPhoneNumber) {
        return getCachedCallState(localPhoneNumber).contactName();
    }

    public static String getCurrentCallPhoneNumber(String localPhoneNumber) {
        return getCachedCallState(localPhoneNumber).remotePhoneNumber();
    }

    public static String getCurrentCallProfileId(String localPhoneNumber) {
        return getCachedCallState(localPhoneNumber).remoteProfileId();
    }

    public static String getOpenPhoneNumber() {
        if (Minecraft.getInstance().screen instanceof PhoneHomeScreen phoneHomeScreen) {
            return phoneHomeScreen.getActivePhoneNumber();
        }

        return "";
    }

    public static void setLoudspeakerState(String localPhoneNumber, boolean enabled) {
        if (enabled) {
            LOUDSPEAKER_STATES_BY_PHONE.put(localPhoneNumber, true);
        } else {
            LOUDSPEAKER_STATES_BY_PHONE.remove(localPhoneNumber);
        }
    }

    public static boolean isLoudspeakerEnabled(String localPhoneNumber) {
        return LOUDSPEAKER_STATES_BY_PHONE.getOrDefault(localPhoneNumber, false);
    }

    public static void setMuteState(String localPhoneNumber, boolean muted) {
        if (muted) {
            MUTE_STATES_BY_PHONE.put(localPhoneNumber, true);
        } else {
            MUTE_STATES_BY_PHONE.remove(localPhoneNumber);
        }
    }

    public static boolean isMuted(String localPhoneNumber) {
        return MUTE_STATES_BY_PHONE.getOrDefault(localPhoneNumber, false);
    }

    public static long getCurrentCallStartedAtMillis(String localPhoneNumber) {
        return getCachedCallState(localPhoneNumber).startedAtMillis();
    }

    private static CachedCallState getCachedCallState(String localPhoneNumber) {
        return CALL_STATES_BY_PHONE.getOrDefault(localPhoneNumber, CachedCallState.NONE);
    }

    private static boolean shouldKeepCallTimer(CachedCallState previousState, SyncCallStatePacket.State state, String remotePhoneNumber) {
        return state == SyncCallStatePacket.State.ACTIVE
                && previousState.state() == SyncCallStatePacket.State.ACTIVE
                && previousState.remotePhoneNumber().equals(remotePhoneNumber);
    }

    private record CachedCallState(SyncCallStatePacket.State state, String contactName, String remotePhoneNumber, String remoteProfileId, long startedAtMillis) {
        private static final CachedCallState NONE = new CachedCallState(SyncCallStatePacket.State.NONE, "", "", "", 0L);
    }
}
