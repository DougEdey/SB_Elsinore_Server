import { LodashDecorator, ResolvableFunction } from './factory';
declare const decorator: (...args: any[]) => LodashDecorator;
export declare function Rearg(indexes: ResolvableFunction | number | number[], ...args: Array<number | number[]>): LodashDecorator;
export { Rearg as rearg };
export default decorator;
