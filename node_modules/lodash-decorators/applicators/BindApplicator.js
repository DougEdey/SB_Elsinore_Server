"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var tslib_1 = require("tslib");
var Applicator_1 = require("./Applicator");
var BindApplicator = (function (_super) {
    tslib_1.__extends(BindApplicator, _super);
    function BindApplicator() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    BindApplicator.prototype.apply = function (_a) {
        var value = _a.value, execute = _a.config.execute, args = _a.args, instance = _a.instance, target = _a.target;
        if (!instance) {
            return value;
        }
        return execute.apply(void 0, [value, instance].concat(args));
    };
    return BindApplicator;
}(Applicator_1.Applicator));
exports.BindApplicator = BindApplicator;
//# sourceMappingURL=BindApplicator.js.map