import { LodashDecorator, ResolvableFunction } from './factory';
declare const decorator: (...args: any[]) => LodashDecorator;
export declare function Negate(fn?: ResolvableFunction): LodashDecorator;
export { Negate as negate };
export default decorator;
