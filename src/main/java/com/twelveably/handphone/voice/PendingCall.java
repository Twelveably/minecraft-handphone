package com.twelveably.handphone.voice;

import com.twelveably.handphone.phone.PhoneLocator.LocatedPhone;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

final class PendingCall {
   final ServerPlayer caller;
   @Nullable
   LocatedPhone target;
   @Nullable
   ServerPlayer targetPlayer;
   final String contactName;
   String targetDisplayName;
   final String targetPhoneNumber;
   final String callerPhoneNumber;
   final long startedAtTick;
   long callerOnlyStartedAtTick;
   long lastTargetRingtoneUpdateTick;
   long lastCallerRingtoneUpdateTick;
   boolean callerOnly;
   long callerOnlyFailureCutAtTick;

   PendingCall(
      ServerPlayer caller,
      LocatedPhone target,
      @Nullable ServerPlayer targetPlayer,
      String contactName,
      String targetDisplayName,
      String targetPhoneNumber,
      String callerPhoneNumber,
      long startedAtTick,
      long lastTargetRingtoneUpdateTick,
      long lastCallerRingtoneUpdateTick,
      boolean callerOnly
   ) {
      this.caller = caller;
      this.target = target;
      this.targetPlayer = targetPlayer;
      this.contactName = contactName;
      this.targetDisplayName = targetDisplayName;
      this.targetPhoneNumber = targetPhoneNumber;
      this.callerPhoneNumber = callerPhoneNumber;
      this.startedAtTick = startedAtTick;
      this.callerOnlyStartedAtTick = startedAtTick;
      this.lastTargetRingtoneUpdateTick = lastTargetRingtoneUpdateTick;
      this.lastCallerRingtoneUpdateTick = lastCallerRingtoneUpdateTick;
      this.callerOnly = callerOnly;
   }
}
