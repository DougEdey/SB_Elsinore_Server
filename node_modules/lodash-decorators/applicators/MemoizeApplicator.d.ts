import { Applicator, ApplicateOptions } from './Applicator';
export declare class MemoizeApplicator extends Applicator {
    apply({value, instance, config: {execute}, args, target}: ApplicateOptions): any;
}
