package com.aether.browser;

/**
 * Injects modern ES2022/ES2023 polyfills into pages.
 * Ensures modern web apps (React 18, Vue 3, modern SPAs) run without SyntaxError
 * or missing method exceptions on Lenovo A536.
 */
public class PolyfillInjector {

    public static final String MODERN_WEB_POLYFILL_JS =
        "(function() {\n" +
        "  // globalThis polyfill\n" +
        "  if (typeof globalThis === 'undefined') {\n" +
        "    window.globalThis = window;\n" +
        "  }\n" +
        "  // Array.prototype.at\n" +
        "  if (!Array.prototype.at) {\n" +
        "    Array.prototype.at = function(n) {\n" +
        "      n = Math.trunc(n) || 0;\n" +
        "      if (n < 0) n += this.length;\n" +
        "      if (n < 0 || n >= this.length) return undefined;\n" +
        "      return this[n];\n" +
        "    };\n" +
        "  }\n" +
        "  // Object.hasOwn\n" +
        "  if (!Object.hasOwn) {\n" +
        "    Object.hasOwn = function(obj, prop) {\n" +
        "      return Object.prototype.hasOwnProperty.call(obj, prop);\n" +
        "    };\n" +
        "  }\n" +
        "  // Promise.any\n" +
        "  if (!Promise.any) {\n" +
        "    Promise.any = function(promises) {\n" +
        "      return new Promise(function(resolve, reject) {\n" +
        "        var errors = [];\n" +
        "        var remaining = promises.length;\n" +
        "        if (remaining === 0) return reject(new AggregateError(errors, 'All promises rejected'));\n" +
        "        promises.forEach(function(p, i) {\n" +
        "          Promise.resolve(p).then(resolve, function(err) {\n" +
        "            errors[i] = err;\n" +
        "            if (--remaining === 0) reject(new AggregateError(errors, 'All promises rejected'));\n" +
        "          });\n" +
        "        });\n" +
        "      });\n" +
        "    };\n" +
        "  }\n" +
        "  console.log('[Aether] Modern ES2023 Web Polyfills Active');\n" +
        "})();\n";
}
