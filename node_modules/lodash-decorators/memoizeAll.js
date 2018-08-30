"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var memoize = require("lodash/memoize");
var factory_1 = require("./factory");
var applicators_1 = require("./applicators");
var decorator = factory_1.DecoratorFactory.createDecorator(new factory_1.DecoratorConfig(memoize, new applicators_1.MemoizeApplicator()));
/**
 * Memoizes a function on the prototype instead of the instance. All instances of the class use the same memoize cache.
 * @param {Function} [resolver] Optional resolver
 */
function MemoizeAll(resolver) {
    return decorator(resolver);
}
exports.MemoizeAll = MemoizeAll;
exports.memoizeAll = MemoizeAll;
exports.default = decorator;
//# sourceMappingURL=memoizeAll.js.map