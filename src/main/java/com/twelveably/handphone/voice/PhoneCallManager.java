package com.twelveably.handphone.voice;

import com.twelveably.handphone.Config;
import com.twelveably.handphone.network.HandphoneNetwork;
import com.twelveably.handphone.network.SyncCallStatePacket;
import com.twelveably.handphone.network.SyncChatMessagesPacket;
import com.twelveably.handphone.network.SyncLoudspeakerStatePacket;
import com.twelveably.handphone.network.SyncMuteStatePacket;
import com.twelveably.handphone.network.SyncCallStatePacket.State;
import com.twelveably.handphone.phone.PhoneChats;
import com.twelveably.handphone.phone.PhoneContacts;
import com.twelveably.handphone.phone.PhoneInventory;
import com.twelveably.handphone.phone.PhoneLocator;
import com.twelveably.handphone.phone.PhoneNumbers;
import com.twelveably.handphone.phone.PhoneSignals;
import com.twelveably.handphone.phone.PhoneSoundPlayer;
import com.twelveably.handphone.phone.PhoneLocator.LocatedPhone;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.StaticAudioChannel;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

public final class PhoneCallManager {
   private static final long PENDING_CALL_TIMEOUT_TICKS = 400L;
   private static final long CALLER_ONLY_NOT_THROUGH_TICKS = 200L;
   private static final long CALLER_ONLY_CUT_TICKS = 20L;
   private static final long RINGTONE_UPDATE_INTERVAL_TICKS = 20L;
   private static final Map<UUID, PhoneCall> ACTIVE_CALLS = new HashMap<>();
   private static final Map<UUID, PendingCall> PENDING_CALLS_BY_CALLER = new HashMap<>();
   private static final Map<UUID, PendingCall> PENDING_CALLS_BY_TARGET = new HashMap<>();
   private static final Map<String, PendingCall> PENDING_CALLS_BY_TARGET_NUMBER = new HashMap<>();
   private static final Map<String, LocationalAudioChannel> LOUDSPEAKER_CHANNELS = new HashMap<>();
   private static final Map<String, StaticAudioChannel> CALL_AUDIO_CHANNELS = new HashMap<>();
   private static final Map<UUID, NoiseCodec> NOISE_CODECS_BY_SPEAKER = new HashMap<>();
   private static VoicechatServerApi voicechatApi;

   private PhoneCallManager() {
   }

   public static void setVoicechatApi(VoicechatServerApi api) {
      voicechatApi = api;
   }

   public static void clearVoicechatApi() {
      ACTIVE_CALLS.clear();
      PENDING_CALLS_BY_CALLER.clear();
      PENDING_CALLS_BY_TARGET.clear();
      PENDING_CALLS_BY_TARGET_NUMBER.clear();
      LOUDSPEAKER_CHANNELS.clear();
      CALL_AUDIO_CHANNELS.clear();
      NOISE_CODECS_BY_SPEAKER.values().forEach(NoiseCodec::close);
      NOISE_CODECS_BY_SPEAKER.clear();
      voicechatApi = null;
   }

