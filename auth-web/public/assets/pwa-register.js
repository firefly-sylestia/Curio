if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js', { scope: '/' }).catch(error => {
      console.warn('[Curio] Offline support could not be enabled.', error);
    });
  });
}
