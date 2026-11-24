package ru.berrytrip.berryup;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

/** Объём ОЗУ машины — для слайдера памяти и диагностики. */
public final class Ram {

    private static int cached = -1;

    private Ram() {
    }

    public static long totalBytes() {
        try {
            OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
            if (bean instanceof com.sun.management.OperatingSystemMXBean) {
                return ((com.sun.management.OperatingSystemMXBean) bean).getTotalPhysicalMemorySize();
            }
        } catch (Throwable ignored) {
        }
        return 0L;
    }

    public static int gb() {
        if (cached > 0) return cached;
        long bytes = totalBytes();
        cached = bytes <= 0 ? 0 : Math.max(1, (int) Math.round(bytes / 1073741824.0));
        return cached;
    }
}
