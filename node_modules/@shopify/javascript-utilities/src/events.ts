import {supportsEventListenerPassiveOption, supportsEventListenerOnceOption} from './feature-detect';

export interface EventListenerArgs {
  capture?: boolean,
}

export interface AddEventListenerOptions extends EventListenerArgs {
  passive?: boolean,
  once?: boolean,
}

type AddEventListener = (
  type: string,
  listener: (event: Event) => void,
  options?: AddEventListenerOptions,
) => void;

export function addEventListener(
  target: EventTarget,
  eventName: string,
  handler: (event: Event) => any,
  options: AddEventListenerOptions = {},
) {
  const wrappedHandler = !supportsEventListenerOnceOption() && options.once
    ? once(target, eventName, handler)
    : handler;

  if (supportsEventListenerPassiveOption() || supportsEventListenerOnceOption()) {
    const addListener = (target.addEventListener as AddEventListener);
    return addListener.call(target, eventName, handler, options);
  }

  return target.addEventListener(eventName, wrappedHandler, options.capture);
}

export function removeEventListener(
  target: EventTarget,
  eventName: string,
  handler: (event: Event) => any,
  capture?: boolean,
) {
  return target.removeEventListener(eventName, handler, capture);
}

function once(
  target: EventTarget,
  eventName: string,
  handler: (event: Event) => any,
) {
  return function selfRemovingHandler(event: Event) {
    handler.call(event.currentTarget, event);
    target.removeEventListener(eventName, handler);
  };
}
