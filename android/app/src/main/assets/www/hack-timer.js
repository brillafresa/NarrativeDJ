// HackTimer pattern — Web Worker backed timers resist background throttling (Phase 2).
(function () {
  "use strict";

  if (window.__NarrativeDJHackTimerInstalled) return;
  window.__NarrativeDJHackTimerInstalled = true;

  var workerSource = [
    "var handles = {}; var nextId = 0;",
    "onmessage = function (event) {",
    "  var msg = event.data;",
    "  if (msg.type === 'setTimeout') {",
    "    nextId += 1;",
    "    var id = nextId;",
    "    handles[id] = setTimeout(function () {",
    "      postMessage({ type: 'run', id: id, token: msg.token });",
    "      delete handles[id];",
    "    }, msg.ms);",
    "  }",
    "};",
  ].join("\n");

  var worker = new Worker(
    URL.createObjectURL(new Blob([workerSource], { type: "application/javascript" })),
  );
  var callbacks = {};

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
})();
