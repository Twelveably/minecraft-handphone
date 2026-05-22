package com.twelveably.handphone.phone;

public record PhoneChatMessageData(String message, boolean outgoing, long timestampMillis) {
    public PhoneChatMessageData(String message, boolean outgoing) {
        this(message, outgoing, 0L);
    }
}
