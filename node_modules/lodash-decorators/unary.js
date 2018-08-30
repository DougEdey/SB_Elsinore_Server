"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var unary = require("lodash/unary");
var factory_1 = require("./factory");
var applicators_1 = require("./applicators");
var decorator = factory_1.DecoratorFactory.createDecorator(new factory_1.DecoratorConfig(unary, new applicators_1.PreValueApplicator()));
function Unary() {
    return decorator();
}
exports.Unary = Unary;
exports.unary = Unary;
exports.default = decorator;
//# sourceMappingURL=unary.js.map