"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var tslib_1 = require("tslib");
var Applicator_1 = require("./Applicator");
var PostValueApplicator = (function (_super) {
    tslib_1.__extends(PostValueApplicator, _super);
    function PostValueApplicator() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    PostValueApplicator.prototype.apply = function (_a) {
        var _b = _a.config, execute = _b.execute, bound = _b.bound, args = _a.args, value = _a.value;
        return execute.apply(void 0, args.concat([value]));
    };
    return PostValueApplicator;
}(Applicator_1.Applicator));
exports.PostValueApplicator = PostValueApplicator;
//# sourceMappingURL=PostValueApplicator.js.map