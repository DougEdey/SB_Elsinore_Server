import { Applicator } from '../applicators';
export interface DecoratorConfigOptions {
    bound?: boolean;
    setter?: boolean;
    getter?: boolean;
    property?: boolean;
    method?: boolean;
}
export declare class DecoratorConfig {
    readonly execute: Function;
    readonly applicator: Applicator;
    readonly options: DecoratorConfigOptions;
    constructor(execute: Function, applicator: Applicator, options?: DecoratorConfigOptions);
    readonly bound: boolean;
    readonly setter: boolean;
    readonly getter: boolean;
    readonly property: boolean;
    readonly method: boolean;
}
