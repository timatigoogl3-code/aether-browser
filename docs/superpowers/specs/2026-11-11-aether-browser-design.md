# Aether Browser: Architecture & Design Specification

> **Target Device:** Lenovo A536  
> **SoC:** MediaTek MT6582M (4× ARM Cortex-A7 @ 1.3 GHz, 32-bit `armeabi-v7a` with NEON)  
> **GPU:** ARM Mali-400 MP2 (OpenGL ES 2.0 compliant)  
> **RAM:** 1 GB LPDDR2/3 (usable app headroom: ~120 MB before Android Low Memory Killer triggers)  
> **Display:** 480 × 854 pixels (FWVGA, ~196 ppi)  
> **OS:** Android 4.4.2 KitKat (Linux Kernel 3.4.67, API Level 19)  
> **License Requirements:** Commercial-friendly open-source (MIT, Apache 2.0, BSD-3-Clause, MPL-2.0)  

---

## 1. Intent & Mission

The goal of **Aether Browser** is to deliver modern web browsing (60–80% of modern web sites, exceeding legacy mobile Firefox) on the **Lenovo A536** running Android 4.4.2 with 1 GB RAM, maintaining:
1. **Buttery 60 FPS** viewport scrolling and touch responsiveness on Mali-400 MP2.
2. **True independence from Android 4.4's obsolete cryptography stack**: built-in Mozilla NSS Root CA store supporting TLS 1.3 with ChaCha20-Poly1305 and AES-GCM.
3. **Virtually unlimited multi-tab capability on 1 GB RAM** via an active tab budget and zero-RAM disk-backed tab hibernation.
4. **Clean, non-neon, Claude-inspired aesthetic**: Warm eggshell/parchment surfaces, understated typography, unobtrusive controls.

---

## 2. Core Constraints & Target Realities

| Constraint | Reality on Lenovo A536 | Architectural Solution |
|---|---|---|
| **RAM Budget** | 1 GB total; Android 4.4 system consumes ~400–500 MB; dalvik.vm.heapgrowthlimit = 96m/128m. Monolithic Chromium/Gecko engines consume 300–800 MB per tab and trigger LMK kill. | **Micro-Engine in Rust**. The active page heap is capped at **40–70 MB**. Background tabs are serialized to disk/cache and consume **0 bytes** of heap. |
| **CPU / ISA** | Quad-core Cortex-A7 (32-bit ARMv7-A with NEON, thumb-2). No 64-bit support. | All native core code compiled with `-target armv7-linux-androideabi` with `-C target-cpu=cortex-a7` and `-C target-feature=+neon`. |
| **GPU Capability** | ARM Mali-400 MP2 (TBDR architecture, OpenGL ES 2.0 only, NO ES 3.0, NO Vulkan). | Dedicated GLES 2.0 batch renderer. Minimal draw calls, no heavy fragment shaders, texture atlas caching for glyphs and images. |
| **SSL / TLS Obsoletion** | Android 4.4 system OpenSSL is ancient: lacks TLS 1.3, ALPN, modern cipher suites, and system root CAs expired in 2021 (e.g. DST Root CA X3). | Standalone Rust TLS 1.3 stack (`rustls` / `ring`) bundling the full current **Mozilla NSS Root CA Certificate Store** in the APK. |
| **Web Compatibility** | Modern websites rely on ES2020+ JavaScript features (Promises, async/await, BigInt, Fetch, DOM manipulation). | Lightweight modern ECMAScript engine (**QuickJS**) consuming only 3–5 MB RAM while supporting full ES2023, paired with custom DOM/WebAPI bindings. |

---

## 3. Subsystem Architecture

```
+-------------------------------------------------------------------------+
|                  Aether Host UI (Claude Aesthetic)                     |
|     [ Clean Minimal Omnibox | Tab Bar | Reader / Render Viewport ]      |
+-------------------------------------------------------------------------+
                                    |
                                    v (JNI / Native Surface)
+-------------------------------------------------------------------------+
|                       Aether Core (Rust Engine)                         |
|                                                                         |
|  +---------------------+   +---------------------+   +----------------+ |
|  |     aether-net      |   |     aether-dom      |   |   aether-js    | |
|  | - TLS 1.3 (Rustls)  |-->| - HTML5 Parser      |<->| - QuickJS ES23 | |
|  | - Mozilla NSS Roots |   | - CSS3 Cascade/Flex |   | - Web APIs     | |
|  | - HTTP/1.1 & HTTP/2 |   | - Render Tree       |   | - DOM Bindings | |
|  +---------------------+   +---------------------+   +----------------+ |
|                                       |                                 |
|                                       v                                 |
|  +--------------------------------------------------------------------+ |
|  |                     aether-render (GLES 2.0)                       | |
|  | - Display List Builder  - Font Glyph Cache  - 60fps Mali-400 Painter| |
|  +--------------------------------------------------------------------+ |
|                                       |                                 |
|  +--------------------------------------------------------------------+ |
|  |                     aether-tab (Tab Hibernation)                   | |
|  | - Active Tab in RAM (max 70MB)  - Inactive Tabs: Disk Serialized   | |
|  +--------------------------------------------------------------------+ |
+-------------------------------------------------------------------------+
```

