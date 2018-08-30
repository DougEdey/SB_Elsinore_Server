import { LodashMethodDecorator } from './factory';
declare const decorator: (...args: any[]) => MethodDecorator & PropertyDecorator;
export declare function OnceAll(): LodashMethodDecorator;
export { OnceAll as onceAll };
export default decorator;