   @SubscribeEvent
   public static void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         onPlayerLogout(player);
      }
   }

   public static void onPlayerLogout(ServerPlayer player) {
      UUID id = player.getUUID();
      NoiseCodec noiseCodec = NOISE_CODECS_BY_SPEAKER.remove(id);
      if (noiseCodec != null) {
         noiseCodec.close();
      }

      PhoneCall activeCall = ACTIVE_CALLS.get(id);
      if (activeCall != null) {
         endCallForLogout(activeCall, player);
      }

      PendingCall pending = PENDING_CALLS_BY_CALLER.remove(id);
      if (pending != null) {
         cancelPendingCall(pending, "Call cancelled.");
      }

      PendingCall incomingPending = PENDING_CALLS_BY_TARGET.get(id);
      if (incomingPending != null) {
         convertPendingCallToCallerOnly(incomingPending, player.serverLevel().getGameTime());
      }
   }

   public static void onPhoneDestroyed(String phoneNumber) {
      for (PhoneCall call : ACTIVE_CALLS.values()
         .toArray(new PhoneCall[0])) {
         if (call.callerPhoneNumber.equals(phoneNumber) || call.targetPhoneNumber.equals(phoneNumber)) {
            endCall(call.callerId);
            return;
         }
      }

      PendingCall pending = PENDING_CALLS_BY_TARGET_NUMBER.get(phoneNumber);
      if (pending != null) {
         cancelPendingCall(pending, "Call cancelled.");
      } else {
         for (PendingCall p : PENDING_CALLS_BY_CALLER.values()
            .toArray(new PendingCall[0])) {
            if (p.callerPhoneNumber.equals(phoneNumber)) {
               cancelPendingCall(p, "Call cancelled.");
               return;
            }
         }
      }
   }

   public static void cancelPendingCalls(String message) {
      for (PendingCall pendingCall : PENDING_CALLS_BY_CALLER.values()
         .toArray(new PendingCall[0])) {
         cancelPendingCall(pendingCall, message);
      }
   }

   public static void requestOrEndCall(ServerPlayer caller, String contactName, String targetPhoneNumber, String callerPhoneNumber) {
      if (!PhoneSignals.isEnabled()) {
         caller.displayClientMessage(Component.literal(contactName + " is not reachable."), true);
      } else if (ACTIVE_CALLS.containsKey(caller.getUUID())) {
         endCall(caller.getUUID());
      } else {
         PendingCall existingPendingCall = PENDING_CALLS_BY_CALLER.remove(caller.getUUID());
         if (existingPendingCall != null) {
            cancelPendingCall(existingPendingCall, "Call cancelled.");
         } else {
            LocatedPhone target = (LocatedPhone)PhoneLocator.findPhone(caller.server, targetPhoneNumber).orElse(null);
            long now = caller.serverLevel().getGameTime();
            ServerPlayer targetPlayer = target == null ? null : (ServerPlayer)target.player().orElse(null);
            boolean targetReachable = target != null && targetPlayer != null && target.level() != null && isSameDimension(caller.serverLevel(), target.level());
            if (!targetReachable) {
               PendingCall pendingCall = new PendingCall(
                  caller, null, null, contactName, targetPhoneNumber, targetPhoneNumber, callerPhoneNumber, now, 0L, 0L, true
               );
               PENDING_CALLS_BY_CALLER.put(caller.getUUID(), pendingCall);
               caller.displayClientMessage(Component.literal("Calling " + contactName + "."), true);
               sendCallState(
                  caller,
                  State.OUTGOING_CALLING,
                  callerPhoneNumber,
                  contactName,
                  targetPhoneNumber,
                  getPhoneContactProfileId(caller.server, callerPhoneNumber, targetPhoneNumber)
               );
               updateCallerRingtone(pendingCall, now);
            } else {
               migratePhoneContacts(caller.server, callerPhoneNumber);
               migratePhoneContacts(caller.server, targetPhoneNumber);
               String targetDisplayName = getContactNameOrNumber(caller.server, targetPhoneNumber, callerPhoneNumber);
               String targetDisplayProfileId = PhoneContacts.getContactProfileId(caller.server, targetPhoneNumber, callerPhoneNumber);
               String callerDisplayProfileId = getPhoneContactProfileId(caller.server, callerPhoneNumber, targetPhoneNumber);
               PendingCall pendingCall = new PendingCall(
                  caller, target, targetPlayer, contactName, targetDisplayName, targetPhoneNumber, callerPhoneNumber, now, 0L, 0L, false
               );
               PENDING_CALLS_BY_CALLER.put(caller.getUUID(), pendingCall);
               PENDING_CALLS_BY_TARGET_NUMBER.put(targetPhoneNumber, pendingCall);
               if (pendingCall.targetPlayer != null) {
                  PENDING_CALLS_BY_TARGET.put(pendingCall.targetPlayer.getUUID(), pendingCall);
                  if (pendingCall.targetPlayer != caller) {
                     sendCallState(
                        pendingCall.targetPlayer, State.INCOMING_RINGING, targetPhoneNumber, targetDisplayName, callerPhoneNumber, targetDisplayProfileId
                     );
                  }
               }

               caller.displayClientMessage(Component.literal("Calling " + contactName + "."), true);
               if (target.player().filter(player -> player == caller).isPresent()) {
                  sendCallState(caller, State.INCOMING_RINGING, targetPhoneNumber, targetDisplayName, callerPhoneNumber, targetDisplayProfileId);
               } else {
                  sendCallState(caller, State.OUTGOING_RINGING, callerPhoneNumber, contactName, targetPhoneNumber, callerDisplayProfileId);
               }

               updateTargetRingtone(pendingCall, now);
               updateCallerRingtone(pendingCall, now);
            }
         }
      }
   }

   public static void syncCallStateForPhone(ServerPlayer player, ItemStack phone) {
      PhoneNumbers.getPhoneNumber(phone)
         .ifPresent(
            phoneNumber -> {
               PendingCall pendingCall = PENDING_CALLS_BY_TARGET_NUMBER.get(phoneNumber);
               if (pendingCall != null) {
                  pendingCall.targetPlayer = player;
                  pendingCall.targetDisplayName = getContactNameOrNumber(player.server, phoneNumber, pendingCall.callerPhoneNumber);
                  PENDING_CALLS_BY_TARGET.put(player.getUUID(), pendingCall);
                  sendCallState(
                     player,
                     State.INCOMING_RINGING,
                     phoneNumber,
                     pendingCall.targetDisplayName,
                     pendingCall.callerPhoneNumber,
                     PhoneContacts.getContactProfileId(player.server, phoneNumber, pendingCall.callerPhoneNumber)
                  );
               } else {
                  for (PendingCall callerPendingCall : PENDING_CALLS_BY_CALLER.values()) {
                     if (callerPendingCall.callerPhoneNumber.equals(phoneNumber)) {
                        State state = callerPendingCall.callerOnly ? State.OUTGOING_CALLING : State.OUTGOING_RINGING;
                        sendCallState(
                           player,
                           state,
                           phoneNumber,
                           callerPendingCall.contactName,
                           callerPendingCall.targetPhoneNumber,
                           PhoneContacts.getContactProfileId(player.server, phoneNumber, callerPendingCall.targetPhoneNumber)
                        );
                        return;
                     }
                  }

                  PhoneCall activeCall = findActiveCallByPhoneNumber(phoneNumber);
                  if (activeCall == null) {
                     sendCallState(player, State.NONE, phoneNumber, "", "");
                     sendLoudspeakerState(player, phoneNumber, false);
                     sendMuteState(player, phoneNumber, false);
                  } else {
                     transferCallSideToHolder(activeCall, phoneNumber, player);
                     if (activeCall.callerPhoneNumber.equals(phoneNumber)) {
                        sendCallState(
                           player,
                           State.ACTIVE,
                           phoneNumber,
                           activeCall.getRemoteProfileName(phoneNumber),
                           activeCall.targetPhoneNumber,
                           activeCall.getRemoteProfileId(phoneNumber)
                        );
                        sendLoudspeakerState(player, phoneNumber, activeCall.isLoudspeakerEnabled(phoneNumber));
                        sendMuteState(player, phoneNumber, activeCall.isMuted(phoneNumber));
                     } else if (activeCall.targetPhoneNumber.equals(phoneNumber)) {
                        sendCallState(
                           player,
                           State.ACTIVE,
                           phoneNumber,
                           activeCall.getRemoteProfileName(phoneNumber),
                           activeCall.callerPhoneNumber,
                           activeCall.getRemoteProfileId(phoneNumber)
                        );
                        sendLoudspeakerState(player, phoneNumber, activeCall.isLoudspeakerEnabled(phoneNumber));
                        sendMuteState(player, phoneNumber, activeCall.isMuted(phoneNumber));
                     }
                  }
               }
            }
         );
   }

   private static void tryTransferCallToNewHolder(ServerPlayer newHolder, String phoneNumber, ItemStack phone) {
      for (PhoneCall call : ACTIVE_CALLS.values()
         .toArray(new PhoneCall[0])) {
         boolean isCallerPhone = call.callerPhoneNumber.equals(phoneNumber);
         boolean isTargetPhone = call.targetPhoneNumber.equals(phoneNumber);
         if (isCallerPhone || isTargetPhone) {
            UUID originalOwnerId = isCallerPhone ? call.callerId : call.targetId;
            if (newHolder.getUUID().equals(originalOwnerId)) {
               return;
            } else if (voicechatApi == null) {
               return;
            } else {
               VoicechatConnection newConnection = voicechatApi.getConnectionOf(newHolder.getUUID());
               if (newConnection == null) {
                  return;
               } else {
                  closeCallAudioChannel(phoneNumber);
                  if (isCallerPhone) {
                     call.transferCaller(newHolder);
                  } else {
                     call.transferTarget(newHolder);
                  }

                  reindexActiveCall(call);
                  closeLoudspeaker(phoneNumber);
                  call.setLoudspeakerEnabled(phoneNumber, false);
                  sendLoudspeakerState(newHolder, phoneNumber, false);
                  String otherNumber = isCallerPhone ? call.targetPhoneNumber : call.callerPhoneNumber;
                  sendCallState(newHolder, State.ACTIVE, phoneNumber, call.getRemoteProfileName(phoneNumber), otherNumber, call.getRemoteProfileId(phoneNumber));
                  sendMuteState(newHolder, phoneNumber, call.isMuted(phoneNumber));
                  return;
               }
            }
         }
      }
   }

   public static void respondToPendingCall(ServerPlayer target, String targetPhoneNumber, boolean accepted) {
      if (!PhoneSignals.isEnabled()) {
         sendCallState(target, State.NONE, targetPhoneNumber, "", "");
         target.displayClientMessage(Component.literal("No signal."), true);
      } else {
         PendingCall pendingCall = PENDING_CALLS_BY_TARGET_NUMBER.get(targetPhoneNumber);
         if (pendingCall == null) {
            pendingCall = PENDING_CALLS_BY_TARGET.get(target.getUUID());
         }

         if (pendingCall == null) {
            sendCallState(target, State.NONE, targetPhoneNumber, "", "");
         } else {
            removePendingCall(pendingCall);
            stopPendingRingtones(pendingCall);
            if (!accepted) {
               addCallLog(pendingCall.caller.server, pendingCall.callerPhoneNumber, pendingCall.targetPhoneNumber, "Declined voice call", true);
               addCallLog(pendingCall.caller.server, pendingCall.targetPhoneNumber, pendingCall.callerPhoneNumber, "Declined voice call", false);
               pendingCall.caller.displayClientMessage(Component.literal(target.getName().getString() + " declined the call."), true);
               sendCallState(pendingCall.caller, State.NONE, pendingCall.callerPhoneNumber, "", "");
               sendCallState(target, State.NONE, pendingCall.targetPhoneNumber, "", "");
            } else {
               startVoiceCall(pendingCall, target);
            }
         }
      }
   }

   public static void toggleLoudspeaker(ServerPlayer player, String sourcePhoneNumber) {
      PhoneCall call = findActiveCallByPhoneNumber(sourcePhoneNumber);
      if (call != null && PhoneInventory.isCarryingPhone(player, sourcePhoneNumber)) {
         boolean enabled = !call.isLoudspeakerEnabled(sourcePhoneNumber);
         call.setLoudspeakerEnabled(sourcePhoneNumber, enabled);
         if (!enabled) {
            closeLoudspeaker(sourcePhoneNumber);
         }

         sendLoudspeakerState(player, sourcePhoneNumber, enabled);
         player.displayClientMessage(Component.literal("Loudspeaker " + (enabled ? "on." : "off.")), true);
      } else {
         player.displayClientMessage(Component.literal("Loudspeaker needs an active phone call."), true);
      }
   }

   public static void toggleMute(ServerPlayer player, String sourcePhoneNumber) {
      PhoneCall call = findActiveCallByPhoneNumber(sourcePhoneNumber);
      if (call != null && PhoneInventory.isCarryingPhone(player, sourcePhoneNumber)) {
         boolean muted = !call.isMuted(sourcePhoneNumber);
         call.setMuted(sourcePhoneNumber, muted);
         sendMuteState(player, sourcePhoneNumber, muted);
         player.displayClientMessage(Component.literal("Phone mic " + (muted ? "muted." : "unmuted.")), true);
      } else {
         player.displayClientMessage(Component.literal("Mute needs an active phone call."), true);
      }
   }

   public static void handleMicrophonePacket(MicrophonePacketEvent event) {
      if (voicechatApi != null && event.getSenderConnection() != null) {
         UUID speakerId = event.getSenderConnection().getPlayer().getUuid();
         ServerPlayer speaker = findServerPlayer(event.getSenderConnection());
         PhoneCall call = findActiveCallForSpeaker(speaker, speakerId);
         if (call == null) {
            relayNearbyPlayerBleed(speakerId, event);
         } else if (speaker != null) {
            if (!isSameDimension(call.caller.serverLevel(), call.target.serverLevel())) {
               endCall(call.callerId);
            } else {
               String speakerPhoneNumber = getCarriedCallPhoneNumber(speaker, call);
               if (speakerPhoneNumber.isBlank()) {
                  speakerPhoneNumber = call.getPhoneNumber(speakerId);
               }

               if (!speakerPhoneNumber.isBlank()) {
                  transferCallSideToHolder(call, speakerPhoneNumber, speaker);
                  String receiverPhoneNumber = call.getOtherPhoneNumber(speakerPhoneNumber);
                  LocatedPhone locatedPhone = (LocatedPhone)PhoneLocator.findPhone(speaker.server, speakerPhoneNumber).orElse(null);
                  boolean phoneIsDropped = locatedPhone != null && locatedPhone.player().isEmpty();
                  boolean phoneIsLoaded = locatedPhone != null && locatedPhone.isLoaded();
                  double microphoneGain = 1.0;
                  if (!call.isMuted(speakerPhoneNumber)) {
                     if (phoneIsDropped && phoneIsLoaded) {
                        BlockPos phonePos = locatedPhone.soundPos();
                        double distSq = speaker.blockPosition().distSqr(phonePos);
                        double radius = getCallAudioRadius();
                        if (distSq > radius * radius) {
                           return;
                        }

                        microphoneGain = linearVolume(distSq, radius);
                     } else if (phoneIsDropped && !phoneIsLoaded) {
                        return;
                     }

                     if (phoneIsDropped && !call.isLoudspeakerEnabled(speakerPhoneNumber)) {
                        call.setLoudspeakerAutoEnabled(speakerPhoneNumber, true);
                     } else if (!phoneIsDropped && call.isLoudspeakerAutoEnabled(speakerPhoneNumber)) {
                        call.setLoudspeakerAutoEnabled(speakerPhoneNumber, false);
                        closeLoudspeaker(speakerPhoneNumber);
                        sendLoudspeakerState(speaker, speakerPhoneNumber, false);
                     }

                     byte[] phoneAudio = applyConfiguredVoiceEffects(speakerId, event, microphoneGain);
                     sendCallAudio(call, receiverPhoneNumber, event, phoneAudio);
                     sendLoudspeakerAudio(call, receiverPhoneNumber, event, phoneAudio);
                  }
               }
            }
         }
      }
   }

   private static void relayNearbyPlayerBleed(UUID speakerId, MicrophonePacketEvent event) {
      if (voicechatApi != null && !ACTIVE_CALLS.isEmpty()) {
         ServerPlayer speaker = findServerPlayer(event.getSenderConnection());
         if (speaker != null) {
            for (PhoneCall call : ACTIVE_CALLS.values()
               .toArray(new PhoneCall[0])) {
         if (call.isPrimaryEntry(ACTIVE_CALLS)) {
                  relayNearbyPlayerBleedIntoPhone(call, speakerId, speaker, call.callerPhoneNumber, call.targetPhoneNumber, event);
                  relayNearbyPlayerBleedIntoPhone(call, speakerId, speaker, call.targetPhoneNumber, call.callerPhoneNumber, event);
               }
            }
         }
      }
   }

   private static void relayNearbyPlayerBleedIntoPhone(
      PhoneCall call,
      UUID speakerId,
      ServerPlayer speaker,
      String microphonePhoneNumber,
      String receiverPhoneNumber,
      MicrophonePacketEvent event
   ) {
      if (!speakerId.equals(call.callerId) && !speakerId.equals(call.targetId)) {
         if (!call.isMuted(microphonePhoneNumber)) {
            LocatedPhone microphonePhone = (LocatedPhone)PhoneLocator.findPhone(speaker.server, microphonePhoneNumber).orElse(null);
            if (microphonePhone != null && microphonePhone.isLoaded()) {
               if (isSameDimension(speaker.serverLevel(), microphonePhone.level())) {
                  double radius = getCallAudioRadius();
                  if (!(speaker.blockPosition().distSqr(microphonePhone.soundPos()) > radius * radius)) {
                     double microphoneGain = linearVolume(speaker.blockPosition().distSqr(microphonePhone.soundPos()), radius);
                     byte[] phoneAudio = applyConfiguredVoiceEffects(speakerId, event, microphoneGain);
                     sendCallAudio(call, receiverPhoneNumber, event, phoneAudio);
                     sendLoudspeakerAudio(call, receiverPhoneNumber, event, phoneAudio);
                  }
               }
            }
         }
      }
   }

   public static void tick(ServerLevel level) {
      long now = level.getGameTime();
      if (!PENDING_CALLS_BY_CALLER.isEmpty()) {
         for (PendingCall pendingCall : PENDING_CALLS_BY_CALLER.values()
            .toArray(new PendingCall[0])) {
            ServerLevel ringLevel = pendingCall.targetPlayer != null
               ? pendingCall.targetPlayer.serverLevel()
               : (pendingCall.target != null ? pendingCall.target.level() : pendingCall.caller.serverLevel());
            if (ringLevel == level) {
               if (pendingCall.callerOnlyFailureCutAtTick > 0L) {
                  if (now >= pendingCall.callerOnlyFailureCutAtTick) {
                     removePendingCall(pendingCall);
                     sendCallState(pendingCall.caller, State.NONE, pendingCall.callerPhoneNumber, "", "");
                     sendLoudspeakerState(pendingCall.caller, pendingCall.callerPhoneNumber, false);
                  }
               } else if (pendingCall.callerOnly && now - pendingCall.callerOnlyStartedAtTick >= 200L) {
                  pendingCall.callerOnlyFailureCutAtTick = now + 20L;
                  PhoneSoundPlayer.stopRingtone(pendingCall.caller.server, pendingCall.callerPhoneNumber);
                  sendCallState(
                     pendingCall.caller,
                     State.ACTIVE,
                     pendingCall.callerPhoneNumber,
                     pendingCall.contactName,
                     pendingCall.targetPhoneNumber,
                     getPhoneContactProfileId(pendingCall.caller.server, pendingCall.callerPhoneNumber, pendingCall.targetPhoneNumber)
                  );
                  PhoneSoundPlayer.playAtPhone(pendingCall.caller.server, pendingCall.callerPhoneNumber, Config.callNotThroughSound);
               } else if (now - pendingCall.startedAtTick >= 400L) {
                  cancelPendingCall(pendingCall, "Call timed out.");
               } else {
                  if (!pendingCall.callerOnly && now - pendingCall.lastTargetRingtoneUpdateTick >= 20L) {
                     updateTargetRingtone(pendingCall, now);
                  }

                  if (now - pendingCall.lastCallerRingtoneUpdateTick >= 20L) {
                     updateCallerRingtone(pendingCall, now);
                  }
               }
            }
         }
      }
   }

   private static void startVoiceCall(PendingCall pendingCall, ServerPlayer target) {
      ServerPlayer caller = pendingCall.caller;
      if (!isSameDimension(caller.serverLevel(), target.serverLevel())) {
         caller.displayClientMessage(Component.literal("No signal â€” cannot call across dimensions."), true);
         sendCallState(caller, State.NONE, pendingCall.callerPhoneNumber, "", "");
         sendCallState(target, State.NONE, pendingCall.targetPhoneNumber, "", "");
      } else if (voicechatApi == null) {
         caller.displayClientMessage(Component.literal("Simple Voice Chat is not ready."), true);
         sendCallState(caller, State.NONE, pendingCall.callerPhoneNumber, "", "");
         sendCallState(target, State.NONE, pendingCall.targetPhoneNumber, "", "");
      } else if (ACTIVE_CALLS.containsKey(caller.getUUID())) {
         endCall(caller.getUUID());
      } else if (ACTIVE_CALLS.containsKey(target.getUUID())) {
         caller.displayClientMessage(Component.literal(target.getName().getString() + " is already in a phone call."), true);
         sendCallState(caller, State.NONE, pendingCall.callerPhoneNumber, "", "");
      } else {
         VoicechatConnection callerConnection = voicechatApi.getConnectionOf(caller.getUUID());
         VoicechatConnection targetConnection = voicechatApi.getConnectionOf(target.getUUID());
         if (callerConnection != null && targetConnection != null) {
            String callerRemoteName = getPhoneContactName(caller.server, pendingCall.callerPhoneNumber, pendingCall.targetPhoneNumber, pendingCall.contactName);
            String callerRemoteProfileId = getPhoneContactProfileId(caller.server, pendingCall.callerPhoneNumber, pendingCall.targetPhoneNumber);
            String targetRemoteName = getPhoneContactName(
               caller.server, pendingCall.targetPhoneNumber, pendingCall.callerPhoneNumber, pendingCall.targetDisplayName
            );
            String targetRemoteProfileId = getPhoneContactProfileId(caller.server, pendingCall.targetPhoneNumber, pendingCall.callerPhoneNumber);
            PhoneCall call = new PhoneCall(
               caller.getUUID(),
               target.getUUID(),
               caller,
               target,
               pendingCall.callerPhoneNumber,
               pendingCall.targetPhoneNumber,
               targetRemoteProfileId,
               callerRemoteProfileId,
               targetRemoteName,
               callerRemoteName,
               System.currentTimeMillis()
            );
            ACTIVE_CALLS.put(caller.getUUID(), call);
            ACTIVE_CALLS.put(target.getUUID(), call);
            caller.displayClientMessage(Component.literal("Calling " + callerRemoteName + "."), true);
            target.displayClientMessage(Component.literal(pendingCall.targetDisplayName + " is calling you."), true);
            sendCallState(caller, State.ACTIVE, pendingCall.callerPhoneNumber, callerRemoteName, pendingCall.targetPhoneNumber, callerRemoteProfileId);
            sendCallState(target, State.ACTIVE, pendingCall.targetPhoneNumber, targetRemoteName, pendingCall.callerPhoneNumber, targetRemoteProfileId);
            sendLoudspeakerState(caller, pendingCall.callerPhoneNumber, false);
            sendLoudspeakerState(target, pendingCall.targetPhoneNumber, false);
            sendMuteState(caller, pendingCall.callerPhoneNumber, false);
            sendMuteState(target, pendingCall.targetPhoneNumber, false);
         } else {
            caller.displayClientMessage(Component.literal("Both players need Simple Voice Chat connected."), true);
            sendCallState(caller, State.NONE, pendingCall.callerPhoneNumber, "", "");
            sendCallState(target, State.NONE, pendingCall.targetPhoneNumber, "", "");
         }
      }
   }

   private static void endCall(UUID playerId) {
      PhoneCall call = ACTIVE_CALLS.remove(playerId);
      if (call != null) {
         ACTIVE_CALLS.remove(call.callerId);
         ACTIVE_CALLS.remove(call.targetId);
         String durationText = formatDuration(System.currentTimeMillis() - call.startedAtMillis);
         addCallLog(call.caller.server, call.callerPhoneNumber, call.targetPhoneNumber, "Voice call " + durationText, true);
         addCallLog(call.caller.server, call.targetPhoneNumber, call.callerPhoneNumber, "Voice call " + durationText, false);
         closeLoudspeaker(call.callerPhoneNumber);
         closeLoudspeaker(call.targetPhoneNumber);
         closeCallAudioChannel(call.callerPhoneNumber);
         closeCallAudioChannel(call.targetPhoneNumber);
         sendLoudspeakerState(call.caller, call.callerPhoneNumber, false);
         sendLoudspeakerState(call.target, call.targetPhoneNumber, false);
         sendMuteState(call.caller, call.callerPhoneNumber, false);
         sendMuteState(call.target, call.targetPhoneNumber, false);
         sendCallState(call.caller, State.NONE, call.callerPhoneNumber, "", "");
         sendCallState(call.target, State.NONE, call.targetPhoneNumber, "", "");
      }
   }

   private static void endCallForLogout(PhoneCall call, ServerPlayer loggingOutPlayer) {
      ACTIVE_CALLS.entrySet().removeIf(entry -> entry.getValue() == call);
      String durationText = formatDuration(System.currentTimeMillis() - call.startedAtMillis);
      addCallLog(call.caller.server, call.callerPhoneNumber, call.targetPhoneNumber, "Voice call " + durationText, true);
      addCallLog(call.caller.server, call.targetPhoneNumber, call.callerPhoneNumber, "Voice call " + durationText, false);
      closeLoudspeaker(call.callerPhoneNumber);
      closeLoudspeaker(call.targetPhoneNumber);
      closeCallAudioChannel(call.callerPhoneNumber);
      closeCallAudioChannel(call.targetPhoneNumber);
      ServerPlayer remainingPlayer = call.callerId.equals(loggingOutPlayer.getUUID()) ? call.target : call.caller;
      String remainingPhoneNumber = call.callerId.equals(loggingOutPlayer.getUUID()) ? call.targetPhoneNumber : call.callerPhoneNumber;
      sendLoudspeakerState(remainingPlayer, remainingPhoneNumber, false);
      sendMuteState(remainingPlayer, remainingPhoneNumber, false);
      sendCallState(remainingPlayer, State.NONE, remainingPhoneNumber, "", "");
   }

   private static boolean isSameDimension(ServerLevel a, ServerLevel b) {
      return a.dimension().equals(b.dimension());
   }

   private static void updateTargetRingtone(PendingCall pendingCall, long now) {
      pendingCall.lastTargetRingtoneUpdateTick = now;
      if (pendingCall.target != null) {
         PhoneSoundPlayer.updateRingtoneAtPhone(pendingCall.caller.server, pendingCall.targetPhoneNumber, Config.incomingCallSound);
      }
   }

   private static void updateCallerRingtone(PendingCall pendingCall, long now) {
      pendingCall.lastCallerRingtoneUpdateTick = now;
      PhoneSoundPlayer.updateRingtoneAtPhone(pendingCall.caller.server, pendingCall.callerPhoneNumber, Config.callingSound);
   }

   private static void cancelPendingCall(PendingCall pendingCall, String message) {
      removePendingCall(pendingCall);
      stopPendingRingtones(pendingCall);
      if ("Call timed out.".equals(message)) {
         addCallLog(pendingCall.caller.server, pendingCall.callerPhoneNumber, pendingCall.targetPhoneNumber, "Missed voice call", true);
         addCallLog(pendingCall.caller.server, pendingCall.targetPhoneNumber, pendingCall.callerPhoneNumber, "Missed voice call", false);
      }

      pendingCall.caller.displayClientMessage(Component.literal(message), true);
      sendCallState(pendingCall.caller, State.NONE, pendingCall.callerPhoneNumber, "", "");
      sendLoudspeakerState(pendingCall.caller, pendingCall.callerPhoneNumber, false);
      if (pendingCall.targetPlayer != null) {
         sendCallState(pendingCall.targetPlayer, State.NONE, pendingCall.targetPhoneNumber, "", "");
         sendLoudspeakerState(pendingCall.targetPlayer, pendingCall.targetPhoneNumber, false);
      }
   }

   private static void stopPendingRingtones(PendingCall pendingCall) {
      PhoneSoundPlayer.stopRingtone(pendingCall.caller.server, pendingCall.callerPhoneNumber);
      PhoneSoundPlayer.stopRingtone(pendingCall.caller.server, pendingCall.targetPhoneNumber);
   }

   private static void convertPendingCallToCallerOnly(PendingCall pendingCall, long now) {
      if (pendingCall.targetPlayer != null) {
         PENDING_CALLS_BY_TARGET.remove(pendingCall.targetPlayer.getUUID());
      }

      PENDING_CALLS_BY_TARGET_NUMBER.remove(pendingCall.targetPhoneNumber);
      pendingCall.targetPlayer = null;
      pendingCall.target = null;
      pendingCall.callerOnly = true;
      pendingCall.callerOnlyStartedAtTick = now;
      pendingCall.callerOnlyFailureCutAtTick = 0L;
      PhoneSoundPlayer.stopRingtone(pendingCall.caller.server, pendingCall.targetPhoneNumber);
      sendCallState(
         pendingCall.caller,
         State.OUTGOING_CALLING,
         pendingCall.callerPhoneNumber,
         pendingCall.contactName,
         pendingCall.targetPhoneNumber,
         getPhoneContactProfileId(pendingCall.caller.server, pendingCall.callerPhoneNumber, pendingCall.targetPhoneNumber)
      );
      updateCallerRingtone(pendingCall, now);
   }

   private static void removePendingCall(PendingCall pendingCall) {
      PENDING_CALLS_BY_CALLER.remove(pendingCall.caller.getUUID());
      PENDING_CALLS_BY_TARGET_NUMBER.remove(pendingCall.targetPhoneNumber);
      if (pendingCall.targetPlayer != null) {
         PENDING_CALLS_BY_TARGET.remove(pendingCall.targetPlayer.getUUID());
      }
   }

   private static void sendCallState(ServerPlayer player, State state, String localPhoneNumber, String contactName, String phoneNumber) {
      sendCallState(player, state, localPhoneNumber, contactName, phoneNumber, "");
   }

   private static void sendCallState(ServerPlayer player, State state, String localPhoneNumber, String contactName, String phoneNumber, String remoteProfileId) {
      HandphoneNetwork.CHANNEL
         .send(PacketDistributor.PLAYER.with(() -> player), new SyncCallStatePacket(state, localPhoneNumber, contactName, phoneNumber, remoteProfileId));
   }

   private static void sendLoudspeakerState(ServerPlayer player, String localPhoneNumber, boolean enabled) {
      HandphoneNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncLoudspeakerStatePacket(localPhoneNumber, enabled));
   }

   private static void sendMuteState(ServerPlayer player, String localPhoneNumber, boolean muted) {
      HandphoneNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncMuteStatePacket(localPhoneNumber, muted));
   }

   private static void addCallLog(MinecraftServer server, String phoneNumber, String conversationPhoneNumber, String message, boolean outgoing) {
      long timestampMillis = System.currentTimeMillis();
      PhoneLocator.findPhone(server, phoneNumber)
         .flatMap(phone -> phone.phoneStack(phoneNumber))
         .ifPresent(
            phoneStack -> {
               PhoneChats.migrateLegacyMessages(server, phoneStack);
               PhoneChats.addMessage(server, phoneNumber, conversationPhoneNumber, message, outgoing, timestampMillis);
               PhoneLocator.findPhone(server, phoneNumber)
                  .<ServerPlayer>flatMap(LocatedPhone::player)
                  .ifPresent(
                     player -> HandphoneNetwork.CHANNEL
                           .send(
                              PacketDistributor.PLAYER.with(() -> player),
                              new SyncChatMessagesPacket(conversationPhoneNumber, PhoneChats.getMessages(server, phoneNumber, conversationPhoneNumber))
                           )
                  );
            }
         );
   }

   private static String formatDuration(long durationMillis) {
      long elapsedSeconds = Math.max(0L, durationMillis / 1000L);
      long minutes = elapsedSeconds / 60L;
      long seconds = elapsedSeconds % 60L;
      return String.format("%02d:%02d", minutes, seconds);
   }

   private static void sendCallAudio(
      PhoneCall call, String receiverPhoneNumber, MicrophonePacketEvent event, byte[] opusAudio
   ) {
      if (voicechatApi != null) {
         ServerPlayer receiver = getCurrentPhoneHolder(call, receiverPhoneNumber);
         if (receiver != null) {
            UUID senderId = event.getSenderConnection().getPlayer().getUuid();
            if (!receiver.getUUID().equals(senderId)) {
               VoicechatConnection receiverConnection = voicechatApi.getConnectionOf(receiver.getUUID());
               if (receiverConnection != null) {
                  StaticAudioChannel channel = CALL_AUDIO_CHANNELS.get(receiverPhoneNumber);
                  if (channel == null || channel.isClosed()) {
                     channel = voicechatApi.createStaticAudioChannel(
                        UUID.randomUUID(), voicechatApi.fromServerLevel(receiver.serverLevel()), receiverConnection
                     );
                     if (channel == null) {
                        return;
                     }

                     CALL_AUDIO_CHANNELS.put(receiverPhoneNumber, channel);
                  }

                  LocatedPhone receiverPhone = (LocatedPhone)PhoneLocator.findPhone(receiver.getServer(), receiverPhoneNumber).orElse(null);
                  if (receiverPhone == null || !receiverPhone.player().isEmpty()) {
                     if (opusAudio == null) {
                        channel.send((MicrophonePacket)event.getPacket());
                     } else {
                        channel.send(opusAudio);
                     }
                  }
               }
            }
         }
      }
   }

   private static void sendLoudspeakerAudio(
      PhoneCall call, String speakerPhoneNumber, MicrophonePacketEvent event, byte[] opusAudio
   ) {
      if (call.isLoudspeakerEnabled(speakerPhoneNumber) || call.isLoudspeakerAutoEnabled(speakerPhoneNumber)) {
         if (voicechatApi != null) {
            UUID senderId = event.getSenderConnection().getPlayer().getUuid();
            LocatedPhone speakerPhone = (LocatedPhone)PhoneLocator.findPhone(call.caller.server, speakerPhoneNumber).orElse(null);
            if (speakerPhone != null && speakerPhone.isLoaded()) {
               BlockPos soundPos = speakerPhone.soundPos();
               ServerLevel soundLevel = speakerPhone.level();
               LocationalAudioChannel channel = LOUDSPEAKER_CHANNELS.get(speakerPhoneNumber);
               if (channel != null && !channel.isClosed()) {
                  channel.updateLocation(
                     voicechatApi.createPosition((double)soundPos.getX() + 0.5, (double)soundPos.getY() + 0.5, (double)soundPos.getZ() + 0.5)
                  );
                  ServerPlayer speakerReceiver = getCurrentPhoneHolder(call, call.getOtherPhoneNumber(speakerPhoneNumber));
                  UUID receiverUUID = speakerReceiver != null ? speakerReceiver.getUUID() : null;
                  channel.setFilter(
                     player -> !player.getUuid().equals(senderId)
                           && (receiverUUID == null || !player.getUuid().equals(receiverUUID))
                           && isWithinRadius(player.getPosition(), soundPos)
                  );
               } else {
                  channel = voicechatApi.createLocationalAudioChannel(
                     UUID.randomUUID(),
                     voicechatApi.fromServerLevel(soundLevel),
                     voicechatApi.createPosition((double)soundPos.getX() + 0.5, (double)soundPos.getY() + 0.5, (double)soundPos.getZ() + 0.5)
                  );
                  if (channel == null) {
                     return;
                  }

                  ServerPlayer speakerReceiver = getCurrentPhoneHolder(call, call.getOtherPhoneNumber(speakerPhoneNumber));
                  UUID receiverUUID = speakerReceiver != null ? speakerReceiver.getUUID() : null;
                  channel.setFilter(
                     player -> !player.getUuid().equals(senderId)
                           && (receiverUUID == null || !player.getUuid().equals(receiverUUID))
                           && isWithinRadius(player.getPosition(), soundPos)
                  );
                  LOUDSPEAKER_CHANNELS.put(speakerPhoneNumber, channel);
               }

               if (opusAudio == null) {
                  channel.send((MicrophonePacket)event.getPacket());
               } else {
                  channel.send(opusAudio);
               }
            }
         }
      }
   }

   private static void closeLoudspeaker(String phoneNumber) {
      LocationalAudioChannel channel = LOUDSPEAKER_CHANNELS.remove(phoneNumber);
      if (channel != null && !channel.isClosed()) {
         channel.flush();
      }
   }

   private static void closeCallAudioChannel(String phoneNumber) {
      StaticAudioChannel channel = CALL_AUDIO_CHANNELS.remove(phoneNumber);
      if (channel != null && !channel.isClosed()) {
         channel.flush();
      }
   }

   private static double getCallAudioRadius() {
      return Math.max(1.0, Config.callAudioRadiusBlocks);
   }

   private static double linearVolume(double distanceSq, double radius) {
      double distance = Math.sqrt(distanceSq);
      double volume = 1.0 - distance / radius;
      return Math.max(0.05, Math.min(1.0, volume));
   }

   @Nullable
   private static byte[] applyConfiguredVoiceEffects(UUID speakerId, MicrophonePacketEvent event, double gain) {
      double noisePercent = Math.max(0.0, Math.min(100.0, Config.phoneVoiceNoisePercent));
      double clampedGain = Math.max(0.0, Math.min(1.0, gain));
      if ((!(noisePercent <= 0.0) || !(clampedGain >= 0.999)) && voicechatApi != null) {
         NoiseCodec codec = NOISE_CODECS_BY_SPEAKER.computeIfAbsent(
            speakerId, ignored -> NoiseCodec.create(voicechatApi)
         );
         return codec != null && codec.isReady()
            ? codec.apply(((MicrophonePacket)event.getPacket()).getOpusEncodedData(), noisePercent / 100.0, clampedGain)
            : null;
      } else {
         return null;
      }
   }

   @Nullable
   private static PhoneCall findActiveCallByPhoneNumber(String phoneNumber) {
      for (PhoneCall call : ACTIVE_CALLS.values()
         .toArray(new PhoneCall[0])) {
         if (call.hasPhoneNumber(phoneNumber)) {
            return call;
         }
      }

      return null;
   }

   @Nullable
   private static PhoneCall findActiveCallForSpeaker(@Nullable ServerPlayer speaker, UUID speakerId) {
      if (speaker != null) {
         for (PhoneCall call : ACTIVE_CALLS.values()
            .toArray(new PhoneCall[0])) {
            if (!getCarriedCallPhoneNumber(speaker, call).isBlank()) {
               return call;
            }
         }
      }

      return ACTIVE_CALLS.get(speakerId);
   }

   private static String getCarriedCallPhoneNumber(ServerPlayer player, PhoneCall call) {
      if (PhoneInventory.isCarryingPhone(player, call.callerPhoneNumber)) {
         return call.callerPhoneNumber;
      } else {
         return PhoneInventory.isCarryingPhone(player, call.targetPhoneNumber) ? call.targetPhoneNumber : "";
      }
   }

   private static void transferCallSideToHolder(PhoneCall call, String phoneNumber, ServerPlayer holder) {
      if (call.callerPhoneNumber.equals(phoneNumber) && !call.callerId.equals(holder.getUUID())) {
         call.transferCaller(holder);
         reindexActiveCall(call);
         closeCallAudioChannel(phoneNumber);
      } else if (call.targetPhoneNumber.equals(phoneNumber) && !call.targetId.equals(holder.getUUID())) {
         call.transferTarget(holder);
         reindexActiveCall(call);
         closeCallAudioChannel(phoneNumber);
      }
   }

   private static void reindexActiveCall(PhoneCall call) {
      ACTIVE_CALLS.entrySet().removeIf(entry -> entry.getValue() == call);
      ACTIVE_CALLS.put(call.callerId, call);
      ACTIVE_CALLS.put(call.targetId, call);
   }

   @Nullable
   private static ServerPlayer getCurrentPhoneHolder(PhoneCall call, String phoneNumber) {
      LocatedPhone locatedPhone = (LocatedPhone)PhoneLocator.findPhone(call.caller.server, phoneNumber).orElse(null);
      if (locatedPhone != null && locatedPhone.player().isPresent()) {
         ServerPlayer holder = (ServerPlayer)locatedPhone.player().get();
         transferCallSideToHolder(call, phoneNumber, holder);
         return holder;
      } else {
         return call.getHolder(phoneNumber);
      }
   }

   private static boolean isWithinRadius(Position position, BlockPos blockPos) {
      double dx = position.getX() - ((double)blockPos.getX() + 0.5);
      double dy = position.getY() - ((double)blockPos.getY() + 0.5);
      double dz = position.getZ() - ((double)blockPos.getZ() + 0.5);
      double radius = getCallAudioRadius();
      return dx * dx + dy * dy + dz * dz <= radius * radius;
   }

   @Nullable
   private static ServerPlayer findServerPlayer(VoicechatConnection connection) {
      if (connection == null) {
         return null;
      } else {
         UUID playerId = connection.getPlayer().getUuid();

         for (PhoneCall call : ACTIVE_CALLS.values()
            .toArray(new PhoneCall[0])) {
            ServerPlayer callerServerPlayer = call.caller.server.getPlayerList().getPlayer(playerId);
            if (callerServerPlayer != null) {
               return callerServerPlayer;
            }
         }

         return null;
      }
   }

   private static String getContactNameOrNumber(MinecraftServer server, String localPhoneNumber, String phoneNumber) {
      return PhoneContacts.getContactName(server, localPhoneNumber, phoneNumber, phoneNumber);
   }

   private static String getPhoneContactName(MinecraftServer server, String localPhoneNumber, String remotePhoneNumber, String fallbackName) {
      return PhoneContacts.getContactName(server, localPhoneNumber, remotePhoneNumber, fallbackName);
   }

   private static String getPhoneContactProfileId(MinecraftServer server, String localPhoneNumber, String remotePhoneNumber) {
      return PhoneContacts.getContactProfileId(server, localPhoneNumber, remotePhoneNumber);
   }

   private static void migratePhoneContacts(MinecraftServer server, String phoneNumber) {
      PhoneLocator.findPhone(server, phoneNumber)
         .flatMap(phone -> phone.phoneStack(phoneNumber))
         .ifPresent(stack -> PhoneContacts.migrateLegacyContacts(server, stack));
   }

}

