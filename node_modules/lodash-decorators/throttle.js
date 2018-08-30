"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var throttle = require("lodash/throttle");
var factory_1 = require("./factory");
var applicators_1 = require("./applicators");
var decorator = factory_1.DecoratorFactory.createInstanceDecorator(new factory_1.DecoratorConfig(throttle, new applicators_1.PreValueApplicator(), { setter: true, getter: true }));
var decoratorGetter = factory_1.DecoratorFactory.createInstanceDecorator(new factory_1.DecoratorConfig(throttle, new applicators_1.PreValueApplicator(), { getter: true }));
var decoratorSetter = factory_1.DecoratorFactory.createInstanceDecorator(new factory_1.DecoratorConfig(throttle, new applicators_1.PreValueApplicator(), { setter: true }));
function Throttle(wait, options) {
    return decorator(wait, options);
}
exports.Throttle = Throttle;
exports.throttle = Throttle;
function ThrottleGetter(wait, options) {
    return decoratorGetter(wait, options);
}
exports.ThrottleGetter = ThrottleGetter;
exports.throttleGetter = ThrottleGetter;
function ThrottleSetter(wait, options) {
    return decoratorSetter(wait, options);
}
exports.ThrottleSetter = ThrottleSetter;
exports.throttleSetter = ThrottleSetter;
exports.default = decorator;
//# sourceMappingURL=throttle.js.map