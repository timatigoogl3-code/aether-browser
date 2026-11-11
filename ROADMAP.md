# Aether Browser: Project Roadmap

**Target Device:** Lenovo A536 (MT6582M, 4x Cortex-A7, Mali-400 MP2, 1GB RAM, Android 4.4.2 API 19)  
**Primary Goal:** Modern Web Browser (60-80% Web compatibility) running at 60 FPS, with custom TLS 1.3 cert store, zero-RAM tab hibernation, and Claude minimalist UI.

---

## Roadmap Phases

- [ ] **Phase 1: Project Scaffolding & CI Foundation**
  - [ ] Initialize Cargo workspace (`aether-net`, `aether-dom`, `aether-js`, `aether-layout`, `aether-render`, `aether-tab`, `aether-core`).
  - [ ] Configure cross-compilation target (`armv7-linux-androideabi` API 19 + `x86_64` for host verification).
  - [ ] Set up Android project shell (`android-19`, `minSdkVersion 19`).

- [ ] **Phase 2: Independent TLS 1.3 & Certificate Store (`aether-net`)**
  - [ ] Embed current Mozilla NSS Root CA bundle (`webpki-roots` / custom PEM).
  - [ ] Implement TLS 1.3 client using `rustls` + `ring` with ARMv7 NEON crypto acceleration.
  - [ ] Implement HTTP/1.1 and HTTP/2 pipeline with Brotli and Gzip decompression.
  - [ ] Unit & integration tests against live modern HTTPS endpoints (`cloudflare.com`, `wikipedia.org`).

- [ ] **Phase 3: Ultra-Lean DOM & Modern JavaScript Engine (`aether-js`)**
  - [ ] Integrate QuickJS engine with memory allocation cap (max 10MB per runtime).
  - [ ] Implement DOM Core bindings: `Window`, `Document`, `Element`, `Node`, `EventTarget`.
  - [ ] Implement Web API bindings: `fetch()`, `XMLHttpRequest`, `setTimeout`, `setInterval`, `requestAnimationFrame`.
  - [ ] Verify execution of modern ES2023 constructs (async/await, Promises, Map/Set, arrow functions).

- [ ] **Phase 4: HTML5 Parsing & CSS Flexbox Layout (`aether-dom` / `aether-layout`)**
  - [ ] Streaming HTML5 tokenizer & DOM tree construction.
  - [ ] CSS parser & selector matching (class, ID, tag, pseudo-classes, media queries).
  - [ ] Box model calculation & Flexbox layout engine targeting 480px FWVGA width.
  - [ ] Test layout against reference mobile pages (Wikipedia, HackerNews, Lite search).

- [ ] **Phase 5: 60 FPS GLES 2.0 Renderer & Compositor (`aether-render`)**
  - [ ] Display list builder: rectangles, borders, glyphs, images, clips.
  - [ ] OpenGL ES 2.0 batched shader pipeline optimized for Mali-400 MP2 TBDR architecture.
  - [ ] Font glyph texture atlas with subpixel positioning.
  - [ ] Independent render thread with kinetic touch scrolling (target: <= 16.6 ms per frame).

- [ ] **Phase 6: Multi-Tab Zero-RAM Hibernation Engine (`aether-tab`)**
  - [ ] Active tab memory budget (max 70 MB heap).
  - [ ] Tab hibernation serializer: writes DOM tree, navigation history, and scroll offset to disk.
  - [ ] Tab rehydration deserializer (<80 ms switch time).
  - [ ] Test with 15 simultaneous tabs; verify total process RSS < 100 MB.

- [ ] **Phase 7: Android 4.4 Host Application & Claude Aesthetic UI (`aether-android`)**
  - [ ] Native JNI bridge connecting Android `SurfaceView` and touch events to `libaether_core.so`.
  - [ ] Claude-inspired warm parchment theme (`#FAF7F2`, `#1F1E1D` text, `#D97706` amber accents, zero neon).
  - [ ] Omnibox URL bar, minimal tab switcher, reader mode button.
  - [ ] APK packaging, zipalign, signing.

- [ ] **Phase 8: Empirical Verification on Android 4.4 Environment**
  - [ ] Deploy and launch on Android 4.4 emulator / VM.
  - [ ] Test real-world modern websites: Wikipedia mobile, DuckDuckGo HTML, HackerNews, GitHub Lite.
  - [ ] Verify memory benchmarks and 60 FPS rendering under ADB telemetry (`dumpsys meminfo`, `dumpsys gfxinfo`).
