import { LodashMethodDecorator } from './factory';
declare const decorator: (...args: any[]) => MethodDecorator & PropertyDecorator;
export declare function Once(): LodashMethodDecorator;
export { Once as once };
export default decorator;
