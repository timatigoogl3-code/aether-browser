// Aether Web Compatibility: ES2022/ES2023 polyfills injected at document_start
(function() {
  'use strict';

  // 1. globalThis polyfill
  if (typeof globalThis === 'undefined') {
    window.globalThis = window;
  }

  // 2. Array.prototype.at
  if (!Array.prototype.at) {
    Array.prototype.at = function(n) {
      n = Math.trunc(n) || 0;
      if (n < 0) n += this.length;
      if (n < 0 || n >= this.length) return undefined;
      return this[n];
    };
  }

  // 3. String.prototype.at
  if (!String.prototype.at) {
    String.prototype.at = function(n) {
      n = Math.trunc(n) || 0;
      if (n < 0) n += this.length;
      if (n < 0 || n >= this.length) return undefined;
      return this.charAt(n);
    };
  }

  // 4. Object.hasOwn
  if (!Object.hasOwn) {
    Object.hasOwn = function(obj, prop) {
      return Object.prototype.hasOwnProperty.call(obj, prop);
    };
  }

  // 5. Promise.any
  if (!Promise.any) {
    Promise.any = function(promises) {
      return new Promise(function(resolve, reject) {
        var errors = [];
        var remaining = promises.length;
        if (remaining === 0) return reject(new Error('All promises were rejected'));
        promises.forEach(function(p, i) {
          Promise.resolve(p).then(resolve, function(err) {
            errors[i] = err;
            if (--remaining === 0) reject(new Error('All promises were rejected: ' + errors.join(', ')));
          });
        });
      });
    };
  }

  // 6. crypto.randomUUID
  if (!window.crypto) window.crypto = {};
  if (!window.crypto.randomUUID) {
    window.crypto.randomUUID = function() {
      return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = Math.random() * 16 | 0, v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
      });
    };
  }

  // 7. structuredClone
  if (typeof structuredClone === 'undefined') {
    window.structuredClone = function(obj) {
      if (obj === undefined) return undefined;
      return JSON.parse(JSON.stringify(obj));
    };
  }

  // 8. ResizeObserver fallback
  if (typeof ResizeObserver === 'undefined') {
    window.ResizeObserver = function(callback) {
      this.observe = function(target) {};
      this.unobserve = function(target) {};
      this.disconnect = function() {};
    };
  }

  // 9. requestIdleCallback
  if (!window.requestIdleCallback) {
    window.requestIdleCallback = function(cb) {
      return setTimeout(function() {
        cb({ didTimeout: false, timeRemaining: function() { return 50; } });
      }, 1);
    };
    window.cancelIdleCallback = function(id) { clearTimeout(id); };
  }

  // 10. navigator.clipboard
  if (!navigator.clipboard) {
    navigator.clipboard = {
      writeText: function(text) { return Promise.resolve(); },
      readText: function() { return Promise.resolve(''); }
    };
  }

  console.log('[Aether] Modern Web Compatibility Polyfills Initialized at document_start');
})();
