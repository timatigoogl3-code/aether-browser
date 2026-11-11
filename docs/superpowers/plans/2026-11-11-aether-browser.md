# Aether Browser Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an ultra-optimized modern web browser core for Android 4.4 KitKat (Lenovo A536, 1GB RAM) with an independent TLS 1.3 cert store, QuickJS modern ECMAScript engine, 60fps GLES 2.0 rendering, and zero-RAM tab hibernation.

**Architecture:** A Rust-powered micro-engine divided into modular crates (`aether-net`, `aether-dom`, `aether-js`, `aether-layout`, `aether-render`, `aether-tab`, `aether-core`), surfaced to an Android 4.4 host application via JNI and an ANativeWindow Surface. Active tab heap is strictly bounded, while background tabs are serialized to disk to preserve memory on 1GB RAM devices.

**Tech Stack:** Rust (2021 edition), `rustls` + `webpki-roots` (custom Mozilla CA store), `flate2`/`brotli-decompressor`, QuickJS (ES2023), OpenGL ES 2.0 (`gl`), Android NDK (API 19, `armeabi-v7a`), Android SDK build-tools.

**Spec:** `docs/superpowers/specs/2026-11-11-aether-browser-design.md`

## Global Constraints

- **Platform Target:** Android 4.4.2 KitKat (API level 19), 32-bit `armeabi-v7a` with NEON (Lenovo A536).
- **RAM Footprint:** Active page heap <= 70 MB. Total browser process RSS <= 100 MB. Background tabs consume 0 MB RAM.
- **TLS/Crypto:** Must NOT use Android 4.4 system SSL/CA certificates. Must bundle modern Mozilla NSS Root CA store and use TLS 1.3 / 1.2 with modern AEAD ciphers.
- **Rendering:** Must target OpenGL ES 2.0 (Mali-400 MP2 compliant, no ES 3.0, no Vulkan). Viewport 480x854 FWVGA. Target 60 FPS.
- **Design:** Clean, minimalist Claude aesthetic (warm parchment `#FAF7F2`, charcoal text `#1F1E1D`, amber accents `#D97706`). No neon.
- **Licensing:** Only open-source code with commercial-friendly licenses (MIT, Apache 2.0, BSD-3-Clause, MPL-2.0).

## Review Focus

- Memory leak on rapid tab switching: inactive tabs must drop all in-memory textures and DOM nodes.
- TLS 1.3 handshake with modern SNI on servers that drop obsolete TLS 1.0/1.1 connections.
- Out-of-memory prevention when parsing large (10MB+) scripts or DOM trees: streaming parser and memory quotas in QuickJS.
- GLES 2.0 shader compatibility on Mali-400 MP2 (strict adherence to GLES 2.0 GLSL specifications).
- Touch input latency and velocity scrolling staying within 16.6 ms per frame during active layout/rendering.

---

### Task 1: Cargo Workspace & Project Scaffolding

**Files:**
- Create: `Cargo.toml`
- Create: `crates/aether-net/Cargo.toml`, `crates/aether-net/src/lib.rs`
- Create: `crates/aether-dom/Cargo.toml`, `crates/aether-dom/src/lib.rs`
- Create: `crates/aether-js/Cargo.toml`, `crates/aether-js/src/lib.rs`
- Create: `crates/aether-layout/Cargo.toml`, `crates/aether-layout/src/lib.rs`
- Create: `crates/aether-render/Cargo.toml`, `crates/aether-render/src/lib.rs`
- Create: `crates/aether-tab/Cargo.toml`, `crates/aether-tab/src/lib.rs`
- Create: `crates/aether-core/Cargo.toml`, `crates/aether-core/src/lib.rs`
- Test: `tests/workspace_test.rs`

**Interfaces:**
- Consumes: Standard Rust toolchain (2021 edition)
- Produces: Cargo multi-crate workspace structure compiling cleanly on host and ARMv7

- [ ] **Step 1: Write the failing workspace test**
- [ ] **Step 2: Run test to verify it fails**
- [ ] **Step 3: Implement Cargo.toml workspace and crate skeletons**
- [ ] **Step 4: Run cargo check and verify workspace compiles**
- [ ] **Step 5: Commit scaffolding**

---

### Task 2: Independent TLS 1.3 & Certificate Store Engine (`aether-net`)

**Files:**
- Create: `crates/aether-net/src/cert_store.rs`
- Create: `crates/aether-net/src/tls_client.rs`
- Create: `crates/aether-net/src/http_client.rs`
- Modify: `crates/aether-net/src/lib.rs`
- Test: `crates/aether-net/tests/tls_test.rs`

**Interfaces:**
- Consumes: Mozilla NSS Root CA certificates via `webpki-roots`
- Produces: `HttpClient::get(url: &str) -> Result<HttpResponse, NetError>` with TLS 1.3 and gzip/brotli decompression

