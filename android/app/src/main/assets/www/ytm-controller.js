// YT Music WebView controller — uses SVD-resolved selectors (Phase 1-B).
(function () {
  "use strict";

  function readIsPlaying(playButton) {
    var fixtureFlag = document.getElementById("poc-is-playing");
    if (fixtureFlag) {
      return fixtureFlag.textContent.trim() === "true";
    }
    if (!playButton) return false;
    var label = (playButton.getAttribute("aria-label") || "").toLowerCase();
    if (label.indexOf("pause") !== -1) return true;
    if (label.indexOf("play") !== -1) return false;
    var title = (playButton.getAttribute("title") || "").toLowerCase();
    return title.indexOf("pause") !== -1;
  }

  function ensureSvd() {
    if (window.NarrativeDJSvd && !window.NarrativeDJSvd.getReport()) {
      window.NarrativeDJSvd.run();
    } else if (window.NarrativeDJSvd) {
      window.NarrativeDJSvd.run();
    }
  }

  window.NarrativeDJYtm = {
    isMusicPageLoaded: function () {
      var href = window.location.href || "";
      if (href.indexOf("music.youtube.com") !== -1) return true;
      return document.getElementById("poc-fixture") !== null;
    },

    runSvd: function () {
      ensureSvd();
      return window.NarrativeDJSvd ? window.NarrativeDJSvd.run() : "{}";
    },

    getNowPlaying: function () {
      ensureSvd();
      var svd = window.NarrativeDJSvd;
      var titleEl = svd ? svd.query("title") : null;
      var artistEl = svd ? svd.query("artist") : null;
      var playBtn = svd ? svd.query("playButton") : null;
      var payload = {
        title: titleEl ? titleEl.textContent.trim() : null,
        artist: artistEl ? artistEl.textContent.trim() : null,
        isPlaying: readIsPlaying(playBtn),
        pageUrl: window.location.href || "",
        svdHealthy: svd ? JSON.parse(svd.getReport()).healthy : false,
      };
      return JSON.stringify(payload);
    },
  };
})();
