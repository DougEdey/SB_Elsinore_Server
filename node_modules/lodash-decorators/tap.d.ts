import { LodashMethodDecorator } from './factory';
declare const decorator: (...args: any[]) => MethodDecorator & PropertyDecorator;
/**
 * Returns the first argument from the function regardless of
 * the decorated functions return value.
 */
export declare function Tap(): LodashMethodDecorator;
export { Tap as tap };
export default decorator;