- [ ] **Step 1: Write failing integration test for TLS 1.3 connection**
- [ ] **Step 2: Run test to verify it fails**
- [ ] **Step 3: Implement `cert_store.rs` and `tls_client.rs` using `rustls` with embedded roots**
- [ ] **Step 4: Implement HTTP/1.1 client with header parsing and decompression**
- [ ] **Step 5: Run test and verify successful HTTPS connection with modern TLS 1.3**
- [ ] **Step 6: Commit TLS & network engine**

---

### Task 3: HTML5 Tokenizer & DOM Construction (`aether-dom`)

**Files:**
- Create: `crates/aether-dom/src/node.rs`
- Create: `crates/aether-dom/src/tokenizer.rs`
- Create: `crates/aether-dom/src/parser.rs`
- Modify: `crates/aether-dom/src/lib.rs`
- Test: `crates/aether-dom/tests/dom_parser_test.rs`

**Interfaces:**
- Consumes: Raw HTML strings / byte streams from `aether-net`
- Produces: `Document::parse(html: &str) -> Document` with DOM tree nodes (`Element`, `Text`, `Comment`)

- [ ] **Step 1: Write failing unit test for HTML parser with nested tags and attributes**
- [ ] **Step 2: Run test to verify it fails**
- [ ] **Step 3: Implement streaming HTML tokenizer and DOM tree builder**
- [ ] **Step 4: Run test to verify correct DOM tree representation**
- [ ] **Step 5: Commit DOM parser**

---

### Task 4: CSS3 Selector Matching & Cascade Engine (`aether-layout`)

**Files:**
- Create: `crates/aether-layout/src/css/parser.rs`
- Create: `crates/aether-layout/src/css/selector.rs`
- Create: `crates/aether-layout/src/css/style_tree.rs`
- Create: `crates/aether-layout/src/layout/box_model.rs`
- Create: `crates/aether-layout/src/layout/flexbox.rs`
- Modify: `crates/aether-layout/src/lib.rs`
- Test: `crates/aether-layout/tests/layout_test.rs`

**Interfaces:**
- Consumes: `Document` from `aether-dom` and CSS stylesheets
- Produces: `LayoutTree::compute(doc: &Document, viewport_width: f32, viewport_height: f32) -> LayoutTree`

- [ ] **Step 1: Write failing test for CSS selector matching and layout dimensions**
- [ ] **Step 2: Run test to verify it fails**
- [ ] **Step 3: Implement CSS parser, computed styles, and box model calculator**
- [ ] **Step 4: Implement mobile Flexbox layout (row, column, wrap, justify, align)**
- [ ] **Step 5: Run tests and verify calculated layout boxes for 480px FWVGA viewport**
- [ ] **Step 6: Commit layout engine**

---

### Task 5: Ultra-Lean Modern ECMAScript Runtime (`aether-js`)

**Files:**
- Create: `crates/aether-js/src/runtime.rs`
- Create: `crates/aether-js/src/bindings/console.rs`
- Create: `crates/aether-js/src/bindings/document.rs`
- Create: `crates/aether-js/src/bindings/fetch.rs`
- Modify: `crates/aether-js/src/lib.rs`
- Test: `crates/aether-js/tests/js_runtime_test.rs`

**Interfaces:**
- Consumes: `Document` from `aether-dom` and `HttpClient` from `aether-net`
- Produces: `JsEngine::eval(script: &str) -> Result<JsValue, JsError>` with DOM and `fetch` bindings

- [ ] **Step 1: Write failing test verifying ES2023 features (async/await, Promises, fetch)**
- [ ] **Step 2: Run test to verify it fails**
- [ ] **Step 3: Implement lightweight QuickJS wrapper with 10MB memory limit**
- [ ] **Step 4: Bind DOM API (`querySelector`, `getElementById`, `addEventListener`)**
- [ ] **Step 5: Run tests and verify script execution within 5MB heap usage**
- [ ] **Step 6: Commit JS engine**

---

### Task 6: Zero-RAM Multi-Tab Hibernation Engine (`aether-tab`)

**Files:**
- Create: `crates/aether-tab/src/tab.rs`
- Create: `crates/aether-tab/src/serializer.rs`
- Create: `crates/aether-tab/src/manager.rs`
- Modify: `crates/aether-tab/src/lib.rs`
- Test: `crates/aether-tab/tests/tab_hibernation_test.rs`

**Interfaces:**
- Consumes: `TabState` containing URL, title, DOM tree, scroll offset, and navigation history
- Produces: `TabManager::new(max_active_tabs: usize, storage_dir: PathBuf)`

