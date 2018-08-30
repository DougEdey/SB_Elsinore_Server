"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var tslib_1 = require("tslib");
var Applicator_1 = require("./Applicator");
var utils_1 = require("../utils");
var WrapApplicator = (function (_super) {
    tslib_1.__extends(WrapApplicator, _super);
    function WrapApplicator() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    WrapApplicator.prototype.apply = function (_a) {
        var args = _a.args, execute = _a.config.execute, target = _a.target, value = _a.value;
        return function () {
            var invokeArgs = [];
            for (var _i = 0; _i < arguments.length; _i++) {
                invokeArgs[_i] = arguments[_i];
            }
            return execute(utils_1.resolveFunction(args[0], this, target), value).apply(this, invokeArgs);
        };
    };
    return WrapApplicator;
}(Applicator_1.Applicator));
exports.WrapApplicator = WrapApplicator;
//# sourceMappingURL=WrapApplicator.js.map