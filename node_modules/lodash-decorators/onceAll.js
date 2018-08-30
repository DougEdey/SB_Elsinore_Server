"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var once = require("lodash/once");
var factory_1 = require("./factory");
var applicators_1 = require("./applicators");
var decorator = factory_1.DecoratorFactory.createDecorator(new factory_1.DecoratorConfig(once, new applicators_1.PreValueApplicator(), { setter: true }));
function OnceAll() {
    return decorator();
}
exports.OnceAll = OnceAll;
exports.onceAll = OnceAll;
exports.default = decorator;
//# sourceMappingURL=onceAll.js.map