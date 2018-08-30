import { Applicator, ApplicateOptions } from './Applicator';
export declare class PreValueApplicator extends Applicator {
    apply({value, config: {execute}, args}: ApplicateOptions): any;
}
