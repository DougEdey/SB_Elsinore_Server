"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var tslib_1 = require("tslib");
var Applicator_1 = require("./Applicator");
var utils_1 = require("../utils");
var PartialApplicator = (function (_super) {
    tslib_1.__extends(PartialApplicator, _super);
    function PartialApplicator() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    PartialApplicator.prototype.apply = function (_a) {
        var args = _a.args, target = _a.target, execute = _a.config.execute;
        return function () {
            var invokeArgs = [];
            for (var _i = 0; _i < arguments.length; _i++) {
                invokeArgs[_i] = arguments[_i];
            }
            return execute.apply(void 0, [utils_1.resolveFunction(args[0], this, target)].concat(args.slice(1))).apply(this, invokeArgs);
        };
    };
    return PartialApplicator;
}(Applicator_1.Applicator));
exports.PartialApplicator = PartialApplicator;
//# sourceMappingURL=PartialApplicator.js.map