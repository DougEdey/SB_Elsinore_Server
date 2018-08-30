declare const decorator: (...args: any[]) => MethodDecorator & PropertyDecorator;
export declare function Partial(...partials: any[]): PropertyDecorator;
export { Partial as partial };
export default decorator;
