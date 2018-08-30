import { LodashMethodDecorator } from './factory';
import { MemoizeConfig } from './shared';
declare const decorator: (...args: any[]) => MethodDecorator & PropertyDecorator;
/**
 * Memoizes a function on the prototype instead of the instance. All instances of the class use the same memoize cache.
 * @param {Function} [resolver] Optional resolver
 */
export declare function MemoizeAll(resolver?: Function | MemoizeConfig<any, any>): LodashMethodDecorator;
export { MemoizeAll as memoizeAll };
export default decorator;
