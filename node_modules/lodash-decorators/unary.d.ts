import { LodashMethodDecorator } from './factory';
declare const decorator: (...args: any[]) => MethodDecorator & PropertyDecorator;
export declare function Unary(): LodashMethodDecorator;
export { Unary as unary };
export default decorator;
