import { Applicator } from '../applicators';
import { CompositeKeyWeakMap } from '../utils';
export declare type ApplicatorToken = {
    new (): Applicator;
};
export declare type LodashMethodDecorator = MethodDecorator;
export declare type LodashDecorator = MethodDecorator & PropertyDecorator;
export declare type ResolvableFunction = string | Function;
export interface InstanceChainContext {
    getter?: boolean;
    setter?: boolean;
    method?: boolean;
    property?: boolean;
    value: any;
}
export interface InstanceChainData {
    properties: string[];
    fns: Function[];
    isGetter: boolean;
    isSetter: boolean;
    isMethod: boolean;
    isProperty: boolean;
}
export declare const InstanceChainMap: CompositeKeyWeakMap<InstanceChainData>;
