import { LodashDecorator } from './factory';
declare const decorator: (...args: any[]) => LodashDecorator;
/**
 * Creates a function that invokes func, with the this binding and arguments of the created function, while it's called less than n times.
 * Subsequent calls to the created function return the result of the last func invocation.
 * @param {number} n The number of calls at whichc func is no longer invoked.
 * @example
 *
 * let calls = 0;
 *
 * class MyClass {
 *   @BeforeAll(3)
 *   fn() {
 *     calls++;
 *   }
 * }
 *
 * const myClass = new MyClass();
 * const myClass2 = new MyClass();
 *
 * myClass.fn();
 * myClass.fn();
 * myClass.fn();
 * myClass.fn();
 *
 * myClass2.fn();
 *
 * calls === 3; // => true
 */
export declare function BeforeAll(n: number): LodashDecorator;
export { BeforeAll as beforeAll };
export default decorator;
