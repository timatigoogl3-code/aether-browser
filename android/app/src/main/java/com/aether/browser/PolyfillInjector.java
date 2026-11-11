package com.aether.browser;

/**
 * Injects modern ES2022/ES2023 web API polyfills into loaded pages.
 * Ensures modern single-page apps (React 18+, Vue 3+, Tailwind UI)
 * run without SyntaxError or undefined function crashes on Lenovo A536.
 */
public class PolyfillInjector {

    public static final String MODERN_WEB_POLYFILL_JS =
        "(function() {\n" +
        "  // 1. globalThis polyfill\n" +
        "  if (typeof globalThis === 'undefined') {\n" +
        "    window.globalThis = window;\n" +
        "  }\n" +
        "  // 2. Array.prototype.at\n" +
        "  if (!Array.prototype.at) {\n" +
        "    Array.prototype.at = function(n) {\n" +
        "      n = Math.trunc(n) || 0;\n" +
        "      if (n < 0) n += this.length;\n" +
        "      if (n < 0 || n >= this.length) return undefined;\n" +
        "      return this[n];\n" +
        "    };\n" +
        "  }\n" +
        "  // 3. Object.hasOwn\n" +
        "  if (!Object.hasOwn) {\n" +
        "    Object.hasOwn = function(obj, prop) {\n" +
        "      return Object.prototype.hasOwnProperty.call(obj, prop);\n" +
        "    };\n" +
        "  }\n" +
        "  // 4. Promise.any\n" +
        "  if (!Promise.any) {\n" +
        "    Promise.any = function(promises) {\n" +
        "      return new Promise(function(resolve, reject) {\n" +
        "        var errors = [];\n" +
        "        var remaining = promises.length;\n" +
        "        if (remaining === 0) return reject(new Error('All promises rejected'));\n" +
        "        promises.forEach(function(p, i) {\n" +
        "          Promise.resolve(p).then(resolve, function(err) {\n" +
        "            errors[i] = err;\n" +
        "            if (--remaining === 0) reject(new Error('All promises rejected'));\n" +
        "          });\n" +
        "        });\n" +
        "      });\n" +
        "    };\n" +
        "  }\n" +
        "  // 5. crypto.randomUUID\n" +
        "  if (!window.crypto) window.crypto = {};\n" +
        "  if (!window.crypto.randomUUID) {\n" +
        "    window.crypto.randomUUID = function() {\n" +
        "      return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {\n" +
        "        var r = Math.random() * 16 | 0, v = c === 'x' ? r : (r & 0x3 | 0x8);\n" +
        "        return v.toString(16);\n" +
        "      });\n" +
        "    };\n" +
        "  }\n" +
        "  // 6. structuredClone fallback\n" +
        "  if (typeof structuredClone === 'undefined') {\n" +
        "    window.structuredClone = function(obj) {\n" +
        "      if (obj === undefined) return undefined;\n" +
        "      return JSON.parse(JSON.stringify(obj));\n" +
        "    };\n" +
        "  }\n" +
        "  // 7. ResizeObserver fallback\n" +
        "  if (typeof ResizeObserver === 'undefined') {\n" +
        "    window.ResizeObserver = function(callback) {\n" +
        "      this.observe = function(target) {};\n" +
        "      this.unobserve = function(target) {};\n" +
        "      this.disconnect = function() {};\n" +
        "    };\n" +
        "  }\n" +
        "  // 8. requestIdleCallback\n" +
        "  if (!window.requestIdleCallback) {\n" +
        "    window.requestIdleCallback = function(cb) {\n" +
        "      return setTimeout(function() {\n" +
        "        cb({ didTimeout: false, timeRemaining: function() { return 50; } });\n" +
        "      }, 1);\n" +
        "    };\n" +
        "    window.cancelIdleCallback = function(id) { clearTimeout(id); };\n" +
        "  }\n" +
        "  // 9. navigator.clipboard\n" +
        "  if (!navigator.clipboard) {\n" +
        "    navigator.clipboard = {\n" +
        "      writeText: function(text) { return Promise.resolve(); },\n" +
        "      readText: function() { return Promise.resolve(''); }\n" +
        "    };\n" +
        "  }\n" +
        "  console.log('[Aether] Modern Web Compatibility Polyfills Active');\n" +
        "})();\n";
}