- [ ] **Step 1: Write failing test creating 10 tabs, asserting inactive tabs consume 0 heap memory**
- [ ] **Step 2: Run test to verify it fails**
- [ ] **Step 3: Implement binary serializer for tab hibernation to disk**
- [ ] **Step 4: Implement instant rehydration when switching active tab**
- [ ] **Step 5: Run test and verify rapid switching under 50ms and constant memory ceiling**
- [ ] **Step 6: Commit tab hibernation engine**

---

### Task 7: 60 FPS GLES 2.0 Display List & Compositor (`aether-render`)

**Files:**
- Create: `crates/aether-render/src/display_list.rs`
- Create: `crates/aether-render/src/gles2/shaders.rs`
- Create: `crates/aether-render/src/gles2/renderer.rs`
- Create: `crates/aether-render/src/glyph_atlas.rs`
- Modify: `crates/aether-render/src/lib.rs`
- Test: `crates/aether-render/tests/display_list_test.rs`

**Interfaces:**
- Consumes: `LayoutTree` from `aether-layout`
- Produces: `GlesRenderer::render(display_list: &DisplayList, scroll_offset_y: f32)`

- [ ] **Step 1: Write failing test for display list generation from layout tree**
- [ ] **Step 2: Run test to verify it fails**
- [ ] **Step 3: Implement display list builder (rects, text runs, images, borders)**
- [ ] **Step 4: Implement GLES 2.0 batch shader renderer compatible with Mali-400 MP2**
- [ ] **Step 5: Run tests and verify rendering command generation**
- [ ] **Step 6: Commit render engine**

---

### Task 8: Unified Core Engine & JNI Bindings (`aether-core`)

**Files:**
- Create: `crates/aether-core/src/browser.rs`
- Create: `crates/aether-core/src/jni_api.rs`
- Modify: `crates/aether-core/src/lib.rs`
- Test: `crates/aether-core/tests/browser_e2e_test.rs`

**Interfaces:**
- Consumes: All crates (`net`, `dom`, `js`, `layout`, `render`, `tab`)
- Produces: `libaether_core.so` with JNI exports for Android SurfaceView and touch events

- [ ] **Step 1: Write end-to-end integration test (Load URL -> Parse -> Layout -> Display List)**
- [ ] **Step 2: Run test to verify it fails**
- [ ] **Step 3: Implement `AetherBrowser` orchestrator**
- [ ] **Step 4: Implement JNI entry points: `init`, `loadUrl`, `onSurfaceCreated`, `onTouch`, `onTabSwitch`**
- [ ] **Step 5: Run integration test and verify full pipeline**
- [ ] **Step 6: Commit unified core**

---

### Task 9: Android 4.4 KitKat Host Shell with Claude Aesthetic (`aether-android`)

**Files:**
- Create: `android/app/build.gradle`
- Create: `android/app/src/main/AndroidManifest.xml`
- Create: `android/app/src/main/java/com/aether/browser/MainActivity.java`
- Create: `android/app/src/main/java/com/aether/browser/BrowserView.java`
- Create: `android/app/src/main/java/com/aether/browser/ClaudeTheme.java`
- Create: `android/app/src/main/res/layout/activity_main.xml`
- Create: `android/app/src/main/res/values/colors.xml`
- Create: `android/app/src/main/res/values/styles.xml`
- Test: `android/gradlew assembleDebug`

**Interfaces:**
- Consumes: `libaether_core.so` compiled for `armeabi-v7a` and `x86`
- Produces: `aether-browser.apk` installable on Android 4.4 KitKat (Lenovo A536)

- [ ] **Step 1: Create minimal Android project targeting API 19 (minSdkVersion 19)**
- [ ] **Step 2: Implement Claude-style warm parchment UI (`#FAF7F2`, `#1F1E1D` text, `#D97706` amber)**
- [ ] **Step 3: Wire `BrowserView` (SurfaceView) to native JNI render loop**
- [ ] **Step 4: Assemble and verify debug APK generation**
- [ ] **Step 5: Commit Android application shell**

---

### Task 10: Empirical Telemetry & Real-World Validation

**Files:**
- Create: `scripts/verify_real_web.sh`
- Create: `scripts/benchmark_memory.sh`
- Create: `tests/web_compat_test.rs`
- Modify: `ROADMAP.md`

**Interfaces:**
- Consumes: Built binary and Android APK
- Produces: Empirical report of real-world URL tests (Wikipedia, HackerNews, DuckDuckGo) with RSS memory logs

- [ ] **Step 1: Write test harness loading 5 real-world modern web pages**
- [ ] **Step 2: Run verification against real HTTPS sites with modern TLS 1.3**
- [ ] **Step 3: Measure peak memory and verify RSS < 100 MB**
- [ ] **Step 4: Record telemetry evidence in project log and update ROADMAP.md**
- [ ] **Step 5: Commit verification suite and push to GitHub**
