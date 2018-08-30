"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var tslib_1 = require("tslib");
var isFunction = require("lodash/isFunction");
var isObject = require("lodash/isObject");
var Applicator_1 = require("./Applicator");
var utils_1 = require("../utils");
var MemoizeApplicator = (function (_super) {
    tslib_1.__extends(MemoizeApplicator, _super);
    function MemoizeApplicator() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    MemoizeApplicator.prototype.apply = function (_a) {
        var value = _a.value, instance = _a.instance, execute = _a.config.execute, args = _a.args, target = _a.target;
        var resolver = utils_1.resolveFunction(isFunction(args[0]) ? args[0] : isObject(args[0]) ? args[0].resolver : args[0], instance, target, false);
        if (resolver && instance) {
            resolver = resolver.bind(instance);
        }
        var memoized = resolver ? execute(value, resolver) : execute(value);
        if (isObject(args[0])) {
            var _b = args[0], cache = _b.cache, type = _b.type;
            if (cache) {
                memoized.cache = cache;
            }
            else if (isFunction(type)) {
                memoized.cache = new type();
            }
        }
        return memoized;
    };
    return MemoizeApplicator;
}(Applicator_1.Applicator));
exports.MemoizeApplicator = MemoizeApplicator;
//# sourceMappingURL=MemoizeApplicator.js.map