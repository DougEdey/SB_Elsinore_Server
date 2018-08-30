import { LodashMethodDecorator } from './factory';
declare const decorator: (...args: any[]) => MethodDecorator & PropertyDecorator;
export declare function Rest(start?: number): LodashMethodDecorator;
export { Rest as rest };
export default decorator;
