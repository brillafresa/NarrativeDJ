// Web Audio API ducking stub (Phase 1-D).
// See docs/research.md section 3.3 for full GainNode pipeline.
(function () {
  "use strict";
  window.NarrativeDJ = window.NarrativeDJ || {};
  window.NarrativeDJ.duck = function (targetVolume, rampSeconds) {
    console.log("[audio-ducking stub] duck to", targetVolume, "in", rampSeconds, "s");
  };
  window.NarrativeDJ.restore = function (rampSeconds) {
    console.log("[audio-ducking stub] restore in", rampSeconds, "s");
  };
})();
