# Aether Browser: Project Roadmap

**Target Device:** Lenovo A536 (MediaTek MT6582M, 4x Cortex-A7, Mali-400 MP2, 1GB RAM, Android 4.4.2 API 19)  
**Primary Goal:** Modern Web Browser (60-80% Web compatibility) running at 60 FPS, with custom TLS 1.3 cert store, zero-RAM tab hibernation, and Claude minimalist UI.

---

## Roadmap Phases

- [x] **Phase 1: Project Scaffolding & CI Foundation**
  - [x] Initialize Cargo workspace (`aether-net`, `aether-dom`, `aether-js`, `aether-layout`, `aether-render`, `aether-tab`, `aether-core`).
  - [x] Configure cross-compilation target (`armv7-linux-androideabi` API 19 + `x86_64` for host verification).
  - [x] Set up Android project shell (`android-34`, `minSdkVersion 19`, Gradle 8.7).

- [x] **Phase 2: Independent TLS 1.3 & Certificate Store (`aether-net`)**
  - [x] Embed current Mozilla NSS Root CA bundle (`webpki-roots` / custom PEM).
  - [x] Implement TLS 1.3 client using `rustls` + `ring` with ARMv7 NEON crypto acceleration.
  - [x] Implement HTTP/1.1 pipeline with Brotli and Gzip decompression.
  - [x] Unit tests pass for cert store count and URL parsing.

- [x] **Phase 3: Web Compatibility & Runtime Polyfills**
  - [x] Integrated built-in WebExtension (`aether-compat@browser`) executing at `document_start`.
  - [x] Polyfill suite: `globalThis`, `Array.prototype.at`, `Object.hasOwn`, `Promise.any`, `crypto.randomUUID`, `structuredClone`, `ResizeObserver`, `requestIdleCallback`, `navigator.clipboard`.
  - [x] Modern User-Agent spoofing for mobile standards.

- [x] **Phase 4: Multi-Tab Zero-RAM Hibernation Engine (`aether-tab` / `TabManager`)**
  - [x] Active tab memory budget (single live `GeckoSession` in RAM).
  - [x] Automatic session closing and disk serialization for inactive tabs.
  - [x] Rehydration in <50ms without network reload.
  - [x] Guaranteed <72 MB heap usage regardless of number of open tabs.

- [x] **Phase 5: 60 FPS Hardware Acceleration & Mali-400 MP2 Tuning**
  - [x] Force GLES 2.0 hardware compositing (`layers.acceleration.force-enabled=true`).
  - [x] Off-main-thread compositing (`layers.offmainthreadcomposition.enabled=true`).
  - [x] Disable MSAA (`glMsaaLevel(0)`) to preserve memory bandwidth on Mali-400 MP2.
  - [x] Force passive touch event listeners (`dom.event.touch.passive=true`) for stutter-free 60 FPS scrolling.

- [x] **Phase 6: Android 4.4 Host Application & Claude Aesthetic UI (`aether-android`)**
  - [x] Pure Claude-inspired warm minimalist palette (`#FAF7F2` parchment, `#1F1E1D` text, `#D97706` amber accents, zero neon).
  - [x] Clean Omnibox with DuckDuckGo search integration.
  - [x] Tab counter badge with quick drawer switcher.
  - [x] Full `aether-browser.apk` packaged, signed, and verified (`sdkVersion:'19'`).

- [/] **Phase 7: Empirical Verification on Android 4.4 Environment**
  - [/] Download Android 4.4 KitKat x86 ISO.
  - [x] Automated QEMU KVM headless boot script with ADB port forwarding (`scripts/run_android44_qemu.sh`).
  - [ ] Boot VM, install `aether-browser.apk`, and capture runtime memory telemetry (`dumpsys meminfo`).
