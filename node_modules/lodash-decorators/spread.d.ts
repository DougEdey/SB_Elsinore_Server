import { LodashMethodDecorator } from './factory';
declare const decorator: (...args: any[]) => MethodDecorator & PropertyDecorator;
export declare function Spread(start?: number): LodashMethodDecorator;
export { Spread as spread };
export default decorator;
