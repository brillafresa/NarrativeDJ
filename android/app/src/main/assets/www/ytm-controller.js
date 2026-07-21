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

  function isFixturePage() {
    return document.getElementById("poc-fixture") !== null;
  }

  function fixtureSearchAndPlay(query) {
    var titleEl = document.getElementById("poc-track-title");
    var artistEl = document.getElementById("poc-track-artist");
    var playFlag = document.getElementById("poc-is-playing");
    var searchInput = document.getElementById("poc-search-input");
    if (searchInput) searchInput.value = query;
    if (titleEl) titleEl.textContent = query;
    if (artistEl) artistEl.textContent = "NarrativeDJ Fixture";
    if (playFlag) playFlag.textContent = "true";
    var playBtn = document.getElementById("poc-play-button");
    if (playBtn) playBtn.setAttribute("aria-label", "Pause");
    return JSON.stringify({ ok: true, query: query, mode: "fixture" });
  }

  function liveSearchAndPlay(query) {
    var searchBtn = document.querySelector(
      'button[aria-label*="Search"], button[aria-label*="검색"], tp-yt-paper-button[aria-label*="Search"]'
    );
    if (searchBtn) searchBtn.click();

    var input = document.querySelector(
      'input[type="search"], input[placeholder*="Search"], input[placeholder*="검색"]'
    );
    if (!input) {
      return JSON.stringify({ ok: false, error: "search_input_not_found", query: query });
    }
    input.focus();
    input.value = query;
    input.dispatchEvent(new Event("input", { bubbles: true }));

    var result = document.querySelector(
      'ytmusic-responsive-list-item-renderer, ytmusic-two-row-item-renderer, .song-title'
    );
    if (result) result.click();

    setTimeout(function () {
      window.NarrativeDJYtm.playPause(true);
    }, 800);

    return JSON.stringify({ ok: true, query: query, mode: "live" });
  }

  window.NarrativeDJYtm = {
    isMusicPageLoaded: function () {
      var href = window.location.href || "";
      if (href.indexOf("music.youtube.com") !== -1) return true;
      return isFixturePage();
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

    playPause: function (preferPlay) {
      ensureSvd();
      if (isFixturePage()) {
        var playFlag = document.getElementById("poc-is-playing");
        var playBtn = document.getElementById("poc-play-button");
        var playing = playFlag && playFlag.textContent.trim() === "true";
        if (preferPlay && !playing) {
          if (playFlag) playFlag.textContent = "true";
          if (playBtn) playBtn.setAttribute("aria-label", "Pause");
        } else if (!preferPlay && playing) {
          if (playFlag) playFlag.textContent = "false";
          if (playBtn) playBtn.setAttribute("aria-label", "Play");
        } else if (!preferPlay) {
          var btn = playBtn || document.querySelector("button[aria-label*=Play], button[aria-label*=Pause]");
          if (btn) btn.click();
        }
        return JSON.stringify({ ok: true, mode: "fixture" });
      }
      var svd = window.NarrativeDJSvd;
      var playBtnLive = svd ? svd.query("playButton") : null;
      if (playBtnLive) {
        playBtnLive.click();
        return JSON.stringify({ ok: true, mode: "live" });
      }
      var fallback = document.querySelector("button[aria-label*=Play], button[aria-label*=Pause]");
      if (fallback) {
        fallback.click();
        return JSON.stringify({ ok: true, mode: "live_fallback" });
      }
      return JSON.stringify({ ok: false, error: "play_button_not_found" });
    },

    searchAndPlay: function (query) {
      if (!query) return JSON.stringify({ ok: false, error: "empty_query" });
      if (isFixturePage()) return fixtureSearchAndPlay(query);
      return liveSearchAndPlay(query);
    },
  };
})();
