"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var throttle = require("lodash/throttle");
var factory_1 = require("./factory");
var applicators_1 = require("./applicators");
var decorator = factory_1.DecoratorFactory.createDecorator(new factory_1.DecoratorConfig(throttle, new applicators_1.PreValueApplicator(), { setter: true }));
function ThrottleAll(wait, options) {
    return decorator(wait, options);
}
exports.ThrottleAll = ThrottleAll;
exports.throttleAll = ThrottleAll;
exports.default = decorator;
//# sourceMappingURL=throttleAll.js.map