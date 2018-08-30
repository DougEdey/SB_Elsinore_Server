"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var tslib_1 = require("tslib");
var isFunction = require("lodash/isFunction");
var Applicator_1 = require("./Applicator");
var utils_1 = require("../utils");
var PartialValueApplicator = (function (_super) {
    tslib_1.__extends(PartialValueApplicator, _super);
    function PartialValueApplicator() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    PartialValueApplicator.prototype.apply = function (_a) {
        var args = _a.args, target = _a.target, value = _a.value, execute = _a.config.execute;
        return function () {
            var invokeArgs = [];
            for (var _i = 0; _i < arguments.length; _i++) {
                invokeArgs[_i] = arguments[_i];
            }
            var fn = value;
            var argIndex = 0;
            if (!isFunction(fn)) {
                fn = utils_1.resolveFunction(args[0], this, target);
                argIndex = 1;
            }
            return execute.apply(void 0, [fn].concat(args.slice(argIndex))).apply(this, invokeArgs);
        };
    };
    return PartialValueApplicator;
}(Applicator_1.Applicator));
exports.PartialValueApplicator = PartialValueApplicator;
//# sourceMappingURL=PartialValueApplicator.js.map