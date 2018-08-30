import { ResolvableFunction, LodashMethodDecorator } from './factory';
declare const decorator: (...args: any[]) => MethodDecorator & PropertyDecorator;
export declare function Wrap(fnToWrap?: ResolvableFunction): LodashMethodDecorator;
export { Wrap as wrap };
export default decorator;
