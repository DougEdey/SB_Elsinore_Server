"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var tslib_1 = require("tslib");
var Applicator_1 = require("./Applicator");
var InvokeApplicator = (function (_super) {
    tslib_1.__extends(InvokeApplicator, _super);
    function InvokeApplicator() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    InvokeApplicator.prototype.apply = function (_a) {
        var args = _a.args, target = _a.target, execute = _a.config.execute, value = _a.value;
        return function () {
            var invokeArgs = [];
            for (var _i = 0; _i < arguments.length; _i++) {
                invokeArgs[_i] = arguments[_i];
            }
            return execute.apply(void 0, [value.bind(this)].concat(invokeArgs, args));
        };
    };
    return InvokeApplicator;
}(Applicator_1.Applicator));
exports.InvokeApplicator = InvokeApplicator;
//# sourceMappingURL=InvokeApplicator.js.map