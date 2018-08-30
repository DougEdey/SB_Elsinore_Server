// tslint:disable-next-line:no-empty
export function noop() {}

export function createUniqueIDFactory(prefix: string) {
  let index = 1;
  return () => `${prefix}${index++}`;
}
