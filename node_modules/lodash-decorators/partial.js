"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var partial = require("lodash/partial");
var factory_1 = require("./factory");
var applicators_1 = require("./applicators");
var decorator = factory_1.DecoratorFactory.createInstanceDecorator(new factory_1.DecoratorConfig(partial, new applicators_1.PartialApplicator(), { property: true, method: false }));
function Partial() {
    var partials = [];
    for (var _i = 0; _i < arguments.length; _i++) {
        partials[_i] = arguments[_i];
    }
    return decorator.apply(void 0, partials);
}
exports.Partial = Partial;
exports.partial = Partial;
exports.default = decorator;
//# sourceMappingURL=partial.js.map