"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var negate = require("lodash/negate");
var factory_1 = require("./factory");
var applicators_1 = require("./applicators");
var decorator = factory_1.DecoratorFactory.createInstanceDecorator(new factory_1.DecoratorConfig(negate, new applicators_1.PartialValueApplicator(), { property: true }));
function Negate(fn) {
    return decorator(fn);
}
exports.Negate = Negate;
exports.negate = Negate;
exports.default = decorator;
//# sourceMappingURL=negate.js.map