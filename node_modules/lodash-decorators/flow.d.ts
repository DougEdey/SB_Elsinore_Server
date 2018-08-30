import { ResolvableFunction, LodashDecorator } from './factory';
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
 *   @Flow('getName', toUpperCase)
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
export declare function Flow(...fns: ResolvableFunction[]): LodashDecorator;
export { Flow as flow };
export default decorator;
