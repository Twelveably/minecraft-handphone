package com.twelveably.handphone.phone;

public record PhoneContactData(String name, String phoneNumber, String latestMessage, long latestMessageTimeMillis, String profileId, int unreadCount) {
}
