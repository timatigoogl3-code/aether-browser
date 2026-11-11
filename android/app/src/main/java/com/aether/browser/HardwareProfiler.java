package com.aether.browser;

import android.os.Build;
import android.util.Log;

/**
 * Lenovo A536 Hardware Profiler and System Optimizer.
 * Target Hardware:
 *   SoC: MediaTek MT6582M (Quad-core 1.3 GHz ARM Cortex-A7, 32-bit armeabi-v7a + NEON)
 *   GPU: ARM Mali-400 MP2 (OpenGL ES 2.0 Tile-Based Deferred Renderer)
 *   Display: 480x854 FWVGA (~196 ppi)
 *   RAM: 1 GB LPDDR2/3 (usable heap: 64MB - 128MB)
 *   OS: Android 4.4.2 KitKat (API 19)
 */
public class HardwareProfiler {
    private static final String TAG = "Aether/Hardware";

    public static final int TARGET_WIDTH = 480;
    public static final int TARGET_HEIGHT = 854;
    public static final int MAX_ACTIVE_HEAP_MB = 72; // Strict heap limit for 1GB RAM

    public static boolean isLenovoA536() {
        String model = Build.MODEL;
        String device = Build.DEVICE;
        String hardware = Build.HARDWARE;
        return (model != null && model.contains("A536")) ||
               (device != null && device.contains("A536")) ||
               (hardware != null && hardware.toLowerCase().contains("mt6582"));
    }

    public static void logHardwareTelemetry() {
        Log.i(TAG, "=== Lenovo A536 Optimization Profile Active ===");
        Log.i(TAG, "Device: " + Build.MANUFACTURER + " " + Build.MODEL + " (" + Build.DEVICE + ")");
        Log.i(TAG, "Board/Hardware: " + Build.BOARD + " / " + Build.HARDWARE);
        Log.i(TAG, "OS: Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        Log.i(TAG, "CPU Cores: " + Runtime.getRuntime().availableProcessors());
        Log.i(TAG, "Max JVM Heap: " + (Runtime.getRuntime().maxMemory() / (1024 * 1024)) + " MB");
        Log.i(TAG, "Resolution Target: " + TARGET_WIDTH + "x" + TARGET_HEIGHT + " @ 60 FPS");
    }
}
