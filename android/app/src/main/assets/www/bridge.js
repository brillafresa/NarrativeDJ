// NativeAudioBridge stub — injected into WebView after CSP bypass (Phase 1-C).
window.NativeAudioBridge = window.NativeAudioBridge || {
  postMessage: function (data) {
    console.log("[NativeAudioBridge stub]", data);
  },
};