### 3.1 Subsystem 1: `aether-net` (Networking & Root CA Store)
- Built-in embedded root CA store containing ~140 trusted Mozilla NSS certificates (`webpki-roots` / bundled PEM).
- Standalone TLS 1.3 / 1.2 client via `rustls` with `ring` crypto accelerated by ARMv7 NEON assembly.
- Custom HTTP/1.1 & HTTP/2 pipeline with Brotli (`brotli-decompressor`), Gzip (`flate2`), and connection keep-alive pooling.
- Disk-backed HTTP cache with ETag and Cache-Control compliance.

### 3.2 Subsystem 2: `aether-dom` & `aether-layout` (Parser & Layout)
- Fast streaming HTML5 tokenizer and DOM tree constructor.
- CSS selector engine supporting class, ID, element, attribute, pseudo-classes (`:hover`, `:active`, `:first-child`), and media queries (`@media (max-width: 480px)`).
- Layout engine implementing CSS Box Model, Flow layout, and **CSS Flexbox** (via `tauri-layout` / `parley` / `yoga` / custom minimal flex engine) tailored to 480px viewport width.

### 3.3 Subsystem 3: `aether-js` (JavaScript Runtime & Web APIs)
- Embedded **QuickJS** engine (MIT license). Memory overhead per runtime: **~2 MB**.
- DOM binding layer exposing:
  - `window`, `document`, `navigator`, `location`, `history`
  - `document.querySelector`, `querySelectorAll`, `getElementById`, `createElement`, etc.
  - `EventTarget` (`addEventListener`, `removeEventListener`, `dispatchEvent`)
  - `fetch()`, `XMLHttpRequest` (connected directly to `aether-net`)
  - `setTimeout`, `setInterval`, `requestAnimationFrame` (synchronized to 60 Hz display refresh)
  - `localStorage`, `sessionStorage`

### 3.4 Subsystem 4: `aether-render` (60 FPS Compositor on Mali-400 MP2)
- Target display: 480 × 854.
- Direct OpenGL ES 2.0 surface rendering via Android NDK `ANativeWindow`.
- Display list decomposition: Rectangles, Text runs, Border strokes, Images, Clips.
- Single-pass batched draw calls with pre-warmed glyph atlas.
- Velocity-based touch gesture scrolling with physics deceleration running on the render thread to ensure 60 FPS even during script processing.

### 3.5 Subsystem 5: `aether-tab` (Unlimited Tabs via Zero-RAM Hibernation)
- Total RAM budget: max 1 active tab alive in full heap (~50-80 MB).
- When a new tab is opened or switched:
  1. The outgoing tab's DOM tree, scroll position, and navigation history are compressed and saved to an on-disk SQLite/binary cache file (`/data/data/.../cache/tab_<id>.bin`).
  2. Outgoing tab's JS runtime and render textures are freed immediately.
  3. Incoming tab state is read from disk and rehydrated in <80 ms.
- Background tabs use **0 MB RAM**, allowing 20+ tabs simultaneously without risk of LMK termination on Lenovo A536.

### 3.6 Subsystem 6: `aether-android` (Host Shell & Claude UI)
- Android App (API 19, `minSdkVersion 19`, `targetSdkVersion 19`).
- Pure, clean aesthetic:
  - Base background: `#FAF7F2` (Warm Claude Parchment).
  - Surface cards / Omnibox: `#FFFFFF` with `#E5E0D8` borders.
  - Text: `#1F1E1D` (High-contrast charcoal).
  - Secondary text / icons: `#6E6A63`.
  - Accent / Focus: `#D97706` (Warm Terracotta / Amber).
  - Zero neon, zero glow, zero unnecessary shadows.

---

## 4. Empirical Verification & Milestones

1. **Phase 1: Crypto & Transport Verification**:
   - Query modern HTTPS servers (e.g. `https://cloudflare.com`, `https://google.com`, `https://wikipedia.org`) from native code using bundled CA store.
   - Verify TLS 1.3 handshake succeeds where Android 4.4 default trust store fails with `SSLHandshakeException`.
2. **Phase 2: HTML/CSS & Modern JS Verification**:
   - Load HTML5 test suites and execute modern ES2023 constructs (`async/await`, `fetch`, Promises, class syntax).
   - Render layout passes for typical modern responsive layouts (Wikipedia, Hacker News, GitHub mobile, DuckDuckGo).
3. **Phase 3: Tab Hibernation & Memory Verification**:
   - Open 10 active tabs sequentially in test harness.
   - Measure RSS memory usage via Linux `/proc/self/status` (`VmRSS` and `VmHWM`).
   - Hard assertion: total process memory stays under 100 MB RSS at all times.
4. **Phase 4: GLES 2.0 Frame Rate & Android 4.4 Host Verification**:
   - Measure frame times during kinetic scroll over 1,000 DOM elements.
   - Assert median frame render time <= 16.6 ms (60 FPS).
   - Package APK and test on Android 4.4 emulator / device via ADB.
