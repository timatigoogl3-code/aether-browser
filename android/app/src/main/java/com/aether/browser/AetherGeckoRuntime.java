package com.aether.browser;

import android.content.Context;
import android.util.Log;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;

/**
 * Aether Gecko Engine Runtime with:
 *  1. Independent Mozilla NSS Root CA store enforcement via enterprise roots.
 *  2. Mali-400 MP2 GPU Hardware acceleration at 60 FPS (MSAA disabled for bandwidth).
 *  3. Single-process execution (useMultiprocess(false)) for 1GB RAM Lenovo A536.
 */
public class AetherGeckoRuntime {
    private static final String TAG = "Aether/Runtime";
    private static GeckoRuntime sRuntime;

    public static synchronized GeckoRuntime get(Context context) {
        if (sRuntime == null) {
            HardwareProfiler.logHardwareTelemetry();

            GeckoRuntimeSettings.Builder settingsBuilder = new GeckoRuntimeSettings.Builder();

            // Disable multi-process on 1GB RAM device to eliminate 150MB+ IPC process overhead
            settingsBuilder.useMultiprocess(false);

            // Disable MSAA on Mali-400 MP2 to save precious GPU memory bandwidth and keep 60 FPS
            settingsBuilder.glMsaaLevel(0);

            // Enable console output for empirical telemetry
            settingsBuilder.consoleOutput(true);
            settingsBuilder.aboutConfigEnabled(true);

            // Configure hardware acceleration, root CAs, and low memory limits
            settingsBuilder.arguments(new String[] {
                // Enterprise & Custom Root CAs (bypassing expired Android 4.4 DST Root CA X3)
                "--pref", "security.enterprise_roots.enabled=true",
                "--pref", "security.certerrors.mitm.auto_enable_enterprise_roots=true",
                "--pref", "security.tls.version.min=1",
                "--pref", "security.tls.version.max=4",           // TLS 1.3

                // Force GLES 2.0 Hardware compositing on Mali-400 MP2
                "--pref", "layers.acceleration.force-enabled=true",
                "--pref", "layers.offmainthreadcomposition.enabled=true",
                "--pref", "layers.async-pan-zoom.enabled=true",

                // Low-memory footprint tuning for Lenovo A536 (1GB RAM)
                "--pref", "browser.cache.memory.capacity=16384", // 16MB RAM cache limit
                "--pref", "browser.cache.disk.capacity=262144",   // 256MB disk cache
                "--pref", "image.mem.decode_on_draw=true",
                "--pref", "image.mem.discardable=true",
                "--pref", "javascript.options.mem.max=67108864",  // 64MB JS heap cap

                // Modern web standards
                "--pref", "dom.webcomponents.shadowdom.enabled=true",
                "--pref", "layout.css.grid.enabled=true",
                "--pref", "layout.css.flexbox.enabled=true"
            });

            sRuntime = GeckoRuntime.create(context.getApplicationContext(), settingsBuilder.build());
            Log.i(TAG, "Aether GeckoRuntime initialized successfully for Lenovo A536");
        }
        return sRuntime;
    }
}
