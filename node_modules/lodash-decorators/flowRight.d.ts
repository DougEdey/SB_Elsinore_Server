import { LodashDecorator, ResolvableFunction } from './factory';
declare const decorator: (...args: any[]) => LodashDecorator;
/**
 * Creates a function that returns the result of invoking the given functions with the this binding of the created function,
 * where each successive invocation is supplied the return value of the previous.
 *
 * @example
 *
 * class MyClass {
 *   name = 'Ted';
 *
 *   @FlowRight(toUpperCase, 'getName')
 *   getUpperCaseName: () => string;
 *
 *   getName() {
 *     return this.name;
 *   }
 * }
 *
 * const myClass = new MyClass();
 *
 * myClass.getUpperCaseName(); // => 'TED'
 */
export declare function FlowRight(...fns: ResolvableFunction[]): LodashDecorator;
export { FlowRight as flowRight };
export default decorator;
