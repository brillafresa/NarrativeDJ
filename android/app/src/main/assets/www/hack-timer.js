// HackTimer pattern — Web Worker backed timers resist background throttling (Phase 2).
// Native setTimeout shim is always installed; Worker upgrade is best-effort.
(function () {
  "use strict";

  if (window.__NarrativeDJHackTimerInstalled) return;
  window.__NarrativeDJHackTimerInstalled = true;

  function installNativeFallback() {
    window.NarrativeDJHackTimer = {
      setTimeout: function (fn, ms) {
        return setTimeout(fn, ms || 0);
      },
      isInstalled: function () {
        return true;
      },
    };
  }

  installNativeFallback();

  try {
    var callbacks = {};
    var workerSource = [
      "onmessage = function (event) {",
      "  var msg = event.data;",
      "  if (msg.type === 'setTimeout') {",
      "    setTimeout(function () { postMessage({ type: 'run', token: msg.token }); }, msg.ms);",
      "  }",
      "};",
    ].join("\n");
    var worker = new Worker(
      URL.createObjectURL(new Blob([workerSource], { type: "application/javascript" })),
    );
    worker.onmessage = function (event) {
      var msg = event.data;
      if (msg.type === "run") {
        var fn = callbacks[msg.token];
        delete callbacks[msg.token];
        if (fn) fn();
      }
    };
    window.NarrativeDJHackTimer = {
      setTimeout: function (fn, ms) {
        var token = "t_" + String(Date.now()) + "_" + String(Math.random());
        callbacks[token] = fn;
        worker.postMessage({ type: "setTimeout", ms: ms || 0, token: token });
        return token;
      },
      isInstalled: function () {
        return true;
      },
    };
  } catch (_err) {
    // keep native fallback installed above
  }
})();
