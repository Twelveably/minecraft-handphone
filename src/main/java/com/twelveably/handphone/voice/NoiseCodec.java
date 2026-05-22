package com.twelveably.handphone.voice;

import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;

import javax.annotation.Nullable;
import java.util.Random;

final class NoiseCodec {
   @Nullable
   private final OpusDecoder decoder;
   @Nullable
   private final OpusEncoder encoder;
   private final Random random = new Random();

   private NoiseCodec(@Nullable OpusDecoder decoder, @Nullable OpusEncoder encoder) {
      this.decoder = decoder;
      this.encoder = encoder;
   }

   static NoiseCodec create(VoicechatServerApi api) {
      return new NoiseCodec(api.createDecoder(), api.createEncoder());
   }

   boolean isReady() {
      return decoder != null && encoder != null && !decoder.isClosed() && !encoder.isClosed();
   }

   @Nullable
   byte[] apply(byte[] opusAudio, double amount, double gain) {
      if (!isReady()) {
         return null;
      }

      short[] rawAudio = decoder.decode(opusAudio);
      if (rawAudio == null || rawAudio.length == 0) {
         return null;
      }

      int noiseAmplitude = (int) Math.round(3932.04D * amount);
      double clampedGain = Math.max(0.0D, Math.min(1.0D, gain));

      for (int i = 0; i < rawAudio.length; i++) {
         int noisySample = (int) Math.round(rawAudio[i] * clampedGain);
         if (noiseAmplitude > 0) {
            noisySample += random.nextInt(noiseAmplitude * 2 + 1) - noiseAmplitude;
         }

         rawAudio[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, noisySample));
      }

      return encoder.encode(rawAudio);
   }

   void close() {
      if (decoder != null && !decoder.isClosed()) {
         decoder.close();
      }

      if (encoder != null && !encoder.isClosed()) {
         encoder.close();
      }
   }
}
