"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var tslib_1 = require("tslib");
var Applicator_1 = require("./Applicator");
var PreValueApplicator = (function (_super) {
    tslib_1.__extends(PreValueApplicator, _super);
    function PreValueApplicator() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    PreValueApplicator.prototype.apply = function (_a) {
        var value = _a.value, execute = _a.config.execute, args = _a.args;
        return execute.apply(void 0, [value].concat(args));
    };
    return PreValueApplicator;
}(Applicator_1.Applicator));
exports.PreValueApplicator = PreValueApplicator;
//# sourceMappingURL=PreValueApplicator.js.map