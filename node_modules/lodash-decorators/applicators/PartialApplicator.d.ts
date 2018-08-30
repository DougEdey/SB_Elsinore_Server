import { Applicator, ApplicateOptions } from './Applicator';
export declare class PartialApplicator extends Applicator {
    apply({args, target, config: {execute}}: ApplicateOptions): any;
}
