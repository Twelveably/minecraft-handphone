package com.twelveably.handphone.voice;

import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;

@ForgeVoicechatPlugin
public class HandphoneVoicechatPlugin implements VoicechatPlugin {
    @Override
    public String getPluginId() {
        return "handphone";
    }

    @Override
    public void initialize(VoicechatApi api) {
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onVoicechatServerStarted);
        registration.registerEvent(VoicechatServerStoppedEvent.class, this::onVoicechatServerStopped);
        registration.registerEvent(MicrophonePacketEvent.class, PhoneCallManager::handleMicrophonePacket);
    }

    private void onVoicechatServerStarted(VoicechatServerStartedEvent event) {
        VoicechatServerApi api = event.getVoicechat();
        PhoneCallManager.setVoicechatApi(api);
    }

    private void onVoicechatServerStopped(VoicechatServerStoppedEvent event) {
        PhoneCallManager.clearVoicechatApi();
    }
}
