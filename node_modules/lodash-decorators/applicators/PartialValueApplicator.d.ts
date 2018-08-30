import { Applicator, ApplicateOptions } from './Applicator';
export declare class PartialValueApplicator extends Applicator {
    apply({args, target, value, config: {execute}}: ApplicateOptions): any;
}
