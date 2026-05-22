package com.twelveably.handphone.voice;

import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

final class PhoneCall {
   UUID callerId;
   UUID targetId;
   ServerPlayer caller;
   ServerPlayer target;
   final String callerPhoneNumber;
   final String targetPhoneNumber;
   final String callerProfileId;
   final String targetProfileId;
   final String callerProfileName;
   final String targetProfileName;
   final long startedAtMillis;
   private boolean callerLoudspeaker;
   private boolean targetLoudspeaker;
   private boolean callerAutoLoudspeaker;
   private boolean targetAutoLoudspeaker;
   private boolean callerMuted;
   private boolean targetMuted;

   PhoneCall(
      UUID callerId,
      UUID targetId,
      ServerPlayer caller,
      ServerPlayer target,
      String callerPhoneNumber,
      String targetPhoneNumber,
      String callerProfileId,
      String targetProfileId,
      String callerProfileName,
      String targetProfileName,
      long startedAtMillis
   ) {
      this.callerId = callerId;
      this.targetId = targetId;
      this.caller = caller;
      this.target = target;
      this.callerPhoneNumber = callerPhoneNumber;
      this.targetPhoneNumber = targetPhoneNumber;
      this.callerProfileId = callerProfileId;
      this.targetProfileId = targetProfileId;
      this.callerProfileName = callerProfileName;
      this.targetProfileName = targetProfileName;
      this.startedAtMillis = startedAtMillis;
   }

   void transferCaller(ServerPlayer newHolder) {
      callerId = newHolder.getUUID();
      caller = newHolder;
   }

   void transferTarget(ServerPlayer newHolder) {
      targetId = newHolder.getUUID();
      target = newHolder;
   }

   boolean hasPhoneNumber(UUID playerId, String phoneNumber) {
      if (playerId.equals(callerId)) {
         return callerPhoneNumber.equals(phoneNumber);
      }

      return playerId.equals(targetId) && targetPhoneNumber.equals(phoneNumber);
   }

   boolean hasPhoneNumber(String phoneNumber) {
      return callerPhoneNumber.equals(phoneNumber) || targetPhoneNumber.equals(phoneNumber);
   }

   boolean isLoudspeakerEnabled(String phoneNumber) {
      return callerPhoneNumber.equals(phoneNumber) ? callerLoudspeaker : targetPhoneNumber.equals(phoneNumber) && targetLoudspeaker;
   }

   boolean isLoudspeakerAutoEnabled(String phoneNumber) {
      return callerPhoneNumber.equals(phoneNumber) ? callerAutoLoudspeaker : targetPhoneNumber.equals(phoneNumber) && targetAutoLoudspeaker;
   }

   void setLoudspeakerEnabled(UUID playerId, boolean enabled) {
      if (playerId.equals(callerId)) {
         callerLoudspeaker = enabled;
      } else if (playerId.equals(targetId)) {
         targetLoudspeaker = enabled;
      }
   }

   void setLoudspeakerEnabled(String phoneNumber, boolean enabled) {
      if (callerPhoneNumber.equals(phoneNumber)) {
         callerLoudspeaker = enabled;
      } else if (targetPhoneNumber.equals(phoneNumber)) {
         targetLoudspeaker = enabled;
      }
   }

   void setLoudspeakerAutoEnabled(String phoneNumber, boolean enabled) {
      if (callerPhoneNumber.equals(phoneNumber)) {
         callerAutoLoudspeaker = enabled;
      } else if (targetPhoneNumber.equals(phoneNumber)) {
         targetAutoLoudspeaker = enabled;
      }
   }

   String getPhoneNumber(UUID playerId) {
      if (playerId.equals(callerId)) {
         return callerPhoneNumber;
      }

      return playerId.equals(targetId) ? targetPhoneNumber : "";
   }

   String getOtherPhoneNumber(String phoneNumber) {
      if (callerPhoneNumber.equals(phoneNumber)) {
         return targetPhoneNumber;
      }

      return targetPhoneNumber.equals(phoneNumber) ? callerPhoneNumber : "";
   }

   @Nullable
   ServerPlayer getHolder(String phoneNumber) {
      if (callerPhoneNumber.equals(phoneNumber)) {
         return caller;
      }

      return targetPhoneNumber.equals(phoneNumber) ? target : null;
   }

   boolean isMuted(String phoneNumber) {
      return callerPhoneNumber.equals(phoneNumber) ? callerMuted : targetPhoneNumber.equals(phoneNumber) && targetMuted;
   }

   void setMuted(String phoneNumber, boolean muted) {
      if (callerPhoneNumber.equals(phoneNumber)) {
         callerMuted = muted;
      } else if (targetPhoneNumber.equals(phoneNumber)) {
         targetMuted = muted;
      }
   }

   String getRemoteProfileId(String localPhoneNumber) {
      if (callerPhoneNumber.equals(localPhoneNumber)) {
         return targetProfileId;
      }

      return targetPhoneNumber.equals(localPhoneNumber) ? callerProfileId : "";
   }

   String getRemoteProfileName(String localPhoneNumber) {
      if (callerPhoneNumber.equals(localPhoneNumber)) {
         return targetProfileName;
      }

      return targetPhoneNumber.equals(localPhoneNumber) ? callerProfileName : "";
   }

   boolean isPrimaryEntry(Map<UUID, PhoneCall> activeCalls) {
      return activeCalls.get(callerId) == this;
   }
}
