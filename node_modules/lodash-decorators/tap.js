"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var factory_1 = require("./factory");
var applicators_1 = require("./applicators");
var utils_1 = require("./utils");
var decorator = factory_1.DecoratorFactory.createDecorator(new factory_1.DecoratorConfig(function (fn) { return utils_1.returnAtIndex(fn, 0); }, new applicators_1.PreValueApplicator()));
/**
 * Returns the first argument from the function regardless of
 * the decorated functions return value.
 */
function Tap() {
    return decorator();
}
exports.Tap = Tap;
exports.tap = Tap;
exports.default = decorator;
//# sourceMappingURL=tap.js.map