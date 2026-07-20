// NativeAudioBridge — posts to Android JavascriptInterface when available.
window.NativeAudioBridge = window.NativeAudioBridge || {
  postMessage: function (data) {
    if (window.NarrativeDJAndroid && window.NarrativeDJAndroid.postMessage) {
      window.NarrativeDJAndroid.postMessage(String(data));
    }
  },
};