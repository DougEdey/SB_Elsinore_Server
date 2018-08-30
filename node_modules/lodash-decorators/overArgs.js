"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var overArgs = require("lodash/overArgs");
var factory_1 = require("./factory");
var applicators_1 = require("./applicators");
var decorator = factory_1.DecoratorFactory.createDecorator(new factory_1.DecoratorConfig(overArgs, new applicators_1.PreValueApplicator(), { setter: true }));
function OverArgs() {
    var transforms = [];
    for (var _i = 0; _i < arguments.length; _i++) {
        transforms[_i] = arguments[_i];
    }
    return decorator.apply(void 0, transforms);
}
exports.OverArgs = OverArgs;
exports.overArgs = OverArgs;
exports.default = decorator;
//# sourceMappingURL=overArgs.js.map