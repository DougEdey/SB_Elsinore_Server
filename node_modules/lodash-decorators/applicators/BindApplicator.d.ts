import { Applicator, ApplicateOptions } from './Applicator';
export declare class BindApplicator extends Applicator {
    apply({value, config: {execute}, args, instance, target}: ApplicateOptions): any;
}
