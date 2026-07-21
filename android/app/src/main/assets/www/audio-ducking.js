// Web Audio API ducking pipeline — Phase 2 low-latency polish.
(function () {
  "use strict";

  var audioContext = null;
  var musicGain = null;
  var speechGain = null;
  var mediaElement = null;
  var wired = false;
  var defaultMusicVolume = 1.0;
  var duckDepth = 0.18;
  var rampInSec = 0.25;
  var rampOutSec = 0.45;

  function findMediaElement() {
    return document.querySelector("video") || document.querySelector("audio");
  }

  function notifyBridge(event, detail) {
    if (window.NativeAudioBridge && window.NativeAudioBridge.postMessage) {
      window.NativeAudioBridge.postMessage(JSON.stringify({ event: event, detail: detail || {} }));
    }
  }

  function ensureGraph() {
    if (wired) return true;
    mediaElement = findMediaElement();
    if (!mediaElement) return false;

    var Ctx = window.AudioContext || window.webkitAudioContext;
    if (!Ctx) return false;

    audioContext = audioContext || new Ctx();
    musicGain = audioContext.createGain();
    speechGain = audioContext.createGain();
    speechGain.gain.value = 1.0;

    try {
      var source = audioContext.createMediaElementSource(mediaElement);
      source.connect(musicGain);
      musicGain.connect(audioContext.destination);
      speechGain.connect(audioContext.destination);
      musicGain.gain.value = defaultMusicVolume;
      wired = true;
      notifyBridge("ducking_graph_ready", {});
      return true;
    } catch (err) {
      console.warn("[NarrativeDJ ducking] graph wiring failed", err);
      return false;
    }
  }

  function rampGain(gainNode, target, rampSeconds, curve) {
    if (!audioContext || !gainNode) return;
    var now = audioContext.currentTime;
    var duration = Math.max(0.05, rampSeconds || 0);
    gainNode.gain.cancelScheduledValues(now);
    gainNode.gain.setValueAtTime(gainNode.gain.value, now);
    if (curve === "exp") {
      gainNode.gain.exponentialRampToValueAtTime(Math.max(target, 0.0001), now + duration);
    } else {
      gainNode.gain.linearRampToValueAtTime(target, now + duration);
    }
  }

  window.NarrativeDJ = window.NarrativeDJ || {};

  window.NarrativeDJ.configureDucking = function (opts) {
    opts = opts || {};
    if (typeof opts.defaultVolume === "number") defaultMusicVolume = opts.defaultVolume;
    if (typeof opts.duckDepth === "number") duckDepth = opts.duckDepth;
    if (typeof opts.rampInSec === "number") rampInSec = opts.rampInSec;
    if (typeof opts.rampOutSec === "number") rampOutSec = opts.rampOutSec;
  };

  window.NarrativeDJ.duck = function (targetVolume, rampSeconds) {
    if (!ensureGraph()) {
      notifyBridge("ducking_stub", { targetVolume: targetVolume });
      return false;
    }
    if (audioContext.state === "suspended") audioContext.resume();
    var target = typeof targetVolume === "number" ? targetVolume : duckDepth;
    var ramp = typeof rampSeconds === "number" ? rampSeconds : rampInSec;
    rampGain(musicGain, target, ramp, "exp");
    notifyBridge("ducking_in", { target: target, ramp: ramp });
    return true;
  };

  window.NarrativeDJ.restore = function (rampSeconds) {
    if (!ensureGraph()) {
      notifyBridge("ducking_restore_stub", {});
      return false;
    }
    var ramp = typeof rampSeconds === "number" ? rampSeconds : rampOutSec;
    rampGain(musicGain, defaultMusicVolume, ramp, "exp");
    notifyBridge("ducking_out", { ramp: ramp });
    return true;
  };

  window.NarrativeDJ.duckForSpeech = function (durationMs, targetVolume) {
    if (!ensureGraph()) return false;
    window.NarrativeDJ.duck(targetVolume, rampInSec);
    var restoreDelay = Math.max(500, durationMs || 2000);
    var restoreFn = function () { window.NarrativeDJ.restore(rampOutSec); };
    if (window.NarrativeDJHackTimer && window.NarrativeDJHackTimer.setTimeout) {
      window.NarrativeDJHackTimer.setTimeout(restoreFn, restoreDelay);
    } else {
      setTimeout(restoreFn, restoreDelay);
    }
    return true;
  };

  window.NarrativeDJ.playSpeechBuffer = function (arrayBuffer, rampIn, rampOut) {
    if (!ensureGraph()) return Promise.resolve(false);
    if (audioContext.state === "suspended") audioContext.resume();

    window.NarrativeDJ.duck(duckDepth, rampIn || rampInSec);
    return audioContext.decodeAudioData(arrayBuffer.slice(0))
      .then(function (buffer) {
        var source = audioContext.createBufferSource();
        source.buffer = buffer;
        source.connect(speechGain);
        source.onended = function () {
          window.NarrativeDJ.restore(rampOut || rampOutSec);
          notifyBridge("speech_ended", { duration: buffer.duration });
        };
        source.start(0);
        notifyBridge("speech_started", { duration: buffer.duration });
        return true;
      })
      .catch(function (err) {
        console.warn("[NarrativeDJ ducking] decode failed", err);
        window.NarrativeDJ.restore(rampOut || rampOutSec);
        return false;
      });
  };

  window.NarrativeDJ.playSpeechBufferFromBase64 = function (base64, rampIn, rampOut) {
    if (!base64) return Promise.resolve(false);
    var binary = atob(base64);
    var bytes = new Uint8Array(binary.length);
    for (var i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
    return window.NarrativeDJ.playSpeechBuffer(bytes.buffer, rampIn, rampOut);
  };

  window.NarrativeDJ.isGraphReady = function () {
    return wired;
  };
})();
