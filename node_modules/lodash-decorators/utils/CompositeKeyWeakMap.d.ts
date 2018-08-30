/**
 * A map for weakly holding nested references.
 * @private
 * @export
 * @class CompositeKeyWeakMap
 * @template T
 */
export declare class CompositeKeyWeakMap<T> {
    private _weakMap;
    set(keys: any[], value: T): void;
    get(keys: any[]): T;
    has(keys: any[]): boolean;
}
