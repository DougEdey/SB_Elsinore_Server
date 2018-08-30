declare const decorator: (...args: any[]) => MethodDecorator & PropertyDecorator;
export declare function PartialRight(...partials: any[]): PropertyDecorator;
export { PartialRight as partialRight };
export default decorator;
