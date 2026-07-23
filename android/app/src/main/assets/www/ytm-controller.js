// YT Music WebView controller — uses SVD-resolved selectors (Phase 1-B).
(function () {
  "use strict";

  var PENDING_SEARCH_KEY = "ndj_pending_search";
  var SEARCH_RESULT_SELECTORS = [
    "ytmusic-responsive-list-item-renderer",
    "ytmusic-card-shelf-renderer ytmusic-responsive-list-item-renderer",
  ];

  function readIsPlaying(playButton) {
    var fixtureFlag = document.getElementById("poc-is-playing");
    if (fixtureFlag) {
      return fixtureFlag.textContent.trim() === "true";
    }
    if (!playButton) return false;
    var label = (playButton.getAttribute("aria-label") || "").toLowerCase();
    if (label.indexOf("pause") !== -1 || label.indexOf("일시정지") !== -1) return true;
    if (label.indexOf("play") !== -1 || label.indexOf("재생") !== -1) return false;
    var title = (playButton.getAttribute("title") || "").toLowerCase();
    return title.indexOf("pause") !== -1 || title.indexOf("일시정지") !== -1;
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

  function isSearchFixturePage() {
    return document.getElementById("poc-search-fixture") !== null;
  }

  function setPendingSearch(query) {
    try {
      sessionStorage.setItem(PENDING_SEARCH_KEY, query);
    } catch (e) {
      window.__NarrativeDJ_PENDING_SEARCH__ = query;
    }
  }

  function getPendingSearch() {
    try {
      var stored = sessionStorage.getItem(PENDING_SEARCH_KEY);
      if (stored) return stored;
    } catch (e) {
      // fall through
    }
    return window.__NarrativeDJ_PENDING_SEARCH__ || null;
  }

  function clearPendingSearch() {
    try {
      sessionStorage.removeItem(PENDING_SEARCH_KEY);
    } catch (e) {
      // fall through
    }
    delete window.__NarrativeDJ_PENDING_SEARCH__;
  }

  function isSearchResultsPage() {
    if (isSearchFixturePage()) return true;
    var href = window.location.href || "";
    return href.indexOf("/search") !== -1 || href.indexOf("search?") !== -1;
  }

  function searchResultsUrl(query) {
    return "https://music.youtube.com/search?q=" + encodeURIComponent(query);
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

  function shelfTitle(shelf) {
    var el =
      shelf.querySelector("#title") ||
      shelf.querySelector("h2") ||
      shelf.querySelector(".title") ||
      shelf.querySelector("[class*='title']");
    return ((el && el.textContent) || "").trim().toLowerCase();
  }

  function isSongsShelf(shelf) {
    var title = shelfTitle(shelf);
    return (
      title.indexOf("song") !== -1 ||
      title.indexOf("노래") !== -1 ||
      title.indexOf("곡") !== -1 ||
      title.indexOf("tracks") !== -1
    );
  }

  function hasWatchLink(item) {
    return !!item.querySelector('a[href*="watch?v="], a[href*="/watch"]');
  }

  function findSongPlayButton(item) {
    var buttons = item.querySelectorAll("button, ytmusic-play-button-renderer button");
    for (var i = 0; i < buttons.length; i++) {
      var label = (buttons[i].getAttribute("aria-label") || "").toLowerCase();
      if (
        label.indexOf("play") !== -1 ||
        label.indexOf("재생") !== -1 ||
        label.indexOf("play song") !== -1
      ) {
        if (label.indexOf("playlist") !== -1 || label.indexOf("재생목록") !== -1) continue;
        return buttons[i];
      }
    }
    var renderer = item.querySelector("ytmusic-play-button-renderer button");
    return renderer || null;
  }

  function findSongResult() {
    var shelves = document.querySelectorAll("ytmusic-shelf-renderer");
    for (var s = 0; s < shelves.length; s++) {
      if (!isSongsShelf(shelves[s])) continue;
      var shelfItem = shelves[s].querySelector("ytmusic-responsive-list-item-renderer");
      if (shelfItem) return shelfItem;
    }

    var items = document.querySelectorAll("ytmusic-responsive-list-item-renderer");
    for (var i = 0; i < items.length; i++) {
      if (hasWatchLink(items[i]) || findSongPlayButton(items[i])) {
        return items[i];
      }
    }
    return null;
  }

  function findFirstSearchResult() {
    var song = findSongResult();
    if (song) return song;

    for (var i = 0; i < SEARCH_RESULT_SELECTORS.length; i++) {
      var el = document.querySelector(SEARCH_RESULT_SELECTORS[i]);
      if (el) return el;
    }
    return null;
  }

  function clickSearchResult(item) {
    var playBtn = findSongPlayButton(item);
    if (playBtn) {
      playBtn.click();
      return "play_button";
    }
    var watchLink = item.querySelector('a[href*="watch?v="], a[href*="/watch"]');
    if (watchLink) {
      watchLink.click();
      return "watch_link";
    }
    // Avoid opening albums/playlists — that path often fails with YTM toast errors.
    return "skipped_non_song";
  }

  function waitForFirstSearchResult(maxMs, intervalMs, done) {
    var elapsed = 0;
    var timer = setInterval(function () {
      var item = findFirstSearchResult();
      if (item) {
        clearInterval(timer);
        done(item);
        return;
      }
      elapsed += intervalMs;
      if (elapsed >= maxMs) {
        clearInterval(timer);
        done(null);
      }
    }, intervalMs);
  }

  function resolvePlayButton() {
    ensureSvd();
    var svd = window.NarrativeDJSvd;
    var svdBtn = svd ? svd.query("playButton") : null;
    if (svdBtn) return svdBtn;
    return document.querySelector(
      'button[aria-label*="Play"], button[aria-label*="Pause"], button[aria-label*="재생"], button[aria-label*="일시정지"]'
    );
  }

  function ensurePlaying() {
    var playBtn = resolvePlayButton();
    if (!playBtn) return JSON.stringify({ ok: false, error: "play_button_not_found" });
    if (readIsPlaying(playBtn)) {
      return JSON.stringify({ ok: true, mode: "already_playing" });
    }
    playBtn.click();
    return JSON.stringify({ ok: true, mode: "ensure_play_clicked" });
  }

  function clickSongsFilterChip() {
    var nodes = document.querySelectorAll(
      "ytmusic-chip-cloud-chip-renderer, yt-chip-cloud-chip-renderer, chip-shape, button, a"
    );
    for (var i = 0; i < nodes.length; i++) {
      var text = ((nodes[i].textContent || "") + " " + (nodes[i].getAttribute("title") || ""))
        .trim()
        .toLowerCase();
      if (
        text === "songs" ||
        text === "노래" ||
        text === "곡" ||
        text.indexOf("songs") === 0 ||
        text.indexOf("노래") === 0
      ) {
        var selected =
          nodes[i].getAttribute("aria-selected") === "true" ||
          nodes[i].getAttribute("aria-pressed") === "true" ||
          (nodes[i].className || "").toString().indexOf("selected") !== -1;
        if (!selected) nodes[i].click();
        return true;
      }
    }
    return false;
  }

  function resumePendingSearch() {
    var query = getPendingSearch();
    if (!query) {
      return JSON.stringify({ ok: false, mode: "resume", error: "no_pending" });
    }
    if (!isSearchResultsPage()) {
      return JSON.stringify({ ok: false, mode: "resume", error: "not_search_page" });
    }

    clickSongsFilterChip();

    waitForFirstSearchResult(14000, 400, function (item) {
      clearPendingSearch();
      if (!item) return;
      var mode = clickSearchResult(item);
      if (mode === "play_button") {
        // Song play already triggered — only nudge if still paused.
        setTimeout(function () {
          ensurePlaying();
        }, 2500);
        return;
      }
      if (mode === "watch_link") {
        setTimeout(function () {
          ensurePlaying();
        }, 3000);
      }
    });

    return JSON.stringify({ ok: true, mode: "resume_started", query: query });
  }

  function liveSearchAndPlay(query) {
    setPendingSearch(query);
    var target = searchResultsUrl(query);
    var current = window.location.href || "";
    if (
      current.split("#")[0] === target ||
      current.indexOf("search?q=" + encodeURIComponent(query)) !== -1
    ) {
      return resumePendingSearch();
    }
    try {
      window.__NarrativeDJ_navigatingSearch__ = true;
    } catch (e) {}
    window.location.href = target;
    return JSON.stringify({ ok: true, query: query, mode: "live_navigate" });
  }

  window.NarrativeDJYtm = {
    isMusicPageLoaded: function () {
      var href = window.location.href || "";
      if (href.indexOf("music.youtube.com") !== -1) return true;
      return isFixturePage() || isSearchFixturePage();
    },

    onPageReady: function () {
      if (getPendingSearch() && isSearchResultsPage()) {
        return resumePendingSearch();
      }
      return JSON.stringify({ ok: true, mode: "idle" });
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
        } else if (preferPlay === undefined || preferPlay === null) {
          var btn = playBtn || document.querySelector("button[aria-label*=Play], button[aria-label*=Pause]");
          if (btn) btn.click();
        }
        return JSON.stringify({ ok: true, mode: "fixture" });
      }
      if (preferPlay === true) return ensurePlaying();
      if (preferPlay === false) {
        var liveBtn = resolvePlayButton();
        if (!liveBtn) return JSON.stringify({ ok: false, error: "play_button_not_found" });
        if (readIsPlaying(liveBtn)) {
          liveBtn.click();
          return JSON.stringify({ ok: true, mode: "paused" });
        }
        return JSON.stringify({ ok: true, mode: "already_paused" });
      }
      var toggleBtn = resolvePlayButton();
      if (!toggleBtn) return JSON.stringify({ ok: false, error: "play_button_not_found" });
      toggleBtn.click();
      return JSON.stringify({ ok: true, mode: "toggled" });
    },

    searchAndPlay: function (query) {
      if (!query) return JSON.stringify({ ok: false, error: "empty_query" });
      if (isFixturePage()) return fixtureSearchAndPlay(query);
      return liveSearchAndPlay(query);
    },
  };
})();
