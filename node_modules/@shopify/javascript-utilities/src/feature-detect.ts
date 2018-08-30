/*
https://github.com/WICG/EventListenerOptions/blob/gh-pages/explainer.md#feature-detection
*/
const noop = () => undefined;

let supportsPassive: boolean | null = null;
export function supportsEventListenerPassiveOption() {
  if (supportsPassive !== null) {
    return supportsPassive;
  }

  try {
    const opts = Object.defineProperty({}, 'passive', {
      get() { supportsPassive = true; },
    });
    document.addEventListener('test', noop, opts);
  } catch (error) {
    supportsPassive = false;
  }

  document.removeEventListener('test', noop);
  return supportsPassive;
}

let supportsOnce: boolean | null = null;
export function supportsEventListenerOnceOption() {
  if (supportsOnce !== null) {
    return supportsOnce;
  }

  try {
    const opts = Object.defineProperty({}, 'once', {
      get() { supportsOnce = true; },
    });
    document.addEventListener('test', noop, opts);
  } catch (error) {
    supportsOnce = false;
  }

  document.removeEventListener('test', noop);
  return supportsOnce;
}
