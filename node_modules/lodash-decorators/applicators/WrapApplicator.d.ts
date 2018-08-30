import { Applicator, ApplicateOptions } from './Applicator';
export declare class WrapApplicator extends Applicator {
    apply({args, config: {execute}, target, value}: ApplicateOptions): any;
}
