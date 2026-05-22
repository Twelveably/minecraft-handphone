package com.twelveably.handphone.phone;

public final class PhoneSignals {
    private static boolean enabled = true;

    private PhoneSignals() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        PhoneSignals.enabled = enabled;
    }
}
