package com.bugfunbug.linearreader.linear;

public final class LinearCoreTestHooks {

    private LinearCoreTestHooks() {}

    public static void setZstdStartupFailure(Throwable failure) {
        ZstdSupport.setTestFailure(failure);
    }

    public static void clearZstdStartupFailure() {
        ZstdSupport.clearTestFailure();
    }

    public static void setFixedClock(Long nowNs, Long nowMs) {
        LinearRegionFile.setTestStateClock(nowNs, nowMs);
    }

    public static void setPregenActive(Boolean active) {
        DHPregenMonitor.setTestPregenActive(active);
    }
}
