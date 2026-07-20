// Selector Self-Validation (SVD) pipeline — Phase 1-B.
(function () {
  "use strict";

  var cachedReport = null;
  var cachedSelectors = {};

  function dictionary() {
    return window.__NarrativeDJ_SELECTORS__ || {};
  }

  function validateField(fieldName, selectors) {
    for (var i = 0; i < selectors.length; i++) {
      var selector = selectors[i];
      var el = document.querySelector(selector);
      if (el) {
        return {
          field: fieldName,
          selector: selector,
          ok: true,
          index: i,
        };
      }
    }
    return {
      field: fieldName,
      selector: null,
      ok: false,
      index: -1,
    };
  }

  function buildReport(dict) {
    var report = { fields: {}, healthy: true };
    for (var fieldName in dict) {
      if (!Object.prototype.hasOwnProperty.call(dict, fieldName)) continue;
      var result = validateField(fieldName, dict[fieldName] || []);
      report.fields[fieldName] = result;
      if (!result.ok) report.healthy = false;
    }
    return report;
  }

  function refreshCache() {
    var dict = dictionary();
    cachedReport = buildReport(dict);
    cachedSelectors = {};
    for (var fieldName in cachedReport.fields) {
      var entry = cachedReport.fields[fieldName];
      if (entry.ok) cachedSelectors[fieldName] = entry.selector;
    }
    return cachedReport;
  }

  window.NarrativeDJSvd = {
    run: function () {
      return JSON.stringify(refreshCache());
    },

    getReport: function () {
      if (!cachedReport) refreshCache();
      return JSON.stringify(cachedReport);
    },

    resolve: function (fieldName) {
      if (!cachedReport) refreshCache();
      return cachedSelectors[fieldName] || null;
    },

    query: function (fieldName) {
      var selector = this.resolve(fieldName);
      if (!selector) return null;
      return document.querySelector(selector);
    },
  };
})();
