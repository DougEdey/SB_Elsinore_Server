/**
 * Wraps a constructor in a wrapper function and copies all static properties
 * and methods to the new constructor.
 * @export
 * @param {Function} Ctor
 * @param {(Ctor: Function, ...args: any[]) => any} wrapper
 * @returns {Function}
 */
export declare function wrapConstructor(Ctor: Function, wrapper: (Ctor: Function, ...args: any[]) => any): Function;
