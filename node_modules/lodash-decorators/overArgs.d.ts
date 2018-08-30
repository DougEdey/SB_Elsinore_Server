import { LodashMethodDecorator } from './factory';
declare const decorator: (...args: any[]) => MethodDecorator & PropertyDecorator;
export declare function OverArgs(...transforms: Function[]): LodashMethodDecorator;
export { OverArgs as overArgs };
export default decorator;
