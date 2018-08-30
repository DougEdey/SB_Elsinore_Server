import { LodashMethodDecorator } from './factory';
import { ThrottleOptions } from './shared';
declare const decorator: (...args: any[]) => MethodDecorator & PropertyDecorator;
export declare function ThrottleAll(wait?: number, options?: ThrottleOptions): LodashMethodDecorator;
export { ThrottleAll as throttleAll };
export default decorator;
